/**
 * ValidateReport.java
 *
  * Licensed Material - Property of IBM
 * © Copyright IBM Corp. 2003, 2010
 *
 * Description: Technote 1344197 - SDK Sample to Validate a Report Specification
 *
 * Tested with: IBM Cognos BI 10.1, Java 5.0
 */
import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import javax.xml.namespace.QName;
import org.apache.axis.AxisFault;
import com.cognos.developer.schemas.bibus._3.*;

public class ValidateReport {
	public ContentManagerService_PortType cmService = null;
	public ReportService_PortType reportService = null;

	public void connectToReportServer(String endPoint) throws Exception {
		ContentManagerService_ServiceLocator cmServiceLocator = new ContentManagerService_ServiceLocator();
		ReportService_ServiceLocator reportServiceLocator = new ReportService_ServiceLocator();

		try {
			cmService = cmServiceLocator
					.getcontentManagerService(new java.net.URL(endPoint));
			reportService = reportServiceLocator
					.getreportService(new java.net.URL(endPoint));
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public void logon(String namespace, String uid, String pwd)
			throws Exception {
		StringBuffer credentialXML = new StringBuffer();

		credentialXML.append("<credential>");
		credentialXML.append("<namespace>").append(namespace).append(
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

		} catch (Exception ex) {
			System.out.println("exception thrown " + ex);
			return;
		}
		System.out.println("Logon successful as " + uid);
	}

	public void validateReport(String reportSearchPath, char severityLevel)
			throws Exception {

		XmlEncodedXML ValidationDefects = null;
		PropEnum props[] = new PropEnum[] { PropEnum.specification,
				PropEnum.searchPath };

		ReportServiceSpecification rsSpec = new ReportServiceReportSpecification();

		AsynchReply asynchReply = null;

		// Support severity level
		ValidateSeverityEnum severityLevelEnum = null;

		Option validateOptions[] = new Option[1];

		ValidateOptionValidateSeverity validateSeverity = new ValidateOptionValidateSeverity();
		validateSeverity.setName(ValidateOptionEnum.severity);
		switch (severityLevel) {
		case 'E':
			severityLevelEnum = ValidateSeverityEnum.error;
			break;
		case 'W':
			severityLevelEnum = ValidateSeverityEnum.warning;
			break;
		case 'K':
			severityLevelEnum = ValidateSeverityEnum.keyTransformation;
			break;
		case 'I':
			severityLevelEnum = ValidateSeverityEnum.information;
			break;
		default:
			severityLevelEnum = ValidateSeverityEnum.error;
		}
		validateSeverity.setValue(severityLevelEnum);
		validateOptions[0] = validateSeverity;
		System.out.println("Severity level set to "
				+ severityLevelEnum.toString() + "\n");

		try {
			BaseClass[] result = cmService
					.query(new SearchPathMultipleObject(reportSearchPath),
							props, new Sort[] {}, new QueryOptions());

			for (int i = 0; i < result.length; i++) {
				Report report = (Report) result[i];
				rsSpec.setValue(new Specification(report.getSpecification()
						.getValue()));
				String reportPath = report.getSearchPath().getValue();
				System.out.println(reportPath);
				try {
					// Support reports with prompts
					BaseParameter myParams[] = getReportParameters(reportPath);
					if (myParams != null && myParams.length > 0) {
						asynchReply = getReportService().validateSpecification(
								rsSpec, setReportParameters(myParams),
								validateOptions);
					} else {
						asynchReply = getReportService().validateSpecification(
								rsSpec, new ParameterValue[] {},
								validateOptions);
					}
				} catch (Exception e) {
					String exceptionMsg = "ERROR - Exception getting parameters - "
							+ report.getSearchPath().getValue()
							+ "\nErrors.....";
					System.out.println(exceptionMsg);
					String details = getDetailedException(e);
					System.out.println(details);
					continue;
				}

				// If response is not immediately complete, call wait until
				// complete
				if (!asynchReply.getStatus().equals(
						AsynchReplyStatusEnum.complete)
						&& !asynchReply.getStatus().equals(
								AsynchReplyStatusEnum.conversationComplete)) {
					while (!asynchReply.getStatus().equals(
							AsynchReplyStatusEnum.complete)
							&& !asynchReply.getStatus().equals(
									AsynchReplyStatusEnum.conversationComplete)) {
						// before calling wait, double check that it is okay
						if (hasSecondaryRequest(asynchReply, "wait")) {
							asynchReply = getReportService().wait(
									asynchReply.getPrimaryRequest(),
									new ParameterValue[] {}, new Option[] {});
						} else {
							System.out
									.println("Error: Wait method not available as expected.");
							return;
						}
					}
				}
				// Check the defects tag
				for (int j = 0; j < asynchReply.getDetails().length; j++) {
					if (asynchReply.getDetails()[j] instanceof AsynchDetailReportValidation)

					{
						ValidationDefects = ((AsynchDetailReportValidation) asynchReply
								.getDetails()[j]).getDefects();
						String results = ValidationDefects.get_value()
								.toString();
						int index = results.indexOf("</defects>");
						if (index > 9) {
							System.out.println(results + "\n");
						} else {
							System.out.println("was validated\n");
						}
					}
				}
			}
		} catch (java.rmi.RemoteException remoteEx) {
			// remoteEx.printStackTrace();
			System.out.println("Exception caught during validation:\n"
					+ remoteEx);
			return;
		}

	}

	public boolean hasSecondaryRequest(AsynchReply response,
			String secondaryRequest) {
		AsynchSecondaryRequest[] secondaryRequests = response
				.getSecondaryRequests();
		for (int i = 0; i < secondaryRequests.length; i++) {
			if (secondaryRequests[i].getName().compareTo(secondaryRequest) == 0) {
				return true;
			}
		}
		return false;
	}

	public String getDetailedException(Exception e) {
		AxisFault f = (AxisFault) e;
		String message;
		String a1 = f.dumpToString();
		int start = a1.indexOf("<ns1:messageString xsi:type=\"xs:string\">");
		int end = a1.indexOf("</ns1:messageString>");
		if (start > 0 && end > 0) {
			message = a1.substring(start + 40, end - 1) + "\n";
			int start2 = a1.indexOf(
					"<ns1:messageString xsi:type=\"xs:string\">", end);
			int end2 = a1.indexOf("</ns1:messageString>", end + 24);
			if (start2 > 0 && end2 > 0) // more than one msg.
				message = message + a1.substring(start2 + 40, end2 - 1) + "\n";
			return message;
		} else
			return a1;
	}

	public BaseParameter[] getReportParameters(String reportPathString)
			throws java.rmi.RemoteException {
		BaseParameter params[] = new Parameter[] {};
		AsynchReply response;
		SearchPathSingleObject reportPath = new SearchPathSingleObject();
		reportPath.set_value(reportPathString);

		response = getReportService().getParameters(reportPath,
				new ParameterValue[] {}, new Option[] {});

		// If response is not immediately complete, call wait until complete
		if (!response.getStatus().equals(
				AsynchReplyStatusEnum.conversationComplete)) {
			while (!response.getStatus().equals(
					AsynchReplyStatusEnum.conversationComplete)) {
				response = getReportService().wait(
						response.getPrimaryRequest(), new ParameterValue[] {},
						new Option[] {});
			}
		}
		for (int i = 0; i < response.getDetails().length; i++) {
			if (response.getDetails()[i] instanceof AsynchDetailParameters)
				params = ((AsynchDetailParameters) response.getDetails()[i])
						.getParameters();
		}
		return params;
	}

	public static ParameterValue[] setReportParameters(BaseParameter[] prm) {
		try {
			int numberOfParameters = 0;

			// Select the parameter values for the specified report.
			if (prm.length > 0) {
				numberOfParameters = prm.length;

				ParameterValue[] params = new ParameterValue[numberOfParameters];

				// Repeat for each parameter. Set to a value that conforms to
				// the required format for each type
				for (int i = 0; i < prm.length; i++) {
					String inputValue = "";
					ParameterDataTypeEnum paramType = prm[i].getType();
					if (paramType.equals(ParameterDataTypeEnum.xsdDate)) {
						inputValue = "2001-05-31";
					} else {
						if (paramType.equals(ParameterDataTypeEnum.xsdDateTime)) {
							inputValue = "2001-05-31T14:39:25";
						} else {
							inputValue = "1"; // valid for string, int, double,
							// etc
						}
					}

					SimpleParmValueItem item1 = new SimpleParmValueItem();
					item1.setUse(inputValue);

					// Create a new array to contains the values for the
					// parameter.
					ParmValueItem pvi[] = new ParmValueItem[1];
					pvi[0] = item1;

					// Assign the values to the parameter.
					params[i] = new ParameterValue();
					params[i].setName(prm[i].getName());
					params[i].setValue(pvi);
				}
				return params;
			} else {
				return null;
			}
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}

	public ReportService_PortType getReportService(boolean isNewConversation,
			String RSGroup) {

		BiBusHeader bibus = null;
		bibus = getHeaderObject(((Stub) reportService).getResponseHeader(
				"http://developer.cognos.com/schemas/bibus/3/", "biBusHeader"),
				isNewConversation, RSGroup);

		if (bibus == null) {
			BiBusHeader CMbibus = null;
			CMbibus = getHeaderObject(((Stub) cmService).getResponseHeader(
					"http://developer.cognos.com/schemas/bibus/3/",
					"biBusHeader"), true, RSGroup);

			((Stub) reportService).setHeader(
					"http://developer.cognos.com/schemas/bibus/3/",
					"biBusHeader", CMbibus);
		} else {
			((Stub) reportService).clearHeaders();
			((Stub) reportService).setHeader(
					"http://developer.cognos.com/schemas/bibus/3/",
					"biBusHeader", bibus);

		}

		return reportService;
	}

	// handle report service requests that do not specify new conversation for
	// backwards compatibility
	public ReportService_PortType getReportService() {

		return getReportService(false, "");

	}

	// Use this method when copying headers, such as for requests to services
	public static BiBusHeader getHeaderObject(SOAPHeaderElement SourceHeader,
			boolean isNewConversation, String RSGroup) {
		if (SourceHeader == null)
			return null;

		BiBusHeader bibus = null;
		try {
			bibus = (BiBusHeader) SourceHeader.getValueAsType(new QName(
					"http://developer.cognos.com/schemas/bibus/3/",
					"biBusHeader"));

			// If the header will be used for a new conversation, clear
			// tracking information, and set routing if supplied (clear if not)
			if (isNewConversation) {

				bibus.setTracking(null);

				// If a Routing Server Group is specified, direct requests to it
				if (RSGroup.length() > 0) {
					RoutingInfo routing = new RoutingInfo(RSGroup);
					bibus.setRouting(routing);
				} else {
					bibus.setRouting(null);
				}
			}
		} catch (Exception e) {

			e.printStackTrace();
		}

		return bibus;
	}

	public static void usage() {
		System.out.println("");
		System.out.println("");
		System.out.println("Command Line Parameters:");
		System.out.println("");
		System.out.println("[ <severityLevel> ]");
		System.out.println("");
		System.out.println("  Optional argument:");
		System.out.println("");
		System.out.println("     severityLevel - e|E Error (Default)");
		System.out.println("                   - w|W Warning");
		System.out.println("                   - k|K Key Transformation");
		System.out.println("                   - i|I Information\n\n");
	}

	public static void main(String args[]) {
		usage();
		char severityLevel = 'E'; // default Error
		if ((args.length > 0)) {
			severityLevel = args[0].substring(0, 1).toUpperCase().toCharArray()[0];
		}
		// connection to the Cognos service
		String endPoint = "http://localhost:9300/p2pd/servlet/dispatch";
		// log in as a System Administrator to ensure you have the necessary
		// permissions to query any report
		// NB: remember to remove slashes from the test.logon line below if you
		// set any of the following 3 variables
		String nameSpaceID = "nameSpaceID";
		String userName = "userName";
		String password = "password";
		// search path of the report to be validated
		String reportSearchPath = "/content/folder[@name='Samples']/folder[@name='Models']/package[@name='GO Data Warehouse (query)']/folder[@name='SDK Report Samples']/report[@name='Product Description List']";

		ValidateReport test = new ValidateReport();

		try {
			test.connectToReportServer(endPoint);
			// remove slashes from next line to logon as a non-anonymous user
			// test.logon(nameSpaceID, userName, password);
			test.validateReport(reportSearchPath, severityLevel);
			System.out.println("\nDone.");
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}
