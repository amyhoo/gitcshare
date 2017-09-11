package AutoDeploy;

import com.cognos.developer.schemas.bibus._3.*;

import common.CognosLogOn;

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

public class ImportPartial {
	private ContentManagerService_PortType cmService = null;
	private MonitorService_PortType monitorService = null;
	
	public  static String logFile = "Import.csv";
	private String arguments[] = {"-a", "-i", "-d", "-s", "-u", "-p", "-c", "-g"};
	private Vector<String> packagesVector = new Vector<String>();
	private String [] packages = null;
	private HashMap  packageInformation = null;
	private HashMap selectedPackageNamePath=null;
	private String deployType = "import";
	private String strLocale = "en";
	
	private static final String DEPLOY_OPTION_NAME = "com.cognos.developer.schemas.bibus._3.DeploymentOptionObjectInformationArray";
	
   	public static void main(String[] args) 
   	{
   		String impDeploymentSpec = new String();
   		String impDeploymentName = new String();
   		String password = null;
		String gateway = "http://localhost:9300/p2pd/servlet/dispatch";
   		String nameSpace= null;
   		String userID = null;
   		String pass = null;
   		Option[] deployOptEnum = new Option[] {};
   		
   		int args_len = args.length ;
   		ImportPartial imp = new ImportPartial();
   		
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
	   							if (args[i].compareToIgnoreCase("-c") == 0)
	   							{	
	   								while (++i < args.length && !imp.isArgument(args[i]))
	   									imp.addPackages(args[i]);
	   								i--;
	   							}
	   							else
	   								imp.displayHelp();
	   }

	   imp.connectToReportServer(gateway);  // setting cmService
		   
	   if (nameSpace != null && userID != null && pass != null)
	   	  imp.quickLogon(nameSpace, userID, pass);	
	   else
	   	  System.out.println("No logon information. Attempting to login as Anonymous user...");

	   
	   System.out.println("\nExisting Archive: " + impDeploymentSpec );
	   System.out.println("\nImporting as: " + impDeploymentName );

	   
		if (impDeploymentSpec != null)     //existing Export Archive ( "ExportSample_SDK" )
		{ 
			
			imp.packages = imp.getPackages();  // return specified input folders as String[]
			if((imp.packages).length == 0)
			{
				System.out.println("\nYou must enter a subset of folders to import from archive");
				imp.printUsage();
			}
				
			String imported = imp.deployContent(impDeploymentName, imp.packages, impDeploymentSpec);
			
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
   	
		
	//Add to packages to be imported
	public void addPackages(String p_package)
	{
		packagesVector.add(p_package);
	}
	

	public String[] getPackages()
	{
		int pacSize = packagesVector.size();
		if ( pacSize > 0  )
		{
			packages = new String [packagesVector.size()];
			packages = packagesVector.toArray( new String[pacSize]);
		}
		return packages;
	}//getPackages
	
	
  	
	// return all the folder content associated with  specific archive
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
			deployOptEnum = cmService.getDeploymentOptions(	myArchive, new Option[] {});  
    
		} catch (RemoteException e) {
			System.out
					.println("An error occurred in getting Deployment options."
							+ "\n" + "The error: " + e.getMessage());
			e.printStackTrace();
		}

		return deployOptEnum;
	} //getDeployedOption


	
	public String deployContent( String strNewImportName, String[] selectedFolders, String strDeployedArchive)
	{
		AsynchReply asynchReply = null;
		String reportEventID = "false";

		String deployPath;
		SearchPathSingleObject searchPathObject = new SearchPathSingleObject();

		// Add an archive name to the content store
		BaseClass[] ArchiveInfo = addArchive(deployType, strNewImportName);
		
		if ((ArchiveInfo != null) && (ArchiveInfo.length == 1)) {
			deployPath = ArchiveInfo[0].getSearchPath().getValue();
			searchPathObject.set_value(deployPath);
			System.out.println("\nImport archive Created: " + deployPath);
		} else {
			return reportEventID;
		}

		String oneArchive = ((String) strDeployedArchive).trim();
		selectedPackageNamePath = getPubFolderContent(oneArchive);  // get folders from  Archive
			
		Option[] myDeploymentOptionsEnum=null;

		myDeploymentOptionsEnum = setDeploymentOptionEnum(strDeployedArchive, selectedFolders);


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
	
   	//This method gets and sets all deploymentOptions from the archive with provided archive password
   	//In this script the deployment options are not changed.
   	public DeploymentOption[] deploymentOptions(String pPackage, String pPassword)
   	{
   		DeploymentOption[] opt = new DeploymentOption[1];
   		DeploymentOptionArrayProp options = new DeploymentOptionArrayProp(null,opt);
   		DeploymentOptionString archiveEncryptPassword = new DeploymentOptionString();
   		archiveEncryptPassword.setValue("<credential><password>" + pPassword + "</password></credential>");
   		archiveEncryptPassword.setName(DeploymentOptionEnum.fromString("archiveEncryptPassword"));
   		opt[0] = archiveEncryptPassword;

   		options.setValue(opt);
   		try
   		{
   			// setup the deployment options.
   			opt = (DeploymentOption[])getDO(pPackage, options);
   			opt = takeOwnership(opt);
   			DeploymentOption optNew[] = new DeploymentOption[opt.length + 1];
   			for (int i=0; i< opt.length; i++ )
   				optNew[i] = opt[i];
   			
   			optNew[opt.length] = archiveEncryptPassword;
   			return optNew;
   		}
   		catch (Exception e)
   		{
   			printErrorMessage(e);
   			return opt;
   		}
   	}//deploymentOptions

   	
   	private Option[] getDO(String p_archive, DeploymentOptionArrayProp p_opt)
   	{
   		try
   		{
   			Option opt[] = new Option[]{};
   			opt = p_opt.getValue();
   			
   			opt = cmService.getDeploymentOptions(p_archive, opt);
	   		DeploymentOption[] optNew = new DeploymentOption[opt.length + 1] ;
	   		DeploymentOptionAuditLevel recordingLevel = new DeploymentOptionAuditLevel();
   	   		recordingLevel.setName(DeploymentOptionEnum.fromString("recordingLevel"));
   	   		recordingLevel.setValue(AuditLevelEnum.full);
   	   		optNew[0] = recordingLevel;
	   		
	   		System.arraycopy(opt, 0, optNew, 1, opt.length);
   			return optNew ;
   		}
   		catch (Exception e)
   		{
   			printErrorMessage(e);
   			return null;
   		}
   	}//getDO
   	
   	public DeploymentOption[] takeOwnership(DeploymentOption[] opt)
   	{
   		for (int i=0; i< opt.length; i++ )
   			if (opt[i].getName() == DeploymentOptionEnum.fromString("takeOwnership"))
   			{
   				//Change the ownership to the user performing the import. Otherwise  
   				//there will be errors
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

   	//Import the archive
	public String importPackageXX(String name, String password, String importDeploymentName) 
	{		
		String impPath="/adminFolder/importDeployment[@name='"+(importDeploymentName.length() > 0 ? importDeploymentName : name)+"']";
		String eventID = null;
		try
		{
			//The timeout has to be increased when the system is running out of resources
			org.apache.axis.client.Stub s = (org.apache.axis.client.Stub) cmService;
			s.setTimeout(0);  // set to not timeout
			
			System.out.println("Importing archive " + name + "... Please wait...");
	
			SearchPathSingleObject spSingle = new SearchPathSingleObject(impPath);
			// See DCF 1375609 for details regarding this change
			AsynchReply reply = monitorService.run(spSingle, new ParameterValue[]{}, new Option[] {});
			if (!reply.getStatus().equals(AsynchReplyStatusEnum.conversationComplete)) 	
			{
			    while (!reply.getStatus().equals(AsynchReplyStatusEnum.conversationComplete)) 
			    {
			    	reply = getMonitorService().wait(reply.getPrimaryRequest(), new ParameterValue[]{},new Option[] {});
			    }
			}
			if (reply.getStatus().getValue().equals("conversationComplete") || 
					reply.getStatus().getValue().equals("complete") )
				 eventID = ((AsynchDetailEventID)reply.getDetails()[0]).getEventID();
		}
		catch (Exception e)
		{
			printErrorMessage(e);
		}
		return eventID;
	}//importPackage
	
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
	
	//This method selects and inserts an archive in CM from all available .zip files
	//in <cognos_install>\deployment if using default configuration
/*	public String insertNewImport(String importName, String password, String importDeploymentName)
	{
		try 
		{
			SearchPathMultipleObject spMulti = new SearchPathMultipleObject();
			spMulti.set_value("/adminFolder");
			
			PropEnum props[] = new PropEnum[]{ PropEnum.searchPath,PropEnum.policies };
			BaseClass importDeplFolder[] = cmService.query(spMulti,props, new Sort[]{}, new QueryOptions());
			
			ImportDeployment newImport = new ImportDeployment();
			
			TokenProp tp = new TokenProp();
			tp.setValue(importDeploymentName.length() > 0 ? importDeploymentName : importName);
			newImport.setDefaultName(tp);
			
			BaseClassArrayProp parent = new BaseClassArrayProp();
			parent.setValue(importDeplFolder);
			newImport.setParent(parent);
		
			if (password != null && password.length() >= 1)
				doap.setValue(deploymentOptions(importName, password));
			else
				doap.setValue(deploymentOptions(importName));
				
			newImport.setDeploymentOptions(doap);
					
			String importPath = "/adminFolder";

			AddOptions ao = new AddOptions();
			ao.setUpdateAction(UpdateActionEnum.replace); //replace if already exists
			
			SearchPathSingleObject spSingle = new SearchPathSingleObject(importPath);
			
			BaseClass archive[] = cmService.add(spSingle,new BaseClass[]{newImport},ao);
			if (archive == null || archive.length <= 0)
				System.out.println("No Import was added to the Content Store.");
			
			 return archive[0].getSearchPath().getValue();
		}
		catch( Exception ex) 
		{
			printErrorMessage(ex);
			return null;
		}
	}  //insertNewImport 
	*/
	
	
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
		usage += "-a archiveName\n\tThe name of the new archive\n";
		usage += "-i archivePassword\n\tThe password for the archive. Mandatory for Content Store import.\n";
		usage += "-d deploymentName\n\tThe name of the new import deployment (optional. Can be used to resolve naming conflicts with existing export deployments)\n";
		usage += "-c package1 [package2]\n\tPackages to be imported\n";
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
		
		
	

	//set up your deployment deployment options
	private Option[] setDeploymentOptionEnum(String nameOfArchive,String[] ListOfFolders )
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
				
		eOptionCount=2;  // for import
		deploymentOptions = new DeploymentOption[eOptionCount + deployOptionEnumBoolean.length
		                                         + deployOptionEnumResolution.length];

		// Define the deployment ( import) options

        deploymentOptions[num]   = this.setImportDeploymentOptionPackageInfo(ListOfFolders);

//same as in Export
		deploymentOptions[++num] = this.setDeploymentOptionString(nameOfArchive);
		deploymentOptions[++num] = this.setArchiveOverWrite(true);
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
/*// use default value
		deploymentOptions[++num] = this.setDataSourceSelect(false);  // default 
		deploymentOptions[++num] = this.setNameSpaceSelect(false);  // default
		deploymentOptions[++num] = this.setNameSpaceThirdParty(true);  // default
		deploymentOptions[++num] = this.setObjectPolicies(false);    //default
		deploymentOptions[++num] = this.setPackageHistories(false);   //default
		deploymentOptions[++num] = this.setPackageOutputs(false);   //default
		deploymentOptions[++num] = this.setPackageSchedules(false);   //default
		deploymentOptions[++num] = this.setPackageSelect(true);    //default
		deploymentOptions[++num] = this.setRecipientsSelect(false); //default
		deploymentOptions[++num] = this.setTakeOwnership(true);  // default is false
		deploymentOptions[++num] = this.setDataSourceConflictResolution(true);
		deploymentOptions[++num] = this.setNamespaceConflictResolution(true);
		deploymentOptions[++num] = this.setObjectPoliciesConflictResolution(true);
		deploymentOptions[++num] = this.setOwnershipConflictResolution(true);
		deploymentOptions[++num] = this.setPackageHistoriesConflictResolution(true);
		deploymentOptions[++num] = this.setPackageOutputsConflictResolution(true);
		deploymentOptions[++num] = this.setPackageSchedulesConflictResolution(true);
		deploymentOptions[++num] = this.setRecipientsConflictResolution(true);
*/
		return deploymentOptions;
	} //setDeploymentOptionEnum

	

	
	//set import deployment option property (mandatory)
	private DeploymentOptionImportRuleArray setImportDeploymentOptionPackageInfo(String[] arrOfFolders) {
		DeploymentImportRule[] pkgDeployInfoArr = new DeploymentImportRule[arrOfFolders.length];
		DeploymentImportRule pkgDeployInfo;
		MultilingualToken[] multilingualTokenArr;
		MultilingualToken multilingualToken;
		SearchPathSingleObject packSearchPath=null;

		for (int i = 0; i < arrOfFolders.length; i++) {
			multilingualToken = new MultilingualToken();
			multilingualTokenArr = new MultilingualToken[1];

			pkgDeployInfo = new DeploymentImportRule();
			multilingualToken.setLocale(strLocale);
			multilingualToken.setValue(arrOfFolders[i]);
			multilingualTokenArr[0] = multilingualToken;
			String myPackageName=arrOfFolders[i];
			HashMap myPackInfo=new HashMap(packageInformation);

			if (myPackInfo.containsKey(myPackageName))
			{
				packSearchPath=(SearchPathSingleObject)myPackInfo.get(myPackageName);
			}

		    pkgDeployInfo.setArchiveSearchPath(packSearchPath);
			pkgDeployInfo.setName(multilingualTokenArr);
			pkgDeployInfo.setParent(new SearchPathSingleObject("/content"));
			pkgDeployInfoArr[i] = pkgDeployInfo;
		}

		DeploymentOptionImportRuleArray deployOptionPkgInfo = new DeploymentOptionImportRuleArray();
		deployOptionPkgInfo.setName(DeploymentOptionEnum.fromString("import"));
		deployOptionPkgInfo.setValue(pkgDeployInfoArr);

		return deployOptionPkgInfo;
	}//setImportDeploymentOptionPackageInfo



	private DeploymentOptionBoolean setEntireContentStoreSelect(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("entireContentStoreSelect"));
		deployOptionBool.setValue(setValue);
		
		return deployOptionBool;
	}//setEntireContentStoreSelect
	
	
	private DeploymentOptionAnyType  setArchiveEncryptPassword( String pPassword) { 
		DeploymentOptionAnyType archiveEncryptPassword  = null;
		if (pPassword != null && pPassword.length() >= 1) {
			archiveEncryptPassword = new DeploymentOptionAnyType();
			archiveEncryptPassword.setValue("<credential><password>" + pPassword + "</password></credential>");
			archiveEncryptPassword.setName(DeploymentOptionEnum.fromString("archiveEncryptPassword"));
		}
		return archiveEncryptPassword;
	}//setArchiveEncryptPassword

	private DeploymentOptionBoolean setArchiveOverWrite(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("archiveOverwrite"));
		deployOptionBool.setValue(setValue);
		
		return deployOptionBool;
	}//setArchiveOverWrite
	

	// allow the deployment overwrites the archive
	private DeploymentOptionBoolean setPersonalDataSelect(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("personalDataSelect"));
		deployOptionBool.setValue(setValue);

		return deployOptionBool;
	}//setPersonalDataSelect
	
	// set DeploymentOptionString property (mandatory)
	private DeploymentOptionString setDeploymentOptionString(String archiveName) {
		MultilingualString archiveDefault = new MultilingualString();
		archiveDefault.setLocale(strLocale);
		archiveDefault.setValue(archiveName);

		DeploymentOptionString deployOptionStr = new DeploymentOptionString();
		deployOptionStr.setName(DeploymentOptionEnum.fromString("archive"));
		deployOptionStr.setValue(archiveDefault.getValue());

		return deployOptionStr;
	}//setDeploymentOptionString


	// set dataSourceSelect as default value - 'false'
	private DeploymentOptionBoolean setDataSourceSelect(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("dataSourceSelect"));
		deployOptionBool.setValue(setValue);
		
		return deployOptionBool;
	}//setDataSourceSelect
	

	// set namespaceSelect as default value - 'false'
	private DeploymentOptionBoolean setNameSpaceSelect(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("namespaceSelect"));
		deployOptionBool.setValue(setValue);

		return deployOptionBool;
	}//setNameSpaceSelect
	

	// Not include references to external namespaces - value is false
	private DeploymentOptionBoolean setNameSpaceThirdParty(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("namespaceThirdParty"));
		deployOptionBool.setValue(setValue);

		return deployOptionBool;
	}//setNameSpaceThirdParty
	

	// set objectPolicies as default value - 'false'
	private DeploymentOptionBoolean setObjectPolicies(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("objectPolicies"));
		deployOptionBool.setValue(setValue);

		return deployOptionBool;
	}//setObjectPolicies

	
	// set packageHistories as default value - 'false'
	private DeploymentOptionBoolean setPackageHistories(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("packageHistories"));
		deployOptionBool.setValue(setValue);
		return deployOptionBool;
	}//setPackageHistories

	
	// set packageOutputs as default value - 'false'
	private DeploymentOptionBoolean setPackageOutputs(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("packageOutputs"));
		deployOptionBool.setValue(setValue);
		
		return deployOptionBool;
	}//setPackageOutputs
	

	// set packageSchedules as default value - 'false'
	private DeploymentOptionBoolean setPackageSchedules(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("packageSchedules"));
		deployOptionBool.setValue(setValue);
		
		return deployOptionBool;
	}//setPackageSchedules
	

	// set packageSelect as default value - 'true'
	private DeploymentOptionBoolean setPackageSelect(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("packageSelect"));
		deployOptionBool.setValue(setValue);

		return deployOptionBool;
	}//setPackageSelect
	

	// set recipientsSelect as default value - 'false'
	private DeploymentOptionBoolean setRecipientsSelect(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("recipientsSelect"));
		deployOptionBool.setValue(setValue);

		return deployOptionBool;
	}//setRecipientsSelect
	

	// set the owner to the owner from the source - the value is 'true'
	private DeploymentOptionBoolean setTakeOwnership(boolean setValue) {
		DeploymentOptionBoolean deployOptionBool = new DeploymentOptionBoolean();
		deployOptionBool.setName(DeploymentOptionEnum.fromString("takeOwnership"));
		deployOptionBool.setValue(setValue);

		return deployOptionBool;
	}//setTakeOwnership
	

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


}
