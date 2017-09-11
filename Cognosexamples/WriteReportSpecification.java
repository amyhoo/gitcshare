// Licensed Material - Property of IBM
// © Copyright IBM Corp. 2003, 2010

/**
 * WriteReportSpecification.java
 *
 *
 * Description: Technote 1335733 - SDK Sample to extract the report specification and store it on the local machine.
 * 
 * Tested with: IBM Cognos BI 10.1, Java 5.0 	
 * 
 */
import com.cognos.developer.schemas.bibus._3.*;
import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import javax.xml.namespace.QName;


import java.io.*;
import org.dom4j.Document;

public class WriteReportSpecification 
{
	private ContentManagerService_ServiceLocator cmServiceLocator = null;
	private ContentManagerService_PortType cmService = null;
	
	public Document oDocument; 
	
	
	public WriteReportSpecification(String sendPoint)
	{
		//Connect to Cognos 8
		String endPoint = sendPoint;
		try
		{
			cmServiceLocator = new ContentManagerService_ServiceLocator();
			cmService = cmServiceLocator.getcontentManagerService(new java.net.URL(endPoint));
		}
		catch (Exception e)
		{
			System.out.println(e);
		}
	}
	public void logon(String ns, String user, String pass)
	{
		//logon first. namespaceID, username, password
		StringBuffer credentialXML = new StringBuffer();
		credentialXML.append("<credential>");
		credentialXML.append("<namespace>").append(ns).append("</namespace>");
		credentialXML.append("<username>").append(user).append("</username>");
		credentialXML.append("<password>").append(pass).append("</password>");
		credentialXML.append("</credential>");
			
		String encodedCredentials = credentialXML.toString();
		try
		{
			cmService.logon(new XmlEncodedXML(encodedCredentials), new SearchPathSingleObject[]{}/* this parameter does nothing, but is required */);

			//TODO Set the BiBusHeader
		SOAPHeaderElement temp = ((Stub)cmService).getResponseHeader 
			 ("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader");
		BiBusHeader cmBiBusHeader = (BiBusHeader)temp.getValueAsType
			(new QName ("http://developer.cognos.com/schemas/bibus/3/","biBusHeader"));
		((Stub)cmService).setHeader
			("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", cmBiBusHeader);

		}
		catch (Exception ex)
		{
			System.out.println("exception thrown " + ex);
		}
   }
	
  public void storeReportSpec(String reportPath, String pathToSave)
  {
	PropEnum props[] = new PropEnum[]{ PropEnum.searchPath,
									   PropEnum.defaultName,
									   PropEnum.specification };
	
	String sReportSpec = null;
	try 
	{
		//this is our object, store in a byte array
		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		
		BaseClass[] repPth = cmService.query(new SearchPathMultipleObject(reportPath),props,new Sort[]{},new QueryOptions());
		// loop through the report spec that were found.
		// Save the output to a file on the local system
		File oFile = new File(pathToSave);
		FileOutputStream fos = new FileOutputStream(oFile);
		
		for(int i = 0; i < repPth.length; i ++)
		{	
			sReportSpec =  ( ((Report)repPth[i]).getSpecification().getValue() );
			// extract the report spec
			baos.write(sReportSpec.getBytes());
			//System.out.println(sReportSpec);
		}
		fos.write(baos.toByteArray());
		System.out.println("Report Specification has been save successfully under " + pathToSave );
	}
	catch (Exception e)
	{
		System.out.println(e);
	}
 }

  public static void main(String[] args) 
  {
		// endpoint URL to Cognos 8
  		String cognos8EndPoint = "http://localhost:9300/p2pd/servlet/dispatch";
  		// User information. Don't needed if Anonymous is used
  		String username = "username";
  		String password = "password";
  		String namespaceID = "namespaceID";
  		
  		// search path of the report whose report specification will be extracted
   		String reportPath = "/content/package[@name='GO Data Warehouse (query)']/folder[@name='SDK Report Samples']/report[@name='Order Product List']";
		// location where the report specification will be saved
		String pathToSave = "c:\\temp\\ReportSpec" + ".XML";

   		WriteReportSpecification sq = new WriteReportSpecification(cognos8EndPoint);
		
		// if logon needed uncomment if anonoymous access is disabled in Cognos Connection.
		sq.logon(namespaceID, username, password);
		sq.storeReportSpec(reportPath,pathToSave);
  }
}

