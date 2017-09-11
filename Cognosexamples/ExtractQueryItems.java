//  Licensed Material - Property of IBM													
//  © Copyright IBM Corp. 2003, 2010 
/**													
 * ExtractQueryItems.java													
 *																				
 *													
 * Description: Technote 1373042 - SDK Sample to retrieve the Query Item for each Data item from a report spec
 * 													
 * Tested with: IBM Cognos BI 10.1, Java 5.0 		
 * 											
 */
													

import java.io.ByteArrayInputStream;
import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.rmi.RemoteException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import com.cognos.developer.schemas.bibus._3.*;

public class ExtractQueryItems 
{
	public ContentManagerService_PortType cmService = null;
	String contents;
	
	
	public ExtractQueryItems(String sendPoint)
	{
		String endPoint = sendPoint;
		ContentManagerService_ServiceLocator cmServiceLocator = new ContentManagerService_ServiceLocator();
		
		try
		{
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
			cmService.logon(new XmlEncodedXML(encodedCredentials), null);

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
	
	
	public void getQueryItems(String reportPath)
	{
		String sReportSpec = getReportSpec(reportPath);
		
		if (sReportSpec != null)
		{
		    Document xmlDoc = getDocument(sReportSpec);
		    if (xmlDoc != null)
		    {
		    	NodeList dataItem = xmlDoc.getElementsByTagName("dataItem");
		    
		    	for (int i=0; i< dataItem.getLength(); i++)
		    	{
		    		System.out.println(dataItem.item(i).getTextContent());
		    		Node qse = dataItem.item(i);
					if (qse != null)
					{
						Node nameAttr = qse.getAttributes().getNamedItem("name");
						
						contents += reportPath + "," + nameAttr.getNodeValue() +",";
						System.out.println("DataItem name: " + nameAttr.getNodeValue());  //get the Data Item name
								
						contents += qse.getTextContent() + System.getProperty("line.separator");;
						System.out.println("   Query Item: " + qse.getTextContent());  //get the Query Item
					}
		    	}
		    }		    
		}else
			System.out.println("Not a valid Report Specification");
	}
	
	//write result to a file
	private void writeDoc(String doc)
	{		
		//file name and path to write the information to
		String fileName = "c:/Content.csv";
		Writer fw = null;
		try
		{
			fw = new FileWriter(fileName);
			fw.write(doc);
			fw.flush() ;
			fw.close();
			System.out.println("The contents were written to " + fileName);
			
		}catch (Exception ioe)
		{
			ioe.printStackTrace() ;
		}
	} 
  
	//load the specification into DOM
	private Document  getDocument(String sReportSpec)
	{
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance(); 
	    try {
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			
			InputStream is = new ByteArrayInputStream(sReportSpec.getBytes("UTF-8")); 
			return builder.parse(is);
		} catch (Exception e) {			
			e.printStackTrace();		
		}
		return null;
	}
	
	//query Content Store for the report specification
	private String getReportSpec(String reportPath){
		String sReportSpec = null;
		PropEnum props[] = new PropEnum[]{ PropEnum.searchPath,
				   PropEnum.defaultName,
				   PropEnum.specification };
			SearchPathMultipleObject spMulti = new SearchPathMultipleObject();
			spMulti.set_value(reportPath);
						
			try {
				BaseClass[] repPth = cmService.query(spMulti,props,new Sort[]{},new QueryOptions());
				// There should be only one report with the specified searchPath 
				if (repPth != null && repPth.length > 0)
				{
				// extract the report spec
					sReportSpec = (((Report)repPth[0]).getSpecification().getValue() );
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			return sReportSpec;
	}
	
  
  public static void main(String[] args) 
  {
		// endpoint URL to CRN
  		String reportNetEndPoint = "http://localhost:9300/p2pd/servlet/dispatch";
  		// search path to the report which will be updated
   		String reportPath = "/content/package[@name='GO Sales and Retailers']/folder[@name='Documentation Report Samples']/report[@name='Add Color']";
   		
   		ExtractQueryItems sq = new ExtractQueryItems(reportNetEndPoint);
		
		// if logon needed uncomment if anonoymous access is disabled in Cognos Connection.
		sq.logon("SDK","admin","password");
		
		sq.contents ="Report Search Path,Data Item Name,Query Item" +  System.getProperty("line.separator");
		// set oldModelName = null if you wish to update all reports regardless 
		// of what the oldModelName was.
		sq.getQueryItems(reportPath);
		
		sq.writeDoc(sq.contents);
  }
}

