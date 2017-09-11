/**
 * changeModelConnection.java
 * 
 * Licensed Material - Property of IBM
 * © Copyright IBM Corp. 2003, 2010
 *
 * Description: Technote 1344097 - SDK Sample to modify the modelConnection of a report specification
 *
 * Tested with: IBM Cognos BI 10.1, Java 5.0
 */
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;

import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.cognos.developer.schemas.bibus._3.AnyTypeProp;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BaseClassArrayProp;
import com.cognos.developer.schemas.bibus._3.BiBusHeader;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.Model;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.Report;
import com.cognos.developer.schemas.bibus._3.ReportService_PortType;
import com.cognos.developer.schemas.bibus._3.ReportService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.cognos.developer.schemas.bibus._3.StringProp;
import com.cognos.developer.schemas.bibus._3.UpdateOptions;
import com.cognos.developer.schemas.bibus._3.XmlEncodedXML;

public class changeModelConnection {
	private ContentManagerService_ServiceLocator cmServiceLocator = null;
	private ContentManagerService_PortType cmService = null;
	private ReportService_ServiceLocator rsServiceLocator = null;
	private ReportService_PortType rsService = null;

	public changeModelConnection(String sendPoint) {
		// Connect to Cognos
		String endPoint = sendPoint;

		cmServiceLocator = new ContentManagerService_ServiceLocator();
		rsServiceLocator = new ReportService_ServiceLocator();
		try {
			cmService = cmServiceLocator
					.getcontentManagerService(new java.net.URL(endPoint));
			rsService = rsServiceLocator.getreportService(new java.net.URL(
					endPoint));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public void logon(String ns, String user, String pass) {
		// logon first. nameSpaceID, userName, password
		StringBuffer credentialXML = new StringBuffer();
		credentialXML.append("<credential>");
		credentialXML.append("<namespace>").append(ns).append("</namespace>");
		credentialXML.append("<username>").append(user).append("</username>");
		credentialXML.append("<password>").append(pass).append("</password>");
		credentialXML.append("</credential>");

		String encodedCredentials = credentialXML.toString();
		XmlEncodedXML xmlCredentials = new XmlEncodedXML();
		xmlCredentials.set_value(encodedCredentials);

		try {
			cmService.logon(xmlCredentials, null/*
												 * this parameter does nothing,
												 * but is required
												 */);

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

			((Stub) rsService).setHeader(
					"http://developer.cognos.com/schemas/bibus/3/",
					"biBusHeader", cmBiBusHeader);

		} catch (Exception ex) {
			System.out.println("exception thrown " + ex);
		}
	}

	public void modifyConnection(String reportPath, String oldModelName,
			String newModelName) {
		PropEnum props[] = new PropEnum[] { PropEnum.searchPath,
				PropEnum.defaultName, PropEnum.specification };
		try {
			String sReportSpec = null;
			SearchPathMultipleObject spMulti = new SearchPathMultipleObject();
			spMulti.set_value(reportPath);
			BaseClass[] repPth = cmService.query(spMulti, props, new Sort[] {},
					new QueryOptions());
			// loop through all the reports that were found.
			for (int i = 0; i < repPth.length; i++) {
				// extract the report spec
				sReportSpec = (((Report) repPth[i]).getSpecification()
						.getValue());

				// load the specification into the DOM
				ByteArrayInputStream bais = new ByteArrayInputStream(
						sReportSpec.getBytes("UTF-8"));
				DocumentBuilderFactory builderFactory = DocumentBuilderFactory
						.newInstance();

				try {
					DocumentBuilder builder = builderFactory
							.newDocumentBuilder();

					org.w3c.dom.Document document = builder.parse(bais);
					Source source = new DOMSource(document);
					StringWriter stringWriter = new StringWriter();
					Result result = new StreamResult(stringWriter);
					TransformerFactory factory = TransformerFactory
							.newInstance();
					Transformer transformer = factory.newTransformer();
					transformer.transform(source, result);
					NodeList modelPath_nodes = document
							.getElementsByTagName("modelPath");
					if (modelPath_nodes != null) {
						// change the model connection and the report spec if it matches with the
						// oldName
						// or if oldModelName = null
						String value = modelPath_nodes.item(0).getTextContent();
						if (oldModelName == null
								|| oldModelName.equals(value) == true) {
							modelPath_nodes.item(0)
									.setTextContent(newModelName);
							// set the report spec to the new one.
							source = new DOMSource(document);
							stringWriter = new StringWriter();
							result = new StreamResult(stringWriter);
							factory = TransformerFactory.newInstance();
							transformer = factory.newTransformer();
							transformer.transform(source, result);
							AnyTypeProp ap = new AnyTypeProp();
							ap.setValue(stringWriter.getBuffer().toString());
							Report updateReport = ((Report) repPth[i]);
							updateReport.setSpecification(ap);

							StringProp modelSearchPath = new StringProp();
							modelSearchPath.setValue(newModelName);
							BaseClass[] model = new BaseClass[1];
							model[0] = new Model();
							model[0].setSearchPath(modelSearchPath);
							BaseClassArrayProp metadataModel = new BaseClassArrayProp();
							metadataModel.setValue(model);
							updateReport.setMetadataModel(metadataModel);

							// now update the report. Using the rsService will
							// update the Package in the General properties page
							rsService.update(updateReport, new UpdateOptions());

							System.out.println("model connection and report spec UPDATED --- "
									+ repPth[i].getSearchPath().getValue());
						} else {
							if (newModelName.equals(value) == true) {
								// change the model connection
								Report updateReport = ((Report) repPth[i]);

								StringProp modelSearchPath = new StringProp();
								modelSearchPath.setValue(newModelName);
								BaseClass[] model = new BaseClass[1];
								model[0] = new Model();
								model[0].setSearchPath(modelSearchPath);
								BaseClassArrayProp metadataModel = new BaseClassArrayProp();
								metadataModel.setValue(model);
								updateReport.setMetadataModel(metadataModel);

								// now update the report. Using the rsService
								// will update the Package in the General
								// properties page
								rsService.update(updateReport,
										new UpdateOptions());

								System.out.println("model connection UPDATE --- "
										+ repPth[i].getSearchPath().getValue());
							} else {
								System.out.println("NOT UPDATED --- "
										+ repPth[i].getSearchPath().getValue());
							}
						}
					}

				} catch (SAXException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				}

			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public static void main(String[] args) {
		// endpoint URL to Cognos
		String cognosEndPoint = "http://localhost:9300/p2pd/servlet/dispatch";
		// search path to the report which will be updated
		String reportPath = "/content/folder[@name='test']//report";
		// old Model Name
		String oldModelName = "/content/folder[@name='Samples']/folder[@name='Models']/package[@name='GO Sales (query)']/model[@name='model']";
		// new Model Name
		String newModelName = "/content/folder[@name='Samples']/folder[@name='Models']/package[@name='GO Data Warehouse (query)']/model[@name='model']";

		changeModelConnection sq = new changeModelConnection(cognosEndPoint);

		sq.logon("nameSpaceID", "userName", "password");
		// set oldModelName = null if you wish to update all reports regardless
		// of what the oldModelName was.
		sq.modifyConnection(reportPath, oldModelName, newModelName);
	}
}
