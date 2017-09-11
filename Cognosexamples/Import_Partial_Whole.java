/* 
 * Licensed Materials - Property of IBM
 * IBM Cognos Products: SDK Support
 * (C) Copyright IBM Corp. 2003, 2016
 * US Government Users Restricted Rights - Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
*/

/*
 * Import_Partial_Whole
 * Description: Technote 1338960 SDK Sample to export or import a deployment
 * Tested with:  IBM Cognos BI 10.2.1 FP1, 10.2.2, IBM Java 1.6, 1.7, Axis 1.4
*/
// Import_Partial_Whole.java class imports all folders/packages from the archive exported by Export_Partial.java 
// You have no choice of importing subset of folders from archive


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
import com.cognos.developer.schemas.bibus._3.DeploymentObjectInformation;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionObjectInformationArray;
import com.cognos.developer.schemas.bibus._3.DispatcherTransportVar;
import com.cognos.developer.schemas.bibus._3.ImportDeployment;
import com.cognos.developer.schemas.bibus._3.MonitorService_PortType;
import com.cognos.developer.schemas.bibus._3.MonitorService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.MultilingualToken;
import com.cognos.developer.schemas.bibus._3.MultilingualTokenProp;
import com.cognos.developer.schemas.bibus._3.Option;
import com.cognos.developer.schemas.bibus._3.OptionArrayProp;
import com.cognos.developer.schemas.bibus._3.ParameterValue;
import com.cognos.developer.schemas.bibus._3.SearchPathSingleObject;
import com.cognos.developer.schemas.bibus._3.UpdateActionEnum;
import com.cognos.developer.schemas.bibus._3.UpdateOptions;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;





public class Import_Partial_Whole {
	
	private ContentManagerService_PortType cmService = null;
	private MonitorService_PortType monitorService = null;
	
	String BUS_NS = "http://developer.cognos.com/schemas/bibus/3/";
	String BUS_HEADER = "biBusHeader";
	

	private HashMap  packageInformation = null;
	private String deployType = "import";
	private String strLocale = "en";
	
	private static final String DEPLOY_OPTION_NAME = "com.cognos.developer.schemas.bibus._3.DeploymentOptionObjectInformationArray";
	
   	public static void main(String[] args) 
   	{
   		String impDeploymentSpec = new String();
   		String impDeploymentName = new String();
		String gateway = "http://localhost:9300/p2pd/servlet/dispatch";
   		String nameSpace= null;
   		String userID = null;
   		String pass = null;
		
   		
   		int args_len = args.length ;
   		Import_Partial_Whole imp = new Import_Partial_Whole();
   		
  		if (args.length < 1)
   		{
   			//If archiveName is more than one word, put it in quotes, ex. "SDK Import"
   			imp.printUsage();
   		}
	    for (int i=0; i<args_len; i++)
	    {
	   		if (args[i].compareToIgnoreCase("-s") == 0)  //nameSpace
	   			nameSpace = args[++i];
   			else
	   			if (args[i].compareToIgnoreCase("-u") == 0) //userID
	   				userID = args[++i];
	   			else
	   				if (args[i].compareToIgnoreCase("-p") == 0)  //user password
	   					pass = args[++i];
	   				else
	   					if (args[i].compareToIgnoreCase("-a") == 0)  // deploymentName from Archive 
	   						impDeploymentSpec =  args[++i];	   
	   					else
		   					if (args[i].compareToIgnoreCase("-d") == 0)  //my import Name ( imp_DeploymentSpec)
		   						impDeploymentName =  args[++i];	   
		   					else
	   						if (args[i].compareToIgnoreCase("-g") == 0) //CognosBIDispatcher
	   							gateway =  args[++i];	   	
	   						else
	   							imp.displayHelp();
	   }//for

	   imp.connectToReportServer(gateway);  // setting cmService
		   
	   if (nameSpace != null && userID != null && pass != null)
	   	  imp.quickLogon(nameSpace, userID, pass);	
	   else
	   	  System.out.println("No logon information. Attempting to login as Anonymous user...");
	   
	   System.out.println("\nExisting Archive: " + impDeploymentSpec );
	   System.out.println("\nImporting as: " + impDeploymentName );
   
		if (impDeploymentSpec != null)     //existing Export Archive ( "ExportSample_SDK" )
		{ 
			String imported = imp.deployContent(impDeploymentName, impDeploymentSpec);
			
		   if (imported == "false")
				System.out.println("Problems occured while importing archive in CM " + impDeploymentSpec);
		   else
		   {
			System.out.println("\nImport was sucessful");  
		   }
	 	}
	 	else
	 		System.out.println("No Valid Archive Name Provided.");

   } //main
		
   	
	/**
	 * use this method to return all the public folder content associated with
	 * one specific archive
	 */
	// sn_dg_sdk_method_contentManagerService_getDeploymentOptions_start_0
	public HashMap getPubFolderContent(String myArchive )
	{

		Option[] deployOptEnum = new Option[] {};
		HashMap arrOfPublicFolder = new HashMap();
   		
		try {
			
			deployOptEnum = cmService.getDeploymentOptions(	myArchive, new Option[] {});
			
	       // sn_dg_sdk_method_contentManagerService_getDeploymentOptions_end_0
			for (int i = 0; i < deployOptEnum.length; i++) {
				if (deployOptEnum[i].getClass().getName() == DEPLOY_OPTION_NAME) {

					DeploymentObjectInformation[] packDeployInfo = ((DeploymentOptionObjectInformationArray) deployOptEnum[i])
							.getValue();
					int packLen = packDeployInfo.length;

					for (int j = 0; j < packLen; j++) {
					    String packFolderName=packDeployInfo[j].getDefaultName();
					    System.out.println("\nImporting Package: " + packFolderName);
					    SearchPathSingleObject packagePath=packDeployInfo[j].getSearchPath();

					    arrOfPublicFolder.put(packFolderName, packagePath);
					}
				}
			}
		} catch (RemoteException e) {
			System.out
					.println("An error occurred in getting Deployment options."
							+ "\n" + "The error: " + e.getMessage());
			e.printStackTrace();
		}
		packageInformation=new HashMap(arrOfPublicFolder);

		return arrOfPublicFolder;
	} //getPubFolderContent

	
	
	// sn_dg_sdk_method_contentManagerService_getDeploymentOptions_start_0
	public Option[] getDeployedOption(String myArchive)
	{
		Option[] deployOptEnum = new Option[] {};
  		
		try {
			//deployOptEnum = cmService.getDeploymentOptions(	myArchive, opt);
			deployOptEnum = cmService.getDeploymentOptions(	myArchive, new Option[] {});  
		} catch (RemoteException e) {
			System.out.println("An error occurred in getting Deployment options."
							+ "\n" + "The error: " + e.getMessage());
			e.printStackTrace();
		}

		return deployOptEnum;
	} //getDeployedOption


	
	public String deployContent( String strNewImportName, String strDeployedArchive)
	{
		AsynchReply asynchReply = null;
		String reportEventID = "false";

		String deployPath = null;
		SearchPathSingleObject searchPathObject = new SearchPathSingleObject();

		// Add an archive name to the content store
		BaseClass[] ArchiveInfo = addArchive(deployType, strNewImportName);
		
		if ((ArchiveInfo != null) && (ArchiveInfo.length == 1)) {
			deployPath = ArchiveInfo[0].getSearchPath().getValue();
			searchPathObject.set_value(deployPath);
			System.out.println("\nImport Archive Created: " + deployPath);
		} else {
			return reportEventID;
		}

			
		Option[] myDeploymentOptionsEnum=null;
		myDeploymentOptionsEnum = getDeployedOption(strDeployedArchive);
		OptionArrayProp deploymentOptionsArray = new OptionArrayProp();
		deploymentOptionsArray.setValue(myDeploymentOptionsEnum);

		((ImportDeployment) ArchiveInfo[0]).setOptions(deploymentOptionsArray);

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

		if (asynchReply != null)
			reportEventID = "Success";

		return reportEventID;
	}//deployContent
	
	public MonitorService_PortType getMonitorService()
	{
		BiBusHeader bibus = getHeaderObject(((Stub)monitorService).
	  getResponseHeader(BUS_NS, BUS_HEADER), false);
			
		if (bibus == null) 
		{
			bibus = getHeaderObject(((Stub)cmService).
			getResponseHeader(BUS_NS, BUS_HEADER), false);	
		}
		((Stub)monitorService).clearHeaders();
		((Stub)monitorService).setHeader(BUS_NS, BUS_HEADER, bibus);	
		return monitorService;
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
   	

	//Print usage of the script
	public void printUsage()
	{
		String usage = "\njava Import -a <archiveName> [-d deploymentName] [-s <namespaceID> -u <userID> -p <userPassword>] [-g <CognosBIDispatcher>]";
		String example = "Example: \njava Import -a CMarchive  -d CMarchive_import -s \"LDAPID\" -u \"User\" -p \"UserPassword\" -g http://server:9300/p2pd/servlet/dispatch";
		
		System.out.println(usage);
		System.out.println(example);
		displayHelp();
		System.exit(1);
	}
	
	//Displays help
	public void displayHelp()
	{
		String usage = "";
		usage += "Import the contents of a Deployment archive.\n\n";
		usage += "Usage:\n\n";
		usage += "-a archiveName\n\tThe name of the new archive\n";
		usage += "-d deploymentName\n\tThe name of the new import deployment (optional. Can be used to resolve naming conflicts with existing export deployments)\n";
		usage += "-s namespaceID\n\tNamespaceID the user belongs to.\n";
		usage += "-u userID\n\tUserID of a System Administrator.\n";
		usage += "-p userPassword\n\tPassword for the UserID.\n";
		usage += "-g CognosBIDispatcher\n\tDispatcher URL for Cognos BI.\n";
		usage += "\t Default: http://localhost:9300/p2pd/servlet/dispatch\n";
		
		System.out.println(usage);
		System.exit(1);
	}
	
    //This method logs the user to Cognos BI
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
			((Stub)monitorService).setHeader
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
		MonitorService_ServiceLocator  moServiceLocator = new MonitorService_ServiceLocator();
	    try 
	    {
			cmService = cmServiceLocator.getcontentManagerService(new java.net.URL(endPoint));
			monitorService = moServiceLocator.getmonitorService(new java.net.URL(endPoint));
//			 set the Axis request timeout
			((Stub) cmService).setTimeout(0); // in milliseconds, 0 turns the timeout off
			((Stub)monitorService).setTimeout(0); // in milliseconds, 0 turns the timeout off

		} 
	    catch (MalformedURLException e) 
		{
			e.printStackTrace();
		} 
		catch (ServiceException e) 
		{
			e.printStackTrace();
		}
	} //connectToReportServer

	private BaseClass[] addArchive(String deploySpec, String nameOfArchive)
	{

		ImportDeployment importDeploy = null;
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

		importDeploy = new ImportDeployment();
		addOpts = new AddOptions();
		importDeploy.setName(multilingualTokenProperty);
		addOpts.setUpdateAction(UpdateActionEnum.replace);
		bca[0] = importDeploy;

		try {
			addedDeploymentObjects = cmService.add(objOfSearchPath,	bca, addOpts);
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
	


}//Import_Partial_Whole

