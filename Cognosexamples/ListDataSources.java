/**
 * ListDataSources.java
 *
 * Licensed Material - Property of IBM
 * © Copyright IBM Corp. 2003, 2010
 *
 * Description: Technote 1344205 - SDK Sample to Extract Data Sources Names and Their Connection Information
 *
 * Tested with: IBM Cognos BI 10.1, Java 5.0
 * 
 * Note:  You must log in as a system administrator.
 */

import javax.xml.namespace.QName;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;

import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BiBusHeader;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.DataSourceConnection;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;

public class ListDataSources
{

  public ContentManagerService_PortType cmService = null;
  
  public void connectToReportServer (String endPoint) throws Exception
  {
  	//Create a connection to the Cognos service
  	
  	ContentManagerService_ServiceLocator cmServiceLocator = new ContentManagerService_ServiceLocator();
  	try
	{
		cmService = cmServiceLocator.getcontentManagerService(new java.net.URL(endPoint));
	}
	catch (Exception e)
	{
		System.out.println(e);
	}
  }

  public String logon(String namespaceID, String uid, String pwd) throws Exception
  {
	  StringBuffer credentialXML = new StringBuffer();

	  credentialXML.append("<credential>");
	  credentialXML.append("<namespace>").append(namespaceID).append("</namespace>");
	  credentialXML.append("<username>").append(uid).append("</username>");
	  credentialXML.append("<password>").append(pwd).append("</password>");
	  credentialXML.append("</credential>");

	  String encodedCredentials = credentialXML.toString();

	  try
		{
			cmService.logon(new XmlEncodedXML(encodedCredentials), null);

		SOAPHeaderElement temp = ((Stub)cmService).getResponseHeader 
			 ("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader");
		BiBusHeader cmBiBusHeader = (BiBusHeader)temp.getValueAsType
			(new QName ("http://developer.cognos.com/schemas/bibus/3/","biBusHeader"));
		((Stub)cmService).setHeader
			("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", cmBiBusHeader);

		}
		catch(Exception e)
		{
			e.printStackTrace();
			return(e.toString());
			
		}
	  return ("Logon successful as " + uid);
  }

	public void listDataSourcesAndConnections() throws Exception
	{
		PropEnum dataSourceProps[] = new PropEnum[]{PropEnum.defaultName, PropEnum.searchPath};
		PropEnum dataConnectionProps[] = new PropEnum[]{PropEnum.defaultName, PropEnum.connectionString};

		BaseClass[] dataSources = cmService.query(new SearchPathMultipleObject("CAMID(\":\")//dataSource"), dataSourceProps, new Sort[]{}, new QueryOptions());

		for(int i = 0; i < dataSources.length; i++)
		{
			System.out.println("\nData Source name: " + dataSources[i].getDefaultName().getValue());
			BaseClass[] dataConnections = cmService.query(new SearchPathMultipleObject(dataSources[i].getSearchPath().getValue() + "//dataSourceConnection"), dataConnectionProps, new Sort[]{}, new QueryOptions());
			for(int j = 0; j < dataConnections.length; j++)
			{
				System.out.println("\tData Connection name: " + dataConnections[j].getDefaultName().getValue());
				System.out.println("\tConnection string: " + ((DataSourceConnection)dataConnections[j]).getConnectionString().getValue());
			}
		}
	}

  public static void main(String args[])
  {
  	String output = null;
    String endPoint = "http://localhost:9300/p2pd/servlet/dispatch";
    // You must log in as a system administrator
	String nameSpaceID = "nameSpaceID";
	String userName = "userName";
	String password = "password";
	 
	ListDataSources security = new ListDataSources();

	try
	{
		security.connectToReportServer(endPoint);
		output = security.logon(nameSpaceID, userName, password);
		System.out.println(output);
		security.listDataSourcesAndConnections();
	}
	catch (Exception ex)
	{
		ex.printStackTrace();
		System.out.println("\nAn error occurred\n");
	}
	System.out.println("\n\nDone");
  }
}

