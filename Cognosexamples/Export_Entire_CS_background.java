
/* 
 * Licensed Materials - Property of IBM
 * IBM Cognos Products: SDK Support
 * (C) Copyright IBM Corp. 2003, 2016
 * US Government Users Restricted Rights - Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
*/

/*
 * Export_Entire_CS_background  - this sample runs export in the background
 * Description: Technote 1338960 SDK Sample to export or import a deployment
 * Tested with:  IBM Cognos BI 10.2.1 FP1, 10.2.2, IBM Java 1.6, 1.7, Axis 1.4
*/


//This sample exports entire Content Store secured by mandatory export password

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.HashMap;

import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;

import com.cognos.developer.schemas.bibus._3.AddOptions;
import com.cognos.developer.schemas.bibus._3.AsynchReply;
import com.cognos.developer.schemas.bibus._3.AsynchReplyStatusEnum;
import com.cognos.developer.schemas.bibus._3.AsynchSecondaryRequest;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BiBusHeader;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.DeploymentOption;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionAnyType;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionBoolean;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionEnum;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionString;
import com.cognos.developer.schemas.bibus._3.DispatcherTransportVar;
import com.cognos.developer.schemas.bibus._3.ExportDeployment;
import com.cognos.developer.schemas.bibus._3.MonitorService_PortType;
import com.cognos.developer.schemas.bibus._3.MonitorService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.MultilingualString;
import com.cognos.developer.schemas.bibus._3.MultilingualToken;
import com.cognos.developer.schemas.bibus._3.MultilingualTokenProp;
import com.cognos.developer.schemas.bibus._3.Option;
import com.cognos.developer.schemas.bibus._3.OptionArrayProp;
import com.cognos.developer.schemas.bibus._3.ParameterValue;
import com.cognos.developer.schemas.bibus._3.SearchPathSingleObject;
import com.cognos.developer.schemas.bibus._3.UpdateActionEnum;
import com.cognos.developer.schemas.bibus._3.UpdateOptions;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;
import com.cognos.developer.schemas.bibus._3.MonitorOptionEnum;
import com.cognos.developer.schemas.bibus._3.MonitorOptionBoolean;

public class Export_Entire_CS_background {
	private ContentManagerService_PortType cmService = null;
	private MonitorService_PortType mService = null;
	String BUS_NS = "http://developer.cognos.com/schemas/bibus/3/";
	String BUS_HEADER = "biBusHeader";

	public static String logFile = "Export_Final.csv";
	String arguments[] = { "-a", "-i", "-s", "-u", "-p", "-g" };
	private String[] packages = null;
	private String deployType = "export";
	String strLocale = "en";
	private HashMap packageInformation = null;
	private static String password = null;

	public static void main(String[] args) {
		Export_Entire_CS_background exp = new Export_Entire_CS_background();

		String expDeploymentName = null;
		String gateway = "http://localhost:9300/p2pd/servlet/dispatch";
		String nameSpace = null;
		String userID = null;
		String pass = null;

   		if (args.length < 1)
   		{
   			exp.printUsage(); 
   		}
   		
  	   int args_len = args.length ;
   	
	   for (int i=0; i<args_len; i++)
	   {
		   System.out.println("\nExport parm: " + args[i] + " " +  args[i+1]);
	   		if (args[i].compareToIgnoreCase("-s") == 0)
	   			nameSpace = args[++i];  // namespace
	   		else 
	   			if (args[i].compareToIgnoreCase("-i") == 0)
	   				exp.password = args[++i];  // secure export
	   			else
		   			if (args[i].compareToIgnoreCase("-u") == 0)
		   				userID = args[++i];  // user_name
		   			else
		   				if (args[i].compareToIgnoreCase("-p") == 0)
		   					pass = args[++i];  // user_password
		   				else
		   					if (args[i].compareToIgnoreCase("-a") == 0)
		   						expDeploymentName =  args[++i];	 //export name  
		   					else
		   						if (args[i].compareToIgnoreCase("-g") == 0)
		   							gateway =  args[++i];	  //entry point 	
	   }// run time parameters
			 
	
		exp.connectToReportServer(gateway);
		if (nameSpace != null && userID != null && pass != null) { // must secure by password
			exp.quickLogon(nameSpace, userID, pass);
		} else {
			System.out.println("Missing logon information. Attempting to login as Anonymous user...");
		}

		if (expDeploymentName != null) {
			if (password != null) { // required
				exp.deployContentCS(expDeploymentName);
			} else {
				System.out
						.println("You must provide a password to create CM deployment archive \"t\".");
			}
		} else {
			System.out.println("No Valid Archive Name Provided.");
		}
	} // main
	
	
	//Prints Usage if not enough parameters passed
	public void printUsage()
	{
		String usage = "\njava Export -a <archiveName> [-i <archivePassword>] [-s <namespaceID> -u <userID> -p <userPassword>] [-g <CognosBIDispatcher>]";
		String example = "Example: \njava Export -a CMarchive -i password -s \"LDAPID\" -u \"User\" -p \"UserPassword\" -g http://localhost:9300/p2pd/servlet/dispatch";
		
		System.out.println(usage);
		System.out.println(example);
		displayHelp();
		System.exit(1);
	}
	
	//Displays help for the parameters
	public void displayHelp()
	{
		String usage = "";
		usage += "Create a Deployment package and export the contents.\n\n";
		usage += "usage:\n\n";
		usage += "-a archiveName\n\tThe name of the new archive\n";
		usage += "-i archivePassword\n\tThe password for the archive. Mandatory for Content Store export.\n";
		//usage += "-c package1 [package2]\n\tPackages to be exported\n";
		usage += "-s namespaceID\n\tNamespaceID the user belongs to.\n";
		usage += "-u userID\n\tUserID of a System Administrator.\n";
		usage += "-p userPassword\n\tPassword for the UserID.\n";
		usage += "-g CognosBIDispatcher\n\tDispatcher URL for Cognos BI.\n";
		usage += "\t Default: http://localhost:9300/p2pd/servlet/dispatch\n";
			
		System.out.println(usage);
		System.exit(1);
		
	}

	// Deployment object to the content store
	private BaseClass[] addArchive(String deploySpec, String nameOfArchive) {
		ExportDeployment exportDeploy = null;
		BaseClass[] addedDeploymentObjects = null;
		BaseClass[] bca = new BaseClass[1];
		AddOptions addOpts = null;

		SearchPathSingleObject objOfSearchPath = new SearchPathSingleObject(
				"/adminFolder");

		MultilingualTokenProp multilingualTokenProperty = new MultilingualTokenProp();
		MultilingualToken[] multilingualTokenArr = new MultilingualToken[1];
		MultilingualToken myMultilingualToken = new MultilingualToken();

		myMultilingualToken.setLocale(strLocale);
		myMultilingualToken.setValue(nameOfArchive);
		multilingualTokenArr[0] = myMultilingualToken;
		multilingualTokenProperty.setValue(multilingualTokenArr);

		exportDeploy = new ExportDeployment();
		addOpts = new AddOptions();
		exportDeploy.setName(multilingualTokenProperty);
		addOpts.setUpdateAction(UpdateActionEnum.replace);
		bca[0] = exportDeploy;
		try {
			addedDeploymentObjects = cmService.add(objOfSearchPath, bca,
					addOpts);
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
	} // addArchive

	// setting options to export ENTIRE CS
	private Option[] setDeploymentOptionEnumCS(String deploymentType,
			String nameOfArchive, String exportPassword) {
		Option[] deploymentOptions = null;
		int num = 0;
		int eOptionCount = 0;

		System.out.println("\nIn setDeploymentOptionEnumCS");
		String[] deployOptionEnumBoolean = { "entireContentStoreSelect", "archiveOverwrite", "personalDataSelect" };
		String[] deployOptionEnumResolution = { "archive",	"archiveEncryptPassword" };

		deploymentOptions = new DeploymentOption[eOptionCount  +
		                    deployOptionEnumBoolean.length + deployOptionEnumResolution.length];
		
		deploymentOptions[num]   = this.setEntireContentStoreSelect(true); // choose entire CS
		deploymentOptions[++num] = this.setArchiveOverWrite(true); // overwrite
		deploymentOptions[++num] = this.setPersonalDataSelect(true); // default is false
		
		deploymentOptions[++num] = this.setDeploymentOptionString(nameOfArchive); // archive name
		deploymentOptions[++num] = this.setArchiveEncryptPassword(exportPassword);  //secure by password

		return deploymentOptions;
	} // setDeploymentOptionEnumCS
	
	
	public Option[] setRunOptions() 
	{
	Option ro[] = new Option[1];
	MonitorOptionBoolean background = new MonitorOptionBoolean();

	// run in the background. This enables the event ID being returned in
	// the event of a failure of the run.
	background.setName(MonitorOptionEnum.background);
	background.setValue(true);

	ro[0] = background;
	return ro;

	} //setRunOptions
	

	public String deployContentCS(String strArchiveName) {
		System.out.println("\nIn deployContentCS ");

		AsynchReply asynchReply = null;
		String reportEventID = "-1";

		String deployPath;
		SearchPathSingleObject searchPathObject = new SearchPathSingleObject();

		// Add an archive name to the content store
		BaseClass[] ArchiveInfo = addArchive(deployType, strArchiveName);

		System.out.println("\nAdded archive " + strArchiveName);
		if ((ArchiveInfo != null) && (ArchiveInfo.length == 1)) {
			deployPath = ArchiveInfo[0].getSearchPath().getValue();
			searchPathObject.set_value(deployPath); 
		} else {
			return reportEventID;
		}
		
		//System.out.println("\nPassword = " + password);
		Option[] myDeploymentOptionsEnum = null;
		myDeploymentOptionsEnum = setDeploymentOptionEnumCS(deployType,	strArchiveName, password);

		OptionArrayProp deploymentOptionsArray = new OptionArrayProp();
		deploymentOptionsArray.setValue(myDeploymentOptionsEnum);
		((ExportDeployment) ArchiveInfo[0]).setOptions(deploymentOptionsArray);

		try {
			cmService.update(ArchiveInfo, new UpdateOptions());
			asynchReply = getMonitorService().run(searchPathObject,
					new ParameterValue[] {}, setRunOptions());

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
			System.out.println("\nStatus value "+  asynchReply.getStatus().getValue() );
			reportEventID = "Success";
		} else {
			reportEventID = "Failed";
		}

		System.out.println("\nRESULTS: " + reportEventID);
		return reportEventID;
	} // deployContentCS
		
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

	// /This method logs the user to Cognos BI
	public String quickLogon(String namespace, String uid, String pwd) {
		StringBuffer credentialXML = new StringBuffer();

		credentialXML.append("<credential>");
		credentialXML.append("<namespace>").append(namespace)
				.append("</namespace>");
		credentialXML.append("<username>").append(uid).append("</username>");
		credentialXML.append("<password>").append(pwd).append("</password>");
		credentialXML.append("</credential>");

		String encodedCredentials = credentialXML.toString();
		XmlEncodedXML xmlCredentials = new XmlEncodedXML();
		xmlCredentials.set_value(encodedCredentials);

		// Invoke the ContentManager service logon() method passing the
		// credential string
		// You will pass an empty string in the second argument. Optionally,
		// you could pass the Role as an argument
		try {
			cmService.logon(xmlCredentials, null);
			SOAPHeaderElement temp = ((Stub) cmService).getResponseHeader(
					"http://developer.cognos.com/schemas/bibus/3/",	"biBusHeader");
			BiBusHeader cmBiBusHeader = (BiBusHeader) temp.getValueAsType(new QName(
							"http://developer.cognos.com/schemas/bibus/3/",	"biBusHeader"));
			((Stub) cmService).setHeader(
					"http://developer.cognos.com/schemas/bibus/3/",
					"biBusHeader", cmBiBusHeader);
			((Stub) mService).setHeader(
					"http://developer.cognos.com/schemas/bibus/3/",
					"biBusHeader", cmBiBusHeader);
		} catch (Exception e) {
			System.out.println(e);
		}
		return ("Logon successful as " + uid);
	}// quickLogon

	public void connectToReportServer(String endPoint) { // This method connects to Cognos BI
		ContentManagerService_ServiceLocator cmServiceLocator = new ContentManagerService_ServiceLocator();
		MonitorService_ServiceLocator mServiceLocator = new MonitorService_ServiceLocator();

		try {
			cmService = cmServiceLocator.getcontentManagerService(new java.net.URL(endPoint));
			mService = mServiceLocator.getmonitorService(new java.net.URL(endPoint));

			// set the Axis request timeout
			((Stub) cmService).setTimeout(0); // in milliseconds, 0 turns the timeout off
			((Stub) mService).setTimeout(0);  // in milliseconds, 0 turns the timeout off
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ServiceException e) {
			e.printStackTrace();
		}
	}// connectToReportServer
	

	private DeploymentOptionBoolean setEntireContentStoreSelect(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("entireContentStoreSelect"));
		deployOptionBool.setValue(setValue);
		
		return deployOptionBool;
	}// setArchiveOverWrite
	

	private DeploymentOptionAnyType setArchiveEncryptPassword(String pPassword) {
		DeploymentOptionAnyType archiveEncryptPassword = null;
		if (pPassword != null && pPassword.length() >= 1) {
			archiveEncryptPassword = new DeploymentOptionAnyType();
			archiveEncryptPassword.setValue("<credential><password>"
					+ pPassword + "</password></credential>");
			archiveEncryptPassword.setName(DeploymentOptionEnum.fromString("archiveEncryptPassword"));
		}
		return archiveEncryptPassword;
	}// setArchiveEncryptPassword
	

	private DeploymentOptionString setDeploymentOptionString(String archiveName) { // mandatory
		MultilingualString archiveDefault = new MultilingualString();
		archiveDefault.setLocale(strLocale);
		archiveDefault.setValue(archiveName);

		DeploymentOptionString deployOptionStr = new DeploymentOptionString();
		deployOptionStr.setName(DeploymentOptionEnum.fromString("archive"));
		deployOptionStr.setValue(archiveDefault.getValue());
		
		return deployOptionStr;
	}// setDeploymentOptionString
	

	private DeploymentOptionBoolean setArchiveOverWrite(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("archiveOverwrite"));
		deployOptionBool.setValue(setValue);

		return deployOptionBool;
	}// setArchiveOverWrite
	

	// allow the deployment overwrites the archive
	private DeploymentOptionBoolean setPersonalDataSelect(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("personalDataSelect"));
		deployOptionBool.setValue(setValue);

		return deployOptionBool;
	}// setPersonalDataSelect

} // Export_Entire_CS_background

