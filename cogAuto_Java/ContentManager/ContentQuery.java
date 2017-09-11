package ContentManager;
import common.CognosLogOn;
import common.AllProperties;
import com.cognos.developer.schemas.bibus._3.*;

public class ContentQuery { 
	CognosLogOn conn= null;
	ContentManagerService_PortType cmService=null;
	ReportService_PortType reportService = null;
	public void init(){
		conn=new CognosLogOn("DEV");
		cmService=conn.getContentManagerService();
		reportService=conn.getReportService();
		conn.logonToCognos();
	}
	
	public static void main(String args[]) {
		//setProxy();
		ContentQuery mainClass = new ContentQuery(); // instantiate the class
		
		mainClass.init();
		BaseClass[] bs = mainClass.getPackages();
//		BaseClass[] bs = mainClass.getDatesources();
		//BaseClass[] bs = mainClass.getCredentials();
//		for (int i=0;i<bs.length;i++){
//			
//		}
		
		mainClass.printInfo(bs);
	}
	public void PrintDataSigon(BaseClass[] bs){
		for (int i = 0; i < bs.length; i++) {
			String datasource_name=bs[i].getDefaultName().getValue();
			BaseClass[] connections=getDatesourceConnections(datasource_name);
			for (int j = 0; j < connections.length; j++) {
				String connection_name=connections[j].getDefaultName().getValue();
				String connection_string=((DataSourceConnection)connections[j]).getConnectionString().getValue();	
				String credential=((DataSourceCredential)bs[i]).getDataSourceConnectionName().getValue();
				System.out.println( datasource_name +"`"+connection_name+"`"+connection_string+"`"+credential);
//				BaseClass[] signons=mainClass.getConnectionSignon(datasource_name,connection_name);
//				for (int k = 0; k < signons.length; k++) {
//					System.out.println( signons[k].getDefaultName().getValue());					
//				}
			}
		}				
	}
	public BaseClass[] getObjects(){
		//
		PropEnum props[] = new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName,PropEnum.creationTime,
				PropEnum.modificationTime, PropEnum.objectClass,PropEnum.usage,PropEnum.owner,PropEnum.permissions,PropEnum.policies};		
		String searchPath = "//*";		
		return queryInfo(searchPath,props);		
	}
	
	public BaseClass[] getPackages() {
		//
//		PropEnum props[] =  new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName};	
		PropEnum props[] =  AllProperties.getProperties();		
		String searchPath = "/content/package[@name='Audit']";				
		return queryInfo(searchPath,props);
	}
	public BaseClass[] getDatesources() {
		//
		PropEnum props[] =  new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName};	
//		PropEnum props[] = AllProperties.getProperties();		
		String searchPath ="//dataSource";				
		return queryInfo(searchPath,props);
	}	
	public BaseClass[] getCredentials() {
		//
		PropEnum props[] =  new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName};	
//		PropEnum props[] = AllProperties.getProperties();		
		String searchPath ="//dataSourceCredential";				
		return queryInfo(searchPath,props);
	}		
	public BaseClass[] getReports(){
		//
		PropEnum props[] =  new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName};	
//		PropEnum props[] = AllProperties.getProperties();		
		String searchPath = "/content//reports";			
		return queryInfo(searchPath,props);		
	}

	public void ownDatasourceSignons(){
		PropEnum props[] =  new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName};	
//		PropEnum props[] = AllProperties.getProperties();
		String searchPath = "/capability/securedFunction[@name=â€™Manage own data source signonsâ€™]";			
	}
	
	public void getPermissions(){
		//
		PropEnum props[] =  new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName};	
//		PropEnum props[] = AllProperties.getProperties();
		String searchPath = "";		
	}
	public void getSchedules(){
		//
		PropEnum props[] =  new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName};	
//		PropEnum props[] = AllProperties.getProperties();
		String searchPath = "";		
	}	
	public void getJobs(){
		//		
		PropEnum props[] =  new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName};	
//		PropEnum props[] = AllProperties.getProperties();
		String searchPath = "";		
	}
	public void getAgents(){
		//		
		PropEnum props[] =  new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName};	
//		PropEnum props[] = AllProperties.getProperties();
		String searchPath = "";		
	}	
	public BaseClass[] getPackageModel(String package_name) {
		//
		PropEnum props[] = new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName,PropEnum.model };		
		String searchPath = "/content//package[@name='"+package_name+"']/model";				
		return queryInfo(searchPath,props);
	}	

	public BaseClass[] getPackageDatesource(String package_name) {
		//
		PropEnum props[] = new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName};		
		String searchPath ="/content//package[@name='"+package_name+"']/*";				
		return queryInfo(searchPath,props);
	}
	public BaseClass[] getDatesourceConnections(String datasource_name) {
		//
		PropEnum props[] = new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName,PropEnum.connectionString,PropEnum.binding };	
//		PropEnum props[] = AllProperties.getProperties();		
		String searchPath ="//dataSource[@name='"+datasource_name+"']/dataSourceConnection";				
		return queryInfo(searchPath,props);
	}
	public BaseClass[] getConnectionSignon(String datasource_name,String connection_name) {
		//
		PropEnum props[] = new PropEnum[] { PropEnum.searchPath, PropEnum.defaultName };
//		PropEnum props[] = AllProperties.getProperties();		
		String searchPath ="//dataSource[@name='"+datasource_name+"']/dataSourceConnection[@name='"+connection_name+"']/dataSourceSignon";				
		return queryInfo(searchPath,props);
	}	


	boolean hasSecondaryRequest(AsynchReply response,	String secondaryRequest)
	{
		AsynchSecondaryRequest[] secondaryRequests =
			response.getSecondaryRequests();
		for (int i = 0; i < secondaryRequests.length; i++)
		{
			if (secondaryRequests[i].getName().compareTo(secondaryRequest)
				== 0)
			{
				return true;
			}
		}
		return false;
	}	

	
	public void printInfo(BaseClass[] bc){
		//print information
		System.out.println("packages:\n");
		if (bc != null) {
			for (int i = 0; i < bc.length; i++) {
				System.out.println( bc[i].getDefaultName().getValue()		
						);
			}
		}		
	}
	private BaseClass[] queryInfo_with(String searchPath,PropEnum[] props){
		BaseClass bc[] = null;
		try {
			SearchPathMultipleObject spMulti =new SearchPathMultipleObject(searchPath);
			PropEnum referenceProps[] =new PropEnum[] { PropEnum.user};
		                
			QueryOptions options = new QueryOptions();
			options.setDataEncoding(EncodingEnum.MIME);
			RefProp refPropArray[] = { new RefProp()};
			refPropArray[0].setProperties(referenceProps);
			refPropArray[0].setRefPropName(PropEnum.credentials);
			options.setRefProps(refPropArray);
			bc =cmService.query(spMulti, props, new Sort[] {}, options);
		} catch (Exception e) {
			e.printStackTrace();		
		}
		return bc;
	}	
	
	private BaseClass[] queryInfo(String searchPath,PropEnum[] props){
		BaseClass bc[] = null;
		try {
			SearchPathMultipleObject spMulti =new SearchPathMultipleObject(searchPath);					                
			bc =cmService.query(spMulti, props, new Sort[] {}, new QueryOptions());
		} catch (Exception e) {
			e.printStackTrace();		
		}
		return bc;
	}	
}
