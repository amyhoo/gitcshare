/**
 * Licensed Material - Property of IBM
 * © Copyright IBM Corp. 2010 
 *
 * PersonalDataSourceCredentials.java
 *
 * Description: Technote 1455555 - SDK Sample - Getting the Search Path of All the Personal Data Source Credential Objects
 * 
 * Tested with: IBM Cognos BI 10.1.0, IBM Java 5.0, Axis 1.4
 * 
 */

import javax.xml.namespace.QName;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;

import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BiBusHeader;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.DataSourceCredential;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;

public class PersonalDataSourceCredentials{

	//TODO: change the following variables depending on your installation and setup

	//--- begin changes ----
	private static String dispatcherURL = "http://host.domain:9300/p2pd/servlet/dispatch";

	//Change the next three variables to a valid namespace ID as defined in Cognos Configuration and NOT the namespace name
	//Provide a valid Cognos System Administrator user ID and password if Anonymous is disabled	
	private static String nameSpaceID 	= "SDK";
	private static String userName 		= "admin";
	private static String password 		= "password";

	//--- end changes ---

	//Services to be used in the sample
	private ContentManagerService_PortType cmService = null;

	
	/**
	 * Displays the search path of all the personal data source credential objects in the Content Store
	 */
	private void doWork() {
		
		PropEnum props[] = new PropEnum[] { PropEnum.dataSourceName, PropEnum.searchPath };
		
		BaseClass bc[] = null;
		String searchPath = "//dataSourceCredential";
		
		try {
			SearchPathMultipleObject spMulti = new SearchPathMultipleObject();			
			spMulti.set_value(searchPath);
			bc = cmService.query(spMulti, props, new Sort[] {},	new QueryOptions());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("PERSONAL DATA SOURCE CREDENTIALS AND THEIR ASSOCIATED DATA SOURCE:\n");
		if (bc != null) {
			for (int i = 0; i < bc.length; i++) {
				System.out.println(bc[i].getSearchPath().getValue() + " -- "
						+ ((DataSourceCredential)bc[i]).getDataSourceName().getValue());
			}
		}
	}
	
	
	

	/**
	 * This is the main class, which connects to Cognos BI and calls the 
	 * method doWork(), containing the specific code for this sample
	 */
	public static void main(String args[]) {

		PersonalDataSourceCredentials mainClass = new PersonalDataSourceCredentials();
		
		mainClass.connectToCognos();

		// If Anonymous is disabled, then logon
		if (nameSpaceID.length() > 0) {
			mainClass.logon(nameSpaceID, userName, password);
		}

		// do the main work
		mainClass.doWork();
	}

	/**
	 * Establish a connection to Cognos BI and initialize the different services
	 */
	private void connectToCognos() {
		ContentManagerService_ServiceLocator cmServiceLocator = new ContentManagerService_ServiceLocator();

		try {
			cmService = cmServiceLocator.getcontentManagerService(new java.net.URL(dispatcherURL));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Logon to Cognos BI using valid credentials information
	 */
	public void logon(String nameSpaceID, String userName, String password) {
		StringBuffer credentialXML = new StringBuffer();

		credentialXML.append("<credential>");
		credentialXML.append("<namespace>").append(nameSpaceID).append("</namespace>");
		credentialXML.append("<username>").append(userName).append("</username>");
		credentialXML.append("<password>").append(password).append("</password>");
		credentialXML.append("</credential>");

		String encodedCredentials = credentialXML.toString();
		XmlEncodedXML xmlCredentials = new XmlEncodedXML();
		xmlCredentials.set_value(encodedCredentials);

		try {
			cmService.logon(xmlCredentials, null);
			
			getSetHeaders();			

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void getSetHeaders(){
		String BiBus_NS = "http://developer.cognos.com/schemas/bibus/3/";
		String BiBus_H = "biBusHeader";
			
		BiBusHeader CMbibus = null;
		
		SOAPHeaderElement temp = ((Stub)cmService).getResponseHeader(BiBus_NS, BiBus_H);
	
		try {
			CMbibus = (BiBusHeader)temp.getValueAsType(new QName (BiBus_NS, BiBus_H));
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (CMbibus != null)
		{
			((Stub)cmService).setHeader(BiBus_NS, BiBus_H, CMbibus);
		}
	}
}

