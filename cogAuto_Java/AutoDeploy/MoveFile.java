package au.com.suncorp.AutoDeploy;

import java.util.Map;

import au.com.suncorp.common.SCPUtils;
import au.com.suncorp.process.ExecutionStep;

public class MoveFile implements ExecutionStep {
	
	public static String COGNOS_DEPLOY_DIR = "/cognosdata/c10/deployment/";
	public static String Username = "a390925";
	public static String Password = "Kevin@123c";
	
	public static void main(String[] args) throws Exception {
		SCPUtils.move("cogad1",COGNOS_DEPLOY_DIR,"expDev.zip","cogat1", COGNOS_DEPLOY_DIR, Username, Password); 
	}

	@Override
	public void execute(Map map) throws Exception {
		String filename = null;
		Object fn = map.get("filename");
		if(fn != null){
			filename = String.valueOf(fn);
		}
		String source = Deployment.getServerNameByEnv(String.valueOf(map.get("source")));
		String remoteip = Deployment.getServerNameByEnv(String.valueOf(map.get("remoteip")));

		String susername = null;
		String spassword = null;
		if(map.get("username") == null && map.get("password") == null){
			susername = Username;
			spassword = Password;
		}else{
			susername = String.valueOf(map.get("username"));
			spassword = String.valueOf(map.get("password"));
		}
		SCPUtils.move(source,COGNOS_DEPLOY_DIR, filename, remoteip, COGNOS_DEPLOY_DIR, susername, spassword);
	}

}
