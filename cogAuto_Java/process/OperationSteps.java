package au.com.suncorp.process;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.suncorp.common.JsonUtils;

public class OperationSteps {
	
	private static String arguments[] = { "-s", "-d", "-sf", "-df", "-dp", "-t", "-bakName", "-impName", "-expName" };//
	String source = null;// enviornment of source
	String destination = null;// enviornment of destination
	String bakName = null;
	String impName = null;
	String expName = null;
	String execFile= null;
	List sFolders = new ArrayList();// folders of source
	List dFolders = new ArrayList();// folders of destination
	List dParents = new ArrayList();
	// parent folder of folders of destination
	List<Object> exec_steps = new ArrayList<Object>();
	String[] sections = null;// the whole deployment include many process
								// sections like datasource create and
								// foldersMove
	String dataSourceName = null;
	String dbserver = null;
	String dbport = null;
	String dbname = null;
	String signonuid = null;
	String signonpwd = null;
	

	public Map datasourceCreate() {
		Map step = new HashMap<String, Object>();
		step.put("step", "createdatasource");
		Map parameters = new HashMap<String, Object>();
		parameters.put("env", destination);
		parameters.put("name", dataSourceName);
		parameters.put("dbserver", dbserver);
		parameters.put("dbname", dbname);
		parameters.put("dbport", dbport);
		if(signonuid != null && signonpwd != null) {
			parameters.put("signonuid", signonuid);
			parameters.put("signonpwd", signonpwd);
		}
		step.put("parameters", parameters);
		return step;
	}
	
	public Map exportContent(String env, String Archive,List path) {
		Map step = new HashMap<String, Object>();
		step.put("step", "export");
		Map parameters = new HashMap<String, Object>();
		parameters.put("env", env);
		parameters.put("archive", Archive);
		parameters.put("path", path);
		if (dParents.size() > 0) 
			parameters.put("parent", dParents);	
		step.put("parameters", parameters);
		return step;
	}
	
	public Map importContent() {
		Map step = new HashMap<String, Object>();
		step.put("step", "import");
		Map parameters = new HashMap<String, Object>();
		parameters.put("env", destination);
		parameters.put("archive", impName);
		parameters.put("file", expName);
		step.put("parameters", parameters);
		return step;
	}

	public Map foldersMove() {
		Map step = new HashMap<String, Object>();
		step.put("step", "movefile");
		Map parameters = new HashMap<String, Object>();
		parameters.put("source", source);
		parameters.put("filename", expName + ".zip");
		parameters.put("remoteip", this.destination);
		step.put("parameters", parameters);
		return step;
	}
	
	public void generateSteps() {
		for (int i = 0; i < sections.length; i++) {
			switch (sections[i]) {
			case "foldersMove":
				exec_steps.add(foldersMove());
				break;
			case "datasource":
				exec_steps.add(datasourceCreate());
				break;
			case "import":
				exec_steps.add(importContent());
				break;
			case "exportsource":
				exec_steps.add(exportContent(source,expName,sFolders));
				break;
			case "exportdestination":
				exec_steps.add(exportContent(destination,bakName,dFolders));
				break;
			}

		}
	}

	public void writeJson(String filePath)throws IOException  {
		String json = JsonUtils.getJsonString4JavaPOJO(exec_steps);
		FileWriter fw = new FileWriter(filePath);
		PrintWriter out = new PrintWriter(fw);
		out.write(JsonUtils.formatJson(json));
		fw.close();
		out.close();
	}

	public void loadParams(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].compareToIgnoreCase("-s") == 0)
				source = args[++i];
			else if (args[i].compareToIgnoreCase("-d") == 0)
				destination = args[++i];
			else if (args[i].compareToIgnoreCase("-bakname") == 0)
				bakName = args[++i];
			else if (args[i].compareToIgnoreCase("-impName") == 0)
				impName = args[++i];
			else if (args[i].compareToIgnoreCase("-expName") == 0)
				expName = args[++i];
			else if (args[i].compareToIgnoreCase("-sf") == 0) {
				String temp = args[++i];
				sFolders = Arrays.asList(temp.split(","));
			} else if (args[i].compareToIgnoreCase("-df") == 0) {
				String temp = args[++i];
				dFolders = Arrays.asList(temp.split(","));
			} else if (args[i].compareToIgnoreCase("-dp") == 0) {
				while (++i < args.length && !isArgument(args[i]))
					dParents.add(args[i]);
				i--;
			} else if (args[i].compareToIgnoreCase("-t") == 0) {
				String temp = args[++i];
				sections = temp.split(",");
			} else if (args[i].compareToIgnoreCase("-dsn") == 0)
				dataSourceName = args[++i];
			 else if (args[i].compareToIgnoreCase("-dbserver") == 0)
				 dbserver = args[++i];
			 else if (args[i].compareToIgnoreCase("-dbport") == 0)
				 dbport = args[++i];
			 else if (args[i].compareToIgnoreCase("-dbname") == 0)
				 dbname = args[++i];
			 else if (args[i].compareToIgnoreCase("-signonuid") == 0)
				 signonuid = args[++i];
			 else if (args[i].compareToIgnoreCase("-signonpwd") == 0)
				 signonpwd = args[++i];
		}
	}
	
	private static boolean isArgument(String p_argument) {
		for (int i = 0; i < arguments.length; i++) {
			if (p_argument.equals(arguments[i]))
				return true;
		}
		return false;
	}
	

	public static void main(String[] args) throws Exception {
		OperationSteps mainClass = new OperationSteps();
		String jsonFile = OperationSteps.class.getResource("/").getPath() + "../config/exec.json";
		System.out.println("json file="+ jsonFile);
		mainClass.execFile = jsonFile;
		mainClass.loadParams(args);
		mainClass.generateSteps();
		mainClass.writeJson(mainClass.execFile);
	}
}
