/**
 * signOn.java
 *
 * Licensed Material - Property of IBM
 * © Copyright IBM Corp. 2010 
 *
 * Description: Technote 1370529 - SDK - Create or modify a database signon in the content store using the SDK
 * 
 * Tested with: IBM Cognos BI 10.1, IBM Java 1.5 
 * 
 */

import com.cognos.developer.schemas.bibus._3.*;
import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import javax.xml.namespace.QName;

public class signOn {

	// content manager service
	public ContentManagerService_ServiceLocator cmServiceLocator = null;
	public ContentManagerService_PortType cmService = null;
	
	public static void main(String[] args) {
		signOn connect = new signOn();
		connect.connectToReportServer();
	
	    String dsConnection = "CAMID(\":\")/dataSource[@name='test']/dataSourceConnection[@name='test']";
	   	PropEnum props[] = {PropEnum.defaultName, PropEnum.user,
		PropEnum.credentials,PropEnum.searchPath, PropEnum.version,
		PropEnum.consumers };
		 
		try
		{
		  //Connect to ReportNet as System Administrator	
		  connect.quickLogon("SDK", "admin", "password");

    	  //Create a new signon
		  DataSourceSignon newdsSignon = new DataSourceSignon();
		  BaseClassArrayProp cons = new BaseClassArrayProp();
		  BaseClass[] oConsumers = new BaseClass [1];
		  Nil c1 = new Nil();
		  StringProp sp = new StringProp();
		  //If you want a specific user/group to have access to the signon,
		  //change the search path to that user/group search path
		  sp.setValue("CAMID(\"::Everyone\")");
		  c1.setSearchPath(sp);
		  oConsumers[0] = c1;
		  cons.setValue(oConsumers);
		  //Set everyone to have access to the signon
		  newdsSignon.setConsumers(cons);
		  		  
		  //Add credentials to the signon 
		  AnyTypeProp credentials = new AnyTypeProp();
		  String credString="<credential><username>sa</username><password>sa</password></credential>";
		  credentials.setValue(credString);
		  //Replace previous 3 lines with the next commented line,  
		  //if you want to use the same credentials as another existing signon
		  //credentials.setValue(dts); 
		  newdsSignon.setCredentials(credentials);
		  
		  TokenProp tp = new TokenProp();
		  tp.setValue("newSignOn");
		  newdsSignon.setDefaultName(tp);
          
		  SearchPathMultipleObject dsConnSearchPobj = new SearchPathMultipleObject();
		  dsConnSearchPobj.set_value(dsConnection);
		
		  BaseClass bc[] = connect.cmService.query(dsConnSearchPobj, props, new Sort[] {}, new QueryOptions());
		  BaseClassArrayProp bcap = new BaseClassArrayProp();
		  bcap.setValue(bc);
		  newdsSignon.setParent(bcap);
		 
		  AddOptions ao = new AddOptions();
		  ao.setUpdateAction(UpdateActionEnum.replace);
		  
		  SearchPathSingleObject newObject = new SearchPathSingleObject();
		  newObject.set_value((bc[0]).getSearchPath().getValue());

		  connect.cmService.add(newObject,new BaseClass[]{newdsSignon},ao);
		  System.out.println("Done");
		}
		catch (Exception e){
		System.out.println(e);
		}
	 }
	

	public String quickLogon(String namespace, String uid, String pwd) throws Exception
	{
		StringBuffer credentialXML = new StringBuffer();

		credentialXML.append("<credential>");
		credentialXML.append("<namespace>").append(namespace).append("</namespace>");
		credentialXML.append("<username>").append(uid).append("</username>");
		credentialXML.append("<password>").append(pwd).append("</password>");
		credentialXML.append("</credential>");

		String encodedCredentials = credentialXML.toString();

		cmService.logon(new XmlEncodedXML(encodedCredentials), new SearchPathSingleObject[]{}/* this parameter does nothing, but is required */);

			//TODO Set the BiBusHeader
		SOAPHeaderElement temp = ((Stub)cmService).getResponseHeader 
			 ("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader");
		BiBusHeader cmBiBusHeader = (BiBusHeader)temp.getValueAsType
			(new QName ("http://developer.cognos.com/schemas/bibus/3/","biBusHeader"));
		((Stub)cmService).setHeader
			("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", cmBiBusHeader);


		return ("Logon successful as " + uid);
	}


	public void connectToReportServer ()
	{		
		// Default URL for Cognos 8 Dispatcher
		String sendPoint = "http://localhost:9300/p2pd/servlet/dispatch";      

		try
		{

			java.net.URL endPoint = new java.net.URL(sendPoint);
		
			
			//content manager service
		
			cmServiceLocator = new ContentManagerService_ServiceLocator();
			cmService = cmServiceLocator.getcontentManagerService(endPoint);

		
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}
}

