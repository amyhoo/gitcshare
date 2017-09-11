/* 
 * Licensed Materials - Property of IBM
 * IBM Cognos Products: SDK Support
 * (C) Copyright IBM Corp. 2003, 2016
 * US Government Users Restricted Rights - Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
*/

/*
 * Export_Entire_CS
 * Description: Technote 1338960 SDK Sample to export or import a deployment
 * Tested with:  IBM Cognos BI 10.2.1 FP1, 10.2.2, IBM Java 1.6, 1.7, Axis 1.4
*/

//This sample imports entire Content Store secured by mandatory import password

import com.cognos.developer.schemas.bibus._3.*;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import javax.xml.namespace.QName;

import org.apache.axis.AxisFault;

import java.io.*;
import java.net.MalformedURLException;
import java.rmi.RemoteException; 
import java.util.*;

import javax.xml.rpc.ServiceException;


import com.cognos.developer.schemas.bibus._3.AddOptions;
import com.cognos.developer.schemas.bibus._3.AsynchReply;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BiBusHeader;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.DeploymentOption;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionBoolean;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionEnum;
import com.cognos.developer.schemas.bibus._3.DeploymentOptionString;
import com.cognos.developer.schemas.bibus._3.ExportDeployment;
import com.cognos.developer.schemas.bibus._3.ImportDeployment;
import com.cognos.developer.schemas.bibus._3.MonitorService_PortType;
import com.cognos.developer.schemas.bibus._3.MonitorService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.MultilingualToken;
import com.cognos.developer.schemas.bibus._3.MultilingualTokenProp;
import com.cognos.developer.schemas.bibus._3.Option;
import com.cognos.developer.schemas.bibus._3.OptionArrayProp;
import com.cognos.developer.schemas.bibus._3.ParameterValue;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.SearchPathSingleObject;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.cognos.developer.schemas.bibus._3.UpdateActionEnum;
import com.cognos.developer.schemas.bibus._3.UpdateOptions;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;


public class Import_Entire_CS {
	
	private ContentManagerService_PortType cmService = null;
	private MonitorService_PortType monitorService = null;
	
	public  static String logFile = "Import.csv";
	private String arguments[] = {"-a", "-i", "-d", "-s", "-u", "-p", "-c", "-g"};
	
	private String strLocale = "en";

	
   	public static void main(String[] args) 
   	{
   		String impDeploymentSpec = new String();
   		String impDeploymentName = new String();
   		String password = null;
		String gateway = "http://localhost:9300/p2pd/servlet/dispatch";
   		String nameSpace= null;
   		String userID = null;
   		String pass = null;
   		
   		int args_len = args.length ;
   		Import_Entire_CS imp = new Import_Entire_CS();
   		
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
	   			if (args[i].compareToIgnoreCase("-i") == 0)  //archive password
	   				password = args[++i];
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
	   							else{
	   								System.out.println("\nArgument not valid: " + args[i]);
	   								imp.displayHelp();
	   							}
	   }// processing args

	   imp.connectToReportServer(gateway);  
		   
	   if (nameSpace != null && userID != null && pass != null)
	   	  imp.quickLogon(nameSpace, userID, pass);	
	   else
	   	  System.out.println("\nNo logon information. Attempting to login as Anonymous user...");

   
		if (impDeploymentSpec != null)     //existing Export Archive 
		{ 
			String oneArchive = ((String) impDeploymentSpec).trim();
			System.out.println("\nImporting Archive = " + oneArchive);
	
			String imported = imp.deployContent(impDeploymentName, impDeploymentSpec, password);
			
		   if (imported == "false")
				System.out.println("Problems occured while importing archive in CM " + impDeploymentSpec);
		   else
		   {
			System.out.println("\n***Import was SUCCESFUL***");  
		   }
	 	}
	 	else
	 		System.out.println("No Valid Archive Name Provided.");

   } //main
		
	// Deployment object to the content store
	private BaseClass[] addArchive( String nameOfArchive)
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

		importDeploy = new ImportDeployment();
		addOpts = new AddOptions();
		importDeploy.setName(multilingualTokenProperty);
		addOpts.setUpdateAction(UpdateActionEnum.replace);
		bca[0] = importDeploy;

		System.out.println( "\nAdded Import Archive:"   + nameOfArchive);
		
		try {
			addedDeploymentObjects = cmService.add(objOfSearchPath,	bca, addOpts);
		} catch (RemoteException remoEx) {
			System.out.println("An error occurred when adding a deployment object:"
							+ "\n" + remoEx.getMessage());
		}
		if ((addedDeploymentObjects != null)&& (addedDeploymentObjects.length > 0)) {
			return addedDeploymentObjects;
		} else {
			return null;
		}
	} //addArchive
	
	
	public Option[] getDeployedOption(String myArchive , String password)
	{
		DeploymentOption[] updatedOption = null;

		try {
			Option[] deployOptEnum = new Option[] {};
			
			DeploymentOption[] opt = new DeploymentOption[1];
	   		DeploymentOptionString archiveEncryptPassword = new DeploymentOptionString();
	   		archiveEncryptPassword.setValue("<credential><password>" + password + "</password></credential>");
	   		archiveEncryptPassword.setName(DeploymentOptionEnum.fromString("archiveEncryptPassword"));
	   		opt[0] = archiveEncryptPassword; 

			deployOptEnum = cmService.getDeploymentOptions(	myArchive, opt);
			
			//overwrite existing import options - user customazation
			for (int i=0; i< deployOptEnum.length; i++ )
			{
				Option oname = deployOptEnum[i];
				DeploymentOption onameS = (DeploymentOption)oname;
				String optionName = onameS.getName().getValue();
				
	   			if (optionName == DeploymentOptionEnum.fromString("dataSourceSelect").toString())
	   			{
	   				((DeploymentOptionBoolean )deployOptEnum[i]).setValue(true);
	   			}
	   			else if (optionName == DeploymentOptionEnum.fromString("dataSourceSignonSelect").toString())
	   			{
	   				((DeploymentOptionBoolean )deployOptEnum[i]).setValue(true);
	   			} 
			}// customize option block
			
			
			updatedOption = new DeploymentOption[deployOptEnum.length + 1];
			updatedOption[0] = opt[0];  // password
	   		
	   		System.arraycopy(deployOptEnum, 0, updatedOption, 1, deployOptEnum.length);
			
		} catch (RemoteException e) {
			System.out.println("An error occurred in getting Deployment options."
							+ "\n" + "The error: " + e.getMessage());
			e.printStackTrace();
		}

		return updatedOption;
	} //getDeployedOption

	
	
	public String deployContent( String strArchiveName, String strArchiveSpec, String exportPassword)
	{
		AsynchReply asynchReply = null;
		String reportEventID = "false";

		String deployPath;
		SearchPathSingleObject searchPathObject = new SearchPathSingleObject();

		// Add an archive name to the content store
		BaseClass[] ArchiveInfo = addArchive( strArchiveName);

		if ((ArchiveInfo != null) && (ArchiveInfo.length == 1)) {
			deployPath = ArchiveInfo[0].getSearchPath().getValue();
			searchPathObject.set_value(deployPath);
			System.out.println("\nDeploy Path: " + deployPath);
		} else {
			return reportEventID;
		}
	
		Option[] myDeploymentOptionsEnum=null;
	
		myDeploymentOptionsEnum = getDeployedOption(strArchiveSpec , exportPassword);
		System.out.println("\nDeployment Options Length = " + myDeploymentOptionsEnum.length);
		
		OptionArrayProp deploymentOptionsArray = new OptionArrayProp();
		deploymentOptionsArray.setValue(myDeploymentOptionsEnum);
		((ImportDeployment) ArchiveInfo[0]).setOptions(deploymentOptionsArray);

		System.out.println("\nFinished Setting Import Options");
		
		try {
			cmService.update(ArchiveInfo, new UpdateOptions());

			asynchReply = getMonitorService().run(searchPathObject,
					new ParameterValue[] {}, myDeploymentOptionsEnum);

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

   	
   	
   	public DeploymentOption[] takeOwnership(DeploymentOption[] opt)
   	{
   		for (int i=0; i< opt.length; i++ )
   			if (opt[i].getName() == DeploymentOptionEnum.fromString("takeOwnership"))
   			{
   				//Change the ownership to the user performing the import. 
   				((DeploymentOptionBoolean )opt[i]).setValue(true);
   			}
   			return opt;
   	}//takeOwnership
   	
   	
   	public void displayImportHistory(String name, String impDeploymentName)
   	{
   		PropEnum props[] = new PropEnum []{PropEnum.defaultName, 
   				PropEnum.searchPath, PropEnum.deployedObjectStatus,
   				PropEnum.objectClass, PropEnum.status, PropEnum.hasMessage,
				PropEnum.deployedObjectClass};

   		String impPath="/adminFolder/importDeployment[@name='"+(impDeploymentName.length() > 0 ? impDeploymentName : name)+"']"  + "//history//*";

   		String msg = "Import started on " +  Calendar.getInstance().getTime().toString() +"\n";
	   	msg += "Importing \"" + name +  "\"";
	   		
  		SearchPathMultipleObject spMulti = new  SearchPathMultipleObject(impPath);
	   	try
	   	{
	   		BaseClass bc[]=cmService.query(spMulti, props, new Sort[] {}, new QueryOptions());
	   		if (bc != null && bc.length > 0)
				for (int i=0; i< bc.length; i++)
				{
		   			if (bc[i].getObjectClass().getValue() == ClassEnum.fromString("deploymentDetail"))
		   			{
		   				DeploymentDetail dd = (DeploymentDetail)bc[i];
		   				//Print messages if any 
		   				if (dd.getMessage() != null)
		   					System.out.println(dd.getMessage().getValue());
		   				if (dd.getDeployedObjectStatus().getValue() != null)
		   					msg += "\n" + dd.getDeployedObjectClass().getValue() + "," + dd.getDefaultName().getValue() + "," +
		   								dd.getDeployedObjectStatus().getValue().getValue();
		   			}
		   		}
	   			writeOutputToFile (logFile, msg);
	   		}
	   		catch (Exception e)
	   		{
	   			printErrorMessage(e);
	   		}
   	}//displayImportHistory

	
	//handle service requests that do not specify new conversation for backwards compatibility
	public MonitorService_PortType getMonitorService() {
		
		return getMonitorService(false, "");
		
	}//getMonitorService
	
	public MonitorService_PortType getMonitorService(boolean isNewConversation, String RSGroup)
	{
		BiBusHeader bibus = null;
		bibus =
			getHeaderObject(((Stub)monitorService).getResponseHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader"), isNewConversation, RSGroup);
		
		if (bibus == null) 
		{
			BiBusHeader CMbibus = null;
			CMbibus =
				getHeaderObject(((Stub)cmService).getResponseHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader"), true, RSGroup);
			
			((Stub)monitorService).setHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", CMbibus);
		}
		else
		{
			((Stub)monitorService).clearHeaders();
			((Stub)monitorService).setHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", bibus);
			
		}
		return monitorService;
	}//getMonitorService
	
	//Use this method when copying headers, such as for requests to services
	public static BiBusHeader getHeaderObject(SOAPHeaderElement SourceHeader, boolean isNewConversation, String RSGroup)
	{
		final String BIBUS_NS = "http://developer.cognos.com/schemas/bibus/3/";
		final String BIBUS_HDR = "biBusHeader";
		final QName BUS_QNAME = new QName(BIBUS_NS, BIBUS_HDR);
		
		if (SourceHeader == null)
			return null;
		
		BiBusHeader bibus = null;
		try {
			bibus = (BiBusHeader)SourceHeader.getValueAsType(BUS_QNAME);
			// Note BUS_QNAME expands to:
			// new QName("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader")
			
			//If the header will be used for a new conversation, clear
			//tracking information, and set routing if supplied (clear if not)
			if (isNewConversation){
				
				bibus.setTracking(null);
				
				//If a Routing Server Group is specified, direct requests to it
				if (RSGroup.length()>0) {
					RoutingInfo routing = new RoutingInfo(RSGroup);
					bibus.setRouting(routing);
				}
				else {
					bibus.setRouting(null);
				}
			}
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		return bibus;
	}//getHeaderObject
	
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
	
	
	//Extracts the error message from the stack trace
	private void printErrorMessage(Exception e)
	{
		AxisFault f = (AxisFault)e;
		String a1 = f.dumpToString();
		int start = a1.indexOf("<messageString>");
		int end = a1.indexOf("</messageString>");
		if (start < end)
		{	
			String message = a1.substring(start+15,end-1);
			System.out.println(message);
		}
		else
			System.out.println(e.getMessage());
	}//printErrorMessage
	
	
	//Write the status of each object to a file
	private void writeOutputToFile (String logFile, String msg)
	{	
		try
		{
			FileOutputStream fos = new FileOutputStream(logFile);
			Writer w = 
			new BufferedWriter(new OutputStreamWriter(fos));
			w.write(msg);
			w.flush();
			w.close();  
			System.out.println("The Import is complete. The details have been saved to a file " + logFile);
		}
		catch (IOException e)
		{
			System.out.println(e);
		}
	}//writeOutputToFile
	
	
	//Checks if a string is one of the arguments
	private boolean isArgument(String p_argument)
	{
		for (int i=0; i<arguments.length; i++)
		{
			if (p_argument.equals(arguments[i]))
				return true;
		}
		return false;
	}
	
	
	//Print usage of the script
	public void printUsage()
	{
		String usage = "\njava Import -a <archiveName> [-i <archivePassword>] [-d deploymentName] [-c package1 package2 ...] [-s <namespaceID> -u <userID> -p <userPassword>] [-g <CognosBIDispatcher>]";
		String example = "Example: \njava Import -a CMarchive -i password -d CMarchive_import -s \"LDAPID\" -u \"User\" -p \"UserPassword\" -g http://server:9300/p2pd/servlet/dispatch";
		
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
		usage += "-a archiveName\n\tThe name of the existing archive\n";
		usage += "-i archivePassword\n\tThe password for the archive. Mandatory for Content Store import.\n";
		usage += "-d deploymentName\n\tThe name of the new import deployment" ;
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


}//Import_Entire_CS

