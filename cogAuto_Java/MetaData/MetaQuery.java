package MetaData;

import com.cognos.developer.schemas.bibus._3.AsynchDetail;
import com.cognos.developer.schemas.bibus._3.AsynchDetailReportMetadata;
import com.cognos.developer.schemas.bibus._3.AsynchDetailReportOutput;
import com.cognos.developer.schemas.bibus._3.AsynchDetailReportStatus;
import com.cognos.developer.schemas.bibus._3.AsynchDetailReportStatusEnum;
import com.cognos.developer.schemas.bibus._3.AsynchReply;
import com.cognos.developer.schemas.bibus._3.AsynchReplyStatusEnum;
import com.cognos.developer.schemas.bibus._3.AsynchSecondaryRequest;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.MetadataService_PortType;
import com.cognos.developer.schemas.bibus._3.Option;
import com.cognos.developer.schemas.bibus._3.ParameterValue;
import com.cognos.developer.schemas.bibus._3.ReportServiceMetadataSpecification;
import com.cognos.developer.schemas.bibus._3.ReportService_PortType;
import com.cognos.developer.schemas.bibus._3.RunOptionBoolean;
import com.cognos.developer.schemas.bibus._3.RunOptionEnum;
import com.cognos.developer.schemas.bibus._3.RunOptionInt;
import com.cognos.developer.schemas.bibus._3.Specification;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;

import ContentManager.ContentQuery;
import common.CognosLogOn;

public class MetaQuery {
	CognosLogOn conn= null;
	ContentManagerService_PortType cmService=null;
	ReportService_PortType reportService = null;
	MetadataService_PortType metadataService = null;
	public void init(){
		conn=new CognosLogOn("DEV");
		cmService=conn.getContentManagerService();
		reportService=conn.getReportService();
		metadataService=conn.getMetadataService();
		conn.logonToCognos();
	}
	public static void main(String args[]) {
		ContentQuery mainClass = new ContentQuery(); // instantiate the class
		mainClass.init();
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
}
