package common;

import java.util.HashMap;
import java.util.Map;

import org.apache.axis.AxisEngine;
import org.apache.axis.client.Stub;

import com.cognos.developer.schemas.bibus._3.AgentService_PortType;
import com.cognos.developer.schemas.bibus._3.BatchReportService_PortType;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.DataIntegrationService_PortType;
import com.cognos.developer.schemas.bibus._3.DeliveryService_PortType;
import com.cognos.developer.schemas.bibus._3.DimensionManagementService_PortType;
import com.cognos.developer.schemas.bibus._3.Dispatcher_PortType;
import com.cognos.developer.schemas.bibus._3.EventManagementService_PortType;
import com.cognos.developer.schemas.bibus._3.JobService_PortType;
import com.cognos.developer.schemas.bibus._3.MetadataService_PortType;
import com.cognos.developer.schemas.bibus._3.MonitorService_PortType;
import com.cognos.developer.schemas.bibus._3.QueryService_PortType;
import com.cognos.developer.schemas.bibus._3.ReportService_PortType;
import com.cognos.developer.schemas.bibus._3.SystemService_PortType;

public class CognosOperation {	
	protected String environment = null;
	protected CognosLogOn conn = null;
	protected ContentManagerService_PortType cmService = null;
	protected ReportService_PortType reportService = null;
	protected MonitorService_PortType monitorService = null;
	protected AgentService_PortType agentService = null;
	protected BatchReportService_PortType batchRepService = null;
	protected DataIntegrationService_PortType dataIntService = null;
	protected DeliveryService_PortType deliveryService = null;
	protected EventManagementService_PortType eventMgmtService = null;
	protected JobService_PortType jobService = null;
	protected QueryService_PortType queryService = null;
	protected SystemService_PortType sysService = null;
	protected Dispatcher_PortType dispatchService = null;
	protected DimensionManagementService_PortType dimensionMgmtService = null;
	protected MetadataService_PortType metadataService = null;	
	public void logon(String environment) {
		this.environment=environment;
		conn = new CognosLogOn(environment);
		cmService = conn.getContentManagerService();
		conn.logonToCognos();		
	}	
	public <T extends java.rmi.Remote> void initService( T service){
		
		if (service==reportService){
			reportService=conn.getReportService();
		}
		else if(service==monitorService){
			monitorService=conn.getMonitorService();			
		}
		else if(service==agentService){
			agentService=conn.getAgentService();
		}
		else if(service==batchRepService){
			batchRepService=conn.getBatchReportService();
		}
		else if(service==dataIntService){
			dataIntService=conn.getDataIntegrationService();
		}
		else if(service==deliveryService){
			deliveryService=conn.getDeliveryService();
		}
		else if(service==eventMgmtService){
			eventMgmtService=conn.getEventManagementService();
		}
		else if(service==jobService){
			jobService=conn.getJobService();
		}
		else if(service==queryService){
			queryService=conn.getQueryService();
		}
		else if(service==sysService){
			sysService=conn.getSystemService();
		}
		else if(service==dispatchService){
			dispatchService=conn.getDispatcher();
		}
		else if(service==dimensionMgmtService){
			dimensionMgmtService=conn.getDimensionManagementService();
		}
		else if(service==metadataService){
			metadataService=conn.getMetadataService();
		}		
		
	}
}
