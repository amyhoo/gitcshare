package AutoDeploy;

import com.cognos.developer.schemas.bibus._3.*;

import common.CognosLogOn;
import common.ExecutionStep;
import common.CognosOperation;
import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import javax.xml.namespace.QName;

//import com.cognos.developer.schemas.bibus._3.holders.*;
import org.apache.axis.AxisFault;
import org.apache.axis.AxisEngine;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.io.*;
import java.util.*;
import javax.xml.rpc.ServiceException;

public class Deployment extends CognosOperation{
	String archiveName = null;
	String archiveParent = null;
	String deployType = "";// export or import
	String strLocale = "en";
	String[] deployOptionEnumBoolean = { "archiveOverwrite", "dataSourceSelect", "namespaceSelect",
			"namespaceThirdParty", "objectPolicies", "packageHistories", "packageOutputs", "packageSchedules",
			"packageSelect", "recipientsSelect", "takeOwnership" };

	String[] deployOptionEnumResolution = { "dataSourceConflictResolution", "namespaceConflictResolution",
			"objectPoliciesConflictResolution", "ownershipConflictResolution", "packageHistoriesConflictResolution",
			"packageOutputsConflictResolution", "packageSchedulesConflictResolution", "recipientsConflictResolution" };
	HashMap<String, Object> deploySet = new HashMap<String, Object>();
	private static final String DEPLOY_OPTION_NAME = "com.cognos.developer.schemas.bibus._3.DeploymentOptionObjectInformationArray";
	
	public static Map<String, String> ENV_TO_SERVER = new HashMap<String, String>();
	static{
		ENV_TO_SERVER.put("dev", "cogad1");
		ENV_TO_SERVER.put("uat", "cogat1");
		ENV_TO_SERVER.put("prod", "cogap1");
	}
	
	public static String getServerNameByEnv(String env){
		Set<String> keys = ENV_TO_SERVER.keySet();
		Iterator<String> it = keys.iterator();
		while(it.hasNext()){
			String key = it.next();
			if(env.equalsIgnoreCase(key)){
				return ENV_TO_SERVER.get(key);
			}
		}
		return null;
	}

	// connect to server and logon
	public void logon() {
		conn = new CognosLogOn(environment);
		Map<String, Object> axisOption = new HashMap<String, Object>();
		axisOption.put(AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
		cmService = conn.getContentManagerService(axisOption);
		monitorService = conn.getMonitorService(axisOption);
		((Stub) cmService).setTimeout(0); // in milliseconds, 0 turns the timeout off 
		((Stub) monitorService).setTimeout(0); // in milliseconds, 0 turns the timeout off
		conn.logonToCognos();		
	}

	public static void main(String[] args) {
		Deployment current = new Deployment();
		current.importTest();
	} // main

	public void exportTest() {
		environment = "DEV";
		archiveName = "AmyExportTest";
		deployType = "export";
		archiveParent = "/adminFolder/adminFolder[@name='Export']";
		logon();
		initOption();
		String[] folders = { "/content/folder[@name='Home']/folder[@name='CFDR']", "/content/package[@name='Audit']" };
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("/content/folder[@name='Home']/folder[@name='CFDR']", "/content/folder[@name='Amy']");
		map.put("/content/package[@name='Audit']", "/content/folder[@name='Amy']");
		setImportMap(map);
		setExportFolders(folders);
		deployImport(deploySet);
	}

	public void importTest() {
		environment = "DEV";
		archiveName = "AmyImportTest";
		deployType = "import";
		archiveParent = "/adminFolder/adminFolder[@name='Import']";
		logon();
		initOption();
		String[] folders = { "/content/folder[@name='Home']/folder[@name='CFDR']", "/content/package[@name='Audit']" };
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("/content/folder[@name='Amy']/folder[@name='CFDR']", "/content/folder[@name='Amy1']");
		Option[] myDeploymentOptionsEnum = getDeployedOption("AmyExportTest");
		setImportMap(map);
		setExportFolders(folders);
		setArchiveFile("AmyExportTest");
		deployExport(deploySet);
	}

	public void initOption() {
		deploySet.put("archiveOverwrite", false);
		deploySet.put("dataSourceSelect", true);
		deploySet.put("namespaceSelect", true);
		deploySet.put("namespaceThirdParty", false);
		deploySet.put("objectPolicies", true);
		deploySet.put("packageHistories", true);
		deploySet.put("packageOutputs", true);
		deploySet.put("packageSchedules", true);
		deploySet.put("packageSelect", true);
		deploySet.put("recipientsSelect", true);
		deploySet.put("takeOwnership", false);
		deploySet.put("dataSourceConflictResolution", true);
		deploySet.put("namespaceConflictResolution", true);
		deploySet.put("objectPoliciesConflictResolution", true);
		deploySet.put("ownershipConflictResolution", true);
		deploySet.put("packageHistoriesConflictResolution", true);
		deploySet.put("packageOutputsConflictResolution", true);
		deploySet.put("packageSchedulesConflictResolution", true);
		deploySet.put("recipientsConflictResolution", true);
		deploySet.put("deployArchiveName", archiveName);
	}

	public String deployImport(HashMap<String, Object> deployinfo) {
		String strArchiveName = (String) deployinfo.get("deployArchiveName");		
		AsynchReply asynchReply = null;
		String reportEventID = "-1";

		String deployPath;
		SearchPathSingleObject searchPathObject = new SearchPathSingleObject();

		// Add an archive name to the content store
		BaseClass[] ArchiveInfo = addArchive(strArchiveName, archiveParent);

		System.out.println("\nCreated Archive " + strArchiveName);
		if ((ArchiveInfo != null) && (ArchiveInfo.length == 1)) {
			deployPath = ArchiveInfo[0].getSearchPath().getValue();
			searchPathObject.set_value(deployPath);
		} else {
			return reportEventID;
		}

		Option[] myDeploymentOptionsEnum = null;
		myDeploymentOptionsEnum = setImportOptionEnum(deployinfo);
		OptionArrayProp deploymentOptionsArray = new OptionArrayProp();
		deploymentOptionsArray.setValue(myDeploymentOptionsEnum);
		((ImportDeployment) ArchiveInfo[0]).setOptions(deploymentOptionsArray);
		try {
			cmService.update(ArchiveInfo, new UpdateOptions());

			asynchReply = ((MonitorService_PortType) conn.ServiceWithHeader((Stub) monitorService,false)).run(searchPathObject,
					new ParameterValue[] {}, new Option[] {});

			// if it has not yet completed, keep waiting until it is done

			if (asynchReply.getStatus() != AsynchReplyStatusEnum.complete
					|| asynchReply.getStatus() != AsynchReplyStatusEnum.conversationComplete) {
				while (asynchReply.getStatus() != AsynchReplyStatusEnum.complete
						&& asynchReply.getStatus() != AsynchReplyStatusEnum.conversationComplete) {
					if (conn.hasSecondaryRequest(asynchReply, "wait")) {
						System.out.println("waiting...");
						asynchReply = ((MonitorService_PortType) conn.ServiceWithHeader((Stub) monitorService,false))
								.wait(asynchReply.getPrimaryRequest(), new ParameterValue[] {}, new Option[] {});
					} else {
						System.out.println("Error: Wait method not available as expected.");
					}
				}
			}

		} catch (RemoteException remoteEx) {
			System.out.println("An error occurred while deploying content:" + "\n" + remoteEx.getMessage());
			remoteEx.printStackTrace();
		}

		if (asynchReply != null) {
			reportEventID = "Success";
		} else {
			reportEventID = "Failed";
		}
		System.out.println("\nImport Status: " + reportEventID);
		return reportEventID;
	} // deployContent

	public String deployExport(HashMap<String, Object> deployinfo) {
		String strArchiveName = (String) deployinfo.get("deployArchiveName");
		String[] selectedPubContent = (String[]) deployinfo.get("ExportFolders");
		AsynchReply asynchReply = null;
		String reportEventID = "-1";

		String deployPath;
		SearchPathSingleObject searchPathObject = new SearchPathSingleObject();

		// Add an archive name to the content store
		BaseClass[] ArchiveInfo = addArchive(strArchiveName, archiveParent);

		System.out.println("\nCreated Archive " + strArchiveName);
		if ((ArchiveInfo != null) && (ArchiveInfo.length == 1)) {
			deployPath = ArchiveInfo[0].getSearchPath().getValue();
			searchPathObject.set_value(deployPath);
		} else {
			return reportEventID;
		}

		if (selectedPubContent != null) {
			int siz = selectedPubContent.length;
			for (int i = 0; i < siz; i++)
				System.out.println("\nDeploying:  " + selectedPubContent[i]); // non printed for Entire CS
		}

		Option[] myDeploymentOptionsEnum = null;

		if (selectedPubContent != null)
			myDeploymentOptionsEnum = setExportOptionEnum(deployinfo);

		OptionArrayProp deploymentOptionsArray = new OptionArrayProp();
		deploymentOptionsArray.setValue(myDeploymentOptionsEnum);
		((ExportDeployment) ArchiveInfo[0]).setOptions(deploymentOptionsArray);			

		try {
			cmService.update(ArchiveInfo, new UpdateOptions());
			asynchReply = ((MonitorService_PortType) conn.ServiceWithHeader((Stub) monitorService,false)).run(searchPathObject,
					new ParameterValue[] {}, new Option[] {});

			// if it has not yet completed, keep waiting until it is done

			if (asynchReply.getStatus() != AsynchReplyStatusEnum.complete
					|| asynchReply.getStatus() != AsynchReplyStatusEnum.conversationComplete) {
				while (asynchReply.getStatus() != AsynchReplyStatusEnum.complete
						&& asynchReply.getStatus() != AsynchReplyStatusEnum.conversationComplete) {
					if (conn.hasSecondaryRequest(asynchReply, "wait")) {
						System.out.println("waiting...");
						asynchReply = ((MonitorService_PortType) conn.ServiceWithHeader((Stub) monitorService,false))
								.wait(asynchReply.getPrimaryRequest(), new ParameterValue[] {}, new Option[] {});
					} else {
						System.out.println("Error: Wait method not available as expected.");
					}
				}
			}

		} catch (RemoteException remoteEx) {
			System.out.println("An error occurred while deploying content:" + "\n" + remoteEx.getMessage());
			remoteEx.printStackTrace();
		}

		if (asynchReply != null) {
			reportEventID = "Success";
		} else {
			reportEventID = "Failed";
		}
		System.out.println("\nExport Status: " + reportEventID);
		return reportEventID;
	} // deployContent
	
	public void setArchiveFile(String filename) {
		// set the archive file that should be imported
		deploySet.put("deployArchiveFile", filename);
	}

	public void setImportMap(HashMap<String, String> folder2parent) {
		// set import
		deploySet.put("ImportMap", folder2parent);
	}

	public void setExportFolders(String[] folders) {
		// set export
		deploySet.put("ExportFolders", folders);
	}

	// get content store object
	/**
	 * use this method to get a object in the content store
	 **/
	public BaseClass[] getCSObject(String myPathStr) { // get CM object
		SearchPathMultipleObject cmSearchPath = new SearchPathMultipleObject(myPathStr);
		BaseClass[] myCMObject = null;

		PropEnum props[] = new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName, PropEnum.storeID,
				PropEnum.parent };

		Sort sortOptions[] = { new Sort() };
		sortOptions[0].setOrder(OrderEnum.ascending);
		sortOptions[0].setPropName(PropEnum.defaultName);

		try {
			myCMObject = cmService.query(cmSearchPath, props, sortOptions, new QueryOptions());
		} catch (RemoteException remoteEx) {
			System.out.println("An error occurred while querying CM object:" + "\n" + remoteEx.getMessage());
		}
		return myCMObject;
	}// getCSObject
	
	public String getParentPath(String searchPath){
		int position = searchPath.lastIndexOf("/");
		return searchPath.substring(0,position);
		
	}
	/**
	 * use this method get archive information by name
	 */
	// sn_dg_sdk_method_contentManagerService_getDeploymentOptions_start_0
	public Option[] getDeployedOption(String archiveName) {
		Option[] deployOptEnum = new Option[] {};

		try {
			// deployOptEnum = cmService.getDeploymentOptions( myArchive, opt);
			deployOptEnum = cmService.getDeploymentOptions(archiveName, new Option[] {});
		} catch (RemoteException e) {
			System.out.println(
					"An error occurred in getting Deployment options." + "\n" + "The error: " + e.getMessage());
			e.printStackTrace();
		}

		return deployOptEnum;
	} // getDeployedOption

	/**
	 * use this method to return all the public folder content associated with
	 * one specific archive
	 */
	// sn_dg_sdk_method_contentManagerService_getDeploymentOptions_start_0
	public HashMap getPubFolderContent(String archiveName) {
		Option[] deployOptEnum = new Option[] {};
		HashMap arrOfPublicFolder = new HashMap();

		try {

			deployOptEnum = cmService.getDeploymentOptions(archiveName, new Option[] {});

			// sn_dg_sdk_method_contentManagerService_getDeploymentOptions_end_0
			for (int i = 0; i < deployOptEnum.length; i++) {
				if (deployOptEnum[i].getClass().getName() == DEPLOY_OPTION_NAME) {
					DeploymentObjectInformation[] packDeployInfo = ((DeploymentOptionObjectInformationArray) deployOptEnum[i])
							.getValue();
					int packLen = packDeployInfo.length;

					for (int j = 0; j < packLen; j++) {
						String packFolderName = packDeployInfo[j].getDefaultName();
						SearchPathSingleObject packagePath = packDeployInfo[j].getSearchPath();
						arrOfPublicFolder.put(packFolderName, packagePath);
					}
				}
			}
		} catch (RemoteException e) {
			System.out.println(
					"An error occurred in getting Deployment options." + "\n" + "The error: " + e.getMessage());
			e.printStackTrace();
		}
		return arrOfPublicFolder;
	} // getPubFolderContent

	// add object in the content store
	/**
	 * use this method to add a Deployment object to the content store
	 **/
	private BaseClass[] addArchive(String nameOfArchive, String adminPath) {
		// "/adminFolder/adminFolder[@name='Export']"
		ImportDeployment importDeploy = null;
		ExportDeployment exportDeploy = null;
		BaseClass[] addedDeploymentObjects = null;
		BaseClass[] bca = new BaseClass[1];
		AddOptions addOpts = null;

		SearchPathSingleObject objOfSearchPath = new SearchPathSingleObject(adminPath);

		MultilingualTokenProp multilingualTokenProperty = new MultilingualTokenProp();
		MultilingualToken[] multilingualTokenArr = new MultilingualToken[1];
		MultilingualToken myMultilingualToken = new MultilingualToken();

		myMultilingualToken.setLocale(strLocale);
		myMultilingualToken.setValue(nameOfArchive);
		multilingualTokenArr[0] = myMultilingualToken;
		multilingualTokenProperty.setValue(multilingualTokenArr);

		if (deployType.equalsIgnoreCase("import")) {
			importDeploy = new ImportDeployment();
			addOpts = new AddOptions();
			importDeploy.setName(multilingualTokenProperty);
			addOpts.setUpdateAction(UpdateActionEnum.replace);
			bca[0] = importDeploy;
		} else {
			exportDeploy = new ExportDeployment();
			addOpts = new AddOptions();
			exportDeploy.setName(multilingualTokenProperty);
			addOpts.setUpdateAction(UpdateActionEnum.replace);
			bca[0] = exportDeploy;
		}

		try {
			addedDeploymentObjects = cmService.add(objOfSearchPath, bca, addOpts);
		} catch (RemoteException remoEx) {
			System.out.println("An error occurred when adding a deployment object:" + "\n" + remoEx.getMessage());
		}
		if ((addedDeploymentObjects != null) && (addedDeploymentObjects.length > 0)) {
			return addedDeploymentObjects;
		} else {
			return null;
		}
	} // addArchive

	// set export deployment option property (mandatory)
	private DeploymentOptionSearchPathSingleObjectArray setExportDeploymentOptionPackageInfo(String[] arrOfFolders) {
		SearchPathSingleObject[] exportPkgDeployInfoArr = new SearchPathSingleObject[arrOfFolders.length];
		SearchPathSingleObject exportPkgDeployInfo;
		String packSearchPath = null;

		for (int i = 0; i < arrOfFolders.length; i++) {
			exportPkgDeployInfo = new SearchPathSingleObject();

			String myPackageName = arrOfFolders[i];
			packSearchPath = myPackageName;
			packSearchPath = "storeID('" + getCSObject(packSearchPath)[0].getStoreID().getValue().toString() + "')";
			;

			exportPkgDeployInfo.set_value(packSearchPath);
			exportPkgDeployInfoArr[i] = exportPkgDeployInfo;
		}
		DeploymentOptionSearchPathSingleObjectArray exportDeployOptionPkgInfo = new DeploymentOptionSearchPathSingleObjectArray();
		exportDeployOptionPkgInfo.setName(DeploymentOptionEnum.fromString("export"));
		exportDeployOptionPkgInfo.setValue(exportPkgDeployInfoArr);

		return exportDeployOptionPkgInfo;
	}// setExportDeploymentOptionPackageInfo

	// set import deployment option property (mandatory)
	private DeploymentOptionImportRuleArray setImportDeploymentOptionPackageInfo(
			HashMap<String, String> folderAndParent) {
		DeploymentImportRule[] pkgDeployInfoArr = new DeploymentImportRule[folderAndParent.size()];
		DeploymentImportRule pkgDeployInfo;
		MultilingualToken[] multilingualTokenArr;
		MultilingualToken multilingualToken;
		SearchPathSingleObject packSearchPath = null;
		BaseClass objToExport = null;
		System.out.println("Length of folders array = <" + folderAndParent.size() + ">");
		Integer i = 0;
		for (String key : folderAndParent.keySet()) { // int i = 0; i <
														// arrOfFolders.length;
														// i++
			objToExport = getCSObject(key)[0];
			multilingualToken = new MultilingualToken();
			multilingualTokenArr = new MultilingualToken[1];

			pkgDeployInfo = new DeploymentImportRule();
			multilingualToken.setLocale(strLocale);
			multilingualToken.setValue(objToExport.getDefaultName().getValue());
			multilingualTokenArr[0] = multilingualToken;
			packSearchPath = new SearchPathSingleObject(key);

			packSearchPath = new SearchPathSingleObject(
					"storeID('" + objToExport.getStoreID().getValue().toString() + "')");
			pkgDeployInfo.setArchiveSearchPath(packSearchPath);
			pkgDeployInfo.setName(multilingualTokenArr);
			pkgDeployInfo.setParent(new SearchPathSingleObject(folderAndParent.get(key)));
			pkgDeployInfoArr[i] = pkgDeployInfo;
			i++;
		}

		DeploymentOptionImportRuleArray deployOptionPkgInfo = new DeploymentOptionImportRuleArray();
		deployOptionPkgInfo.setName(DeploymentOptionEnum.fromString("import"));
		deployOptionPkgInfo.setValue(pkgDeployInfoArr);

		return deployOptionPkgInfo;
	}// setImportDeploymentOptionPackageInfo

	private Option[] setImportOptionEnum(Map optionSet) {
		Option[] deploymentOptions = getDeployedOption((String) optionSet.get("deployArchiveFile"));
		return deploymentOptions;
	}

	private Option[] setExportOptionEnum(Map optionSet) {
		// optionSet include information of deployment like String
		// nameOfArchive,String[] listOfSelectedFolders,
		Option[] deploymentOptions = null;
		int num = 0;
		int eOptionCount = 4;

		deploymentOptions = new DeploymentOption[eOptionCount + deployOptionEnumBoolean.length
				+ deployOptionEnumResolution.length];

		deploymentOptions[num] = this.setImportDeploymentOptionPackageInfo((HashMap) optionSet.get("ImportMap"));
		deploymentOptions[++num] = this.setExportDeploymentOptionPackageInfo((String[]) optionSet.get("ExportFolders"));

		deploymentOptions[++num] = this.setDeploymentName((String) optionSet.get("deployArchiveName"));
		deploymentOptions[++num] = this.setDeploymentOptionString((String) optionSet.get("deployArchiveName"));
		deploymentOptions[++num] = this.setArchiveOverWrite((boolean) optionSet.get("archiveOverwrite"));
		deploymentOptions[++num] = this.setDataSourceSelect((boolean) optionSet.get("dataSourceSelect"));
		deploymentOptions[++num] = this.setNameSpaceSelect((boolean) optionSet.get("namespaceSelect"));
		deploymentOptions[++num] = this.setNameSpaceThirdParty((boolean) optionSet.get("namespaceThirdParty"));
		deploymentOptions[++num] = this.setObjectPolicies((boolean) optionSet.get("objectPolicies"));
		deploymentOptions[++num] = this.setPackageHistories((boolean) optionSet.get("packageHistories"));
		deploymentOptions[++num] = this.setPackageOutputs((boolean) optionSet.get("packageOutputs"));
		deploymentOptions[++num] = this.setPackageSchedules((boolean) optionSet.get("packageSchedules"));

		deploymentOptions[++num] = this.setPackageSelect((boolean) optionSet.get("packageSelect"));
		deploymentOptions[++num] = this.setRecipientsSelect((boolean) optionSet.get("recipientsSelect"));
		deploymentOptions[++num] = this.setTakeOwnership((boolean) optionSet.get("takeOwnership"));
		deploymentOptions[++num] = this
				.setDataSourceConflictResolution((boolean) optionSet.get("dataSourceConflictResolution"));
		deploymentOptions[++num] = this
				.setNamespaceConflictResolution((boolean) optionSet.get("namespaceConflictResolution"));
		deploymentOptions[++num] = this
				.setObjectPoliciesConflictResolution((boolean) optionSet.get("objectPoliciesConflictResolution"));
		deploymentOptions[++num] = this
				.setOwnershipConflictResolution((boolean) optionSet.get("ownershipConflictResolution"));
		deploymentOptions[++num] = this
				.setPackageHistoriesConflictResolution((boolean) optionSet.get("packageHistoriesConflictResolution"));
		deploymentOptions[++num] = this
				.setPackageOutputsConflictResolution((boolean) optionSet.get("packageOutputsConflictResolution"));
		deploymentOptions[++num] = this
				.setPackageSchedulesConflictResolution((boolean) optionSet.get("packageSchedulesConflictResolution"));
		deploymentOptions[++num] = this
				.setRecipientsConflictResolution((boolean) optionSet.get("recipientsConflictResolution"));

		return deploymentOptions;
	}

	/**
	 * use this method to define the deployment options
	 */
	private Option[] setDeploymentOptionEnum(String deploymentType, Map optionSet) {
		// optionSet include information of deployment like String
		// nameOfArchive,String[] listOfSelectedFolders,
		Option[] deploymentOptions = null;
		int num = 0;
		int eOptionCount = 0;

		if (deploymentType.equalsIgnoreCase("import")) {
			eOptionCount = 3;
		} else {
			eOptionCount = 4;
		}

		deploymentOptions = new DeploymentOption[eOptionCount + deployOptionEnumBoolean.length
				+ deployOptionEnumResolution.length];

		// Define the deployment options
		if (deploymentType.equalsIgnoreCase("import")) {
			deploymentOptions[num] = this.setImportDeploymentOptionPackageInfo((HashMap) optionSet.get("ImportMap"));
		}

		if (deploymentType.equalsIgnoreCase("export")) {
			deploymentOptions[num] = this.setImportDeploymentOptionPackageInfo((HashMap) optionSet.get("ImportMap"));
			deploymentOptions[++num] = this
					.setExportDeploymentOptionPackageInfo((String[]) optionSet.get("ExportFolders"));
		}

		deploymentOptions[++num] = this.setDeploymentName((String) optionSet.get("deployArchiveName"));
		deploymentOptions[++num] = this.setDeploymentOptionString((String) optionSet.get("deployArchiveName"));
		deploymentOptions[++num] = this.setArchiveOverWrite((boolean) optionSet.get("archiveOverwrite"));
		deploymentOptions[++num] = this.setDataSourceSelect((boolean) optionSet.get("dataSourceSelect"));
		deploymentOptions[++num] = this.setNameSpaceSelect((boolean) optionSet.get("namespaceSelect"));
		deploymentOptions[++num] = this.setNameSpaceThirdParty((boolean) optionSet.get("namespaceThirdParty"));
		deploymentOptions[++num] = this.setObjectPolicies((boolean) optionSet.get("objectPolicies"));
		deploymentOptions[++num] = this.setPackageHistories((boolean) optionSet.get("packageHistories"));
		deploymentOptions[++num] = this.setPackageOutputs((boolean) optionSet.get("packageOutputs"));
		deploymentOptions[++num] = this.setPackageSchedules((boolean) optionSet.get("packageSchedules"));

		deploymentOptions[++num] = this.setPackageSelect((boolean) optionSet.get("packageSelect"));
		deploymentOptions[++num] = this.setRecipientsSelect((boolean) optionSet.get("recipientsSelect"));
		deploymentOptions[++num] = this.setTakeOwnership((boolean) optionSet.get("takeOwnership"));
		deploymentOptions[++num] = this
				.setDataSourceConflictResolution((boolean) optionSet.get("dataSourceConflictResolution"));
		deploymentOptions[++num] = this
				.setNamespaceConflictResolution((boolean) optionSet.get("namespaceConflictResolution"));
		deploymentOptions[++num] = this
				.setObjectPoliciesConflictResolution((boolean) optionSet.get("objectPoliciesConflictResolution"));
		deploymentOptions[++num] = this
				.setOwnershipConflictResolution((boolean) optionSet.get("ownershipConflictResolution"));
		deploymentOptions[++num] = this
				.setPackageHistoriesConflictResolution((boolean) optionSet.get("packageHistoriesConflictResolution"));
		deploymentOptions[++num] = this
				.setPackageOutputsConflictResolution((boolean) optionSet.get("packageOutputsConflictResolution"));
		deploymentOptions[++num] = this
				.setPackageSchedulesConflictResolution((boolean) optionSet.get("packageSchedulesConflictResolution"));
		deploymentOptions[++num] = this
				.setRecipientsConflictResolution((boolean) optionSet.get("recipientsConflictResolution"));

		return deploymentOptions;
	} // setDeploymentOptionEnum

	// set DeploymentOptionString property (mandatory)
	private DeploymentOptionString setDeploymentOptionString(String archiveName) {
		MultilingualString archiveDefault = new MultilingualString();
		archiveDefault.setLocale(strLocale);
		archiveDefault.setValue(archiveName);

		DeploymentOptionString deployOptionStr = new DeploymentOptionString();
		deployOptionStr.setName(DeploymentOptionEnum.fromString("archive"));
		deployOptionStr.setValue(archiveDefault.getValue());

		return deployOptionStr;
	}

	// set the default deployment name
	private DeploymentOptionMultilingualString setDeploymentName(String archiveName) {
		MultilingualString deploymentName = new MultilingualString();
		deploymentName.setLocale("en-us");
		deploymentName.setValue(archiveName);

		DeploymentOptionMultilingualString dName = new DeploymentOptionMultilingualString();
		dName.setName(DeploymentOptionEnum.fromString("deploymentName"));
		dName.setValue(new MultilingualString[] { deploymentName });
		return dName;
	}

	// allow the deployment overwrites the archive
	private DeploymentOptionBoolean setArchiveOverWrite(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("archiveOverwrite"));
		if (setValue) {
			deployOptionBool.setValue(false);
		} else {
			deployOptionBool.setValue(true);
		}
		return deployOptionBool;
	}

	// set dataSourceSelect as default value - 'false'
	private DeploymentOptionBoolean setDataSourceSelect(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("dataSourceSelect"));
		if (setValue) {
			deployOptionBool.setValue(false);
		} else {
			deployOptionBool.setValue(true);
		}
		return deployOptionBool;
	}

	// set namespaceSelect as default value - 'false'
	private DeploymentOptionBoolean setNameSpaceSelect(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("namespaceSelect"));
		if (setValue) {
			deployOptionBool.setValue(false);
		} else {
			deployOptionBool.setValue(true);
		}
		return deployOptionBool;
	}

	// Not include references to external namespaces - value is false
	private DeploymentOptionBoolean setNameSpaceThirdParty(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("namespaceThirdParty"));
		if (setValue) {
			deployOptionBool.setValue(true);
		} else {
			deployOptionBool.setValue(false);
		}
		return deployOptionBool;
	}

	// set objectPolicies as default value - 'false'
	private DeploymentOptionBoolean setObjectPolicies(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("objectPolicies"));
		if (setValue) {
			deployOptionBool.setValue(false);
		} else {
			deployOptionBool.setValue(true);
		}
		return deployOptionBool;
	}

	// set packageHistories as default value - 'false'
	private DeploymentOptionBoolean setPackageHistories(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("packageHistories"));
		if (setValue) {
			deployOptionBool.setValue(false);
		} else {
			deployOptionBool.setValue(true);
		}
		return deployOptionBool;
	}

	// set packageOutputs as default value - 'false'
	private DeploymentOptionBoolean setPackageOutputs(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("packageOutputs"));
		if (setValue) {
			deployOptionBool.setValue(false);
		} else {
			deployOptionBool.setValue(true);
		}
		return deployOptionBool;
	}

	// set packageSchedules as default value - 'false'
	private DeploymentOptionBoolean setPackageSchedules(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("packageSchedules"));
		if (setValue) {
			deployOptionBool.setValue(false);
		} else {
			deployOptionBool.setValue(true);
		}
		return deployOptionBool;
	}

	// set packageSelect as default value - 'true'
	private DeploymentOptionBoolean setPackageSelect(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("packageSelect"));
		if (setValue) {
			deployOptionBool.setValue(true);
		} else {
			deployOptionBool.setValue(false);
		}
		return deployOptionBool;
	}

	// set recipientsSelect as default value - 'false'
	private DeploymentOptionBoolean setRecipientsSelect(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("recipientsSelect"));
		if (setValue) {
			deployOptionBool.setValue(false);
		} else {
			deployOptionBool.setValue(true);
		}
		return deployOptionBool;
	}

	// set the owner to the owner from the source - the value is 'true'
	private DeploymentOptionBoolean setTakeOwnership(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("takeOwnership"));
		if (setValue) {
			deployOptionBool.setValue(false);
		} else {
			deployOptionBool.setValue(true);
		}
		return deployOptionBool;
	}

	// set dataSourceConflictResolution as default value - 'replace'
	private DeploymentOptionResolution setDataSourceConflictResolution(boolean setValue) {
		DeploymentOptionResolution deployOptionResolute = new DeploymentOptionResolution();
		deployOptionResolute.setName(DeploymentOptionEnum.fromString("dataSourceConflictResolution"));
		if (setValue) {
			deployOptionResolute.setValue(ConflictResolutionEnum.replace);

		} else {
			deployOptionResolute.setValue(ConflictResolutionEnum.keep);
		}
		return deployOptionResolute;
	}

	// set namespaceConflictResolution as default value - 'replace'
	private DeploymentOptionResolution setNamespaceConflictResolution(boolean setValue) {
		DeploymentOptionResolution deployOptionResolute = new DeploymentOptionResolution();
		deployOptionResolute.setName(DeploymentOptionEnum.fromString("namespaceConflictResolution"));
		if (setValue) {
			deployOptionResolute.setValue(ConflictResolutionEnum.replace);

		} else {
			deployOptionResolute.setValue(ConflictResolutionEnum.keep);
		}
		return deployOptionResolute;
	}

	// set objectPoliciesConflictResolution as default value - 'replace'
	private DeploymentOptionResolution setObjectPoliciesConflictResolution(boolean setValue) {
		DeploymentOptionResolution deployOptionResolute = new DeploymentOptionResolution();
		deployOptionResolute.setName(DeploymentOptionEnum.fromString("objectPoliciesConflictResolution"));
		if (setValue) {
			deployOptionResolute.setValue(ConflictResolutionEnum.replace);

		} else {
			deployOptionResolute.setValue(ConflictResolutionEnum.keep);
		}
		return deployOptionResolute;
	}

	// set ownershipConflictResolution as default value - 'replace'
	private DeploymentOptionResolution setOwnershipConflictResolution(boolean setValue) {
		DeploymentOptionResolution deployOptionResolute = new DeploymentOptionResolution();
		deployOptionResolute.setName(DeploymentOptionEnum.fromString("ownershipConflictResolution"));
		if (setValue) {
			deployOptionResolute.setValue(ConflictResolutionEnum.replace);

		} else {
			deployOptionResolute.setValue(ConflictResolutionEnum.keep);
		}
		return deployOptionResolute;
	}

	// set packageHistoriesConflictResolution as default value - 'replace'
	private DeploymentOptionResolution setPackageHistoriesConflictResolution(boolean setValue) {
		DeploymentOptionResolution deployOptionResolute = new DeploymentOptionResolution();
		deployOptionResolute.setName(DeploymentOptionEnum.fromString("packageHistoriesConflictResolution"));
		if (setValue) {
			deployOptionResolute.setValue(ConflictResolutionEnum.replace);

		} else {
			deployOptionResolute.setValue(ConflictResolutionEnum.keep);
		}
		return deployOptionResolute;
	}

	// set packageOutputsConflictResolution as default value - 'replace'
	private DeploymentOptionResolution setPackageOutputsConflictResolution(boolean setValue) {
		DeploymentOptionResolution deployOptionResolute = new DeploymentOptionResolution();
		deployOptionResolute.setName(DeploymentOptionEnum.fromString("packageOutputsConflictResolution"));
		if (setValue) {
			deployOptionResolute.setValue(ConflictResolutionEnum.replace);

		} else {
			deployOptionResolute.setValue(ConflictResolutionEnum.keep);
		}
		return deployOptionResolute;
	}

	// set packageSchedulesConflictResolution as default value - 'replace'
	private DeploymentOptionResolution setPackageSchedulesConflictResolution(boolean setValue) {
		DeploymentOptionResolution deployOptionResolute = new DeploymentOptionResolution();
		deployOptionResolute.setName(DeploymentOptionEnum.fromString("packageSchedulesConflictResolution"));
		if (setValue) {
			deployOptionResolute.setValue(ConflictResolutionEnum.replace);

		} else {
			deployOptionResolute.setValue(ConflictResolutionEnum.keep);
		}
		return deployOptionResolute;
	}

	// set recipientsConflictResolution as default value - 'replace'
	private DeploymentOptionResolution setRecipientsConflictResolution(boolean setValue) {
		DeploymentOptionResolution deployOptionResolute = new DeploymentOptionResolution();
		deployOptionResolute.setName(DeploymentOptionEnum.fromString("recipientsConflictResolution"));
		if (setValue) {
			deployOptionResolute.setValue(ConflictResolutionEnum.replace);

		} else {
			deployOptionResolute.setValue(ConflictResolutionEnum.keep);
		}
		return deployOptionResolute;
	}
}
