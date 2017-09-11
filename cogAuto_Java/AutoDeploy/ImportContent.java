package AutoDeploy;

import com.cognos.developer.schemas.bibus._3.*;

import common.CognosLogOn;
import common.ExecutionStep;

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

public class ImportContent extends Deployment implements ExecutionStep{

	public ImportContent() {

	}

	public ImportContent(String environment_string, String archiveName, String importFileName) {
		
		
		archiveName = archiveName;
		setArchiveFile(importFileName);		
		environment = environment_string;
	}

	public static void main(String[] args) {		
		int args_len = args.length;
		String temp1=null,temp2=null,temp3=null;
		if (args.length < 1) {
			// If archiveName is more than one word, put it in quotes, ex. "SDK
			// Import"
			printUsage();
		}
		for (int i = 0; i < args_len; i++) {

			if (args[i].compareToIgnoreCase("-e") == 0) // deploymentName from env
				temp1 = args[++i];								 
				
			else if (args[i].compareToIgnoreCase("-a") == 0 ) //import archive Name (imp_DeploymentSpec)
				temp2 = args[++i];
			else if (args[i].compareToIgnoreCase("-d") == 0) // import File (imp_DeploymentSpec)
				temp3 = args[++i];
			else
				displayHelp();
		} // for
		ImportContent imp = new ImportContent(temp1,temp2,temp3);
		System.out.println("\nExisting Archive: " + temp3);
		System.out.println("\nImporting as: " + temp2);

		if (temp3 != null) // existing Export Archive (
									// "ExportSample_SDK" )
		{
			String imported = imp.deployContent();

			if (imported == "false")
				System.out.println("Problems occured while importing archive in CM " + temp3);
			else {
				System.out.println("\nImport was sucessful");
			}
		} else
			System.out.println("No Valid Archive Name Provided.");

	} // main



	// sn_dg_sdk_method_contentManagerService_getDeploymentOptions_start_0
	public Option[] getDeployedOption(String myArchive) {
		Option[] deployOptEnum = new Option[] {};

		try {
			// deployOptEnum = cmService.getDeploymentOptions( myArchive, opt);
			deployOptEnum = cmService.getDeploymentOptions(myArchive, new Option[] {});
		} catch (RemoteException e) {
			System.out.println(
					"An error occurred in getting Deployment options." + "\n" + "The error: " + e.getMessage());
			e.printStackTrace();
		}

		return deployOptEnum;
	} // getDeployedOption

	public String deployContent() {
		return deployImport(deploySet);
	}

	// Print usage of the script
	public static void printUsage() {
		String usage = "\njava Import -a <archiveName> [-d deploymentName] [-s <namespaceID> -u <userID> -p <userPassword>] [-g <CognosBIDispatcher>]";
		String example = "Example: \njava Import -a CMarchive  -d CMarchive_import -s \"LDAPID\" -u \"User\" -p \"UserPassword\" -g http://server:9300/p2pd/servlet/dispatch";

		System.out.println(usage);
		System.out.println(example);
		displayHelp();
		System.exit(1);
	}

	// Displays help
	public static void displayHelp() {
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

	//receive from outside
	public void init_params(Map map){
		deployType = "import";
		archiveParent = "/adminFolder/adminFolder[@name='Import']";			
		environment =(String) map.get("env");
		archiveName =(String) map.get("archive");
		String archiveFile = (String) map.get("file");	
		setArchiveFile(archiveFile);
	};
	
	@Override
	public void execute(Map map) throws Exception {
		// TODO Auto-generated method stub
		environment =(String) map.get("env");
		logon();	
		init_params(map);
		initOption();		
		deployContent();	
	}
}
