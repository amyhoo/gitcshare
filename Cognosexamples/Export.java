
 /* 
 * Licensed Materials - Property of IBM
 * IBM Cognos Products: SDK Support
 * (C) Copyright IBM Corp. 2003, 2013
 * US Government Users Restricted Rights - Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
*/

/*
 * Export.java
 * Description: Technote 1338960 - SDK Sample to export or import a deployment
 * Tested with:  IBM Cognos BI 10.2.0, IBM Java 1.6, Axis 1.4
*/


import com.cognos.developer.schemas.bibus._3.*;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import javax.xml.namespace.QName;

//import com.cognos.developer.schemas.bibus._3.holders.*;
import org.apache.axis.AxisFault;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.io.*;
import java.util.*;

import javax.xml.rpc.ServiceException;

public class Export {
	
    private ContentManagerService_PortType cmService = null;
    private MonitorService_PortType mService = null;
    
	public  static String logFile = "Export.csv";
	String arguments[] = {"-a", "-i", "-s", "-u", "-p", "-c", "-g"};
	private Vector<String> packages = new Vector<String>();
	
	public static void main(String[] args) 
   	{
		Export exp = new Export();
		
   		if (args.length < 1)
   		{
   			//If archiveName is more than one word, put it in quotes, ex. "SDK Export"
   			exp.printUsage();
   		}
   		
   		String expDeploymentSpec = null;
   		String password = null;
   		String gateway = "http://localhost:9300/p2pd/servlet/dispatch";
   		String nameSpace= null;
   		String userID = null;
   		String pass = null;
   		
   		int args_len = args.length ;
   	
	   for (int i=0; i<args_len; i++)
	   {
	   		if (args[i].compareToIgnoreCase("-s") == 0)
	   			nameSpace = args[++i];
	   		else 
	   			if (args[i].compareToIgnoreCase("-i") == 0)
	   				password = args[++i];
	   			else
		   			if (args[i].compareToIgnoreCase("-u") == 0)
		   				userID = args[++i];
		   			else
		   				if (args[i].compareToIgnoreCase("-p") == 0)
		   					pass = args[++i];
		   				else
		   					if (args[i].compareToIgnoreCase("-a") == 0)
		   						expDeploymentSpec =  args[++i];	   
		   					else
		   						if (args[i].compareToIgnoreCase("-g") == 0)
		   							gateway =  args[++i];	   	
		   						else
		   							if (args[i].compareToIgnoreCase("-c") == 0)
		   							{	
		   								while (++i < args.length && !exp.isArgument(args[i]))
		   									exp.addPackages(args[i]);
		   								i--;
		   							}
	   }
	   exp.connectToReportServer(gateway);  
	   
	   //Don't try to login if using Anonymous
	   if (nameSpace != null && userID != null && pass != null)
	   	exp.quickLogon(nameSpace, userID, pass);
	   else
	   	System.out.println("Missing logon information. Attempting to login as Anonymous user...");
	
	   //If no name for the archive is provided, set it to the current date
	   if (expDeploymentSpec == "" || expDeploymentSpec == null)
	   {	
	   	 Calendar currentDate = Calendar.getInstance();
		   expDeploymentSpec = currentDate.getTime().toString();
		   //replace(':', '.') The zip file cannot contain :  
		   expDeploymentSpec = expDeploymentSpec.replace(':','.');   
	   }
	   String impPath = null;
	 	if (expDeploymentSpec != null)
	 		if (password != null || !exp.getPackages().isEmpty()) 
	 		   impPath = exp.createNewExport(expDeploymentSpec, password);
	 		else
	 			System.out.println("You must provide a password to create CM deployment archive \"t\".");
	 	else
	 		System.out.println("No Valid Archive Name Provided.");
	 	
	 	if (impPath != null && impPath != "")
	 	{
	 		exp.printExportHistory(expDeploymentSpec);
	 	}
	 	else
	 		System.out.println("Export history is empty (no eventID). Check for errors.");
	 	
    }
	
	//Add to packages to be exported
	public void addPackages(String p_package)
	{
		packages.add(p_package);
	}
	
	//Returns all the packages to be exported
	public Vector<String> getPackages()
	{
		return packages;
	}

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
   	  	
   	//This method gets and sets all deploymentOptions from the archive with provided archive password
   	//In this script the deployment options are not changed.
   	public DeploymentOption[] deploymentOptionsCS(String pPackage, String pPassword)
   	{
   		int i = 7; //number of deployment options
   		DeploymentOption[] opt = new DeploymentOption[i];
   		DeploymentOptionArrayProp options = new DeploymentOptionArrayProp(null, opt);
   		if (pPassword != null && pPassword.length() >= 1)
   		{	
	   		DeploymentOptionString archiveEncryptPassword = new DeploymentOptionString();
	   		archiveEncryptPassword.setValue("<credential><password>" + pPassword + "</password></credential>");
	   		archiveEncryptPassword.setName(DeploymentOptionEnum.fromString("archiveEncryptPassword"));
	   		opt[--i] = archiveEncryptPassword;
   		}
   		else
   		{
   			this.printUsage();
   			System.exit(1);
   		}
   		
   		DeploymentOptionBoolean overwrite = new DeploymentOptionBoolean();
   		overwrite.setName(DeploymentOptionEnum.fromString("archiveOverwrite"));
   		overwrite.setValue(true);
   		opt[--i] = overwrite;
   		
   		DeploymentOptionBoolean entireCSSelect = new DeploymentOptionBoolean();
   		entireCSSelect.setName(DeploymentOptionEnum.fromString("entireContentStoreSelect"));
   		entireCSSelect.setValue(true);
   		opt[--i] = entireCSSelect;
   		
   		DeploymentOptionString arch = new DeploymentOptionString();
   		arch.setName(DeploymentOptionEnum.fromString("archive"));
   		arch.setValue(pPackage);
   		opt[--i] = arch;
   		
   		DeploymentOptionBoolean personalData = new DeploymentOptionBoolean();
   		personalData.setName(DeploymentOptionEnum.fromString("personalDataSelect"));
   		personalData.setValue(true);
   		opt[--i] = personalData;
   		
   		DeploymentOptionAuditLevel recordingLevel = new DeploymentOptionAuditLevel();
   		recordingLevel.setName(DeploymentOptionEnum.fromString("recordingLevel"));
   		recordingLevel.setValue(AuditLevelEnum.full);
   		opt[--i] = recordingLevel;
   		
   		MultilingualString deploymentName = new MultilingualString();
   		deploymentName.setLocale("en-us");
   		deploymentName.setValue(pPackage);
   		DeploymentOptionMultilingualString doms = new DeploymentOptionMultilingualString();
   		doms.setName(DeploymentOptionEnum.fromString("deploymentName"));
   		doms.setValue(new MultilingualString[] {deploymentName});
   		opt[--i] = doms;

   		options.setValue(opt);
   		return opt;
   	}
   	
   	
   	//This method gets and sets all deploymentOptions from the archive without a password
   	//In this script the deployment options are not changed.
   	public DeploymentOption[] deploymentOptionsPackages(String pArchiveName, String pPassword)
   	{
   		int i=6;  //number of deployment options to be set
   		if (pPassword != null && pPassword.length() >= 1)
   			i++;
   		
   		DeploymentOption[] opt = new DeploymentOption[i];
   		DeploymentOptionArrayProp options = new DeploymentOptionArrayProp();
   		String archive = pArchiveName;
   		   		
   		try
   		{ 		
   			//Set the name of the archive
   			DeploymentOptionString doStr = new DeploymentOptionString();
   			doStr.setName(DeploymentOptionEnum.fromString("archive"));
   			doStr.setValue(archive);
   			opt[--i] = doStr;
   			
   			//Set the password if used
   			if (pPassword != null && pPassword.length() >= 1)
   			{	
	   			DeploymentOptionString archiveEncryptPassword = new DeploymentOptionString();
	   			archiveEncryptPassword.setValue("<credential><password>" + pPassword + "</password></credential>");
	   			archiveEncryptPassword.setName(DeploymentOptionEnum.fromString("archiveEncryptPassword"));
	   			opt[--i] = archiveEncryptPassword;
   			}
   			
   			MultilingualString deploymentName = new MultilingualString();
   			deploymentName.setLocale("en-us");
   			deploymentName.setValue(archive);
   			
   			DeploymentOptionMultilingualString doms = new DeploymentOptionMultilingualString();
   			doms.setName(DeploymentOptionEnum.fromString("deploymentName"));
   			doms.setValue(new MultilingualString[] {deploymentName});
   			opt[--i] = doms;
   				   			
   			int j = packages.size();
   			DeploymentOptionPackageInfo pack = new DeploymentOptionPackageInfo();
   			PackageDeploymentInfo pDeplInfo[] = new  PackageDeploymentInfo[j];
   			
   			for (int k=0; k<j; k++)
   			{	
   				MultilingualToken mt[] = new MultilingualToken[1];
	   			mt[0] = new  MultilingualToken();
	   			mt[0].setLocale("en");
	   			mt[0].setValue(packages.get(k).toString());
	   			pDeplInfo[k] = new PackageDeploymentInfo();
	   			pDeplInfo[k].setSourceName(mt);
	   			pDeplInfo[k].setTargetName(mt);
	   			pDeplInfo[k].setEnabled(true);  
   			}
   			//will not change the target name. Keep by default
   			pack.setName(DeploymentOptionEnum.fromString("package"));
   			pack.setValue(pDeplInfo);
   			opt[--i] = pack;
   		
   			DeploymentOptionBoolean overwrite = new DeploymentOptionBoolean();
   			overwrite.setName(DeploymentOptionEnum.fromString("archiveOverwrite"));
   			overwrite.setValue(true);
   			opt[--i] = overwrite;
   			
   			DeploymentOptionBoolean personalData = new DeploymentOptionBoolean();
   			personalData.setName(DeploymentOptionEnum.fromString("personalDataSelect"));
   			personalData.setValue(true);
   			opt[--i] = personalData;
   			
   	   		DeploymentOptionAuditLevel recordingLevel = new DeploymentOptionAuditLevel();
   	   		recordingLevel.setName(DeploymentOptionEnum.fromString("recordingLevel"));
   	   		recordingLevel.setValue(AuditLevelEnum.full);
   	   		opt[--i] = recordingLevel;

   			
   			options.setValue(opt);
   			return opt;

   		}
   		catch (Exception e)
   		{
   			e.printStackTrace();
   			return opt;
   		}
   	}
   	
  	//Extracts the report history to be written to a file
   	public void printExportHistory(String name)
   	{
   		if (name != null)
   		{	
   	  		String expPath="/adminFolder/exportDeployment[@name='"+name+"']"  + "//history//*";

   			String msg = "Export started on " + Calendar.getInstance().getTime().toString() +"\n";
   			msg += "Exporting \"" + name + "\"";
   			PropEnum props[] = new PropEnum []{PropEnum.defaultName, 
	   				PropEnum.searchPath, PropEnum.deployedObjectStatus,
	   				PropEnum.objectClass, PropEnum.deployedObjectClass };
   			SearchPathMultipleObject spMulti = new SearchPathMultipleObject(expPath);
   				
   	  		try
	   		{
	   			BaseClass bc[]=cmService.query(spMulti, props, new Sort[] {}, new QueryOptions());
	   			if (bc != null && bc.length > 0)
	   			{
		   			for (int i=0; i< bc.length; i++)
		   			{
		   				if (bc[i] instanceof DeploymentDetail)
		   				{
		   					DeploymentDetail dd = (DeploymentDetail)bc[i];
		   				
		   					if (dd.getDeployedObjectStatus().getValue() != null)
		   					{
		   						msg += "\n" + dd.getDeployedObjectClass().getValue()+"," + dd.getDefaultName().getValue() + "," +
		   						dd.getDeployedObjectStatus().getValue().getValue();
		   					}
		   				}
		   			}
		   			writeOutputToFile (logFile, msg);
	   			}
	   		}
	   		catch (IOException e)
	   		{
	   			System.out.println(e);
	   		}
	   		catch (Exception e)
	   		{
	   			e.printStackTrace();
	   		}
   		}
   	}

   	//Writes the reportHistory to a file 
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
   			System.out.println("The Export is complete. The details have been saved to a file " + logFile);
   		}
   		catch (IOException e)
   		{
   			System.out.println(e);
   		}
   	 }
	
	//This method selects and inserts an archive in CM from all available .zip files
	//in <cognos_install>\deployment if using default configuration
	public String createNewExport(String exportName, String password)
	{
		String eventID = "";
		ExportDeployment ed = new ExportDeployment();
		
		MultilingualTokenProp mtp = new MultilingualTokenProp();
		MultilingualToken mt[] = new  MultilingualToken[1];
		mt[0] = new MultilingualToken();
		mt[0].setValue(exportName);
		mt[0].setLocale("en");
		mtp.setValue(mt);
		ed.setName(mtp);
		
		DeploymentOption dop[];

		if (packages != null && packages.size() > 0)
			dop = deploymentOptionsPackages(exportName, password);
		else
			dop = deploymentOptionsCS(exportName, password);
		
		DeploymentOptionArrayProp doap = new DeploymentOptionArrayProp();
		doap.setValue(dop);
		ed.setDeploymentOptions(doap);
		
		String exportPath = "/adminFolder/exportDeployment[@name='"+ exportName + "']"; 
	
		 AddOptions ao = new AddOptions();
		 ao.setUpdateAction(UpdateActionEnum.replace); //replace if already exists
		 ao.setReturnProperties(new PropEnum[]{PropEnum.searchPath});
		 SearchPathSingleObject spSingle = new SearchPathSingleObject("/adminFolder");
		 try
		 {
		 	//System.out.println("Adding deployment to the Content Store ... ");
		 	BaseClass archive[] = cmService.add(spSingle,new BaseClass[]{ed},ao);
		 	if (archive == null || archive.length <=0)
		 		System.out.println("No Export was added to the Content Store.");
		 }
		 catch (RemoteException e)
		 {
		 	printErrorMessage(e);
		 	System.exit(1);
		 }
		System.out.println("Exporting " + exportName + " ... " );
		spSingle.set_value(exportPath);
		AsynchReply res = null;
		
		try
		{
			//handle the replacement issue
			res = mService.run(spSingle,new ParameterValue[]{},dop);
			if (!res.getStatus().equals(AsynchReplyStatusEnum.conversationComplete)) 	
			{
			    while (!res.getStatus().equals(AsynchReplyStatusEnum.conversationComplete)) 
			    {
			    	res = getMonitorService().wait(res.getPrimaryRequest(), new ParameterValue[]{},new Option[] {});
			    }
			}
			if (res.getStatus().getValue().equals("conversationComplete") || 
					res.getStatus().getValue().equals("complete") )
				 eventID = ((AsynchDetailEventID)res.getDetails()[0]).getEventID();
		}
		catch (Exception e)
		{
			//printErrorMessage(e);
			e.printStackTrace();
		}
		return eventID;
	}
	
	//handle service requests that do not specify new conversation for backwards compatibility
	public MonitorService_PortType getMonitorService() {
		
		return getMonitorService(false, "");
		
	}
	
	public MonitorService_PortType getMonitorService(boolean isNewConversation, String RSGroup)
	{
		BiBusHeader bibus = null;
		bibus =
			getHeaderObject(((Stub)mService).getResponseHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader"), isNewConversation, RSGroup);
		
		if (bibus == null) 
		{
			BiBusHeader CMbibus = null;
			CMbibus =
				getHeaderObject(((Stub)cmService).getResponseHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader"), true, RSGroup);
			
			((Stub)mService).setHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", CMbibus);
		}
		else
		{
			((Stub)mService).clearHeaders();
			((Stub)mService).setHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", bibus);
			
		}
		return mService;
	}
	
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
	}
	
	//Prints Usage if not enough parameters passed
	public void printUsage()
	{
		String usage = "\njava Export -a <archiveName> [-i <archivePassword>] [-c package1 package2 ...] [-s <namespaceID> -u <userID> -p <userPassword>] [-g <CognosBIDispatcher>]";
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
		usage += "-c package1 [package2]\n\tPackages to be exported\n";
		usage += "-s namespaceID\n\tNamespaceID the user belongs to.\n";
		usage += "-u userID\n\tUserID of a System Administrator.\n";
		usage += "-p userPassword\n\tPassword for the UserID.\n";
		usage += "-g CognosBIDispatcher\n\tDispatcher URL for Cognos BI.\n";
		usage += "\t Default: http://localhost:9300/p2pd/servlet/dispatch\n";
			
		System.out.println(usage);
		System.exit(1);
		
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
		MonitorService_ServiceLocator  mServiceLocator = new MonitorService_ServiceLocator();
	    try 
	    {
			cmService = cmServiceLocator.getcontentManagerService(new java.net.URL(endPoint));
			mService = mServiceLocator.getmonitorService(new java.net.URL(endPoint));
//			 set the Axis request timeout

			((Stub) cmService).setTimeout(0); // in milliseconds, 0 turns the timeout off
			((Stub)mService).setTimeout(0); // in milliseconds, 0 turns the timeout off

			
		} 
	    catch (MalformedURLException e) 
		{
			e.printStackTrace();
		} 
		catch (ServiceException e) 
		{
			e.printStackTrace();
		}
	}

	//Extracts the error message from the AxisFault
	private void printErrorMessage(Exception e)
	{
		AxisFault f = (AxisFault)e;
		String a1 = f.dumpToString();
		int start = a1.indexOf("<messageString>");
		int end = a1.indexOf("</messageString>");
			String message = a1.substring(start+15,end-1);
			System.out.println(message);
		
	}
}

