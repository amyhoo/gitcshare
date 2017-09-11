package example;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;

import javax.xml.namespace.QName;
import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BiBusHeader;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;

import example.UrlTest.MyAuthenticator;

import com.cognos.developer.schemas.bibus._3.SearchPathSingleObject;

public class ConnectTest {
private static String dispatcherURL = "http://cogap1.int.corp.sun:9300/p2pd/servlet/dispatch";
private static String nameSpaceID = "INT";
private static String userName = "S103946";//S103946
private static String password = "c0gXIpr0d";//c0gXIpr0d
private ContentManagerService_PortType cmService=null;


public static void main(String args[]) {
	//setProxy();
	ConnectTest mainClass = new ConnectTest(); // instantiate the class

	// Step 1: Connect to the Cognos services
	mainClass.connectToCognos (dispatcherURL); 

	// Step 2: Logon to Cognos 
	mainClass.logonToCognos(nameSpaceID, userName, password); 

	// Step 3: Execute tasks 
	mainClass.executeTasks(); 

	// Step 4: Logoff from Cognos 
	mainClass.logoffFromCognos(); 
}

static class MyAuthenticator extends Authenticator {
	//not used in this class
    private String user = "";
    private String password = "";

    public MyAuthenticator(String user, String password) {
        this.user = user;
        this.password = password; 
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(user, password.toCharArray());
    }
}
private static void setProxy(){
	//not used in this class
//	InetSocketAddress addr = new InetSocketAddress("isaproxy.int.corp.sun", 89);     
//	Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
//	Authenticator.setDefault(new MyAuthenticator("U391812", "Xumin&06"));
	System.setProperty("http.proxySet", "true"); 
	System.setProperty("http.proxyHost", "isaproxy.int.corp.sun"); 
	System.setProperty("http.proxyPort", "89");
	System.setProperty("http.proxyUser", ""); 
	System.setProperty("http.proxyPassword", "");        	
}

// Step 1: Connect to the Cognos services
private void connectToCognos(String dispatcherURL) {
	ContentManagerService_ServiceLocator cmServiceLocator =
        new ContentManagerService_ServiceLocator();

	try {
		URL url = new URL(dispatcherURL);	
		cmService = cmServiceLocator.getcontentManagerService(url);
	} catch (Exception e) {
		e.printStackTrace();
	}
}

// Step 2: Logon to Cognos 
private void logonToCognos(String nsID, String user, String pswd) {
	StringBuffer credentialXML = new StringBuffer();
	credentialXML.append("<credential>");
	credentialXML.append("<namespace>").append(nsID).append("</namespace>");
	credentialXML.append("<username>").append(user).append("</username>");
	credentialXML.append("<password>").append(pswd).append("</password>");
	credentialXML.append("</credential>");

	String encodedCredentials = credentialXML.toString();
	XmlEncodedXML xmlCredentials = new XmlEncodedXML();
	xmlCredentials.set_value(encodedCredentials);

	try {
		cmService.logon(xmlCredentials, new SearchPathSingleObject[] {});
		SOAPHeaderElement temp = ((Stub)cmService).getResponseHeader(
			"http://developer.cognos.com/schemas/bibus/3/", "biBusHeader");
		BiBusHeader CMbibus = (BiBusHeader)temp.getValueAsType(
			new QName ("http://developer.cognos.com/schemas/bibus/3/",
				"biBusHeader"));
		((Stub)cmService).setHeader(
            "http://developer.cognos.com/schemas/bibus/3/", 
			"biBusHeader", CMbibus);
	} catch (Exception ex) {
		ex.printStackTrace();
	}
}

// Step 3: Execute tasks 
private void executeTasks() {
	PropEnum props[] = new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName };
	BaseClass bc[] = null;
	String searchPath = "/content//package";

	try {
		SearchPathMultipleObject spMulti =
            new SearchPathMultipleObject(searchPath);
		bc = cmService.query(spMulti, props, new Sort[] {}, new QueryOptions());
	} catch (Exception e) {
		e.printStackTrace();
		return;
	}

	System.out.println("PACKAGES:\n");
	if (bc != null) {
		for (int i = 0; i < bc.length; i++) {
			System.out.println(bc[i].getDefaultName().getValue() + " - "
					+ bc[i].getSearchPath().getValue());
		}
	}
}

// Step 4: Logoff from Cognos 
private void logoffFromCognos() {
	try {
		cmService.logoff();
	} catch (Exception ex) {
		ex.printStackTrace();
	}
}
}