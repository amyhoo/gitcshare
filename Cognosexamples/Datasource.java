/**
 * Datasource.java
 *
 * Licensed Material - Property of IBM
 * © Copyright IBM Corp. 2003, 2010
 *
 * Description: Technote 1344206 - SDK Sample to Create a Data Source and Create/Delete a Data Source Connection
 *
 * Tested with: IBM Cognos BI 10.1, Java 5.0
 * 
 * Note:  You must log in as a system administrator.
 * 
 */

import javax.xml.namespace.QName;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;

import com.cognos.developer.schemas.bibus._3.AddOptions;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BiBusHeader;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.DataSource;
import com.cognos.developer.schemas.bibus._3.DataSourceConnection;
import com.cognos.developer.schemas.bibus._3.DeleteOptions;
import com.cognos.developer.schemas.bibus._3.SearchPathSingleObject;
import com.cognos.developer.schemas.bibus._3.StringProp;
import com.cognos.developer.schemas.bibus._3.TokenProp;
import com.cognos.developer.schemas.bibus._3.UpdateActionEnum;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;

public class Datasource {

	public ContentManagerService_PortType cmService = null;

	public void connectToReportServer(String endPoint) {
		// Create a connection to the Cognos service

		ContentManagerService_ServiceLocator cmServiceLocator = new ContentManagerService_ServiceLocator();

		try {
			cmService = cmServiceLocator
					.getcontentManagerService(new java.net.URL(endPoint));
		} catch (Exception e) {
			System.out.println(e);
		}

	}

	public String logon(String namespaceID, String uid, String pwd)
			throws Exception {
		StringBuffer credentialXML = new StringBuffer();

		credentialXML.append("<credential>");
		credentialXML.append("<namespace>").append(namespaceID).append(
				"</namespace>");
		credentialXML.append("<username>").append(uid).append("</username>");
		credentialXML.append("<password>").append(pwd).append("</password>");
		credentialXML.append("</credential>");

		String encodedCredentials = credentialXML.toString();

		try {
			cmService.logon(new XmlEncodedXML(encodedCredentials), null);

			SOAPHeaderElement temp = ((Stub) cmService).getResponseHeader(
					"http://developer.cognos.com/schemas/bibus/3/",
					"biBusHeader");
			BiBusHeader cmBiBusHeader = (BiBusHeader) temp
					.getValueAsType(new QName(
							"http://developer.cognos.com/schemas/bibus/3/",
							"biBusHeader"));
			((Stub) cmService).setHeader(
					"http://developer.cognos.com/schemas/bibus/3/",
					"biBusHeader", cmBiBusHeader);

		} catch (Exception e) {
			return (e.toString());
		}
		return ("Logon successful as " + uid);
	}

	public void createDataSource(String dataSourceName,
			String dataSourceConnectionString) throws Exception {
		DataSource dataSource = new DataSource();
		TokenProp tp = new TokenProp();
		tp.setValue(dataSourceName);
		dataSource.setDefaultName(tp);

		DataSourceConnection dataSourceConnection = new DataSourceConnection();
		dataSourceConnection.setDefaultName(tp);
		StringProp s = new StringProp();
		s.setValue(dataSourceConnectionString);
		dataSourceConnection.setConnectionString(s);

		AddOptions addOptions = new AddOptions();
		addOptions.setUpdateAction(UpdateActionEnum.replace);

		// Create the data source
		cmService.add(new SearchPathSingleObject("CAMID(\":\")"),
				new BaseClass[] { dataSource }, addOptions);
		// Create the data source connection
		cmService.add(new SearchPathSingleObject(
				"CAMID(\":\")/dataSource[@name='" + dataSourceName + "']"),
				new BaseClass[] { dataSourceConnection }, addOptions);
	}

	public void deleteDataSourceConnection(String dataSourceName)
			throws Exception {
		String dataSourceConnectionSearchPath = null;
		DataSourceConnection dataSourceConnection = new DataSourceConnection();
		dataSourceConnection.setSearchPath(new StringProp());
		dataSourceConnectionSearchPath = "CAMID(\":\")/dataSource[@name='"
				+ dataSourceName + "']/dataSourceConnection[@name='"
				+ dataSourceName + "']";
		dataSourceConnection.getSearchPath().setValue(
				dataSourceConnectionSearchPath);

		DeleteOptions deleteOptions = new DeleteOptions();
		deleteOptions.setForce(true);
		deleteOptions.setRecursive(true);

		cmService.delete(new BaseClass[] { dataSourceConnection },
				deleteOptions);
	}

	public static void main(String args[]) {
		String output = null;
		String endPoint = "http://localhost:9300/p2pd/servlet/dispatch";
		// You must log in as a system administrator
		String nameSpaceID = "nameSpaceID";
		String userName = "userName";
		String password = "password";
		String dataSourceName = "TestDataSource";
		String dataSourceConnectionString = ";LOCAL;XML;C:\\TEMP\\TestData.xml";
		Datasource security = new Datasource();

		security.connectToReportServer(endPoint);
		try {
			output = security.logon(nameSpaceID, userName, password);
			System.out.println(output);
			security.createDataSource(dataSourceName,
					dataSourceConnectionString);
			// Uncomment the line below to delete the data source
			// security.deleteDataSourceConnection(dataSourceName);
			System.out.println("Done.");
		} catch (Exception ex) {
			System.out.println("\nAn error occurred\n");
			ex.printStackTrace();
		}
	}
}
