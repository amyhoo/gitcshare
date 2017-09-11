package common;

import java.util.*;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import com.cognos.developer.schemas.bibus._3.*;

import org.apache.axis.client.Service;
import java.rmi.Remote;

public class CognosLogOn {
	private String dispatcherURL = "";
	private String nameSpaceID = "";
	private String username = "";//
	private String password = "";//
	static final String BUS_NS = "http://developer.cognos.com/schemas/bibus/3/";
	static final String BUS_HEADER = "biBusHeader";
	public ContentManagerService_PortType cmService = null;
	public ReportService_PortType reportService = null;
	public MonitorService_PortType monitorService = null;
	public AgentService_PortType agentService = null;
	public BatchReportService_PortType batchRepService = null;
	public DataIntegrationService_PortType dataIntService = null;
	public DeliveryService_PortType deliveryService = null;
	public EventManagementService_PortType eventMgmtService = null;
	public JobService_PortType jobService = null;
	public QueryService_PortType queryService = null;
	public SystemService_PortType sysService = null;
	public Dispatcher_PortType dispatchService = null;
	public DimensionManagementService_PortType dimensionMgmtService = null;
	public MetadataService_PortType metadataService = null;

	public  CognosLogOn() {
		this("DEV");
	}


	public  CognosLogOn(String portType) {
		switch (portType) {
		case "DEV":
			dispatcherURL = "http://cogad1.int.corp.sun:9300/p2pd/servlet/dispatch";
			nameSpaceID = "INT";
			username = "S102991";
			password = "c0gn05batch";
			break;
		case "UAT":
			dispatcherURL = "http://cogat1.int.corp.sun:9300/p2pd/servlet/dispatch";
			nameSpaceID = "INT";
			username = "s103946";
			password = "c0gXIpr0d";
			break;
		case "PRD":
			dispatcherURL = "http://cogap1.int.corp.sun:9300/p2pd/servlet/dispatch";
			nameSpaceID = "INT";
			username = "S103946";
			password = "c0gXIpr0d";
			break;
		}
	}

	public static void main(String args[]) {
		CognosLogOn mainClass = new CognosLogOn(); // instantiate the class
		mainClass.username = args[0];
		mainClass.password = args[1];

		// Step 1: Connect to the Cognos services
		mainClass.getContentManagerService(null);

		// Step 2: Logon to Cognos
		mainClass.logonToCognos();

		// Step 3: Logoff from Cognos
		mainClass.logoffFromCognos();
	}

	// public Stub getService(Service locator,Map options){
	//
	// }
	public ContentManagerService_PortType getContentManagerService(){
		return getContentManagerService(null);
	}
	public ContentManagerService_PortType getContentManagerService(Map<String, Object> options) {
		ContentManagerService_ServiceLocator ServiceLocator = new ContentManagerService_ServiceLocator();

		if (options != null) {
			for (String key : options.keySet()) {
				ServiceLocator.getEngine().setOption(key, options.get(key));
			}
		}

		try {
			URL url = new URL(dispatcherURL);
			cmService = ServiceLocator.getcontentManagerService(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cmService;
	}
	public ReportService_PortType getReportService() {
		return getReportService(null);
	}
	public ReportService_PortType getReportService(Map<String, Object> options) {
		ReportService_ServiceLocator ServiceLocator = new ReportService_ServiceLocator();

		if (options != null) {
			for (String key : options.keySet()) {
				ServiceLocator.getEngine().setOption(key, options.get(key));
			}
		}

		try {
			URL url = new URL(dispatcherURL);
			reportService = ServiceLocator.getreportService(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return reportService;
	}
	public MonitorService_PortType getMonitorService(){
		return getMonitorService(null);
	}
	public MonitorService_PortType getMonitorService(Map<String, Object> options) {
		MonitorService_ServiceLocator ServiceLocator = new MonitorService_ServiceLocator();
		if (options != null) {
			for (String key : options.keySet()) {
				ServiceLocator.getEngine().setOption(key, options.get(key));
			}
		}
		try {
			URL url = new URL(dispatcherURL);
			monitorService = ServiceLocator.getmonitorService(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return monitorService;
	}
	public AgentService_PortType getAgentService(){
		return getAgentService(null);
	}
	public AgentService_PortType getAgentService(Map<String, Object> options) {
		AgentService_ServiceLocator ServiceLocator = new AgentService_ServiceLocator();

		if (options != null) {
			for (String key : options.keySet()) {
				ServiceLocator.getEngine().setOption(key, options.get(key));
			}
		}
		try {
			URL url = new URL(dispatcherURL);
			agentService = ServiceLocator.getagentService(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return agentService;
	}
	public BatchReportService_PortType getBatchReportService(){
		return getBatchReportService(null);
	}
	public BatchReportService_PortType getBatchReportService(Map<String, Object> options) {
		BatchReportService_ServiceLocator ServiceLocator = new BatchReportService_ServiceLocator();

		if (options != null) {
			for (String key : options.keySet()) {
				ServiceLocator.getEngine().setOption(key, options.get(key));
			}
		}

		try {
			URL url = new URL(dispatcherURL);
			batchRepService = ServiceLocator.getbatchReportService(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return batchRepService;
	}
	public DataIntegrationService_PortType getDataIntegrationService(){
		return getDataIntegrationService(null);
	}
	public DataIntegrationService_PortType getDataIntegrationService(Map<String, Object> options) {
		DataIntegrationService_ServiceLocator ServiceLocator = new DataIntegrationService_ServiceLocator();

		if (options != null) {
			for (String key : options.keySet()) {
				ServiceLocator.getEngine().setOption(key, options.get(key));
			}
		}

		try {
			URL url = new URL(dispatcherURL);
			dataIntService = ServiceLocator.getdataIntegrationService(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataIntService;
	}
	public DeliveryService_PortType getDeliveryService() {
		return getDeliveryService(null);
	}
	public DeliveryService_PortType getDeliveryService(Map<String, Object> options) {
		DeliveryService_ServiceLocator ServiceLocator = new DeliveryService_ServiceLocator();

		if (options != null) {
			for (String key : options.keySet()) {
				ServiceLocator.getEngine().setOption(key, options.get(key));
			}
		}

		try {
			URL url = new URL(dispatcherURL);
			deliveryService = ServiceLocator.getdeliveryService(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return deliveryService;
	}
	public EventManagementService_PortType getEventManagementService() {
		return getEventManagementService(null);
	}
	public EventManagementService_PortType getEventManagementService(Map<String, Object> options) {
		EventManagementService_ServiceLocator ServiceLocator = new EventManagementService_ServiceLocator();

		if (options != null) {
			for (String key : options.keySet()) {
				ServiceLocator.getEngine().setOption(key, options.get(key));
			}
		}

		try {
			URL url = new URL(dispatcherURL);
			eventMgmtService = ServiceLocator.geteventManagementService(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return eventMgmtService;
	}
	public JobService_PortType getJobService() {
		return getJobService(null);
	}
	public JobService_PortType getJobService(Map<String, Object> options) {
		JobService_ServiceLocator ServiceLocator = new JobService_ServiceLocator();

		if (options != null) {
			for (String key : options.keySet()) {
				ServiceLocator.getEngine().setOption(key, options.get(key));
			}
		}

		try {
			URL url = new URL(dispatcherURL);
			jobService = ServiceLocator.getjobService(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jobService;
	}
	public QueryService_PortType getQueryService() {
		return getQueryService(null);
	}
	public QueryService_PortType getQueryService(Map<String, Object> options) {
		QueryService_ServiceLocator ServiceLocator = new QueryService_ServiceLocator();

		if (options != null) {
			for (String key : options.keySet()) {
				ServiceLocator.getEngine().setOption(key, options.get(key));
			}
		}

		try {
			URL url = new URL(dispatcherURL);
			queryService = ServiceLocator.getqueryService(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return queryService;
	}
	public SystemService_PortType getSystemService(){
		return getSystemService(null);
	}
	public SystemService_PortType getSystemService(Map<String, Object> options) {
		SystemService_ServiceLocator ServiceLocator = new SystemService_ServiceLocator();

		if (options != null) {
			for (String key : options.keySet()) {
				ServiceLocator.getEngine().setOption(key, options.get(key));
			}
		}

		try {
			URL url = new URL(dispatcherURL);
			sysService = ServiceLocator.getsystemService(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sysService;
	}
	public Dispatcher_PortType getDispatcher() {
		return getDispatcher(null);
	}
	public Dispatcher_PortType getDispatcher(Map<String, Object> options) {
		Dispatcher_ServiceLocator ServiceLocator = new Dispatcher_ServiceLocator();

		if (options != null) {
			for (String key : options.keySet()) {
				ServiceLocator.getEngine().setOption(key, options.get(key));
			}
		}

		try {
			URL url = new URL(dispatcherURL);
			dispatchService = ServiceLocator.getdispatcher(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dispatchService;
	}
	public DimensionManagementService_PortType getDimensionManagementService(){
		return getDimensionManagementService(null);
	}
	public DimensionManagementService_PortType getDimensionManagementService(Map<String, Object> options) {
		DimensionManagementService_ServiceLocator ServiceLocator = new DimensionManagementService_ServiceLocator();

		if (options != null) {
			for (String key : options.keySet()) {
				ServiceLocator.getEngine().setOption(key, options.get(key));
			}
		}

		try {
			URL url = new URL(dispatcherURL);
			dimensionMgmtService = ServiceLocator.getdimensionManagementService(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dimensionMgmtService;
	}
	public MetadataService_PortType getMetadataService(){
		return getMetadataService(null);
	}
	public MetadataService_PortType getMetadataService(Map<String, Object> options) {
		MetadataService_ServiceLocator ServiceLocator = new MetadataService_ServiceLocator();

		if (options != null) {
			for (String key : options.keySet()) {
				ServiceLocator.getEngine().setOption(key, options.get(key));
			}
		}

		try {
			URL url = new URL(dispatcherURL);
			metadataService = ServiceLocator.getmetadataService(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return metadataService;
	}

	public void logonToCognos_no_encrypted(String nsID, String user, String pswd) {
		try {
			// Sort options: ascending sort on the defaultName property.
			//
			// The cmQuery.pl sample doesn't do this, it returns the default
			// unsorted response.
			Sort[] sortBy = { new Sort() };
			sortBy[0].setOrder(OrderEnum.ascending);
			sortBy[0].setPropName(PropEnum.defaultName);

			// Query options; use the defaults.
			QueryOptions options = new QueryOptions();

			// Add the authentication information, if any.
			//
			// Another option would be to use the logon() and logonAs()
			// methods...
			CAM cam = new CAM();
			cam.setAction("logonAs");

			HdrSession header = new HdrSession();
			if (user != null) {
				FormFieldVar[] vars = new FormFieldVar[3];
				vars[0] = new FormFieldVar();
				vars[0].setName("CAMNamespace");
				vars[0].setValue(nsID);
				vars[0].setFormat(FormatEnum.not_encrypted);

				vars[1] = new FormFieldVar();
				vars[1].setName("CAMUsername");
				vars[1].setValue(user);
				vars[1].setFormat(FormatEnum.not_encrypted);

				vars[2] = new FormFieldVar();
				vars[2].setName("CAMPassword");
				vars[2].setValue(pswd);
				vars[2].setFormat(FormatEnum.not_encrypted);

				header.setFormFieldVars(vars);
			} else {
				cam.setAction("logon");
			}

			BiBusHeader bibus = new BiBusHeader();
			bibus.setCAM(cam);
			bibus.setHdrSession(header);
			((Stub) cmService).setHeader(BUS_NS, BUS_HEADER, bibus);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	// Step 2: Logon to Cognos
	public void logonToCognos(String nsID, String user, String pswd) {
		StringBuffer credentialXML = new StringBuffer();
		credentialXML.append("<credential>");
		credentialXML.append("<namespace>").append(nsID).append("</namespace>");
		credentialXML.append("<username>").append(user).append("</username>");
		credentialXML.append("<password>").append(pswd).append("</password>");
		credentialXML.append("</credential>");

		String encodedCredentials = credentialXML.toString();
		XmlEncodedXML xmlCredentials = new XmlEncodedXML();
		xmlCredentials.set_value(encodedCredentials);

		try {
			cmService.logon(xmlCredentials, new SearchPathSingleObject[] {});
			SOAPHeaderElement temp = ((Stub) cmService).getResponseHeader(BUS_NS, BUS_HEADER);
			BiBusHeader CMbibus = (BiBusHeader) temp.getValueAsType(new QName(BUS_NS, BUS_HEADER));
			((Stub) cmService).setHeader(BUS_NS, BUS_HEADER, CMbibus);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.out.println("log on success...");
	}

	public void logonToCognos() {
		logonToCognos(nameSpaceID, username, password);
	}

	// Step 3: Logoff from Cognos
	private void logoffFromCognos() {
		try {
			cmService.logoff();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public BiBusHeader getHeaderObject(SOAPHeaderElement SourceHeader, boolean isNewConversation) {
		final QName BUS_QNAME = new QName(BUS_NS, BUS_HEADER);
		BiBusHeader bibus = null;

		if (SourceHeader == null)
			return null;

		try {
			bibus = (BiBusHeader) SourceHeader.getValueAsType(BUS_QNAME);

			// If the header will be used for a new conversation, clear
			// tracking information, DispatcherTransportVars and Routing values
			if (isNewConversation) {
				bibus.setTracking(null);
				bibus.setDispatcherTransportVars(new DispatcherTransportVar[] {});
				bibus.setRouting(null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return bibus;
	}

	public Stub ServiceWithHeader(Stub service,Boolean isNew) {
		BiBusHeader bibus = getHeaderObject(((Stub) service).getResponseHeader(BUS_NS, BUS_HEADER), isNew);

		if (bibus == null) {
			bibus = getHeaderObject(((Stub) cmService).getResponseHeader(BUS_NS, BUS_HEADER), isNew);

		}
		((Stub) service).clearHeaders();
		((Stub) service).setHeader(BUS_NS, BUS_HEADER, bibus);
		return service;
	}
	public static boolean hasSecondaryRequest(AsynchReply response,
			String secondaryRequest)
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
}
