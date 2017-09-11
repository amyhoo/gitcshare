package Report;
import javax.xml.namespace.QName;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;

import com.cognos.developer.schemas.bibus._3.AsynchDetail;
import com.cognos.developer.schemas.bibus._3.AsynchDetailReportValidation;
import com.cognos.developer.schemas.bibus._3.AsynchReply;
import com.cognos.developer.schemas.bibus._3.AsynchReplyStatusEnum;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.Option;
import com.cognos.developer.schemas.bibus._3.ParameterValue;
import com.cognos.developer.schemas.bibus._3.ReportService_PortType;
import com.cognos.developer.schemas.bibus._3.SearchPathSingleObject;

import AutoDeploy.Deployment;
import common.CognosLogOn;
import common.CognosOperation;

public class GetReportSQL extends CognosOperation{
	
	public static void main(String args[]){
		GetReportSQL current = new GetReportSQL();
		current.logon("DEV");	
		current.initService(current.reportService);
		current.getSQL("CAMID('INT:u:0a50606a26a37c4e99d560cfe61b9c2c')/folder[@name='My Folders']/folder[@name='Jean']/analysis[@name='test']");
	}
	private ReportService_PortType getReportService(boolean newRequest){
		reportService=(ReportService_PortType)conn.ServiceWithHeader((Stub)conn.reportService,newRequest);
		return reportService;
	}
	public String getSQL(String reportPath){
		SearchPathSingleObject reportSearchPath = new SearchPathSingleObject(reportPath);
		ParameterValue[] params = new ParameterValue[]{};
		Option[] options = new Option[]{};
		
		try 
		{
			AsynchReply reply = getReportService(true).validate(reportSearchPath, params, options);

			if (!(reply.getStatus().equals(AsynchReplyStatusEnum.complete)) && !(reply.getStatus().equals(AsynchReplyStatusEnum.conversationComplete))) 
			{
				while (!(reply.getStatus().equals(AsynchReplyStatusEnum.complete)) && !(reply.getStatus().equals(AsynchReplyStatusEnum.conversationComplete))) 
				{
					// before calling wait, double check that it is okay
					if (conn.hasSecondaryRequest(reply, "wait")) 
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
					return out;
				}
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}		
		return "";
	}
}
