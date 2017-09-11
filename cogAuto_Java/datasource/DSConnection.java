package au.com.suncorp.datasource;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.cognos.developer.schemas.bibus._3.AddOptions;
import com.cognos.developer.schemas.bibus._3.AnyTypeProp;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BaseClassArrayProp;
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

import au.com.suncorp.AutoDeploy.Deployment;
import au.com.suncorp.process.ExecutionStep;

public class DSConnection extends Deployment implements ExecutionStep {
	
	//dataSourceType 
	//refer to https://www.ibm.com/support/knowledgecenter/SSRL5J_1.0.1/com.ibm.swg.ba.cognos.ug_cra.10.1.1.doc/c_odbcdatasources.html#ODBCDataSources
	String dataSourceType = "NZ"; //netezza as default;  
	String dataSourceName = null;//"TEST-D";
	String dbServerName = null;//"srname";
	String databasePort = null;//"9009";
	String databaseName = null;//"dbname";
	String signonuid = null;//"usr";
	String signonpwd = null;//"pwd";

	public static void main(String[] args) {
		if (args.length < 1) {
			displayHelp();
		}
		
		DSConnection ds = new DSConnection();
		
		//-d DEV -dsn TEST-E -dbserver svr -dbport 2001 -dbname dbname
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-d"))
				ds.environment = args[++i];
			else if (args[i].compareToIgnoreCase("-dst") == 0)
				ds.dataSourceType = args[++i];								 
			else if (args[i].compareToIgnoreCase("-dsn") == 0 ) 
				ds.dataSourceName = args[++i];
			else if (args[i].compareToIgnoreCase("-dbserver") == 0) 
				ds.dbServerName = args[++i];
			else if (args[i].compareToIgnoreCase("-dbport") == 0) 
				ds.databasePort = args[++i];
			else if (args[i].compareToIgnoreCase("-dbname") == 0) 
				ds.databaseName = args[++i];
			else if (args[i].compareToIgnoreCase("-dbusr") == 0) 
				ds.signonuid = args[++i];
			else if (args[i].compareToIgnoreCase("-dbpwd") == 0) 
				ds.signonpwd = args[++i];
			else
				displayHelp();
		}
		
		if(ds.dataSourceName == null || ds.dbServerName == null || ds.databasePort == null || ds.databaseName == null){
			displayHelp();
		}
		if(ds.dataSourceType.equalsIgnoreCase("netezza")){
			ds.dataSourceType = "NZ";
		}

		ds.create();
		
	}
	
	public void create(){
		try {
			logon();
			
			String dataSourceConnectionString = "^User ID:^?Password:;LOCAL;"+ dataSourceType +";DSN="+ dataSourceName
					+ ";UID=%s;PWD=%s;@ASYNC=0@0/0@COLSEQ=IBM_JD_CNX_STR:^User ID:^?Password:;LOCAL;JD-NZ;"
					+ "URL=jdbc:netezza://"+ dbServerName + ":" + databasePort +"/"+ databaseName +";"
					+ "DRIVER_NAME=org.netezza.Driver";
			
			this.createDataSource(dataSourceName, dataSourceConnectionString, null, signonuid, signonpwd);
			
		} catch (Exception ex) {
			System.out.println("\nAn error occurred\n");
			ex.printStackTrace();
		}
	}
	
	@Override
	public void execute(Map map) throws Exception {
		Map<String,String> m = (Map<String,String>)map;
		environment = m.get("env");
		dataSourceType = m.get("type");
		if(dataSourceType == null || dataSourceType.equalsIgnoreCase("netezza")){
			dataSourceType = "NZ";
		}
		dataSourceName = m.get("name");
		dbServerName = m.get("dbserver");
		databaseName = m.get("dbname");
		databasePort = m.get("dbport");
		signonuid = m.get("signonuid");
		signonpwd = m.get("signonpwd");
		create();
	}
	
	public static void displayHelp() {
		String usage = "";
		usage += "Create datasource and datasource connection.\n\n";
		usage += "Usage:\n\n";
		usage += "-s namespaceID\n\tNamespaceID the user belongs to.\n";
		usage += "-u userID\n\tUserID of a System Administrator.\n";
		usage += "-p userPassword\n\tPassword for the UserID.\n";
		usage += "-g CognosBIDispatcher\n\tDispatcher URL for Cognos BI.\n";
		usage += "\t Default: http://localhost:9300/p2pd/servlet/dispatch\n";
		
		usage += "-dst dataSourceType\n\tThe type of the datasource(netezza: NZ/netezza)\n";
		usage += "-dsn dataSourceName\n\tThe name of the datasource\n";
		usage += "-server dbServerName\n\tThe server name of the database\n";
		usage += "-dbport databasePort\n\tThe port of database\n";
		usage += "-dbname databaseName\n\tThe database name\n";
		
		usage += "-dbusr dbusername\n\tThe database username\n";
		usage += "-dbpwd dbpassword\n\tThe database password\n";

		System.out.println(usage);
		System.exit(1);
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
		
		if(StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)){
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
		System.out.println("data source connection is created successfully.");
	}

	public void listDataSourcesAndConnections() throws Exception {
		PropEnum dataSourceProps[] = new PropEnum[] { PropEnum.defaultName, PropEnum.searchPath, PropEnum.capabilities };
		PropEnum dataConnectionProps[] = new PropEnum[] { PropEnum.defaultName, PropEnum.connectionString };

		BaseClass[] dataSources = cmService.query(new SearchPathMultipleObject("CAMID(\":\")//dataSource"),
				dataSourceProps, new Sort[] {}, new QueryOptions());

		for (BaseClass dataSource : dataSources) {
			DataSource  ds = (DataSource)dataSource;
			
			System.out.println("\nData Source name: " + dataSource.getDefaultName().getValue() + " ,capa=" + Arrays.asList(ds.getCapabilities().getValue()));
			
			BaseClass[] dataConnections = cmService.query(
					new SearchPathMultipleObject(dataSource.getSearchPath().getValue() + "//dataSourceConnection" ),
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
