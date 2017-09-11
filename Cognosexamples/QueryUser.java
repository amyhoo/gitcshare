// Licensed Material - Property of IBM
// © Copyright IBM Corp. 2003, 2010

/**
 * QueryObject.java
 * 
 * 
 * Description: Technote 1410426- SDK sample to extract user information such as 
 * 				User Name, CAMID, and user class to which the user belongs to 
 * 
 * Tested with: IBM Cognos BI 10.1, Java 5.0
 */


import com.cognos.developer.schemas.bibus._3.*;
import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import javax.xml.namespace.QName;

public class QueryUser {

	private ContentManagerService_PortType cmService = null;
	// Default URL for CRN Content Manager. Change if not using default gateway
	public final String CM_URL = "http://localhost:9300/p2pd/servlet/dispatch";
		  
	public ContentManagerService_PortType getCmService() {return cmService;}
	
	public static void main(String[] args) {
		
		QueryUser cp = new QueryUser();
		//logon info. Comment next line if Anonymous is enabled
		//Login as administrator to have access to the Directory
		cp.quickLogon("Series7", "Administrator", "");
		
		//SearchPath to the report
		String	searchPath = "CAMID(\"DS7:u:authid=2260550617\")";
		
		//get all the properties for an object
		PropEnum props[] = cp.getAllPropEnum();
		
		SearchPathMultipleObject spMulti = new SearchPathMultipleObject();
		spMulti.set_value(searchPath);	
		
		try
		{
			BaseClass bc[] = cp.getCmService().query(spMulti, props,
					new Sort[]{}, new QueryOptions());
			if (bc != null && bc.length > 0)
			{
				if (bc[0] instanceof Account)
				{
					System.out.println("Name: " + (bc[0]).getDefaultName().getValue());
					System.out.println("Search Path: " + bc[0].getSearchPath().getValue());
					String groupSearchPath = (bc[0]).getParent().getValue()[0].getSearchPath().getValue();
					spMulti.set_value(groupSearchPath);	
					BaseClass userClass[] = cp.getCmService().query(spMulti, props,
							new Sort[]{}, new QueryOptions());
					if (userClass != null && userClass.length > 0)
						System.out.println("User Class: " + userClass[0].getDefaultName().getValue());
					
					Policy p[] = bc[0].getPolicies().getValue();
				}
				
			}
			
			System.out.println("Done");
	
		}catch (Exception e)
		{
			e.printStackTrace() ;
		}
		
				
	}
	
	public PropEnum[] getAllPropEnum ()
	  {
	    PropEnum properties[] = new PropEnum[]{
	     PropEnum.active,
	      PropEnum.actualCompletionTime,
	      PropEnum.actualExecutionTime,
	      PropEnum.advancedSettings,
	      PropEnum.ancestors,
	      PropEnum.asOfTime,
	      PropEnum.base,
	      PropEnum.brsAffineConnections,
	      PropEnum.brsMaximumProcesses,
	      PropEnum.brsNonAffineConnections,
	      PropEnum.burstKey,
	      PropEnum.businessPhone,
	      PropEnum.canBurst,
	      PropEnum.capabilities,
	      PropEnum.capacity,
	      PropEnum.connections,
	      PropEnum.connectionString,
	      PropEnum.consumers,
	      PropEnum.contact,
	      PropEnum.contactEMail,
	      PropEnum.contentLocale,
	      PropEnum.creationTime,
	      PropEnum.credential,
	      PropEnum.credentialNamespaces,
	      PropEnum.credentials,
	      PropEnum.dailyPeriod,
	      PropEnum.data,
	      PropEnum.dataSize,
	      PropEnum.dataType,
	      PropEnum.defaultDescription,
	      PropEnum.defaultName,
	      PropEnum.defaultOutputFormat,
	      PropEnum.defaultScreenTip,
	      PropEnum.defaultTriggerDescription,
	      PropEnum.deployedObject,
	      PropEnum.deployedObjectAncestorDefaultNames,
	      PropEnum.deployedObjectClass,
	      PropEnum.deployedObjectDefaultName,
	      PropEnum.deployedObjectStatus,
	      PropEnum.deployedObjectUsage,
	      PropEnum.deploymentOptions,
	      PropEnum.description,
	      PropEnum.disabled,
	      PropEnum.dispatcherID,
	      PropEnum.dispatcherPath,
	      PropEnum.displaySequence,
	      PropEnum.email,
	      PropEnum.endDate,
	      PropEnum.endType,
	      PropEnum.eventID,
	      PropEnum.everyNPeriods,
	      PropEnum.executionFormat,
	      PropEnum.executionLocale,
	      PropEnum.executionPageDefinition,
	      PropEnum.executionPageOrientation,
	      PropEnum.executionPrompt,
	      PropEnum.faxPhone,
	      PropEnum.format,
	      PropEnum.givenName,
	      PropEnum.governors,
	      PropEnum.hasChildren,
	      PropEnum.hasMessage,
	      PropEnum.height,
	      PropEnum.homePhone,
	      PropEnum.horizontalElementsRenderingLimit,
	      PropEnum.identity,
	      PropEnum.isolationLevel,
	      PropEnum.lastConfigurationModificationTime,
	      PropEnum.lastPage,
	      PropEnum.loadBalancingMode,
	      PropEnum.locale,
	      PropEnum.location,
	      PropEnum.members,
	      PropEnum.metadataModel,
	      PropEnum.mobilePhone,
	      PropEnum.model,
	      PropEnum.modelName,
	      PropEnum.modificationTime,
	      PropEnum.monthlyAbsoluteDay,
	      PropEnum.monthlyRelativeDay,
	      PropEnum.monthlyRelativeWeek,
	      PropEnum.name,
	      PropEnum.namespaceFormat,
	      PropEnum.objectClass,
	      PropEnum.output,
	      PropEnum.owner,
	      PropEnum.ownerPassport,
	      PropEnum.packageBase,
	      PropEnum.page,
	      PropEnum.pageOrientation,
	      PropEnum.pagerPhone,
	      PropEnum.parameters,
	      PropEnum.parent,
	      PropEnum.paths,
	      PropEnum.permissions,
	      PropEnum.policies,
	      PropEnum.portalPage,
	      PropEnum.position,
	      PropEnum.postalAddress,
	      PropEnum.printerAddress,
	      PropEnum.productLocale,
	      PropEnum.qualifier,
	      PropEnum.related,
	      PropEnum.recipientsEMail,
	      PropEnum.recipients,
	      PropEnum.related,
	      PropEnum.replacement,
	      PropEnum.requestedExecutionTime,
	      PropEnum.retentions,
	      PropEnum.rsAffineConnections,
	      PropEnum.rsMaximumProcesses,
	      PropEnum.rsNonAffineConnections,
	      PropEnum.rsQueueLimit,
	      PropEnum.runAsOwner,
	      PropEnum.runningState,
	      PropEnum.runOptions,
	      PropEnum.screenTip,
	      PropEnum.searchPath,
	     PropEnum.searchPathForURL,
	      PropEnum.sequencing,
	      PropEnum.serverGroup,
	      PropEnum.source,
	      PropEnum.specification,
	      PropEnum.startAsActive,
	      PropEnum.startDate,
	      PropEnum.state,
	      PropEnum.status,
	      PropEnum.stepObject,
	      PropEnum.surname,
	      PropEnum.target,
	      PropEnum.taskID,
	      PropEnum.timeZoneID,
	      PropEnum.triggerDescription,
	      PropEnum.triggerName,
	      PropEnum.type,
	      PropEnum.unit,
	      PropEnum.uri,
	      PropEnum.usage,
	      PropEnum.user,
	      PropEnum.userCapabilities,
	      PropEnum.userCapability,
	      PropEnum.userName,
	      PropEnum.version,
	      PropEnum.verticalElementsRenderingLimit,
	      PropEnum.viewed,
	      PropEnum.weeklyFriday,
	      PropEnum.weeklyMonday,
	      PropEnum.weeklySaturday,
	      PropEnum.weeklySunday,
	      PropEnum.weeklyThursday,
	      PropEnum.weeklyTuesday,
	      PropEnum.weeklyWednesday,
	      PropEnum.yearlyAbsoluteDay,
	      PropEnum.yearlyAbsoluteMonth,
	      PropEnum.yearlyRelativeDay,
	      PropEnum.yearlyRelativeMonth,
	      PropEnum.yearlyRelativeWeek, 
	    };
	    return properties;
	  }
		
		public QueryUser ()
		{		
			ContentManagerService_ServiceLocator cmServiceLocator = new ContentManagerService_ServiceLocator();   
	 				
	        try
	        {            
	            //create the binding objects 
	            cmService = cmServiceLocator.getcontentManagerService(new java.net.URL(CM_URL));
	        }
	        catch(Exception e)
	        {
	            e.printStackTrace();
	        }
		}
		
		//This method loggs the user to ReportNet
		public String quickLogon(String namespace, String uid, String pwd)
		{
			StringBuffer credentialXML = new StringBuffer();

			credentialXML.append("<credential>");
			credentialXML.append("<namespace>").append(namespace).append("</namespace>");
			credentialXML.append("<username>").append(uid).append("</username>");
			credentialXML.append("<password>").append(pwd).append("</password>");
			credentialXML.append("</credential>");

			String encodedCredentials = credentialXML.toString();
		    XmlEncodedXML xmlCredentials = new XmlEncodedXML();
		    xmlCredentials.set_value(encodedCredentials);
			try
			{		
				cmService.logon(xmlCredentials,null );

			//TODO Set the BiBusHeader
		SOAPHeaderElement temp = ((Stub)cmService).getResponseHeader 
			 ("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader");
		BiBusHeader cmBiBusHeader = (BiBusHeader)temp.getValueAsType
			(new QName ("http://developer.cognos.com/schemas/bibus/3/","biBusHeader"));
		((Stub)cmService).setHeader
			("http://developer.cognos.com/schemas/bibus/3/", "biBusHeader", cmBiBusHeader);

			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			return ("Logon successful as " + uid);
		}
}

