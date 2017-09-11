/**
 * Licensed Materials - Property of IBM
 * 
 * IBM Cognos Products: SDK Support
 * 
 * (c) Copyright IBM Corp. 2015
 * 
 * US Government Users Restricted Rights - Use, duplication or disclosure restricted by GSA ADP Schedule Contract with
 * IBM Corp.
 */
/**
 * ActivateStandbyCM.java
 * 
 * Description: Technote 1700149 - SDK Sample to activate a standby content manager
 * 
 * Base Installation : IBM Cognos 10.2.2
 * Tested with : IBM JDK 7.0
 * Modified Date : 150323
 * 
 */

import java.rmi.RemoteException;

import javax.xml.namespace.QName;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;

import com.cognos.developer.schemas.bibus._3.BiBusHeader;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.SearchPathSingleObject;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;

public class ActivateStandbyCM {
	public ContentManagerService_PortType cmService = null;

	public ActivateStandbyCM(String endPoint) {
		ContentManagerService_ServiceLocator cmServiceLocator = new ContentManagerService_ServiceLocator();

		try {
			cmService = cmServiceLocator.getcontentManagerService(new java.net.URL(endPoint));
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	/**
	 * This method implements the logon to the IBM Cognos Server
	 * 
	 * @param namespace
	 * @param uid
	 * @param pwd
	 * @return
	 */
	public String quickLogon(String namespace, String uid, String pwd) {
		try {
			StringBuffer credentialXML = new StringBuffer();

			credentialXML.append("<credential>");
			credentialXML.append("<namespace>").append(namespace).append("</namespace>");
			credentialXML.append("<username>").append(uid).append("</username>");
			credentialXML.append("<password>").append(pwd).append("</password>");
			credentialXML.append("</credential>");

			String encodedCredentials = credentialXML.toString();

			cmService.logon(new XmlEncodedXML(encodedCredentials), null);

			SOAPHeaderElement temp = ((Stub) cmService).getResponseHeader("http://developer.cognos.com/schemas/bibus/3/",
					"biBusHeader");
			BiBusHeader cmBiBusHeader = (BiBusHeader) temp.getValueAsType(new QName(
					"http://developer.cognos.com/schemas/bibus/3/", "biBusHeader"));
			((Stub) cmService).setHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", cmBiBusHeader);
		} catch (Exception e) {
			System.out.println(e);
		}

		return ("Logon successful as " + uid);

	}

	public void activateCM(String cmPath) {
		SearchPathSingleObject spso = new SearchPathSingleObject();
		spso.set_value(cmPath);

		try {
			System.out.println("Currently active Content Manager: <" + cmService.getActiveContentManager() + ">");
			System.out.println("Activating <" + cmPath + ">");
			cmService.activate(spso);
			System.out.println("Activation request sent... program terminating.");
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Main method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// Variable that contains the default URL for Dispatcher Connection
		String dispatcherEndPoint = "http://localhost:9300/p2pd/servlet/dispatch";

		// must be a user with System administrator privileges.
		String userName = "userName";
		String password = "password";
		String nameSpaceID = "nameSpaceID";
		String cmservicePath = "/configuration/dispatcher[@name='http://localhost:9400/p2pd']/contentManagerService[@name='ContentManagerService']";

		ActivateStandbyCM sc = new ActivateStandbyCM(dispatcherEndPoint);

		// use this logon code to logon as a system administrator, comment out to work anonymously without logon values.
		sc.quickLogon(nameSpaceID, userName, password);

		sc.activateCM(cmservicePath);
	}
}
