package au.com.suncorp.process;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import au.com.suncorp.AutoDeploy.ExportContent;
import au.com.suncorp.AutoDeploy.ImportContent;
import au.com.suncorp.AutoDeploy.MoveFile;
import au.com.suncorp.common.JsonUtils;
import au.com.suncorp.datasource.DSConnection;

public class DeployTest {
	
	public static Map<String, ExecutionStep> STEPS = new HashMap<String, ExecutionStep>();
	static{
		STEPS.put("export", new ExportContent());
		STEPS.put("movefile", new MoveFile());
		STEPS.put("import", new ImportContent());
		STEPS.put("createdatasource", new DSConnection());
	}

	public static void main(String[] args) throws Exception {
		File file = new File(JsonUtils.class.getResource("/").getPath() + "/../config/exec.json");
		String json = IOUtils.toString(new FileInputStream(file));
		// System.out.println(json);
		List<Map> steps = JsonUtils.testJsonArray(json);
		for(Map step : steps){
			String name = StringUtils.lowerCase(String.valueOf(step.get("step")));
			ExecutionStep es = STEPS.get(name);
			if(es != null){
				System.out.println("-----------------");
				System.out.println("step="+step.get("step"));
				es.execute((Map)step.get("parameters"));
			}else{
				throw new Exception("Step "+ name + " is not defined.");
			}
		}
	}

}
