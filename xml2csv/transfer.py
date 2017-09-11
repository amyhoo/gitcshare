'''
Created on 23 Aug 2016

@author: u391812 Xu Ya Min(Amy)
'''
import csv,sys,os,urllib,ast
try: 
  import xml.etree.cElementTree as ET 
except ImportError: 
  import xml.etree.ElementTree as ET 
import logging
import sys,getopt,os
class XmlNodeFilter(ast.NodeTransformer):
    def __init__(self,xml_node):
        self.xml_node=xml_node       
    def visit_Name(self,node):
        name=self.xml_node.get(node.id)        
        return ast.Str(name if name else '')
    def visit_List(self,node):
        item_list=[]
        for item in node.elts:
            if type(item)==ast.Name:
                item_list.append(ast.Str(item.id))  
            else:
                item_list.append(item) 
        node.elts= item_list          
        return node     
           
def getJobNode(xml_path,field=None,node_list=None,script=None):
    '''
    get job which 
    NODEID="NZARP2" or NODEID="BWARE" or NODEID="DB2AP5" or NODEID="COGAP1"
    '''    
    def node_filter(node,script):                                
        tree=ast.parse(script,mode='eval')
        tree=XmlNodeFilter(node).visit(tree)
        try:
            return eval(compile(ast.fix_missing_locations(tree),'temp.py', mode='eval'))
        except Exception as e:
            pass
    try:
        tree=ET.parse(xml_path)
    except Exception as e:
        print ("can't load file from "+xml_path)
        sys.exit(1)    
    root=tree.getroot()
    if node_list:
        node_list=node_list.split(",")
    def get_sub_node(node,subNodeStrList=["INCOND","OUTCOND","VARIABLE"]):
        return_list=[]
        for sub_node_str in subNodeStrList:
            sub_node=job.find(sub_node_str)
            if sub_node!=None:
                return_list.append((sub_node_str,ET.tostring(sub_node)))   
        return return_list         
    for job in root.iter('JOB'):
#         if job.get("CREATED_BY")=="ctmauth" and job.get("CREATION_DATE")=="20141104" and job.get("CREATION_TIME")=="160326":
#             pass    
        yield_list=[]    
        if script:
            if node_filter(job,script):
                yield_list= job.items()
        else:
            if job.get(field) in node_list:    
                yield_list= job.items()                     
        if yield_list:
            sub_list=get_sub_node(job)
            if sub_list:
                yield_list.extend(sub_list)
            yield yield_list
def output (output_file,jobs):
    '''
    output into csv
    '''            
    header=[]
    index=0
    buffer=[]
    for job in jobs:        
        temp=[tag for tag,value in job if tag not in header]
        if temp!=[]:            
            header.extend(temp)
        job={key:value.replace("\n","\r") for key,value in job}
        buffer.append(dict(job))            
        index+=1
#     header=sorted(header)    
    header=[
    "ACTIVE_FROM",
    "ACTIVE_TILL",
    "APPLICATION",
    "APPL_TYPE",
    "APR",
    "AUG",
    "AUTOARCH",
    "CHANGE_DATE",
    "CHANGE_TIME",
    "CHANGE_USERID",
    "CMDLINE",
    "CONFCAL",
    "CONFIRM",
    "CREATED_BY",
    "CREATION_DATE",
    "CREATION_TIME",
    "CREATION_USER",
    "CRITICAL",
    "CYCLIC",
    "CYCLIC_TIMES_SEQUENCE",
    "CYCLIC_TOLERANCE",
    "CYCLIC_TYPE",
    "DATE",
    "DAYS",
    "DAYSCAL",
    "DAYS_AND_OR",
    "DEC",
    "DESCRIPTION",
    "DOCLIB",
    "DOCMEM",
    "FEB",
    "IND_CYCLIC",
    "INSTREAM_JCL",
    "INTERVAL",
    "JAN",
    "JOBISN",
    "JOBNAME",
    "JUL",
    "JUN",
    "MAR",
    "MAXDAYS",
    "MAXRERUN",
    "MAXRUNS",
    "MAXWAIT",
    "MAY",
    "MEMLIB",
    "MEMNAME",
    "MULTY_AGENT",
    "NODEID",
    "NOV",
    "OCT",
    "PARENT_FOLDER",
    "PRIORITY",
    "RETRO",
    "RULE_BASED_CALENDAR_RELATIONSHIP",
    "RUN_AS",
    "SEP",
    "SHIFT",
    "SHIFTNUM",
    "SUB_APPLICATION",
    "SYSDB",
    "TASKTYPE",
    "TIMEFROM",
    "TIMETO",
    "TIMEZONE",
    "USE_INSTREAM_JCL",
    "WEEKDAYS",
    "WEEKSCAL",
    "INCOND",
    "OUTCOND",
    "VARIABLE",
            ]
    with open(output_file,'w+') as outfiler:
        writer=csv.DictWriter(outfiler,header,lineterminator='\n', delimiter=',', quoting=csv.QUOTE_MINIMAL)
        writer.writeheader()
        writer.writerows(buffer)
    print(len(buffer))
def download_file(file_url='//int/GroupData/ITProSup/Sharepoint/EMSCHEDULES/ALL_PROD_EM_SCHEDULES_160827.XLS'):        
    f=open(file_url)
    filename=file_url.split('/')[-1]
    filename=filename.split('.')[0]
    with open(filename+'.xml','w+') as xml_file:
        xml_file.write(f.read())
    return filename+'.xml'    
       
if __name__ == "__main__":    
    sys.path.append(sys.path[0])
    try:
        opts,args=getopt.getopt(sys.argv[1:],'hi:a:v:p:',['help','input=','attri=','value=','python'])
    except Exception as e:
        print ('input right parameters')
    handle_directory=None
    for op,value in opts:
        if op in ('-h','--help'):
            print("""
            this program is filtering the xml file  by the attribute and value of "JOB" node; or by python script to generate csv table that show the nodes and theirs attribute we want  
            -h/--help:show help message
            -i/--input:the original xml location
            -a/--attri:node's attribute name of "JOB" node filtered
            -v/--value:node's attribute value of "JOB" node filtered
            -p/--python:use simple python script to filter, if this parameters is offered,the program will ignore attibute and value filtering
                        example "NODEID in [DB2AP5,NZARP2]" 
            """)
        elif op in ('-i','--input'):
            input_xml=value
        elif op in ('-a','--attri'):
            node_list=value
        elif op in ('-c','--value'):
            field=value
        elif op in ('-p','--python'):
            exec_sens=value
    if input_xml[-4:]==".xml":
        output_csv=input_xml[:-4]+'.csv'    
    if exec_sens:
        get_nodes=getJobNode(input_xml,script=exec_sens)  
    else:
        get_nodes=getJobNode(input_xml,field,node_list)         
    output(output_csv,get_nodes)