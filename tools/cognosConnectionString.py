'''
Created on 23 Sep 2016

@author: u391812
'''
import re
def connectionDict(connectionString):
    segment_list=connectionString.split(";")
    regonize_map={
                  "LOCAL":("from","LOCAL"),
                  "D2":("db_type","db2"),
                  "PC":("db_type","file"),
                  "OR":("db_type","oracle"),
                  "SS":("db_type","SQLServer"),
                  "NZ":("db_type","Netezza"),
                  "TM":("db_type","TM"),
                  "OL":("db_type","OL"),
                  "CS":("db_type","CS"),
                  "OD":("db_type","OD"),
                  "CL":("db_type","CL"),
                  "XML":("db_type","XML"),
                  "VM":("db_type","VM"),
                  "JD-NZ":("drive-type","jdbc netezza"),
                  "JD-D2":("drive-type","jdbc db2"),
                  "DSN=":("DSN",""),
                  "DRIVER_NAME=":("driver",""),
                  "DATABASE=":("Database Name",""),                   
                  "URL=":("",""),
                  }
    return_dict={}
    for item in segment_list:
        sub_str=item.split("=")[0]
        if regonize_map.get(item):
            key,value=regonize_map.get(item)
            return_dict[key]=value
        elif sub_str=="URL": 
            content_str=item.split("=")[1]
            search_pattern=r"://(.+):"
            find=re.search(search_pattern, content_str)
            if find:
                return_dict["DB Server Name"]=find.group(1)
            search_pattern=r"://(.+):.+/([^/]+)$"
            find=re.search(search_pattern, content_str)
            if find:
                return_dict["Database Name"]=find.group(2)
        else:            
            if regonize_map.get(sub_str+"="):
                key,value=regonize_map.get(sub_str+"=")
                return_dict[key]=item
    return return_dict
