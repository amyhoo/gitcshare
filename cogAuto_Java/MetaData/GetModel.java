package MetaData;

/**
 * GetModel.java
 *
 * Licensed Material - Property of IBM
 * � Copyright IBM Corp. 2003, 2010
 *
 * Description: Technote 1344196 - SDK Sample to extract the model of a published package
 *
 * Tested with: IBM Cognos BI 10.1, Java 5.0
 * 
 * If OutOfMemoryError occurs, set a Run configuration VM argument of -Xmx768m
 */
import com.cognos.developer.schemas.bibus._3.*;
import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import common.*;
public class GetModel extends CognosOperation
{	
	
	public void getModel(String packageSearchPath) throws Exception
	{
		PropEnum props[] = new PropEnum[]{PropEnum.searchPath, PropEnum.model};
										  
		BaseClass[] model = cmService.query(new SearchPathMultipleObject(packageSearchPath+"/model"), props, new Sort[]{}, new QueryOptions());

		for(int i = 0; i < model.length; i++)
		{
			System.out.println("\n**********************************************************\n");
			System.out.println("Model: " + model[i].getSearchPath().getValue() + "\n");
			System.out.println(((Model)model[i]).getModel().getValue());
			System.out.println("\n**********************************************************\n");
			
			//If you want to write the model data to a file that can be opened from Framework Manager,
			//Uncomment the following lines
			
			/*String modelFile="d:\\temp\\model"+i+".xml";
			File oFile = new File(modelFile);
			FileOutputStream fos = new FileOutputStream(oFile);
			String temp=((Model)model[i]).getModel().getValue();
			ByteArrayInputStream bais = new ByteArrayInputStream(temp.getBytes("UTF-8"));
			System.out.println("Writing model data to file "+modelFile);
			while (bais.available() > 0) {
					fos.write(bais.read());
				};
			fos.flush();
			fos.close();*/
			
		}
	}
	
	public static void main(String args[])
	{
		// search path of the package from which the model will be extracted 
		String packageSearchPath = "CAMID('INT:u:0a50606a26a37c4e99d560cfe61b9c2c')/folder[@name='My Folders']/folder[@name='Jean']/package[@name='CTP Policy DMR']";
		GetModel current = new GetModel();

		try
		{
			current.logon("DEV");
			current.initService(current.conn.reportService);
			current.getModel(packageSearchPath);
			System.out.println("\nDone.");
		}
		catch (Exception e)
		{
			System.out.println(e);
		}
	}
}

