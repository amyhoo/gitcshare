// Licensed Material - Property of IBM
// © Copyright IBM Corp. 2003, 2010

/**
 * ContentOverview.java
 *
 *
 * Description: Technote 1335653  - Extract information from model.xml and store into the XLS file 
 * 
 * Tested with: IBM Cognos BI 10.1, Java 5.0 	
 * 
 */


import java.io.*;

import java.util.*;
import org.dom4j.*;
import org.dom4j.Element;
import org.dom4j.io.*;

public class ContentOverview {

	Document doc = null;
	File file = null;
	String namespace = null;
	String newLine = System.getProperty("line.separator");
	
	

	public static void main(String[] args) 
	{
		String sModel = null;
		
		if (args.length < 1)
		{
			System.out.println("Usage: \njava ContentOverview <model_path> ");
			System.out.println("Example: \njava ContentOverview d:\\TestProject ");
			System.exit(1);
		}
		else
		{
			 sModel = args[0] + "\\model.xml";
		}
		
		ContentOverview st = new ContentOverview();
		st.setModel(sModel);
		st.parseModel(args[0]);
	}

	public void setModel(String p_sModel)
	{
		file = new File(p_sModel);
	}
	

	/**
	 * ParseModel method will parse the model.xml and create Content.txt 
	 * in the same location
	 */
	public void parseModel(String contentFile)
	{		
		Document doc = this.getDocument();
		String tempDoc = "FrameWork Manager Model Content Report" + newLine + "Catalog Information";
		tempDoc += newLine;
		
		Element project = (Element)doc.selectSingleNode("/project/name");
		if (project != null){
			tempDoc += "Model Name: " + project.getText() + newLine;
		}
		
		Element desc = (Element)doc.selectSingleNode("/project/namespace/name");
		if (desc != null){
			tempDoc += newLine + "Model Namespace: " + desc.getText() ;
		}
		
		List dataSources = doc.selectNodes("//dataSource");
		tempDoc += newLine + "Database Information: " + newLine ;

		for (int z =0; z<dataSources.size(); z++)
		{
			//An XML data source will have different nodes
			Element qse = (Element)dataSources.get(z);
			tempDoc += newLine +"\t" + "Name: " + qse.selectSingleNode("name").getText() + newLine;
			tempDoc += "\t" + "Content Manager Datasource: " + qse.selectSingleNode("cmDataSource").getText() + newLine;
			if (qse.selectSingleNode("catalog") != null)
				tempDoc += "\t" + "Catalog: " + qse.selectSingleNode("catalog").getText() + newLine;
			if (qse.selectSingleNode("schema") != null)
			tempDoc += "\t" + "Schema: " + qse.selectSingleNode("schema").getText() + newLine;
			tempDoc += "\t" + "Type: " + qse.selectSingleNode("type/queryType").getText() + newLine;
			tempDoc += "\t" + "Interface: " + qse.selectSingleNode("type/interface").getText() + newLine;
		}
		
		
		tempDoc += newLine + "Query Subjects: " + newLine;
		List qsEls = doc.selectNodes("//querySubject");
		
		for (int z =0; z<qsEls.size(); z++)
		{
			Element qse = (Element)qsEls.get(z);
			tempDoc += newLine + "Query Subject: " + newLine;
			tempDoc += "\t" + "Name: " + qse.selectSingleNode("name").getText() + newLine;
			tempDoc += newLine + "\tQuery Items: " + newLine;
			List els = qse.selectNodes("queryItem/name");
			tempDoc = addToTempDoc(els, tempDoc);
		}
		
		tempDoc += newLine + "Relationships: " + newLine;
		List rEls = doc.selectNodes("//relationship");
		
		for (int z =0; z<rEls.size(); z++)
		{
			Element re = (Element)rEls.get(z);			
			if (re.selectSingleNode("left/mincard").getText().compareTo("zero") == 0)
				tempDoc += newLine + re.selectSingleNode("name").getText() + ": " + "Outer Join" + newLine;
			else
				tempDoc += newLine + re.selectSingleNode("name").getText() + ": " + "Inner Join" + newLine;
			String expValue = re.selectSingleNode("expression").getStringValue();
			tempDoc += expValue.trim() + newLine;
			
		}
		
			this.writeDoc(tempDoc, contentFile);
		
		}
		

	public String addToTempDoc(List Els, String tempDoc)
	{
		for (int z =0; z<Els.size(); z++)
		{
			Element qse = (Element)Els.get(z);
			tempDoc += "\t\t" + qse.getText() + newLine;
		}
		return tempDoc;
	}
	
	
	private void writeDoc(String doc, String contentFile)
	{		
		String fileName = "Content.xls";
		Writer fw = null;
		try
		{
			fw = new FileWriter(contentFile + "\\" + fileName);
			fw.write(doc);
			fw.flush() ;
			fw.close();
			System.out.println("The contents were written to " + fileName);
			
		}catch (Exception ioe)
		{
			ioe.printStackTrace() ;
		}
	}
	
	private Document getDocument()
	{

		SAXReader sr = new SAXReader();
		//tempDoc is the document without namespaces
		String tempDoc =null;
		try
		{
			tempDoc = sr.read(file).asXML();
		}
		catch (Exception e)
		{
			System.out.println(e);
		}
		
		//temprarily remove the namspaces from the project node  
		//because this causes problems when parsin the XML
		int index = tempDoc.indexOf("<project");
		if (index != -1){
			this.namespace = tempDoc.substring(index, tempDoc.indexOf("BMTModelSpecification.xsd") + 27);
			String start = tempDoc.substring(0,index);
			String end = tempDoc.substring(tempDoc.indexOf("BMTModelSpecification.xsd") + 27);
			tempDoc = start + "<project>" + end;
		}
		
		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(tempDoc.getBytes("UTF-8"));
			doc = sr.read((InputStream) bais);
		}catch (Exception e) 
		{
			System.out.println(e);
		}
		
		return doc;
	}
	
	
}

