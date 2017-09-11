'''
Created on 4 Jul 2017

@author: u391812
'''
import sys,getopt,os,logging
import json
from collections import OrderedDict
from itertools import chain
import subprocess
import re

MAIN_LOC='/apps/dbfit/cdbfit/'
ACCESS_LOC='/apps/dbfit/cdbfit/dbfit_central/'
DEV_LOC="/apps/dbfit/cdbfit/DBFit_git/FitNesseRoot/DBFit_Regression/DEV_Regression/"
UAT_LOC="/apps/dbfit/cdbfit/DBFit_git_uat/FitNesseRoot/DBFit_Regression/"
PRD_LOC="/apps/dbfit/cdbfit/DBFit_git_prd/FitNesseRoot/DBFit_Regression/"

def dictIn(dict1,dictList):
    '''    
    if dict1 have the same in dictList return True else False
    '''    
    
    for dict2 in dictList:
        if dict(dict1)==dict(dict2):
            return True
    return False

def firstCategory(configDict):
    '''
    get category and teamKey from config
    '''
    first_category_set=set() # first folder : teamKey      
    for k1,teamList in configDict.items():
        if k1=="DBFitPort":continue    
        if teamList[0]["Category"]:
            first_category_set.add((teamList[0]["Category"],k1))
        else:
            first_category_set |={(item["ProjectName"],k1) for item in teamList[1:]}   
    return first_category_set
#check the teamKey and stash, and update
def checkAccess(access_pair,team_pair):
    '''
    access_pair:ssh/connectionFile name and teamKey
    team_pair:ssh/connectionFile name and teamKey
    make they are compatible  
    '''
    #get all sshUrl string in a set    
    #team_pair1={key:team_pair[key] for key in team_pair if key not in access_pair} # new keys ,new pairs 
    team_pair2={key:team_pair[key] for key in team_pair if key in access_pair} # keys already exist
    
    if set(team_pair2.items())  < set(access_pair.items()):#old pairs should be compatible with access
        return 0
    else:
        return [key+" has been owned by another team "+"; not team "+value for key,value in set(team_pair2.items()) - set(access_pair.items())]
    
#config file handle
def updateConfig(teamKey,mode,configFileStr,teamCategory=None):
    '''
    configFileStr: a json content 
    teamKey: is a key identify a team, 
    teamCategory:is a category that show in page
    mode: whole, add, delete
    read information from configFile ,and update the Projects.config    
    '''    
    teamConfig=json.loads(configFileStr,object_pairs_hook=OrderedDict)
    with open("Projects.config") as filer:
        config=json.loads(filer.read(),object_pairs_hook=OrderedDict)    
    
    #get stash pair
    access_pair=OrderedDict()# all SSHUrl and teamKeyp mapping    
    for k1,teamList in chain(config["DEV"].items(),config["UAT"].items(),config["PRD"].items()):
        # the first element in teamList is attribute for this team
        if k1=="DBFitPort":continue        
        access_pair.update((project["SSHUrl"],k1) for project in teamList[1:])
    team_pair=OrderedDict((item["SSHUrl"],teamKey) for item in chain(teamConfig.get("DEV",[]),teamConfig.get("UAT",[]),teamConfig.get("PRD",[])))
        
    val1=checkAccess(access_pair, team_pair)
    if val1:
        print("the teamKey is not matching: ")
        print("\n".join(val1))
        return False                        
    
    #the first element
    teamAttribute=OrderedDict([("Category",teamCategory)])
    for envKey in (key for key in teamConfig if key in ["DEV","UAT","PRD"]):# envKey are in DEV, UAT, PRD        
        if not config[envKey].get(teamKey):            
            config[envKey][teamKey]=[teamAttribute]    
        config[envKey][teamKey][0]=teamAttribute
#     if not config["DEV"].get(teamKey):
#         config["DEV"][teamKey]=[teamAttribute]
#     if not config["UAT"].get(teamKey):
#         config["UAT"][teamKey]=[teamAttribute]
#     if not config["PRD"].get(teamKey):
#         config["PRD"][teamKey]=[teamAttribute]

#     config["DEV"][teamKey][0]=teamAttribute
#     config["UAT"][teamKey][0]=teamAttribute
#     config["PRD"][teamKey][0]=teamAttribute
        
    if mode=="whole":
        for envKey in (key for key in teamConfig if key in ["DEV","UAT","PRD"]):
            config[envKey][teamKey][1:]=teamConfig[envKey]
        
    elif mode=="add":
        for envKey in (key for key in teamConfig if key in ["DEV","UAT","PRD"]):
            new_projects=[project for project in teamConfig.get(envKey) if not dictIn(project, config[envKey][teamKey][1:])]
            config[envKey][teamKey].extend(new_projects)
#         #dev
#         new_projects=[project for project in teamConfig.get("DEV",[]) if not dictIn(project, config["DEV"][teamKey][1:])]
#         config["DEV"][teamKey].extend(new_projects)
#         #uat
#         new_projects=[project for project in teamConfig.get("UAT",[]) if not dictIn(project, config["UAT"][teamKey][1:])]
#         config["UAT"][teamKey].extend(new_projects)        
#         #prod
#         new_projects=[project for project in teamConfig.get("PRD",[]) if not dictIn(project, config["PRD"][teamKey][1:])]
#         config["PRD"][teamKey].extend(new_projects)
        
    else:#delete
        for envKey in (key for key in teamConfig if key in ["DEV","UAT","PRD"]):
            for index1,project1 in enumerate(teamConfig[envKey]):
                found=False
                for index2,project2 in enumerate(config[envKey][teamKey]):
                    if dict(project1)==dict(project2):
                        found=True
                        break
                if found:
                    del config[envKey][teamKey][index2]
            
#         #dev        
#         for index1,project1 in enumerate(teamConfig["DEV"]):
#             found=False
#             for index2,project2 in enumerate(config["DEV"][teamKey]):
#                 if dict(project1)==dict(project2):
#                     found=True
#                     break
#             if found:
#                 del config["DEV"][teamKey][index2]
# 
#         #uat        
#         for index1,project1 in enumerate(teamConfig["UAT"]):
#             found=False
#             for index2,project2 in enumerate(config["UAT"][teamKey]):
#                 if dict(project1)==dict(project2):
#                     found=True
#                     break
#             if found:
#                 del config["UAT"][teamKey][index2]
# 
#         #prd        
#         for index1,project1 in enumerate(teamConfig["PRD"]):
#             found=False
#             for index2,project2 in enumerate(config["PRD"][teamKey]):
#                 if dict(project1)==dict(project2):
#                     found=True
#                     break
#             if found:
#                 del config["PRD"][teamKey][index2]                            
   #check first category     
    def checkPageFolder(env):
        #env DEV / UAT / PRD
        configDict=config.get(env)
        first_category_set=firstCategory(configDict) # first folder : teamKey      
        first_category=[category for category,teamKey in first_category_set ]   
        duplicatedVal=[value for value in first_category if first_category.count(value)>1]
        if  duplicatedVal:
            return set(duplicatedVal)
        else:
            return []

    returnVal=True
    for envKey in (key for key in teamConfig if key in ["DEV","UAT","PRD"]):# envKey are in DEV, UAT, PRD
        val=checkPageFolder(envKey)
        if val:
            print("dupliacated names are conflicting with others in "+envKey)
            print (val)
            returnVal=False

#     returnVal=True
#     val=checkPageFolder("DEV")
#     
#     if val:
#         print ("dupliacated names are conflicting with others in DEV")
#         print (val)
#         returnVal=False         
#     val= checkPageFolder("UAT")
#     if val:
#         print ("dupliacated names are conflicting with others in UAT")
#         print(val)
#         returnVal=False   
#     val= checkPageFolder("PRD")          
#     if val:
#         print ("dupliacated names are conflicting with others in PRD")
#         print (val)
#         returnVal=False  
    if not returnVal:
        return False
    with open("Projects.config" ,'w+') as filer:
        filer.write(json.dumps(config,indent=1)) 
    
def createConnectionFile(connectionFile):
    '''
    encrypt the password in connectionFile ,and save in dbfit_central
    '''           
    newFileName=ACCESS_LOC+connectionFile.split("/")[-1]    
    #ssubprocess.call("mv connectionFile "+newFileName)
    buffer=[]
    with open(connectionFile) as filer:        
        buffer=filer.read().split("\n")
        
    #delete the original file
    subprocess.call(["rm  "+ connectionFile],shell=True)
        
    rePassword=r"password[ ]*=[ ]*(.+)[\n&]|password[ ]*=[ ]*(.+)$"
    password=""
    start_index=0
    end_index=0
    find=False
    for index,line in enumerate(buffer):
        line1=line.strip()        
        if line1[0]=="#":continue            
        for m in re.finditer(rePassword, line1):                       
            password=m.group(1)
            start_index,end_index=m.span(1)            
            find=True         
            break   
        if find:break    
    output=subprocess.check_output(["sh dbfit_central/encrypt.sh "+ password],shell=True)    
    encryptedPassword=output.strip().split("\n")[-1]           
    line=line[:start_index]+encryptedPassword+line[end_index:]
    buffer[index]=line    
    #print(buffer)
    with open(newFileName ,'w+') as filer:
        filer.write("\n".join(buffer))    
        
#connectin file handle
def updateConnectionFiles(teamKey,*args):    
    '''
    check if connectionFile and teamKey are matching with Connection.access file
    '''
    with open("Connection.access") as filer:
        accessPair=json.loads(filer.read(),object_pairs_hook=OrderedDict)
        
    teamPair=OrderedDict((file.split("/")[-1],teamKey) for file in args)
    
    
    val1=checkAccess(accessPair, teamPair)
    if val1:
        print("the teamKey is not matching: ")
        print("\n".join(val1))
        return False
    
    #update new connectionFile access     
    accessPair.update([(key,value) for key,value in teamPair.items() if key not in accessPair])
    
    #write access into file
    with open("Connection.access","w+") as filer:
        filer.write(json.dumps(accessPair,indent=1))
        
    #for each file,doing update
    for filer in args:
        createConnectionFile(filer)
#
def adminAccess(user="admin",password=None):
    if user=="admin" and password=="adminPassword":
        return True
    else:
        return False
#check information
def adminCheckStatus(user="admin",password="",mode="teams",teamKey=None):
    '''
    only admin can check status    
    '''
    admin=adminAccess(user, password)
    if not admin:
        print("sorry , you have no admin accessment")
        return False
    with open("Projects.config") as filer:
        config=json.loads(filer.read(),object_pairs_hook=OrderedDict)          
    with open("Connection.access") as filer:
        access=json.loads(filer.read(),object_pairs_hook=OrderedDict)          

    if mode=="teams":
        teams=set()
        for key,value in chain(config["DEV"].items(),config["UAT"].items(),config["PRD"].items()):#
            if key=="DBFitPort":continue
            teams.add(key)
        for key,value in access.items():
            teams.add(value)
        print (teams)
        
    elif mode=="stashes":
        access_pair=OrderedDict()# all SSHUrl and teamKeyp mapping
        for k1,teamList in chain(config["DEV"].items(),config["UAT"].items(),config["PRD"].items()):
            if k1=="DBFitPort":continue
            access_pair.update({project["SSHUrl"]:k1 for project in teamList[1:]})
        print(access_pair)
        
    elif mode=="connections":
        print("access for connection files:")
        print(access)        
        
        print("connection files:")
        # "find . -maxdepth 1 -type f |xargs grep -e 'password' -l"
        output=subprocess.check_output("grep -rl -e 'password[ ]*=' "+ACCESS_LOC)
        for file in output.split("\n"):
            filename=file.split("/")[-1]
            print (filename+":"+access[filename])
            
    elif mode=="category":
        DEV_dict=dict(firstCategory(config["DEV"]))
        UAT_dict=dict(firstCategory(config["UAT"]))
        PRD_dict=dict(firstCategory(config["PRD"]))
        projects_category=[]
        cmd="ls -l "+DEV_LOC+" | grep '^d' | awk '{print $9}'"
        output=subprocess.check_output(cmd,shell=True)
        for category in output.split("\n"):
            projects_category.append(("DEV:"+category,DEV_dict.get(category,None)))
        cmd="ls -l "+UAT_LOC+" | grep '^d' | awk '{print $9}'"
        output=subprocess.check_output(cmd,shell=True)
        for category in output.split("\n"):
            projects_category.append(("UAT:"+category,UAT_dict.get(category,None)))
        cmd="ls -l "+PRD_LOC+" | grep '^d' | awk '{print $9}'"
        output=subprocess.check_output(cmd,shell=True)
        for category in output.split("\n"):
            projects_category.append(("PRD:"+category,PRD_dict.get(category,None)))              
        print(projects_category)                  
        
    elif mode=="teamInfo" and teamKey:
        print("config DEV:")
        print(config["DEV"].get(teamKey))
        
        print("config UAT:")
        print(config["UAT"].get(teamKey))
        
        print("config PRD:")
        print(config["PRD"].get(teamKey))
        
        print("connection access:")
        print({key:teamKey for key in access if access.get(key)==teamKey})
    else:# log information
        pass

def adminUpdate(user="admin",password="",configFile=None, connectionFile=None,action=""):   
    '''
    update Projects.config, connetionFile
    '''  
    admin=adminAccess(user, password)
    if not admin:
        print("sorry , you have no admin accessment")
        return False
    if configFile:
        cmd="mv "+configFile+" "+MAIN_LOC+"Projects.config"
        print(cmd)
        returnCode=subprocess.call(cmd,shell=True)
        print("update the Projects.config")
    if connectionFile:
        cmd="mv "+connectionFile+" "+MAIN_LOC+"Connection.access"
        print(cmd)
        returnCode=subprocess.call(cmd,shell=True)
        print("Connection.access")
    if action=="clear project category":#clear category that are not in use
        pass
    elif action=="clear connection":#clear connection files that are not in use
        pass
    
#update a stash
def updateStash(user="admin",password="",ENV=None,teamKey=None,projectList=None):
    '''
    teamKey:
    admin:
    projectList:
    ENV:List ,DEV/UAT/PRD    
    '''
    admin=adminAccess(user, password)
    if not ENV:
        ENV=["DEV","UAT","PRD"]
    elif set(ENV)<=set(["DEV","UAT","PRD"]):
        pass
    else:
        print("environment should be DEV, UAT, PRD")
        return False
    
    with open("Projects.config") as filer:
        config=json.loads(filer.read(),object_pairs_hook=OrderedDict)
        
    projects_list=[]     
    for env in ENV:
        for k1,teamList in config[env].items():
            if k1=="DBFitPort":continue
            category=teamList[0]["Category"]
            for project in teamList[1:]: 
                project["Category"]=category
                project["TeamKey"]=k1
                project["ENV"]=env 
                #project["GitFolder"]=project["SSHUrl"].split("/")[-1][:-4]
                projects_list.append(project)
                        
    update_list=[]
    if teamKey:
        if projectList:# update_list     
            update_list=[item for item in projects_list if item["TeamKey"]==teamKey and item["ProjectName"] in projectList]            
        else: # update_list
            update_list=[item for item in projects_list if item["TeamKey"]==teamKey]    
    elif admin: # no teamKey
        update_list=projects_list
    else: # no admin no teamKey
        print("you have no right to update,input teamKey or admin account")
    
    #stash and branch
    update_dict={}
    for item in update_list:
        key=item["SSHUrl"]+"|"+item["BranchName"]
        if not update_dict.get(key):
            update_dict[key]=[]        
        update_dict[key].append(item)
      
    for key in update_dict:    
        #get ssh ,
        try:
            ssh=key.split("|")[0]
            branch=key.split("|")[1]
            cmdGitClone="cd " +MAIN_LOC+r"""        
            rm -rf tmp_git
            mkdir tmp_git
            chmod 775 tmp_git
            cd tmp_git
            git clone -b """+ branch+" "+ ssh+" ./"              
            print(cmdGitClone)
            subprocess.check_call(cmdGitClone , shell=True)
                    
            #mv projects into destination           
            for project in update_dict[key]:
                if project["ENV"]=="DEV":
                    destPath=DEV_LOC+ project["Category"]+"/"+project["ProjectName"] if project["Category"] else DEV_LOC+project["ProjectName"]
                elif project["ENV"]=="UAT":
                    destPath=UAT_LOC+project["Category"]+"/"+project["ProjectName"] if project["Category"] else UAT_LOC+project["ProjectName"]
                else:
                    destPath=PRD_LOC+project["Category"]+"/"+project["ProjectName"] if project["Category"] else UAT_LOC+project["ProjectName"]
                cmdMvProject="mkdir -p "+destPath+"\n"
                cmdMvProject+="rm -rf "+destPath+"\n"            
                cmdMvProject+="cp -r ./tmp_git/"+project["FitNesseRootLocation"]+"/FitNesseRoot/"+project["ProjectName"]+" "+destPath+"\n"  
                    
                if project["Category"]:
                    cmdMvProject+="echo '!contents -R1 -g -p -f -h' >"""+destPath+"/../content.txt"+"\n"                                   
                    cmdMvProject+="""echo '<?xml version="1.0" encoding="UTF-8" standalone="no"?>\n<properties>\n<Edit/>\n<Files/>\n<Help>Team """+project["Category"]+"</Help>\n<Properties/>\n<RecentChanges/>\n<Refactor/>\n<Search/>\n<Static/>\n<Versions/>\n<WhereUsed/>\n<secure-write/>\n</properties>' "+">"+destPath+"/../properties.xml"                                                                                 
                print(cmdMvProject)
                subprocess.check_call(cmdMvProject , shell=True)            
        except Exception as e:
            print(e) 
if __name__ == "__main__":              
    try:
        opts,args=getopt.getopt(sys.argv[1:],'hc:d:k:m:u:p:l:a:',['help','config=','conn=','key=','mode=','update=','project=','category=','admin='])
    except Exception as e:
        print ('input right parameters')    
    adminUser=""
    adminPWd=""
    teamKey='default'
    configMode='whole'
    connectionFile=""
    configFile=""
    update=""
    for op,value in opts:
        if op in ('-h','--help'):
           print("""
           this program will update dbfit project configuration, database connection file, update stash                                             
            -h/--help:show help message
            -c/--config:configuration file content
            -d/--conn:the database connection file name 
            -k/--key:teamKey
            -m/--mode:choose configuration file update mode: whole, add, delete
            -u/--update:update stash
            -p/--projects: projects names ,seperate by space /all
            -a/--admin:input admin password to do full changes ignoring teamKey
            """)
                
        elif op in ('-c','--config'):#dev,uat,prd
            configFile=value
        elif op in ('-d','--conn'):
            connectionFile=value
        elif op in ('-k','--key'):
            teamKey=value
        elif op in ('-m','--mode'):
            configMode=value    
        elif op in ('-u','--update'):
            update=value
        elif op in ('-p','--project'):            
            if not value:
                projectList=[]
            else:
                projectList=value.split(" ")
        elif op in ('-l','--category'):
            teamCategory=value
        elif op in ('-a','--admin'):
            adminUser==value.split(" ")[0]
            adminPWd==value.split(" ")[1]
      
    if configFile:
        print("update Config")
        updateConfig(teamKey, configMode, configFile, teamCategory)
    if connectionFile:
        print("update Connection Files")
        updateConnectionFiles(teamKey,connectionFile)
    if update=="ALL":        
                
        updateStash(user=adminUser,password=adminPWd,ENV=[],teamKey=teamKey,projectList=projectList)  
    elif update in ["DEV","UAT","PRD"]:
        print("update", update)        
        updateStash(user=adminUser,password=adminPWd,ENV=[update],teamKey=teamKey,projectList=projectList)
    else:#no update
        pass