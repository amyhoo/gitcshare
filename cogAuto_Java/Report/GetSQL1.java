package Report;
/** 
 * Licensed Materials - Property of IBM
 *
 * IBM Cognos Products: SDK Support
 *
 * (C) Copyright IBM Corp. 2013
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure restricted by GSA ADP Schedule Contract with
 * IBM Corp.
 * 
 * GetSQL.java
 *
 * Description: Technote 1344260 - SDK Sample to extract the SQL statement from a report
 *
 * Tested with: IBM Cognos BI 10.2.1, IBM Java 6.0., Axis 1.4
 * 
 */

import javax.xml.namespace.QName;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;

import com.cognos.developer.schemas.bibus._3.AsynchDetail;
import com.cognos.developer.schemas.bibus._3.AsynchDetailReportValidation;
import com.cognos.developer.schemas.bibus._3.AsynchReply;
import com.cognos.developer.schemas.bibus._3.AsynchReplyStatusEnum;
import com.cognos.developer.schemas.bibus._3.AsynchSecondaryRequest;
import com.cognos.developer.schemas.bibus._3.BiBusHeader;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.DispatcherTransportVar;
import com.cognos.developer.schemas.bibus._3.Option;
import com.cognos.developer.schemas.bibus._3.ParameterValue;
import com.cognos.developer.schemas.bibus._3.ReportService_PortType;
import com.cognos.developer.schemas.bibus._3.ReportService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.SearchPathSingleObject;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;

public class GetSQL1
{

	//TODO: change the following variables depending on your installation and setup

	//--- begin changes ----
	private static String dispatcherURL = "http://cogad1.int.corp.sun:9300/p2pd/servlet/dispatch";

	//Change the next three variables to a valid namespace ID as defined in Cognos Configuration and NOT the namespace name
	//Provide a valid user ID and password if Anonymous is disabled
	private static String nameSpaceID 	= "INT";
	private static String userName 		= "S102991";
	private static String password 		= "c0gn05batch";
	
	private String reportPath 			= "CAMID('INT:u:0a50606a26a37c4e99d560cfe61b9c2c')/folder[@name='My Folders']/folder[@name='Jean']/analysis[@name='test']";
	//--- end changes ---

	//Services to be used in the sample
	private ContentManagerService_PortType cmService = null;
	private ReportService_PortType reportService = null;

	private final String BUS_NS = "http://developer.cognos.com/schemas/bibus/3/";
	private final String BUS_HEADER = "biBusHeader";

	/**
	 * Using the validate() method of the Report Service the executed SQL is retrieved
	 */
	private void doWork() 
	{
		SearchPathSingleObject reportSearchPath = new SearchPathSingleObject(reportPath);
		ParameterValue[] params = new ParameterValue[]{};
		Option[] options = new Option[]{};
		
		try 
		{
			AsynchReply reply = getReportService().validate(reportSearchPath, params, options);

			if (!(reply.getStatus().equals(AsynchReplyStatusEnum.complete)) && !(reply.getStatus().equals(AsynchReplyStatusEnum.conversationComplete))) 
			{
				while (!(reply.getStatus().equals(AsynchReplyStatusEnum.complete)) && !(reply.getStatus().equals(AsynchReplyStatusEnum.conversationComplete))) 
				{
					// before calling wait, double check that it is okay
					if (hasSecondaryRequest(reply, "wait")) 
					{
						reply = getReportService(false).wait(reply.getPrimaryRequest(), new ParameterValue[] {}, new Option[] {});
					}
				}
			}
			
			AsynchDetail[] details = reply.getDetails();
			for (int i = 0; i < details.length; i++) 
			{
				if (details[i] instanceof AsynchDetailReportValidation) 
				{
					String out = ((AsynchDetailReportValidation) details[i]).getQueryInfo().get_value();
					System.out.println(out);
				}
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

	/**
	 * Establish a connection to Cognos BI and initialize the different services
	 */
	private void connectToCognos() 
	{
		ContentManagerService_ServiceLocator cmServiceLocator = new ContentManagerService_ServiceLocator();
		ReportService_ServiceLocator reportServiceLocator=new ReportService_ServiceLocator();

		try 
		{
			cmService = cmServiceLocator.getcontentManagerService(new java.net.URL(dispatcherURL));
			reportService = reportServiceLocator.getreportService(new java.net.URL(dispatcherURL));
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

	/**
	 * Logon to Cognos BI using valid credentials information
	 */
	private void logon(String nameSpaceID, String userName, String password) 
	{
		StringBuffer credentialXML = new StringBuffer();

		credentialXML.append("<credential>");
		credentialXML.append("<namespace>").append(nameSpaceID).append("</namespace>");
		credentialXML.append("<username>").append(userName).append("</username>");
		credentialXML.append("<password>").append(password).append("</password>");
		credentialXML.append("</credential>");

		String encodedCredentials = credentialXML.toString();
		XmlEncodedXML xmlCredentials = new XmlEncodedXML();
		xmlCredentials.set_value(encodedCredentials);

		try 
		{
			cmService.logon(xmlCredentials, null);
			getSetHeaders();
		} 
		catch (Exception ex) 
		{
			ex.printStackTrace();
		}
	}

	private void getSetHeaders()
	{
		BiBusHeader CMbibus = null;

		SOAPHeaderElement temp = ((Stub)cmService).getResponseHeader(BUS_NS, BUS_HEADER);

		try 
		{
			CMbibus = (BiBusHeader)temp.getValueAsType(new QName (BUS_NS, BUS_HEADER));
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}

		if (CMbibus != null)
		{
			((Stub)cmService).setHeader(BUS_NS, BUS_HEADER, CMbibus);
		}
	}

	private ReportService_PortType getReportService()
	{
		return getReportService(true);
	}
	
	private ReportService_PortType getReportService(boolean isNewConversation)
	{
		BiBusHeader bibus = getHeaderObject(((Stub)reportService).getResponseHeader(BUS_NS, BUS_HEADER), isNewConversation);

		if (bibus == null) 
		{
			bibus = getHeaderObject(((Stub)cmService).getResponseHeader(BUS_NS, BUS_HEADER), true);	
		}

		((Stub)reportService).clearHeaders();
		((Stub)reportService).setHeader(BUS_NS, BUS_HEADER, bibus);	

		return reportService;
	}
		
	private BiBusHeader getHeaderObject(SOAPHeaderElement SourceHeader, boolean isNewConversation)
	{
		final QName BUS_QNAME = new QName(BUS_NS, BUS_HEADER);
		BiBusHeader bibus = null;

		if (SourceHeader == null)
		{
			return null;			
		}

		try 
		{
			bibus = (BiBusHeader)SourceHeader.getValueAsType(BUS_QNAME);

			// If the header will be used for a new conversation, clear
			// tracking information, DispatcherTransportVars and Routing values
			if (isNewConversation)
			{
				bibus.setTracking(null);
				bibus.setDispatcherTransportVars(new DispatcherTransportVar[]{});
				bibus.setRouting(null);
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}

		return bibus;
	}
	
	private boolean hasSecondaryRequest(AsynchReply response, String secondaryRequest) 
	{
		AsynchSecondaryRequest[] secondaryRequests = response.getSecondaryRequests();
		
		if (secondaryRequests != null) 
		{
			for (int i = 0; i < secondaryRequests.length; i++) 
			{
				if (secondaryRequests[i].getName().compareTo(secondaryRequest) == 0) 
				{
					return true;
				}
			}
		}
		
		return false;
	}

	/**
	 * This is the main class, which connects to Cognos BI and calls the
	 * method doWork(), containing the specific code for this sample
	 */
	public static void main(String args[]) 
	{
		GetSQL1 mainClass = new GetSQL1();

		mainClass.connectToCognos();

		// If Anonymous is disabled, then logon
		if (nameSpaceID.length() > 0) 
		{
			mainClass.logon(nameSpaceID, userName, password);
		}

		// do the main work
		mainClass.doWork();
	}
}