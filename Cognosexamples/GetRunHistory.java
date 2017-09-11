/**
 * GetRunHistory.java
 *
 * Licensed Material - Property of IBM
 * © Copyright IBM Corp. 2010 
 *
 * Description: Technote 1343791 - SDK - Extract Run History property of a ReportNet object
 * 
 * Tested with: IBM Cognos BI 10.1, IBM Java 1.5, Axis 1.4
 * 
 */

import com.cognos.developer.schemas.bibus._3.*;

import javax.xml.namespace.QName;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;

public class GetRunHistory 
{
	//	ReportNet service
	 public ContentManagerService_ServiceLocator cmServiceLocator = null;
	 public ContentManagerService_PortType 		 cmService    = null;
	
	public static void main(String args[])
	{
		//initialize a connection to cognos
		GetRunHistory connect = new GetRunHistory();
		connect.connectToReportServer();
		connect.logon("SDK", "admin", "password");
		
		try
		{
		 String reportPath = "/content/package[@name='GO Sales (query)']/folder[@name='Report Studio Report Samples']/report[@name='Horizontal Pagination']"; 
		 connect.getRunHistory(reportPath);
		}
		catch (Exception e)
		{
		 System.out.println(e);
		}
	}	
	
	/**
	 * Logon to Cognos using valid credentials information
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

	public void connectToReportServer ()
	{		
		// Default URL for cognos dispatcher
		String endPoint = "http://localhost:9300/p2pd/servlet/dispatch";        
		// Retrieve the service           
		cmServiceLocator = new ContentManagerService_ServiceLocator();

		try
		{
			cmService = cmServiceLocator.getcontentManagerService(new java.net.URL(endPoint));
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}

	//Queries the Content Store for Run History of specified object
	public void getRunHistory( String objectPath)
	{
		SearchPathMultipleObject spMultiple=new SearchPathMultipleObject();
		spMultiple.set_value(objectPath);
		
		PropEnum props[] = new PropEnum[]{PropEnum.searchPath,
										  PropEnum.defaultName,
										  PropEnum.retentions};
		try
		{
			BaseClass[] object = cmService.query(
					spMultiple,props, new Sort[]{}, new QueryOptions());
			RetentionRule myRules[] = new RetentionRule[] {};
			int rule = 0;
			if (object != null && object.length > 0)
			{
				if (object[0] instanceof Report )
				{
					 myRules = ((Report)object[0]).getRetentions().getValue();	 
				}
				else if (object[0] instanceof JobDefinition) 
				{
					myRules = ((JobDefinition)object[0]).getRetentions().getValue();
				}
				else if (object[0] instanceof ReportView)
				{
					myRules = ((ReportView)object[0]).getRetentions().getValue();
				
				}
				if (myRules!= null)
				{
					for (int i=0; i < myRules.length; i++)
					{
						String x = myRules[i].getObjectClass().getValue();
						if (x.endsWith("history"))
							rule=i;	 
					}
					if (myRules[rule].getMaxDuration() != null)
					{
					    //If Duration is set, display how many days the outputs are saved for
						System.out.print("Run History - Duration: ");
						String duration = myRules[rule].getMaxDuration();
						if (duration.endsWith("D"))
							System.out.println(duration.substring(1,duration.length()-1)+ " Days");
						else
							System.out.println(duration.substring(1,duration.length()-1)+ " Months");
					}
					else
					{
						//If Number of Occurrences set, display how many outputs are saved
						System.out.print("Run History - Number of Occurrences: ");
						System.out.println(myRules[rule].getMaxObjects().intValue());
					}
				}
			}	
		}
		catch (Exception ex)
		{
			System.out.println(ex);
		}
	}
}

