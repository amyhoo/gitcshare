'''
Created on 12 Oct 2016

@author: u391812 u391812 Xu Ya Min(Amy)
'''
import sys,getopt,os,logging
import json
from collections import OrderedDict

def updateConn():
    '''
    update the dataconnection file
    '''
def check_endpoint(current,endpoint={}):
    find=False
    for node in current.values():
        if isinstance(node, dict) and (set(endpoint.items()) < set(node.items())):         
            find=True
            break
    return find
if __name__ == "__main__":    
    try:
        opts,args=getopt.getopt(sys.argv[1:],'he:p:u:b:r:d',['help','env=','project=','ssh=','branch=','root=','delete'])
    except Exception as e:
        print ('input right parameters')    
    delete=False
    for op,value in opts:
        if op in ('-h','--help'):
           print("""
           this job will add a new project in bidrd1dbfit server 
           you test case folder in your stash like : {RepositoryName}/{TestRootName}/FitNesseRoot/{ProjectName}                    
            this program is add a new test project into dbfit server bidrd1dbfit
            -h/--help:show help message
            -e/--env:test case in which environment ,such as DEV, UAT, PRD
            -p/--project:the project name , the folder name of  {ProjectName} 
            -u/--ssh:the stash link,you should add our bidadmin user access to the ssh
            -b/--branch:which branch you want to use for test
            -r/--root:the test root folder name, which is {TestRootName}
            -d/--delete: delete the project have already exist
            """)
             
        elif op in ('-e','--env'):#dev,uat,prd
            if value=="dev":
                environment="STASHDEVBRANCH"
            elif value=="uat":
                environment="STASHUATBRANCH"
            elif value=="prd":
                environment="STASHRELEASEBRANCH"
            else:
                raise "the environment setting is not right"
        elif op in ('-p','--project'):
            project_name=value
        elif op in ('-u','--ssh'):
            ssh=value
        elif op in ('-b','--branch'):
            branch=value    
        elif op in ('-r','--root'):
            root=value
        elif op in ('-d','--delete'):            
            delete=True
    
    with open("DBFitProj.config") as filer:
        config=json.loads(filer.read(),object_pairs_hook=OrderedDict)
    current=config[environment]
    if delete:        
        current.pop(project_name)
    else:        
        endpoint={"SSHUrl":ssh,
                  "BranchName":branch
                  }
#         if check_endpoint(current,endpoint):
#             print ("the ssh and branch already exist")
#             exit (1)
#         else:
#             config[environment][project_name]=OrderedDict()
#             config[environment][project_name]["SSHUrl"]=ssh
#             config[environment][project_name]["BranchName"]=branch
#             config[environment][project_name]["FitNesseRootLocation"]=root
        config[environment][project_name]=OrderedDict()
        config[environment][project_name]["SSHUrl"]=ssh
        config[environment][project_name]["BranchName"]=branch
        config[environment][project_name]["FitNesseRootLocation"]=root            
    config_text=json.dumps(config,indent=1)
    print (config_text)
    with open("DBFitProj.config","w+") as filer:
        filer.write(config_text)