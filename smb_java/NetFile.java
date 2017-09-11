package au.com.suncorp.smb;
import java.io.BufferedInputStream;  
import java.io.BufferedOutputStream;  
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;  
import java.io.IOException;  
import java.io.InputStream;  
import java.io.OutputStream;  
import java.net.MalformedURLException;  
import java.util.ArrayList;  
import java.util.List;
import jcifs.smb.*;

public class NetFile {
	
	private static int port=445;
	private static String server=new String("int.corp.sun");
	private static String  userID;
	private static String password;
	private static String domain=new String("int");
	private static boolean ntlm_v2=true;
	private static boolean is_tcp=true;
	private static NtlmPasswordAuthentication authentication;
	private static int timeout=30;
	private static String base_path=new String("/GroupData/ITProSup/Sharepoint/EMSCHEDULES/");
	
    private static final String SUNDAY = "SUNDAY";  
    private static final String MONDAY = "MONDAY";  
    private static final String TUESDAY = "TUESDAY";  
    private static final String WEDNESDAY = "WEDNESDAY";  
    private static final String THURSDAY = "THURSDAY";  
    private static final String FRIDAY = "FRIDAY";  
    private static final String SATURDAY = "SATURDAY";
    
	public NetFile(String user, String passWord){
		userID=user;
		password=passWord;
	}
	private String buildSmb(String path){
		if (path.startsWith("smb://")){
			return path;
		} 
		else if (path.startsWith("//")){
			return "smb:"+path;
		}
			
		else if (path.startsWith("/")){
			//absolute path
			return "smb://"+server+path;
		}
		else{
			//relative path
			return "smb://"+server+base_path+path;
		}
	} 
	public static void main(String args[]) throws MalformedURLException, SmbException {
		//setProxy();
		NetFile mainClass = new NetFile("user","pwd"); // instantiate the class
		mainClass.connect();
		mainClass.retrieveFile("ALL_PROD_EM_SCHEDULES_160831.XLS","H:/projects/controlM/xml2csv/ALL_PROD_EM_SCHEDULES_160831.XLS");
//		SmbFile [] files=
//		for (int i = 0; i < files.length; i++) 
//				System.out.println(files[i].getName());
	}	
	public void connect(){
		authentication = new NtlmPasswordAuthentication("int", userID, password);		
	}
	
	public void close(){
		
	}
	public void listShares(){
		
	}

	public SmbFile [] listPath(String path) throws MalformedURLException, SmbException{
		path=this.buildSmb(path);
		SmbFile file = new SmbFile(path, authentication);
		if (path.endsWith("/")){
			SmbFile [] files=file.listFiles();
			return files;						
		}
		else{
			SmbFile [] files={file};
			return files;
		}				
	}	
	public SmbFile []  listPath(String path,String search,String pattern) throws MalformedURLException, SmbException{
		/*
		path:
		search:the attribute filter
		pattern:
		*/		
		path=buildSmb(path);
		SmbFile file = new SmbFile(path, authentication);
		SmbFile [] files=file.listFiles();
		return files;
	}
	
	public void createDirectory(String path){
		
	}
	public void deleteDirectory(String path){
		
	}
	public void deleteFiles(String pattern){
		
	}
	public void getAttributes(String path){
		
	}		
	public void rename(String old_path,String new_path){
		
	}
	public void retrieveFile(String remote_path,String local_path) throws MalformedURLException, SmbException{
        InputStream in = null;  
        OutputStream out = null; 
        int len;
        remote_path=buildSmb(remote_path);
		try{			
			SmbFile remote_file = new SmbFile(remote_path, authentication);
			File local_file=new File(local_path);			
			in = new BufferedInputStream(new SmbFileInputStream(remote_file));  
			out = new BufferedOutputStream(new FileOutputStream(local_file));  
            byte[] buffer = new byte[1024];  
            while ((len=in.read(buffer)) > 0)  
            {  
                out.write(buffer,0,len);  
                buffer = new byte[1024];  
            }  			
		}
        catch (Exception e)  
        {  
            e.printStackTrace();  
        }  
        finally  
        {  
            try  
            {  
                out.close();  
                in.close();  
            }  
            catch (IOException e)  
            {  
                e.printStackTrace();  
            }  
        }  		
	}	
	public void retrieveFileFromOffset(String remote_path,String local_path,int offset, int  max_length){
		
	}		
	public void storeFile(String remote_path,String local_path){		
        InputStream in = null;  
        OutputStream out = null;  
        try  
        {  
            File localFile = new File(local_path);              
            SmbFile remoteFile = new SmbFile(remote_path);  
            in = new BufferedInputStream(new FileInputStream(localFile));  
            out = new BufferedOutputStream(new SmbFileOutputStream(remoteFile));  
            byte[] buffer = new byte[1024];  
            while (in.read(buffer) != -1)  
            {  
                out.write(buffer);  
                buffer = new byte[1024];  
            }  
        }  
        catch (Exception e)  
        {  
            e.printStackTrace();  
        }  
        finally  
        {  
            try  
            {  
                out.close();  
                in.close();  
            }  
            catch (IOException e)  
            {  
                e.printStackTrace();  
            }  
        }  
	}		
	public void storeFileFromOffset(String remote_path,String local_path,int offset,int max_length){
		
	}			
}
