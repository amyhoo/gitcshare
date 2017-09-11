package datasource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.axis.AxisEngine;

import com.cognos.developer.schemas.bibus._3.AddOptions;
import com.cognos.developer.schemas.bibus._3.AnyTypeProp;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BaseClassArrayProp;
import com.cognos.developer.schemas.bibus._3.ContentManagerService_PortType;
import com.cognos.developer.schemas.bibus._3.DataSource;
import com.cognos.developer.schemas.bibus._3.DataSourceConnection;
import com.cognos.developer.schemas.bibus._3.DataSourceSignon;
import com.cognos.developer.schemas.bibus._3.DeleteOptions;
import com.cognos.developer.schemas.bibus._3.NmtokenProp;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.SearchPathSingleObject;
import com.cognos.developer.schemas.bibus._3.Sort;
import com.cognos.developer.schemas.bibus._3.StringProp;
import com.cognos.developer.schemas.bibus._3.TokenProp;
import com.cognos.developer.schemas.bibus._3.UpdateActionEnum;

import common.*;

public class DataSourceUtils extends CognosOperation {	
	public static void main(String[] args) {
		String nameSpaceID = "";
		String username = "";
		String password = "";
		
		//dataSourceType 
		//refer to https://www.ibm.com/support/knowledgecenter/SSRL5J_1.0.1/com.ibm.swg.ba.cognos.ug_cra.10.1.1.doc/c_odbcdatasources.html#ODBCDataSources
		String dataSourceType = "NZ"; //netezza;  
		String dataSourceName = "TEST-D";
		String serverName = "srname";
		String databaseName = "dbname";
		String databasePort = "9009";
		String dbusername = "usr";
		String dbpassword = "pwd";
		
		try {
			DataSourceUtils ds = new DataSourceUtils();
			
			CognosLogOn conn = new CognosLogOn("DEV");
			Map<String, Object> axisOption = new HashMap<String, Object>();
			axisOption.put(AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
			ds.cmService = conn.getContentManagerService(axisOption);
			
			conn.logonToCognos();
			
			String dataSourceConnectionString = "^User ID:^?Password:;LOCAL;"+ dataSourceType +";DSN="+ dataSourceName
					+ ";UID=%s;PWD=%s;@ASYNC=0@0/0@COLSEQ=IBM_JD_CNX_STR:^User ID:^?Password:;LOCAL;JD-NZ;"
					+ "URL=jdbc:netezza://"+ serverName + ":" + databasePort +"/"+ databaseName +";"
					+ "DRIVER_NAME=org.netezza.Driver";
			
			ds.createDataSource(dataSourceName, dataSourceConnectionString, null, dbusername, dbpassword);
			
			//ds.listDataSourcesAndConnections();
		} catch (Exception ex) {
			System.out.println("\nAn error occurred\n");
			ex.printStackTrace();
		}

	}

	public void createDataSource(String dataSourceName, String dataSourceConnectionString, String isolationLevel, String username, String password) throws Exception {
		DataSource dataSource = new DataSource();
		TokenProp tp = new TokenProp();
		tp.setValue(dataSourceName);
		dataSource.setDefaultName(tp);
		
		DataSourceConnection dataSourceConnection = new DataSourceConnection();
		dataSourceConnection.setDefaultName(tp);
		StringProp s = new StringProp();
		s.setValue(dataSourceConnectionString);
		dataSourceConnection.setConnectionString(s);
		
		if(isolationLevel != null){
			NmtokenProp isoLevel = new NmtokenProp();
			isoLevel.setValue(isolationLevel);//"phantomProtection"
			dataSourceConnection.setIsolationLevel(isoLevel);
		}
		
		AddOptions addOptions = new AddOptions();
		addOptions.setUpdateAction(UpdateActionEnum.replace);
		
		// Create the data source
		cmService.add(new SearchPathSingleObject("CAMID(\":\")"), new BaseClass[] { dataSource }, addOptions);
		// Create the data source connection
		BaseClass bc[] = cmService.add(new SearchPathSingleObject("CAMID(\":\")/dataSource[@name='" + dataSourceName + "']"), 
				new BaseClass[] { dataSourceConnection }, addOptions);
		
		if(username != null && password != null){
			DataSourceSignon signon = new DataSourceSignon();
			signon.setDefaultName(tp);
			
			BaseClassArrayProp bcap = new BaseClassArrayProp();
			bcap.setValue(bc);
			signon.setParent(bcap);
			
			// Add credentials to the signon
			AnyTypeProp credentials = new AnyTypeProp();
			String credString = "<credential><username>"+ username +"</username><password>"+ password +"</password></credential>";
			credentials.setValue(credString);
			signon.setCredentials(credentials);
			
			SearchPathSingleObject newObject = new SearchPathSingleObject();
			newObject.set_value((bc[0]).getSearchPath().getValue());
			cmService.add(newObject, new BaseClass[]{signon}, addOptions);
		}
		System.out.println("Done");
	}

	public void listDataSourcesAndConnections() throws Exception {
		PropEnum dataSourceProps[] = new PropEnum[] { PropEnum.defaultName, PropEnum.searchPath, PropEnum.capabilities };
		PropEnum dataConnectionProps[] = new PropEnum[] { PropEnum.defaultName, PropEnum.connectionString };

		BaseClass[] dataSources = cmService.query(new SearchPathMultipleObject("CAMID(\":\")//dataSource"),
				dataSourceProps, new Sort[] {}, new QueryOptions());

		for (int i = 0; i < dataSources.length; i++) {
			DataSource  ds = (DataSource)dataSources[i];
			
			System.out.println("\nData Source name: " + dataSources[i].getDefaultName().getValue() + " ,capa=" + Arrays.asList(ds.getCapabilities().getValue()));
			
			BaseClass[] dataConnections = cmService.query(
					new SearchPathMultipleObject(dataSources[i].getSearchPath().getValue() + "//dataSourceConnection" ),
					dataConnectionProps, new Sort[] {}, new QueryOptions());
			for (int j = 0; j < dataConnections.length; j++) {
				System.out.println("\tData Connection name: " + dataConnections[j].getDefaultName().getValue());
				System.out.println("\tConnection string: "
						+ ((DataSourceConnection) dataConnections[j]).getConnectionString().getValue());
			}
			break;
		}
	}
	
	public void deleteDataSourceConnection(String dataSourceName) throws Exception {
		String dataSourceConnectionSearchPath = null;
		DataSourceConnection dataSourceConnection = new DataSourceConnection();
		dataSourceConnection.setSearchPath(new StringProp());
		dataSourceConnectionSearchPath = "CAMID(\":\")/dataSource[@name='" + dataSourceName
				+ "']/dataSourceConnection[@name='" + dataSourceName + "']";
		dataSourceConnection.getSearchPath().setValue(dataSourceConnectionSearchPath);

		DeleteOptions deleteOptions = new DeleteOptions();
		deleteOptions.setForce(true);
		deleteOptions.setRecursive(true);

		cmService.delete(new BaseClass[] { dataSourceConnection }, deleteOptions);
	}

}
