
/**
 * PageThroughHistory.java
 * 
 * Licensed Material - Property of IBM
 * © Copyright IBM Corp. 2003, 2010
 *
 * Copyright © Cognos Incorporated. All Rights Reserved.
 * Cognos and the Cognos logo are trademarks of Cognos Incorporated.
 *
 * Description: Technote 1342447 - How to page through history using Cognos 8 SDK
 * 
 * Tested with: IBM Cognos BI 10.1, IBM Java 1.5, Axis 1.4
 */

import java.math.BigInteger;
import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import javax.xml.namespace.QName;
import com.cognos.developer.schemas.bibus._3.BiBusHeader;


import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.cognos.developer.schemas.bibus._3.StringProp;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;
import org.apache.axis.AxisFault;

public class PageThroughHistory {
	private ContentManagerService_ServiceLocator cmServiceLocator = null;

	private ContentManagerService_PortType cmService = null;

	public final static String CM_URL = "http://localhost:9300/p2pd/servlet/dispatch";

	private String objSearchPath = "//history";

	public static void main(String[] args) {
		PageThroughHistory dObj = new PageThroughHistory(CM_URL);
		dObj.quickLogon("SDK", "admin", "password");
		dObj.getPages(dObj.getobjSearchPath());

	}

	public void getPages(String p_objSearchPath) {
		PropEnum props[] = new PropEnum[] { PropEnum.searchPath };

		if (p_objSearchPath != null && p_objSearchPath != "") {
			try {
				SearchPathMultipleObject spMulti = new SearchPathMultipleObject();
				spMulti.set_value(p_objSearchPath);
				
				QueryOptions options = new QueryOptions();

				BigInteger maxObjs = new BigInteger("15");
				options.setMaxObjects(maxObjs);

				BigInteger skipObjs = new BigInteger("0");
				options.setSkipObjects(skipObjs);

				BaseClass bc[] = cmService.query(spMulti, props, new Sort[] {},
						options);
				BigInteger n = new BigInteger("1");
				
				while (bc.length > 0) {
					System.out.println("~~~~~~~~~~~~~~~~~~~Results from query "
							+ n + " ~~~~~~~~~~~~~~~~~~~");
					for (int i = 0; i < bc.length; i++) {
						StringProp theSearchPath = bc[i].getSearchPath();

						System.out.println("SearchPath \t = "
								+ theSearchPath.getValue());
					}
					n = n.add(new BigInteger("1"));
					skipObjs = (n.subtract(new BigInteger("1")))
							.multiply(maxObjs);
					options.setSkipObjects(skipObjs);
					bc = cmService
							.query(spMulti, props, new Sort[] {}, options);
				}
				
			} catch (AxisFault ex) {
				useAxisInterface_dumpToString(ex);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public String getobjSearchPath() {
		return objSearchPath;
	}

	// This method logs the user to Cognos 8
	public String quickLogon(String namespace, String uid, String pwd) {
		StringBuffer credentialXML = new StringBuffer();

		credentialXML.append("<credential>");
		credentialXML.append("<namespace>").append(namespace).append(
				"</namespace>");
		credentialXML.append("<username>").append(uid).append("</username>");
		credentialXML.append("<password>").append(pwd).append("</password>");
		credentialXML.append("</credential>");

		String encodedCredentials = credentialXML.toString();
		XmlEncodedXML xmlCredentials = new XmlEncodedXML();
		xmlCredentials.set_value(encodedCredentials);
		try {
			cmService.logon(xmlCredentials, null);

			//TODO Set the BiBusHeader
		SOAPHeaderElement temp = ((Stub)cmService).getResponseHeader 
			 ("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader");
		BiBusHeader cmBiBusHeader = (BiBusHeader)temp.getValueAsType
			(new QName ("http://developer.cognos.com/schemas/bibus/3/","biBusHeader"));
		((Stub)cmService).setHeader
			("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", cmBiBusHeader);

		} catch (AxisFault ex) {
			useAxisInterface_dumpToString(ex);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ("Logon successful as " + uid);
	}

	// This method connects to Cognos 8
	public PageThroughHistory(String gateway) {
		// Retrieve the service
		cmServiceLocator = new ContentManagerService_ServiceLocator();
		try {
			cmService = cmServiceLocator
					.getcontentManagerService(new java.net.URL(CM_URL));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void useAxisInterface_dumpToString(AxisFault ex) {
		String details = ex.dumpToString();
		System.out.println("\n\n1) CALL dumpToString:");
		System.out.println("-------------------------\n");
		String message = ex.getFaultString();
		System.out.println("message: " + message + "\n");
		System.out.println(details);
	}

}

