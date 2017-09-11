package au.com.suncorp.common;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class SCPUtils {
	
	public static void main(String[] args) throws Exception {
		System.out.println(System.getenv("temp"));
		move("cogad1","/cognosdata/c10/deployment/","expDev.zip","cogat1","/cognosdata/c10/deployment/", "a390925", "Kevin@123c");
	}
	
	public static void move(String source,String sourcedir, String filename, String remoteip, String romoteDir, 
			String sUser, String sPass) throws Exception {
		Connection con = null;
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try{
			con = new Connection(source, 22);
			con.connect();
			boolean isAuthed = con.authenticateWithPassword(sUser, sPass);
			if(isAuthed){
				SCPClient scp = con.createSCPClient();
				scp.get(sourcedir + filename, os); 
			}else{
				throw new Exception("[SCP file]Username or password is not correct.");
			}
		}finally{
			if(con != null)con.close();
		}
		try{
			con = new Connection(remoteip, 22);
			con.connect();
			boolean isAuthed = con.authenticateWithPassword(sUser, sPass);
			if(isAuthed){
				SCPClient scp = con.createSCPClient();
				scp.put(os.toByteArray(), filename, romoteDir, "0755"); //"0755"
			}else{
				throw new Exception("[SCP file]Destination Username or password is not correct.");
			}
		}finally{
			if(con != null)con.close();
		}
		if(os != null)os.close();
	}
	
	public static void runShell(String hostname,String username, String password, String shell) {
		try {
			Connection conn = new Connection(hostname);
			conn.connect();

			boolean isAuthenticated = conn.authenticateWithPassword(username, password);

			if (isAuthenticated == false)
				throw new IOException("Authentication failed.");

			Session sess = conn.openSession();
			sess.execCommand(shell);

			InputStream stdout = new StreamGobbler(sess.getStdout());
			BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null)
					break;
				System.out.println(line);
			}
			System.out.println("ExitCode: " + sess.getExitStatus());

			sess.close();
			conn.close();

		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(2);
		}
	}
	
}
