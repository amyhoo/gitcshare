'''
Created on 9 Aug 2016

@author: u391812
'''
import csv
from ControlMJobsToLogMapper.tools.reDefine import *
import re
import json
from collections import OrderedDict
from functools import cmp_to_key
from ControlMJobsToLogMapper.tools.topological import toposort2
from ControlMJobsToLogMapper.tools.commandClient import *
class JobInfo:
    '''
    combine the three table ,and output the information of  job into the ControlMOutput.csv
    '''     
    def __init__(self,directory):
        self.directory=directory   
    def outputInfo(self):      
        header1, jobList = self.loadJobIndex(filename=self.directory+'\ControlMNow.csv')
        header2, jobBuffer = self.loadJobIndex(filename=self.directory+'\ControlMJobRef.csv')
        header3, paramBuffer = self.loadJobParams(filename=self.directory+'\ControlMJobParam.csv')
        with open(self.directory+'\ControlMOutput.csv', 'w+') as output:
            fieldnames = [field for field in header2]
            fieldnames.append('label1')
            fieldnames.append('params')
            output_writer = csv.DictWriter(output, fieldnames, lineterminator='\n')
            output_writer.writeheader()                          
            for key in jobList:
                line_dict = {field:'' for field in header2}
                line_dict['params'] = ''
                line_dict['label1'] = ''
                line_dict['JOB_NAME'] = key
                if not jobBuffer.get(key):  # the task deleted
                    line_dict['label1'] = 'no job available'
                    output_writer.writerow(line_dict)
                    continue
                
                line_dict.update(jobBuffer.get(key))
                if paramBuffer.get(key) and len(set(paramBuffer.get(key))) > len(set([var for  var, value in paramBuffer.get(key)])):
                    line_dict['label1'] = 'params error:var has different value'                                
                param_dict = dict(paramBuffer.get(key)) if paramBuffer.get(key) else ''
                line_dict['params'] = json.dumps(param_dict)
                output_writer.writerow(line_dict)
    
    def loadJobIndex(self, filename=''):
        '''
    load job file into buffer        
        '''
        buffer = {}
        with open(filename, 'r') as filer:
            reader = csv.DictReader(filer)             
            for line_dict in reader:
                buffer[line_dict["JOB_NAME"]] = line_dict
            return reader.fieldnames, buffer                
    def loadJobParams(self, filename=''):
        buffer = {}
        with open(filename, 'r') as filer:
            reader = csv.DictReader(filer)             
            for line_dict in reader:
                if line_dict['JOB_NAME'] not in buffer:
                    buffer[line_dict['JOB_NAME']] = []
                buffer[line_dict['JOB_NAME']].append((line_dict['VARIABLE_NAME'], line_dict['VARIABLE_VALUE']))                                
            return reader.fieldnames, buffer            

class JobAnalysis():
    '''
    analysis job ,and output the labels of job 
    '''
    def __init__(self,directory):
        self.directory=directory       
    msg={
         'cmd_lack_params':'the command lack the parameters needed',
         'read_false':'there is no such file exist in this server',
         'log_false':'problem in finding the log file',
         'log_no_info':'there is no log info in this file',
         'log_true':'find log file correctly',
         'job_no_exist':'no job available',
         'job_var_no_exist':'job variable not available',
         'ctmfw_job':'ctmfw job',
         'bash_job':'bash job',
         'netdisk':"don't know what server from netdisk path",
         'cmd':"don't know what server with cmd command",
         'bat':"don't know what server bat",
         'sh':'the script is sh file',
         'bsh':'the script is bsh file',
         'extension':"the script is with extension",
         'no_extension':"the script is without extension"
    }
    def getCmd(self, job_dict):
        '''
        get Cmd from record
        '''  
        controlm_system_var = {
                    '%%JOBNAME':'',
                    '%%ODATE':'',
                    '%%OWNER':'',
                    }  

        job_name = job_dict['JOB_NAME']   
        cmd = job_dict['COMMAND_LINE']
        if job_dict['params'] and json.loads(job_dict['params']):
            try:
                params_dict = json.loads(job_dict['params'])
                params_dict['%%JOBNAME'] = job_name
                for param_key in params_dict:
                    cmd = cmd.replace(param_key, params_dict.get(param_key))        
                return cmd
            except Exception as e:
                pass
        else:
            return cmd
        
    def analysisCmd(self):
        '''
        main function to analysis the cmd and classify it
        '''
        buffer={}
        with open(self.directory+'\ControlMOutput.csv', 'r') as inFiler,open(self.directory+'\ControlMInfo.csv','w+') as outFiler:
            reader = csv.DictReader(inFiler)
            header=reader.fieldnames
            header.append('label2')
            header.append('label3')
            header.append('label4')
            header.append('label5')      
            header.append('script_file')    
            writer=csv.DictWriter(outFiler,header,lineterminator='\n')  
            writer.writeheader()
            remote=self.remoteScriptInfo()  
            next(remote)
            for job_dict in reader:        
                jobname=job_dict['JOB_NAME']   
                job_dict['label2']=''
                job_dict['label3']=''
                job_dict['label4']=''
                job_dict['label5']=''          
                job_dict['script_file']=''
                cmd = self.getCmd(job_dict)
                if job_dict['label1'].strip() ==self.msg['job_no_exist'] :                    
                    pass
                elif '%%' in cmd:
                    job_dict['label1']=self.msg['job_var_no_exist']                                        
                elif 'ctmfw ' in cmd:                    
                    job_dict['label1'] = self.msg['ctmfw_job']
                elif 'bash ' in cmd:
                    job_dict['label1'] =self.msg['bash_job']
                else:                              
                    for str1 in cmd.split(" "):                    
                        m = re.match(RE_DEFINE["net-file-path"], str1)                 
                        if m:
                            job_dict['label1'] = self.msg['netdisk']
                            break
                        m = re.match(RE_DEFINE["file-path-cmd"], str1, re.I)
                        if m:
                            job_dict['label1'] =self.msg['cmd']
                            break
                        m = re.match(RE_DEFINE["file-path-bat"], str1, re.I)
                        if m:
                            job_dict['label1'] =self.msg['bat']
                            break 
                        m = re.match(RE_DEFINE["file-path-sh"], str1, re.I)
                        if m:
                            job_dict['label1'] = self.msg['sh'] 
                            job_dict['script_file']= str1    
                            info=remote.send((m.group(),cmd))
                            next(remote)   
                            job_dict['label2']=info['is_ok']    
                            job_dict['label3']=info['msg']    
                            job_dict['label4']=info.get('log')
                            job_dict['label5']=info.get('test')           
                            break
                        m = re.match(RE_DEFINE["file-path-bsh"], str1, re.I)
                        if m:
                            job_dict['label1'] =self.msg['bsh']
                            job_dict['script_file']= str1
                            info=remote.send((m.group(),cmd))
                            next(remote)   
                            job_dict['label2']=info['is_ok']
                            job_dict['label3']=info['msg']
                            job_dict['label4']=info.get('log')
                            job_dict['label5']=info.get('test')                            
                            break
                        m = re.match(RE_DEFINE["file-path-suffix"], str1, re.I)
                        if m:
                            job_dict['label1'] = self.msg['extension']
                            job_dict['script_file']= str1                        
                            break
                        m = re.match(RE_DEFINE["file-path-no-suffix"], str1, re.I)
                        if m:
                            job_dict['label1'] = self.msg['extension']
                            job_dict['script_file']= str1
                            info=remote.send((m.group(),cmd))
                            next(remote)   
                            job_dict['label2']=info['is_ok']
                            job_dict['label3']=info['msg']
                            job_dict['label4']=info.get('log')   
                            job_dict['label5']=info.get('test')                    
                            break         
                writer.writerow(job_dict)
    def scriptLogVar(self,buffer,pattern_str=RE_DEFINE['log_var'],re_params={'flags':re.I}):
        '''
    get variable about 'log' defined in the file        
        '''     
        m = re.findall(pattern_str, buffer, **re_params)       
        if m:
            return list(set(m))
        else:
            return []
        
    def scriptVarTrace(self,buffer,var_list,cmd,pattern_str=RE_DEFINE['log_var'],re_params={'flags':re.I}): 
        '''
    load variables that is contained in the current variable definition  
        '''
        #make order of var_list            
        temp= OrderedDict()
        info = {}
        for var in var_list:
            temp[re.sub(r'[${}]','',var.strip())]=""           
        var_dict=temp        
        params_list=re.sub('^sh ','',cmd).split(" ")                
        limit_time = 100        
        while not all(var_dict.values()) and limit_time:
            limit_time -= 1
            key = [key for key, value in var_dict.items() if not value][0]
            pattern_string = key + RE_DEFINE["assign_line"]
            m = re.search(pattern_string, buffer, re.I)
            if m:
                var_define = m.group(0)
                var_dict[key] = re.sub(key+'=','',var_define).strip(' "\'\n')
                m1 = re.findall(RE_DEFINE["var"], var_define)
                for item in m1:
                    item_key=re.sub(r'[${}]','',item.strip())
                    if item_key in var_dict:
                        continue
                    if re.match('\d+',item_key):#                      
                        try:
                            var_dict[item_key]=params_list[int(item_key)]
                        except Exception as e:
                            var_dict[item_key]=""
                            info["is_ok"] =self.msg['cmd_lack_params']
                    else:
                        var_dict[item_key]=""                                    
        
        info["msg"]=var_dict
        if "is_ok" in info:
            pass
        elif limit_time <= 1:
            info["is_ok"] = self.msg['log_false']            
        else:
            info["is_ok"] = self.msg['log_true']         
        return info           
    
    def reOrder(self, var_dict):
        '''
    order the all variable connected with log,the order of them is defined by inclusion relation        
        '''
        graph_data={}
        sorted_var_dict=OrderedDict()
        for var in var_dict:
            var_value=var_dict[var]
            graph_data[var]=set([re.sub(r'[${}]','',item.strip()) for item in re.findall(RE_DEFINE["var"], var_value)])
        sorted_list=toposort2(graph_data)
        for item in sorted_list: 
            for var in item.split():
                sorted_var_dict[var]=var_dict[var]
        return sorted_var_dict
    
    def assembleLog(self,client,var_dict):        
        '''
        assemble variable,to substitute variable with the real string 
        '''
        log_var={}
        atom_var=['$INSTANCE','${INSTANCE}']
        set_var={'Environment':'production','environment':'production'}
        index=0
        var_dict=self.reOrder(var_dict)
        while index<len(var_dict):            
            key,value=list(var_dict.items())[index]            
            find_var = re.findall(RE_DEFINE["var"], value)
            if key in set_var:
                var_dict[key]=set_var[key]
            elif key in atom_var or re.match('\d+',key):
                pass                                 
            else :#
                for var in find_var:
                    if var not in atom_var:
                        var_key=re.sub(r'[${}]','',var.strip())
                        var_value=var_dict[var_key]                        
                        value=value.replace(var,var_value)                
                for var in re.findall(r'`.+`', value):
                    out, err=client.exec_command(var.strip("`"))
                    if not err and out:
                        out=out.decode('utf8')
                        value=value.replace(var,out.strip())
                var_dict[key]=value
            index+=1
        return var_dict
    
    def testLog(self,client,var_dict):
        '''
        test the following location to look if it really exist
        '''
        test_key=['logDir','LogFileDirectory','LOGDIR','log_dir','LOG_DIR']
        result={}
        for key in test_key:
            if key in var_dict:
                value=var_dict[key]
                out, err=client.exec_command('ls '+value)
                if not err and out:       
                    result[key]='exist'
                else:
                    err=err.decode('utf8')
                    result[key]='no-exist:' +   err
        return result    
    
    def remoteScriptInfo(self):
        '''
        log into server via SSH ,and get the file of the server
        '''        
        with CommandClient() as client:  
            while True:
                filename,cmd = yield
                stdout, stderr = client.exec_command('ls ' + filename)                
                if stderr:
                    error_msg = stderr        
                    yield {
                            'is_ok':self.msg['read_false'],
                            'msg':"No such file or directory " +'in server Nzarp2'
                            }
                else:
                    info = {}
                    with  OpenFile(filename,client=client) as remote_file:    
                        if type(remote_file)==str:
                            yield {
                                   'is_ok':self.msg['read_false'],
                                   'msg':"the file can't be read because of "+remote_file
                            
                                   }                
                        else:
                            file_buffer = remote_file.read()  
                            if isinstance(file_buffer, bytes):
                                file_buffer=file_buffer.decode('utf8')                          
                            msg=self.scriptLogVar(file_buffer)
                            if not msg:
                                yield {
                                        'is_ok':self.msg['log_no_info'],
                                        'msg':"this file has no log info"
                                        }                        
                            else:
                                info=self.scriptVarTrace(file_buffer, msg,cmd)                                
                                vars=self.assembleLog(client, info["msg"])    
                                result=self.testLog(client, vars)                            
                                yield {
                                       'is_ok':info["is_ok"],
                                       'msg':json.dumps(info["msg"]),
                                       'log':json.dumps(vars),
                                       'test':json.dumps(result)
                                       }                                                                        
               
if __name__ == "__main__":
    import sys,getopt
    sys.path.append('/data/home/u391812')
    opts,args=getopt.getopt(sys.argv[1:],'hi:o:')
    handle_directory=None
    for op,value in opts:
        if op=='-o':#destination directory
            handle_directory=value
    if handle_directory:
        JobInfo(handle_directory).outputInfo()            
        JobAnalysis(handle_directory).analysisCmd()
    else:
        print ('please input the files directory!')
    
