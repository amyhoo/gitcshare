/**
 * Licensed Material - Property of IBM
 * © Copyright IBM Corporation 2009,2010
 * 
 * ViewReports_byPackage.java
 * 
 *  Technote 1374788 : SDK Sample to view or list all the Reports by their used Package name.
 *
 *  Description: This java sample lists the all the reports from the Content Store by their used Package name.
 *               Limits can be placed on the search by modifying the Search Path to only do go down from a defined 
 *               folder level.
 *
 *  Base Installation 	    : IBM Cognos BI 10.1
 *  Tested with 	    : JDK/JRE 1.6
 *  Modified Date	    : 100125
 *
 *  Instructions to use this Code Sample:
 *
 *   - Modify the endPoint string value to your External Dispatcher entry found in Cognos Configuration.
 *	            dispatcherURL 	= "http://localhost:9300/p2pd/servlet/dispatch";
 *	
 *   - Modify	namespaceID 	= "SDK"
 *		userID		= "Admin"
 *		password	= "password"  to the appropriate logon values.
 *
 */

import javax.xml.namespace.QName;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;

import com.cognos.developer.schemas.bibus._3.AuthoredReport;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BaseClassArrayProp;
import com.cognos.developer.schemas.bibus._3.BiBusHeader;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.OrderEnum;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.RefProp;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;

public class ViewReports_byPackage {

	// TODO: change the following variables depending on your installation and setup

	private static String dispatcherURL = "http://localhost:9300/p2pd/servlet/dispatch";

	// Change the next three variables to a valid namespaceID as defined in
	// Cognos Configuration and NOT the namespace name.
	// valid user ID and password if Anonymous is disabled
	private static String nameSpaceID = "SDK";
	private static String userName = "Admin";
	private static String password = "password";

	// Search options - using a package name to only give the reports for that package, or
	// not using the compare of package name and have all the reports and their packages listed.
	// See the the two If statements below   :  if (packageName != null && packageName != "")
	
	// Search for Reports using this Package name :
	private static String packname = "GO Data Warehouse (query)";
	

	// Services to be used in the sample
	private ContentManagerService_PortType cmService = null;

	/**
	 * This is the main class, which connects to IBM Cognos BI and calls the
	 * needed method(s), containing the specific code for this sample
	 */
	public static void main(String args[]) {

		ViewReports_byPackage mainClass = new ViewReports_byPackage();

		// Connect to Cognos Content Manager
		mainClass.connectToCognos();

		// If the "Anonymous" is disabled, then logon
		if (nameSpaceID.length() > 0) {
			mainClass.logon(nameSpaceID, userName, password);
		}

		// do the main work
		mainClass.viewReportsAndQueries();
	}

	/**
	 * Establish a connection to Cognos BI and initialize the different services
	 */
	private void connectToCognos() {
		ContentManagerService_ServiceLocator cmServiceLocator = new ContentManagerService_ServiceLocator();


		try {
			cmService = cmServiceLocator.getcontentManagerService(new java.net.URL(dispatcherURL));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Logon to Cognos BI using valid credentials information
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
			SOAPHeaderElement temp = ((Stub)cmService).getResponseHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader");
			BiBusHeader cmBiBusHeader = (BiBusHeader)temp.getValueAsType(new QName ("http://developer.cognos.com/schemas/bibus/3/","biBusHeader"));
			((Stub)cmService).setHeader("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", cmBiBusHeader);


		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void viewReportsAndQueries() {
		String output = new String();

		// set the properties props

		PropEnum props[] = new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName, PropEnum.metadataModel, PropEnum.metadataModelPackage };

		// set the QueryOptions referenceProps option

		PropEnum referenceProps[] = new PropEnum[] { PropEnum.defaultName, PropEnum.searchPath, PropEnum.version };

		RefProp refPropArray[] = { new RefProp() };
		refPropArray[0].setProperties(referenceProps);
		refPropArray[0].setRefPropName(PropEnum.metadataModelPackage);

		QueryOptions qops = new QueryOptions();
		qops.setRefProps(refPropArray);

		Sort sortOptions[] = { new Sort() };
		sortOptions[0].setOrder(OrderEnum.ascending);
		sortOptions[0].setPropName(PropEnum.defaultName);

		try {
			/**
			 * Use this Cognos BI method to query the reports in the content
			 * store.
			 * 
			 * @param "/content//report" Specifies the search path string so
			 *        that Content Manager can locate the requested objects,
			 *        which are reports in this example.
			 * @param props
			 *            Specifies alternate properties that you want returned
			 *            for the report object. When no properties are
			 *            specified, as in this example, the default properties
			 *            of searchPath and defaultName are provided.
			 * @param sortOptions
			 *            Specifies the sort criteria in an array.
			 * @param QueryOptions
			 *            Specifies any options for this ReportNet method.
			 * 
			 * @return Returns an array of reports.
			 */

			BaseClass bc[] = cmService.query(new SearchPathMultipleObject("//report"), props, sortOptions, qops);

			// If reports exist in the content store, the output shows the report name on one line,
			// followed by a second line that shows the search path of the report.
			
			if (bc != null) {
				if (bc.length > 0) {
					output = output.concat("Reports:\n\n");

					for (int i = 0; i < bc.length; i++) {
						BaseClassArrayProp pkg = ((AuthoredReport) bc[i]).getMetadataModelPackage();
						String packageName = "Package name not available";
						String packageSearchPath = "Package Search Path not available";
						if (pkg.getValue() != null) {
							packageName = ((AuthoredReport) bc[i]).getMetadataModelPackage().getValue()[0].getDefaultName().getValue();
							packageSearchPath = ((AuthoredReport) bc[i]).getMetadataModelPackage().getValue()[0].getSearchPath().getValue();
						}

						// Statement to only get the information of a desired package from the content store.
						// compare "packname" variable declared at start with the packageName found for report.

						if (packageName != null && packageName != "" && packageName.compareToIgnoreCase(packname) == 0)
						// Statement to do all packages and reports :  
						// if (packageName != null && packageName != "")
						{
							output = output.concat("\t\t" + bc[i].getDefaultName().getValue() + "\nReport SearchPath:      "
							            + bc[i].getSearchPath().getValue() + "\nPackage Name:\t\t" + packageName + "\n" + "Package Search Path:\t"
							            + packageSearchPath + "\n");
						}
					}
				} else {
					output = output.concat("There are currently no reports to view.\n");
				}
			} else {
				output = output.concat("Error occurred in viewReportsAndQueries().");
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			output = output.concat("View Reports:\nCannot connect to CM.\n" + "Check if CRN is running");
		}

		try {
			/**
			 * Use this method to query the query objects in the content store.
			 * Query objects are created when users save reports that they
			 * create using Query Studio.
			 * 
			 * @param "/content//query" Specifies the search path string so that
			 *        Content Manager can locate the requested objects, which
			 *        are queries in this example.
			 * @param props
			 *            Specifies alternate properties that you want returned
			 *            for the query object. When no properties are
			 *            specified, as in this example, the default properties
			 *            of searchPath and defaultName are provided.
			 * @param sortOptions
			 *            Specifies the sort criteria in an array.
			 * @param QueryOptions
			 *            Specifies any options for this ReportNet method.
			 * 
			 * @return Returns an array of queries.
			 */

			BaseClass bc[] = cmService.query(new SearchPathMultipleObject("/content//query"), props, sortOptions, qops);

			// If queries exist in the content store, the output shows the query
			// name on one line, followed by a second line that shows the search
			// path of the query.
			if (bc != null) {
				if (bc.length > 0) {
					output = output.concat("\n\nQueries:\n\n");

					for (int i = 0; i < bc.length; i++) {
						BaseClassArrayProp pkg = ((AuthoredReport) bc[i]).getMetadataModelPackage();
						String packageName = "Package name not available";
						String packageSearchPath = "Package Search Path not available";
						if (pkg.getValue() != null) {
							packageName = ((AuthoredReport) bc[i]).getMetadataModelPackage().getValue()[0].getDefaultName().getValue();
							packageSearchPath = ((AuthoredReport) bc[i]).getMetadataModelPackage().getValue()[0].getSearchPath().getValue();
						}

						// Statement to only get the information of a desired package from the content store.
						// compare packname variable declared at beginning with the packageName found for report

						if (packageName != null && packageName != "" && packageName.compareToIgnoreCase(packname) == 0)

						// Statement to do all packages and reports :
						// if (packageName != null && packageName != "")
							
						{
							output = output.concat("\t\t" + bc[i].getDefaultName().getValue() + "\nReport SearchPath:      "
							            + bc[i].getSearchPath().getValue() + "\nPackage Name:\t\t" + packageName + "\n" + "Package Search Path:\t"
							            + packageSearchPath + "\n");
						}
					}
				} else {
					output = output.concat("There are no queries to view.\n\n");
				}
			} else {
				output = output.concat("Error occurred in viewReportsAndQueries().\n");
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			output = output.concat("View Reports:\nCannot connect to CM.\n" + "Check if CRN is running\n");
		}

		// return output;
		System.out.print(output);
	}

}
