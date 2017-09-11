/* 
 * Licensed Materials - Property of IBM
 * IBM Cognos Products: SDK Support
 * (C) Copyright IBM Corp. 2003, 2016
 * US Government Users Restricted Rights - Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
*/
/*
 * Export_Partial
 * Description: Technote 1338960 SDK Sample to export a folder
 * Tested with:  IBM Cognos BI 10.2.1 FP1, 10.2.2, IBM Java 1.7, Axis 1.4
*/

//This sample exports partial content store -
//the user specifies the list of folders to export

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.Calendar;

import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisEngine;
import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;

import com.cognos.developer.schemas.bibus._3.AddOptions;
import com.cognos.developer.schemas.bibus._3.AsynchReply;
import com.cognos.developer.schemas.bibus._3.AsynchReplyStatusEnum;
import com.cognos.developer.schemas.bibus._3.AsynchSecondaryRequest;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BiBusHeader;
import com.cognos.developer.schemas.bibus._3.ConflictResolutionEnum;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.DeploymentImportRule;
import com.cognos.developer.schemas.bibus._3.DeploymentOption;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionBoolean;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionEnum;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionImportRuleArray;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionMultilingualString;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionResolution;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionSearchPathSingleObjectArray;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionString;
import com.cognos.developer.schemas.bibus._3.DispatcherTransportVar;
import com.cognos.developer.schemas.bibus._3.ExportDeployment;
import com.cognos.developer.schemas.bibus._3.ImportDeployment;
import com.cognos.developer.schemas.bibus._3.MonitorService_PortType;
import com.cognos.developer.schemas.bibus._3.MonitorService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.MultilingualString;
import com.cognos.developer.schemas.bibus._3.MultilingualToken;
import com.cognos.developer.schemas.bibus._3.MultilingualTokenProp;
import com.cognos.developer.schemas.bibus._3.Option;
import com.cognos.developer.schemas.bibus._3.OptionArrayProp;
import com.cognos.developer.schemas.bibus._3.OrderEnum;
import com.cognos.developer.schemas.bibus._3.ParameterValue;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.SearchPathSingleObject;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.cognos.developer.schemas.bibus._3.UpdateActionEnum;
import com.cognos.developer.schemas.bibus._3.UpdateOptions;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;

public class Export_Partial 
{
	    private ContentManagerService_PortType cmService = null;
	    private MonitorService_PortType mService = null;
	    
		String BUS_NS = "http://developer.cognos.com/schemas/bibus/3/";
		String BUS_HEADER = "biBusHeader";

	    private String [] folders = {"/content/package[@name='GO Data Warehouse (analysis)']", "/content/folder[@name='Samples_Prompt_API']"};
	    		
	    private String deployType = "export";
		String strLocale = "en";
			
	
		
		public static void main(String[] args) 
	   	{
			Export_Partial exp = new Export_Partial();
 		
	   		String expDeploymentSpec = "SDKfoldersAndPackagesExport";

	   		String gateway = "http://localhost:9300/p2pd/servlet/dispatch";
			String nameSpace = null;
			String userID = null;
			String pass = null;
	   		
		   exp.connectToReportServer(gateway);  
 		  
		   if (nameSpace != null && userID != null && pass != null)   //Don't try to login if using Anonymous
			   exp.quickLogon(nameSpace, userID, pass);
		   else
			   System.out.println("Missing logon information. Attempting to login as Anonymous user...");

			
		   //If no name for the archive is provided, set it to the current date
		   if (expDeploymentSpec == "" || expDeploymentSpec == null)
		   {	
			   Calendar currentDate = Calendar.getInstance();
			   expDeploymentSpec = currentDate.getTime().toString();
			   expDeploymentSpec = expDeploymentSpec.replace(':','.');    //replace(':', '.') The zip file cannot contain :  
		   }
		   
		 	if (expDeploymentSpec != null)
		 	{
		 		exp.deployContent(expDeploymentSpec, exp.folders );
		 	}
		 	else
		 		System.out.println("Invalid Archive Name Provided.");
		 	
		 	
	    } //main
		

		public BaseClass[] getCSObject( String myPathStr) {     //get CM object
			SearchPathMultipleObject cmSearchPath = new SearchPathMultipleObject(myPathStr);
			BaseClass[] myCMObject = null;

			PropEnum props[] = new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName, PropEnum.storeID, PropEnum.parent };
			Sort sortOptions[] = { new Sort() };
			sortOptions[0].setOrder(OrderEnum.ascending);
			sortOptions[0].setPropName(PropEnum.defaultName);

			try {
				myCMObject = cmService.query(cmSearchPath, props,
						sortOptions, new QueryOptions());
			} catch (RemoteException remoteEx) {
				System.out.println("An error occurred while querying CM object:"
								+ "\n" + remoteEx.getMessage());
			}
			return myCMObject;
		}//getCSObject
		

	
		/**
		 * use this method to add a Deployment object to the content store
		 */
		private BaseClass[] addArchive(String deploySpec, String nameOfArchive)
		{

			ImportDeployment importDeploy = null;
			ExportDeployment exportDeploy = null;
			BaseClass[] addedDeploymentObjects = null;
			BaseClass[] bca = new BaseClass[1];
			AddOptions addOpts = null;

			SearchPathSingleObject objOfSearchPath = new SearchPathSingleObject("/adminFolder");
			
			MultilingualTokenProp multilingualTokenProperty = new MultilingualTokenProp();
			MultilingualToken[] multilingualTokenArr = new MultilingualToken[1];
			MultilingualToken myMultilingualToken = new MultilingualToken();

			myMultilingualToken.setLocale(strLocale);
			myMultilingualToken.setValue(nameOfArchive);
			multilingualTokenArr[0] = myMultilingualToken;
			multilingualTokenProperty.setValue(multilingualTokenArr);

			if (deploySpec.equalsIgnoreCase("import")) {
				importDeploy = new ImportDeployment();
				addOpts = new AddOptions();
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
				addedDeploymentObjects = cmService.add(objOfSearchPath,
						bca, addOpts);
			} catch (RemoteException remoEx) {
				System.out.println("An error occurred when adding a deployment object:"
								+ "\n" + remoEx.getMessage());
			}
			if ((addedDeploymentObjects != null)
					&& (addedDeploymentObjects.length > 0)) {
				return addedDeploymentObjects;
			} else {
				return null;
			}
		} //addArchive

		
		// set export deployment option property (mandatory)
		private DeploymentOptionSearchPathSingleObjectArray setExportDeploymentOptionPackageInfo(String[] arrOfFolders)
		{
			SearchPathSingleObject[] exportPkgDeployInfoArr = new SearchPathSingleObject[arrOfFolders.length];
			SearchPathSingleObject exportPkgDeployInfo;
			String packSearchPath=null;
			
			for (int i = 0; i < arrOfFolders.length; i++) {
				exportPkgDeployInfo = new SearchPathSingleObject();

				String myPackageName=arrOfFolders[i];
				packSearchPath= myPackageName;
				packSearchPath = "storeID('" + getCSObject(packSearchPath)[0].getStoreID().getValue().toString() + "')";;

				exportPkgDeployInfo.set_value(packSearchPath);
				exportPkgDeployInfoArr[i] = exportPkgDeployInfo;
			}
			DeploymentOptionSearchPathSingleObjectArray exportDeployOptionPkgInfo = new DeploymentOptionSearchPathSingleObjectArray();
			exportDeployOptionPkgInfo.setName(DeploymentOptionEnum.fromString("export"));
			exportDeployOptionPkgInfo.setValue(exportPkgDeployInfoArr);

			return exportDeployOptionPkgInfo;
		}//setExportDeploymentOptionPackageInfo
	   	
		
		// set import deployment option property (mandatory)
		private DeploymentOptionImportRuleArray setImportDeploymentOptionPackageInfo(String[] arrOfFolders) {
			DeploymentImportRule[] pkgDeployInfoArr = new DeploymentImportRule[arrOfFolders.length];
			DeploymentImportRule pkgDeployInfo;
			MultilingualToken[] multilingualTokenArr;
			MultilingualToken multilingualToken;
			SearchPathSingleObject packSearchPath=null;
			BaseClass objToExport = null;
			System.out.println("Length of folders array = <" + arrOfFolders.length + ">");
			for (int i = 0; i < arrOfFolders.length; i++) {
				objToExport = getCSObject(arrOfFolders[i])[0];
				multilingualToken = new MultilingualToken();
				multilingualTokenArr = new MultilingualToken[1];

				pkgDeployInfo = new DeploymentImportRule();
				multilingualToken.setLocale(strLocale);
				multilingualToken.setValue(objToExport.getDefaultName().getValue());
				multilingualTokenArr[0] = multilingualToken;
				packSearchPath= new SearchPathSingleObject(arrOfFolders[i]);

				packSearchPath = new SearchPathSingleObject("storeID('" + objToExport.getStoreID().getValue().toString() + "')");
			    pkgDeployInfo.setArchiveSearchPath(packSearchPath);
				pkgDeployInfo.setName(multilingualTokenArr);
				pkgDeployInfo.setParent(new SearchPathSingleObject(objToExport.getParent().getValue()[0].getSearchPath().getValue().toString()));
				pkgDeployInfoArr[i] = pkgDeployInfo;
			}

			DeploymentOptionImportRuleArray deployOptionPkgInfo = new DeploymentOptionImportRuleArray();
			deployOptionPkgInfo.setName(DeploymentOptionEnum.fromString("import"));
			deployOptionPkgInfo.setValue(pkgDeployInfoArr);

			return deployOptionPkgInfo;
		}//setImportDeploymentOptionPackageInfo

	
	
		/**
		 * use this method to define the deployment options
		 */
		private Option[] setDeploymentOptionEnum(String deploymentType, String nameOfArchive,
				String[] listOfSelectedFolders)
		{
			Option[] deploymentOptions = null;
			int num = 0;
			int eOptionCount=0;
			

			String[] deployOptionEnumBoolean = { "archiveOverwrite",
					"dataSourceSelect", "namespaceSelect", "namespaceThirdParty",
					"objectPolicies", "packageHistories", "packageOutputs",
					"packageSchedules", "packageSelect", "recipientsSelect",
					"takeOwnership" };

			String[] deployOptionEnumResolution = { "dataSourceConflictResolution",
					"namespaceConflictResolution",
					"objectPoliciesConflictResolution",
					"ownershipConflictResolution",
					"packageHistoriesConflictResolution",
					"packageOutputsConflictResolution",
					"packageSchedulesConflictResolution",
					"recipientsConflictResolution" };

			if(deploymentType.equalsIgnoreCase("import"))
			{
				eOptionCount=3;
			}
			else
			{
				eOptionCount=4;
			}

			deploymentOptions = new DeploymentOption[ eOptionCount +
					 deployOptionEnumBoolean.length  +
					 deployOptionEnumResolution.length];

			// Define the deployment options
			if(deploymentType.equalsIgnoreCase("import"))
			{
				deploymentOptions[num] = this.setImportDeploymentOptionPackageInfo(listOfSelectedFolders);
			}
			
			if (deploymentType.equalsIgnoreCase("export"))
			{
				deploymentOptions[num] = this.setImportDeploymentOptionPackageInfo(listOfSelectedFolders);
				deploymentOptions[++num] = this.setExportDeploymentOptionPackageInfo(listOfSelectedFolders);
			}
			   		
	   		deploymentOptions[++num] = this.setDeploymentName(nameOfArchive);
			deploymentOptions[++num] = this.setDeploymentOptionString(nameOfArchive);
			deploymentOptions[++num] = this.setArchiveOverWrite(false);
			deploymentOptions[++num] = this.setDataSourceSelect(true);
			deploymentOptions[++num] = this.setNameSpaceSelect(true);
			deploymentOptions[++num] = this.setNameSpaceThirdParty(false);
			deploymentOptions[++num] = this.setObjectPolicies(true);
			deploymentOptions[++num] = this.setPackageHistories(true);
			deploymentOptions[++num] = this.setPackageOutputs(true);
			deploymentOptions[++num] = this.setPackageSchedules(true);
			deploymentOptions[++num] = this.setPackageSelect(true);
			deploymentOptions[++num] = this.setRecipientsSelect(true);
			deploymentOptions[++num] = this.setTakeOwnership(false);
			deploymentOptions[++num] = this.setDataSourceConflictResolution(true);
			deploymentOptions[++num] = this.setNamespaceConflictResolution(true);
			deploymentOptions[++num] = this.setObjectPoliciesConflictResolution(true);
			deploymentOptions[++num] = this.setOwnershipConflictResolution(true);
			deploymentOptions[++num] = this.setPackageHistoriesConflictResolution(true);
			deploymentOptions[++num] = this.setPackageOutputsConflictResolution(true);
			deploymentOptions[++num] = this.setPackageSchedulesConflictResolution(true);
			deploymentOptions[++num] = this.setRecipientsConflictResolution(true);

			return deploymentOptions;
		} //setDeploymentOptionEnum

   	
	   	
		public String deployContent( String strArchiveName,	String[] selectedPubContent) {
			AsynchReply asynchReply = null;
			String reportEventID = "-1";
			

			
			String deployPath;
			SearchPathSingleObject searchPathObject = new SearchPathSingleObject();

			// Add an archive name to the content store
			BaseClass[] ArchiveInfo = addArchive(deployType, strArchiveName);

			System.out.println("\nCreated Archive " + strArchiveName);
			if ((ArchiveInfo != null) && (ArchiveInfo.length == 1)) {
				deployPath = ArchiveInfo[0].getSearchPath().getValue();
				searchPathObject.set_value(deployPath);
			} else {
				return reportEventID;
			}
			
			if( selectedPubContent!= null )
			{
				int siz = selectedPubContent.length;
				for ( int i= 0 ; i< siz; i++)
					System.out.println("\nDeploying:  " +  selectedPubContent [i] );  // non printed for Entire CS
			}

			Option[] myDeploymentOptionsEnum=null;
			if( folders != null )
			   myDeploymentOptionsEnum = setDeploymentOptionEnum(deployType, strArchiveName, selectedPubContent);
			
			OptionArrayProp deploymentOptionsArray = new OptionArrayProp();
			deploymentOptionsArray.setValue(myDeploymentOptionsEnum);
			((ExportDeployment) ArchiveInfo[0]).setOptions(deploymentOptionsArray);

			try {
				cmService.update(ArchiveInfo, new UpdateOptions());

				asynchReply = getMonitorService().run(searchPathObject,
						new ParameterValue[] {}, new Option[] {});

				// if it has not yet completed, keep waiting until it is done

				if (asynchReply.getStatus() != AsynchReplyStatusEnum.complete
						|| asynchReply.getStatus() != AsynchReplyStatusEnum.conversationComplete) {
					while (asynchReply.getStatus() != AsynchReplyStatusEnum.complete
							&& asynchReply.getStatus() != AsynchReplyStatusEnum.conversationComplete) {
						if (hasSecondaryRequest(asynchReply, "wait")) {
							System.out
							.println("waiting...");
							asynchReply = getMonitorService().wait(
									asynchReply.getPrimaryRequest(),
									new ParameterValue[] {}, new Option[] {});
						} else {
							System.out
									.println("Error: Wait method not available as expected.");
						}
					}
				}

			} catch (RemoteException remoteEx) {
				System.out.println("An error occurred while deploying content:"
						+ "\n" + remoteEx.getMessage());
				remoteEx.printStackTrace();
			}

			if (asynchReply != null) {
				reportEventID = "Success";
			} else {
				reportEventID = "Failed";
			}
			System.out.println("\nExport Status: " + reportEventID);
			return reportEventID;
		} //deployContent
		
		public MonitorService_PortType getMonitorService()
		{
			BiBusHeader bibus = getHeaderObject(((Stub)mService).
		  getResponseHeader(BUS_NS, BUS_HEADER), false);
				
			if (bibus == null) 
			{
				bibus = getHeaderObject(((Stub)cmService).
				getResponseHeader(BUS_NS, BUS_HEADER), false);	
			}
			((Stub)mService).clearHeaders();
			((Stub)mService).setHeader(BUS_NS, BUS_HEADER, bibus);	
			return mService;
		}
		
		public static boolean hasSecondaryRequest(AsynchReply response,
				String secondaryRequest)
		{
			AsynchSecondaryRequest[] secondaryRequests = response
					.getSecondaryRequests();
			if (secondaryRequests != null)
			{
				for (int i = 0; i < secondaryRequests.length; i++)
				{
					if (secondaryRequests[i].getName().compareTo(secondaryRequest) == 0)
					{
						return true;
					}
				}
			}
			return false;
		}
		
		public BiBusHeader getHeaderObject(SOAPHeaderElement SourceHeader, boolean isNewConversation)
		{
			final QName BUS_QNAME = new QName(BUS_NS, BUS_HEADER);
			BiBusHeader bibus = null;
				
			if (SourceHeader == null)
				return null;
				
			try {
				bibus = (BiBusHeader)SourceHeader.getValueAsType(BUS_QNAME);
							
			//If the header will be used for a new conversation, clear
			//tracking information, DispatcherTransportVars and Routing values
			if (isNewConversation)
		{
			  bibus.setTracking(null);
			  bibus.setDispatcherTransportVars(new DispatcherTransportVar[]{});
			  bibus.setRouting(null);
			}
		  } catch (Exception e) {
			e.printStackTrace();
		  }
				
		  return bibus;
		}
		
		
		///This method logs the user to Cognos BI
		public String quickLogon(String namespace, String uid, String pwd)
		{
			StringBuffer credentialXML = new StringBuffer();

			credentialXML.append("<credential>");
			credentialXML.append("<namespace>").append(namespace).append("</namespace>");
			credentialXML.append("<username>").append(uid).append("</username>");
			credentialXML.append("<password>").append(pwd).append("</password>");
			credentialXML.append("</credential>");

			String encodedCredentials = credentialXML.toString();
		    XmlEncodedXML xmlCredentials = new XmlEncodedXML();
		    xmlCredentials.set_value(encodedCredentials);
		        
		   //Invoke the ContentManager service logon() method passing the credential string
		   //You will pass an empty string in the second argument. Optionally,
		   //you could pass the Role as an argument
		    
		   try
			{		
				cmService.logon(xmlCredentials,null );

				SOAPHeaderElement temp = ((Stub)cmService).getResponseHeader 
				 	("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader");
				BiBusHeader cmBiBusHeader = (BiBusHeader)temp.getValueAsType
					(new QName ("http://developer.cognos.com/schemas/bibus/3/","biBusHeader"));
				((Stub)cmService).setHeader
					("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", cmBiBusHeader);
				((Stub)mService).setHeader
					("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", cmBiBusHeader);
			}
			catch (Exception e)
			{
				System.out.println(e);
			}
			
			return ("Logon successful as " + uid);
		}
	   	

	   	
		//This method connects to Cognos BI
		public void connectToReportServer (String endPoint)
		{	
			ContentManagerService_ServiceLocator cmServiceLocator = new ContentManagerService_ServiceLocator();
			cmServiceLocator.getEngine().setOption(AxisEngine. PROP_DOMULTIREFS, Boolean. FALSE);
			MonitorService_ServiceLocator  mServiceLocator = new MonitorService_ServiceLocator();
			mServiceLocator.getEngine().setOption(AxisEngine. PROP_DOMULTIREFS, Boolean. FALSE);
		    try 
		    {
				cmService = cmServiceLocator.getcontentManagerService(new java.net.URL(endPoint));
				mService = mServiceLocator.getmonitorService(new java.net.URL(endPoint));
				
			
				
                // set the Axis request timeout
				((Stub) cmService).setTimeout(0); // in milliseconds, 0 turns the timeout off
				((Stub)mService).setTimeout(0); // in milliseconds, 0 turns the timeout off
				
			} 
		    catch (MalformedURLException e)	{e.printStackTrace();	} 
			catch (ServiceException e) 		{e.printStackTrace();	}
			
		}//connectToReportServer

		
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
	   		dName.setValue(new MultilingualString[] {deploymentName});
	   		return dName;
		}

		// allow the deployment overwrites the archive
		private DeploymentOptionBoolean setArchiveOverWrite(boolean setValue) {
			DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
			deployOptionBool.setName(DeploymentOptionEnum
					.fromString("archiveOverwrite"));
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
			deployOptionBool.setName(DeploymentOptionEnum
					.fromString("dataSourceSelect"));
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
			deployOptionBool.setName(DeploymentOptionEnum
					.fromString("namespaceSelect"));
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
			deployOptionBool.setName(DeploymentOptionEnum
					.fromString("namespaceThirdParty"));
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
			deployOptionBool.setName(DeploymentOptionEnum
					.fromString("objectPolicies"));
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
			deployOptionBool.setName(DeploymentOptionEnum
					.fromString("packageHistories"));
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
			deployOptionBool.setName(DeploymentOptionEnum
					.fromString("packageOutputs"));
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
			deployOptionBool.setName(DeploymentOptionEnum
					.fromString("packageSchedules"));
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
			deployOptionBool.setName(DeploymentOptionEnum
					.fromString("packageSelect"));
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
			deployOptionBool.setName(DeploymentOptionEnum
					.fromString("recipientsSelect"));
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
			deployOptionBool.setName(DeploymentOptionEnum
					.fromString("takeOwnership"));
			if (setValue) {
				deployOptionBool.setValue(false);
			} else {
				deployOptionBool.setValue(true);
			}
			return deployOptionBool;
		}

		// set dataSourceConflictResolution as default value - 'replace'
		private DeploymentOptionResolution setDataSourceConflictResolution(
				boolean setValue) {
			DeploymentOptionResolution deployOptionResolute = new DeploymentOptionResolution();
			deployOptionResolute.setName(DeploymentOptionEnum
					.fromString("dataSourceConflictResolution"));
			if (setValue) {
				deployOptionResolute.setValue(ConflictResolutionEnum.replace);

			} else {
				deployOptionResolute.setValue(ConflictResolutionEnum.keep);
			}
			return deployOptionResolute;
		}

		// set namespaceConflictResolution as default value - 'replace'
		private DeploymentOptionResolution setNamespaceConflictResolution(
				boolean setValue) {
			DeploymentOptionResolution deployOptionResolute = new DeploymentOptionResolution();
			deployOptionResolute.setName(DeploymentOptionEnum
					.fromString("namespaceConflictResolution"));
			if (setValue) {
				deployOptionResolute.setValue(ConflictResolutionEnum.replace);

			} else {
				deployOptionResolute.setValue(ConflictResolutionEnum.keep);
			}
			return deployOptionResolute;
		}

		// set objectPoliciesConflictResolution as default value - 'replace'
		private DeploymentOptionResolution setObjectPoliciesConflictResolution(
				boolean setValue) {
			DeploymentOptionResolution deployOptionResolute = new DeploymentOptionResolution();
			deployOptionResolute.setName(DeploymentOptionEnum
					.fromString("objectPoliciesConflictResolution"));
			if (setValue) {
				deployOptionResolute.setValue(ConflictResolutionEnum.replace);

			} else {
				deployOptionResolute.setValue(ConflictResolutionEnum.keep);
			}
			return deployOptionResolute;
		}

		// set ownershipConflictResolution as default value - 'replace'
		private DeploymentOptionResolution setOwnershipConflictResolution(
				boolean setValue) {
			DeploymentOptionResolution deployOptionResolute = new DeploymentOptionResolution();
			deployOptionResolute.setName(DeploymentOptionEnum
					.fromString("ownershipConflictResolution"));
			if (setValue) {
				deployOptionResolute.setValue(ConflictResolutionEnum.replace);

			} else {
				deployOptionResolute.setValue(ConflictResolutionEnum.keep);
			}
			return deployOptionResolute;
		}

		// set packageHistoriesConflictResolution as default value - 'replace'
		private DeploymentOptionResolution setPackageHistoriesConflictResolution(
				boolean setValue) {
			DeploymentOptionResolution deployOptionResolute = new DeploymentOptionResolution();
			deployOptionResolute.setName(DeploymentOptionEnum
					.fromString("packageHistoriesConflictResolution"));
			if (setValue) {
				deployOptionResolute.setValue(ConflictResolutionEnum.replace);

			} else {
				deployOptionResolute.setValue(ConflictResolutionEnum.keep);
			}
			return deployOptionResolute;
		}

		// set packageOutputsConflictResolution as default value - 'replace'
		private DeploymentOptionResolution setPackageOutputsConflictResolution(
				boolean setValue) {
			DeploymentOptionResolution deployOptionResolute = new DeploymentOptionResolution();
			deployOptionResolute.setName(DeploymentOptionEnum
					.fromString("packageOutputsConflictResolution"));
			if (setValue) {
				deployOptionResolute.setValue(ConflictResolutionEnum.replace);

			} else {
				deployOptionResolute.setValue(ConflictResolutionEnum.keep);
			}
			return deployOptionResolute;
		}

		// set packageSchedulesConflictResolution as default value - 'replace'
		private DeploymentOptionResolution setPackageSchedulesConflictResolution(
				boolean setValue) {
			DeploymentOptionResolution deployOptionResolute = new DeploymentOptionResolution();
			deployOptionResolute.setName(DeploymentOptionEnum
					.fromString("packageSchedulesConflictResolution"));
			if (setValue) {
				deployOptionResolute.setValue(ConflictResolutionEnum.replace);

			} else {
				deployOptionResolute.setValue(ConflictResolutionEnum.keep);
			}
			return deployOptionResolute;
		}

		// set recipientsConflictResolution as default value - 'replace'
		private DeploymentOptionResolution setRecipientsConflictResolution(
				boolean setValue) {
			DeploymentOptionResolution deployOptionResolute = new DeploymentOptionResolution();
			deployOptionResolute.setName(DeploymentOptionEnum
					.fromString("recipientsConflictResolution"));
			if (setValue) {
				deployOptionResolute.setValue(ConflictResolutionEnum.replace);

			} else {
				deployOptionResolute.setValue(ConflictResolutionEnum.keep);
			}
			return deployOptionResolute;
		}
} //ExportPartial
