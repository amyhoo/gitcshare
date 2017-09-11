package MetaData;

/**
 * GetCubeMetadata.java
 *
 * Licensed Material - Property of IBM
 * � Copyright IBM Corp. 2003, 2010 
 *
 * Description: Technote 1339532 - SDK sample to retrieve the metadata from a cube
 * 
 * Tested with: IBM Cognos BI 10.1, IBM Java 5.0 
 * 
 */
import javax.xml.rpc.ServiceException;
import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import javax.xml.namespace.QName;


import com.cognos.developer.schemas.bibus._3.*;

public class GetCubeMetadata {

	private ContentManagerService_PortType cmService = null;
	private ReportService_PortType reportService = null;
	  
	//connect to Cognos BI services
	public void connectToReportServer (String endPoint)
	{
		try
		{
			ContentManagerService_ServiceLocator cmServiceLocator = new ContentManagerService_ServiceLocator();   
		  	ReportService_ServiceLocator  rsServiceLocator = new ReportService_ServiceLocator();
		
	      	cmService = cmServiceLocator.getcontentManagerService(new java.net.URL(endPoint));
	      	reportService = rsServiceLocator.getreportService(new java.net.URL(endPoint));
	      
		}
		catch (ServiceException ex)
		{
			System.out.println("Caught a ServiceException: " + ex.getMessage());
			ex.printStackTrace();
		}
		catch (Exception ex)
		{
			System.out.println("Cognos BI server URL was: " + endPoint);
		}
	}
	
	String getOutput(AsynchReply res)
	{
		AsynchDetail[] details = res.getDetails();	
		for (int i = 0; i < details.length; i++)
		{
			if (details[i] instanceof AsynchDetailReportOutput)
			{
				String[] results =  ((AsynchDetailReportOutput)details[i]).getOutputPages();
				if (results.length > 0){
					return results[0];
				}
			} 
			else if (details[i] instanceof AsynchDetailReportMetadata) 
			{
				XmlEncodedXML result =  ((AsynchDetailReportMetadata)details[i]).getMetadata();
				if (result != null)
				{
					return result.get_value();
				}
			}
		}
		return null;
	}

	boolean outputIsReady(AsynchReply response)
	{
		for (int i = 0; i < response.getDetails().length; i++)
		{
			if ((response.getDetails()[i] instanceof AsynchDetailReportStatus)
				&& (((AsynchDetailReportStatus)response.getDetails()[i])
					.getStatus()
					== AsynchDetailReportStatusEnum.responseReady)
				&& (hasSecondaryRequest(response, "getOutput")))
			{
				return true;
			}
		}
		return false;
	}

	boolean hasSecondaryRequest(AsynchReply response,	String secondaryRequest)
	{
		AsynchSecondaryRequest[] secondaryRequests =
			response.getSecondaryRequests();
		for (int i = 0; i < secondaryRequests.length; i++)
		{
			if (secondaryRequests[i].getName().compareTo(secondaryRequest)
				== 0)
			{
				return true;
			}
		}
		return false;
	}

	public String getMetadata(String metarequest)
	{
		String xmlResult = null;
	
		AsynchReply res = null;
		ParameterValue pv[] = new ParameterValue[] { };
		Option options[] = new Option[2];
		RunOptionBoolean rob = new RunOptionBoolean();
		RunOptionInt roPrimary = new RunOptionInt();
		roPrimary.setName(RunOptionEnum.primaryWaitThreshold);
		roPrimary.setValue(0);
	
		// Do not prompt me.
		rob.setName( RunOptionEnum.prompt );
		rob.setValue( false );
	
		//Fill the array with the run options.
		options[0] = rob;
		options[1] = roPrimary;
	
		ReportServiceMetadataSpecification metaspec = new ReportServiceMetadataSpecification();
		Specification spec = new Specification();
		XmlEncodedXML encodedSpec = new XmlEncodedXML();
		encodedSpec.set_value(metarequest);
		spec.set_value(encodedSpec.toString());
		metaspec.setValue(spec);
		try {
			//Get the initial response.
			res = reportService.runSpecification(metaspec, pv, options );
			
			AsynchReplyStatusEnum status = res.getStatus();
			if(status.equals(AsynchReplyStatusEnum.complete) || status.equals(AsynchReplyStatusEnum.conversationComplete))
			{
				xmlResult = getOutput(res);
				
				if(xmlResult == null  && outputIsReady (res) ) 
				{
					AsynchReply outputResp = reportService.getOutput( res.getPrimaryRequest(), pv,  new Option[] {});
					xmlResult = getOutput(outputResp);
				}
			}
			else
			{
				System.out.println("Unable to retrieve the metadata.  The runSpecification response is: " + res.getStatus().getValue());
			}
		}
		catch (Exception e)
		{
			if ( e.getClass() == org.apache.axis.AxisFault.class ) 
			{
				System.out.println(((org.apache.axis.AxisFault)e).dumpToString());
			}
			else 
			{
				System.out.println(e.getMessage());
			}
			System.out.println(e);
		}
		return xmlResult;
	}
	
	//	logon to Cognos BI
	public String quickLogon(String namespace, String uid, String pwd)
	{
		StringBuffer credentialXML = new StringBuffer();

		credentialXML.append("<credential>");
		credentialXML.append("<namespace>").append(namespace).append("</namespace>");
		credentialXML.append("<username>").append(uid).append("</username>");
		credentialXML.append("<password>").append(pwd).append("</password>");
		credentialXML.append("</credential>");

		String encodedCredentials = credentialXML.toString();
	    XmlEncodedXML xmlCredentials = new XmlEncodedXML();
	    xmlCredentials.set_value(encodedCredentials);
		try
		{		
			cmService.logon(xmlCredentials,null );

			SOAPHeaderElement temp = ((Stub)cmService).getResponseHeader 
				("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader");
			BiBusHeader cmBiBusHeader = (BiBusHeader)temp.getValueAsType
				(new QName ("http://developer.cognos.com/schemas/bibus/3/","biBusHeader"));
			((Stub)cmService).setHeader
				("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", cmBiBusHeader);
			((Stub)reportService).setHeader
				("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", cmBiBusHeader);



		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return ("Logon successful as " + uid);
	}
	
	public static void main(String[] args) 
	{
		String namespaceID = "INT";
		String userID = "S102991";
		String password = "c0gn05batch";
		String endPoint = "http://cogad1.int.corp.sun:9300/p2pd/servlet/dispatch";
		String cubeSearchPath = "CAMID('INT:u:0a50606a26a37c4e99d560cfe61b9c2c')/folder[@name='My Folders']/folder[@name='Jean']/package[@name='CTP Policy DMR']";

//		Retrieve all the functions
//		String metarequest = "<metadataRequest connection=\"" + cubeSearchPath + "/model[last()]\"><Functions authoringLocale=\"en-us\"><Properties><Property name=\"/@listSeparator\"/><Property name=\"*/@name\"/><Property name=\"*/@description\"/><Property name=\"./function\"/><Property name=\"function/@name\"/></Properties></Functions></metadataRequest>";

//		Retrieve all the measures
//		String metarequest = "<metadataRequest connection=\"" + cubeSearchPath + "/model[last()]\"><Metadata authoringLocale=\"en-us\" xml:lang=\"\" Depth=\"0\" start_atPath=\"\" no_collections=\"1\" _enumLabels=\"1\"><Properties><Property name='*/@name'/><Property name='*/@_path'/><Property name='./folder'/><Property name='./measure'/></Properties></Metadata></metadataRequest>";

// 		Change the starting path
//		String metarequest = "<metadataRequest connection=\"" + cubeSearchPath + "/model[last()]\"><Metadata authoringLocale=\"en-us\" xml:lang=\"\" Depth=\"0\" start_atPath=\"[great_outdoor_sales_en]\" no_collections=\"1\" _enumLabels=\"1\"><Properties><Property name='*/@name'/><Property name='*/@_path'/><Property name='./folder'/><Property name='./measure'/></Properties></Metadata></metadataRequest>";

//		Note that Functions elements and Metadata elements cannot be combined in a single metadataRequest. Only the first element is processed, any others are ignored.

// 		Retrieve the queryItem expression where the queryItem is contained in a path that includes the following (folder, dimension, hierarchy, level) 
//		All the intervening elements must be included, except for dimension, which is always returned whenever .folder is included. Note also that level/@* (attributes wildcard) only works in Cognos BI 8.2
//		String metarequest = "<metadataRequest connection=\"" + cubeSearchPath + "/model[last()]\"><Metadata authoringLocale=\"en-us\" xml:lang=\"\" Depth=\"0\" start_atPath=\"\" no_collections=\"1\" _enumLabels=\"1\"><Properties><Property name='level/@*'/><Property name='queryItem/@expression'/><Property name='*/@name'/><Property name='./folder'/><Property name='./hierarchy'/><Property name='./level'/><Property name='./queryItem'/></Properties></Metadata></metadataRequest>";

//		Restrict the depth to 2 levels		
//		String metarequest = "<metadataRequest connection=\"" + cubeSearchPath + "/model[last()]\"><Metadata authoringLocale=\"en-us\" xml:lang=\"\" Depth=\"2\" start_atPath=\"\" no_collections=\"1\" _enumLabels=\"1\"><Properties><Property name=\"*/@name\"/><Property name=\"*/@_path\"/><Property name=\"*/@_ref\"/><Property name=\"*/@isNamespace\"/><Property name=\"*/@screenTip\"/><Property name=\"*/@description\"/><Property name=\"*/@calcType\"/><Property name=\"*/@parentChild\"/><Property name=\"./folder\"/><Property name=\"./measureFolder\"/><Property name=\"./querySubject\"/><Property name=\"./queryItem\"/><Property name=\"./queryItemFolder\"/><Property name=\"./filter\"/><Property name=\"./calculation\"/><Property name=\"queryItem/@datatype\"/><Property name=\"queryItem/@currency\"/><Property name=\"queryItem/@usage\"/><Property name=\"queryItem/@regularAggregate\"/><Property name=\"queryItem/@promptType\"/><Property name=\"queryItem/@promptFilterItemRef\"/><Property name=\"queryItem/@promptDisplayItemRef\"/><Property name=\"queryItem/@promptCascadeOnRef\"/><Property name=\"queryItem/@unSortable\"/><Property name=\"queryItem/@displayType\"/><Property name=\"calculation/@currency\"/><Property name=\"calculation/@usage\"/><Property name=\"calculation/@regularAggregate\"/><Property name=\"calculation/@promptType\"/><Property name=\"calculation/@promptFilterItemRef\"/><Property name=\"calculation/@promptDisplayItemRef\"/><Property name=\"calculation/@promptCascadeOnRef\"/><Property name=\"calculation/@unSortable\"/><Property name=\"calculation/@displayType\"/><Property name=\"measure/@isHierarchical\"/><Property name=\"dimension/@type\"/><Property name=\"./dimension\"/><Property name=\"./hierarchy\"/><Property name=\"./level\"/><Property name=\"./measure\"/><Property name=\"measure/@datatype\"/><Property name=\"measure/@currency\"/><Property name=\"*/@_IntrinsicPropertiesOff\"/></Properties></Metadata></metadataRequest>";

//		Remove the depth restriction by setting it to 0
		String metarequest = "<metadataRequest connection=\"" + cubeSearchPath + "/model[last()]\"><Metadata authoringLocale=\"en-us\" xml:lang=\"\" Depth=\"0\" start_atPath=\"\" no_collections=\"1\" _enumLabels=\"1\"><Properties><Property name=\"*/@name\"/><Property name=\"*/@_path\"/><Property name=\"*/@_ref\"/><Property name=\"*/@isNamespace\"/><Property name=\"*/@screenTip\"/><Property name=\"*/@description\"/><Property name=\"*/@calcType\"/><Property name=\"*/@parentChild\"/><Property name=\"./folder\"/><Property name=\"./measureFolder\"/><Property name=\"./querySubject\"/><Property name=\"./queryItem\"/><Property name=\"./queryItemFolder\"/><Property name=\"./filter\"/><Property name=\"./calculation\"/><Property name=\"queryItem/@datatype\"/><Property name=\"queryItem/@currency\"/><Property name=\"queryItem/@usage\"/><Property name=\"queryItem/@regularAggregate\"/><Property name=\"queryItem/@promptType\"/><Property name=\"queryItem/@promptFilterItemRef\"/><Property name=\"queryItem/@promptDisplayItemRef\"/><Property name=\"queryItem/@promptCascadeOnRef\"/><Property name=\"queryItem/@unSortable\"/><Property name=\"queryItem/@displayType\"/><Property name=\"calculation/@currency\"/><Property name=\"calculation/@usage\"/><Property name=\"calculation/@regularAggregate\"/><Property name=\"calculation/@promptType\"/><Property name=\"calculation/@promptFilterItemRef\"/><Property name=\"calculation/@promptDisplayItemRef\"/><Property name=\"calculation/@promptCascadeOnRef\"/><Property name=\"calculation/@unSortable\"/><Property name=\"calculation/@displayType\"/><Property name=\"measure/@isHierarchical\"/><Property name=\"dimension/@type\"/><Property name=\"./dimension\"/><Property name=\"./hierarchy\"/><Property name=\"./level\"/><Property name=\"./measure\"/><Property name=\"measure/@datatype\"/><Property name=\"measure/@currency\"/><Property name=\"*/@_IntrinsicPropertiesOff\"/></Properties></Metadata></metadataRequest>";

		GetCubeMetadata gc = new GetCubeMetadata();
		gc.connectToReportServer(endPoint);
		// If Anonymous used comment out the logon on call
		gc.quickLogon(namespaceID, userID, password);
		String meta = gc.getMetadata(metarequest);
		System.out.println(meta);
	}
}
