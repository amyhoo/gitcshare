package Tasks;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.JobDefinition;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.Report;
import com.cognos.developer.schemas.bibus._3.ReportView;
import com.cognos.developer.schemas.bibus._3.RetentionRule;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.Sort;

import common.AllProperties;
import common.CognosOperation;
public class ReportInfo extends CognosOperation {

	public static void main(String args[]){
		ReportInfo current=new ReportInfo();
		current.logon("DEV");
		current.initService(current.reportService);
		BaseClass[] bc =current.queryReports();
		for (int i = 0; i < bc.length; i++) {
			System.out.println(bc[i]);
		}
	}
	public BaseClass[] queryReports(){
		PropEnum props[] =  AllProperties.getProperties();	
		String searchPath = "/content/folder[@name='Deployment']//report";
		BaseClass bc[] = null;
		try {
			SearchPathMultipleObject spMulti =new SearchPathMultipleObject(searchPath);					                
			bc =cmService.query(spMulti, props, new Sort[] {}, new QueryOptions());
		} catch (Exception e) {
			e.printStackTrace();		
		}
		return bc;
	}		
	//Queries the Content Store for Run History of specified object
	public void getRunHistory( String objectPath)
	{
		SearchPathMultipleObject spMultiple=new SearchPathMultipleObject();
		spMultiple.set_value(objectPath);
		
		PropEnum props[] = new PropEnum[]{PropEnum.searchPath,
										  PropEnum.defaultName,
										  PropEnum.retentions};
		try
		{
			BaseClass[] object = cmService.query(
					spMultiple,props, new Sort[]{}, new QueryOptions());
			RetentionRule myRules[] = new RetentionRule[] {};
			int rule = 0;
			if (object != null && object.length > 0)
			{
				if (object[0] instanceof Report )
				{
					 myRules = ((Report)object[0]).getRetentions().getValue();	 
				}
				else if (object[0] instanceof JobDefinition) 
				{
					myRules = ((JobDefinition)object[0]).getRetentions().getValue();
				}
				else if (object[0] instanceof ReportView)
				{
					myRules = ((ReportView)object[0]).getRetentions().getValue();
				
				}
				if (myRules!= null)
				{
					for (int i=0; i < myRules.length; i++)
					{
						String x = myRules[i].getObjectClass().getValue();
						if (x.endsWith("history"))
							rule=i;	 
					}
					if (myRules[rule].getMaxDuration() != null)
					{
					    //If Duration is set, display how many days the outputs are saved for
						System.out.print("Run History - Duration: ");
						String duration = myRules[rule].getMaxDuration();
						if (duration.endsWith("D"))
							System.out.println(duration.substring(1,duration.length()-1)+ " Days");
						else
							System.out.println(duration.substring(1,duration.length()-1)+ " Months");
					}
					else
					{
						//If Number of Occurrences set, display how many outputs are saved
						System.out.print("Run History - Number of Occurrences: ");
						System.out.println(myRules[rule].getMaxObjects().intValue());
					}
				}
			}	
		}
		catch (Exception ex)
		{
			System.out.println(ex);
		}
	}	
}
