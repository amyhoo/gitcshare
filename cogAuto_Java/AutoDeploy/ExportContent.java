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

public class ExportContent extends Deployment implements ExecutionStep {
	
	public ExportContent() {

	}

	public ExportContent(String environment_string, String archive, String[] folder_list) {
		environment = environment_string;
		archiveName = archive;
		setExportFolders(folder_list);
	}
	
	public void deployContent(){
		deployExport(deploySet);
	}
	
	public static void main(String[] args) {
		
		String expArchiveName=null;
		String env=null;
		ArrayList<String> temp = new ArrayList<String>();
		// process the argsl
		for (int i = 0; i < args.length; i++) {
			if (args[i].compareToIgnoreCase("-n") == 0)
				expArchiveName = args[++i];
			else if (args[i].compareToIgnoreCase("-e") == 0)
				env = args[++i];
			else
				temp.add(args[i]);
		}
		String[] folders = (String[]) temp.toArray(new String[temp.size()]);
		ExportContent exp = new ExportContent(env,expArchiveName,folders);
		// If no name for the archive is provided, set it to the current date
		if (expArchiveName == "" || expArchiveName == null) {
			System.out.println("Invalid Archive Name Provided.");
		}
		else
		{
			exp.deployContent();
		}

	} // main
	
	//receive from outside
	public void init_params(Map map){
		deployType = "export";
		archiveParent = "/adminFolder/adminFolder[@name='Export']";		
		environment =(String) map.get("env");
		archiveName =(String) map.get("archive");
        List list = (List) map.get("path");
        String[] folders = (String[])list.toArray(new String[list.size()]);    
		setExportFolders(folders);
		HashMap<String,String> foldersAndParent = new HashMap<String,String> ();
		if (map.containsKey("parents")){
			String[] parents=(String[])map.get("parents");
			for (int i = 0; i < parents.length; i++ ){
				foldersAndParent.put(folders[i], parents[i]);				
			}
		}
		else {
			for (int i = 0; i < folders.length; i++ ){
				//BaseClass objToExport = getCSObject(folders[i])[0];
				//String parentFolder=objToExport.getParent().getValue()[0].getSearchPath().getValue().toString();
				String parentFolder=getParentPath(folders[i]);
				foldersAndParent.put(folders[i], parentFolder);				
			}
		}
		setImportMap(foldersAndParent);
		
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
