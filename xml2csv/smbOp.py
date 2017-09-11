'''
Created on 7 Sep 2016

@author: u391812
'''
import os
try:
    from  urllib.request import urlopen 
except ImportError:
    from urllib2 import urlopen
from smb import *        
from smb.SMBConnection import SMBConnection
import socket
from smb.SMBHandler import SMBHandler
try:
    from urllib.request import  build_opener 
except Exception as e:
    from urllib2 import build_opener 
from smb import smb_structs
import logging
def getconn(tcp=True,port=445):       
    server_name="int.corp.sun"
    conn=SMBConnection('u391812','Xumin&06',socket.gethostname(),server_name,use_ntlm_v2 = True,is_direct_tcp=tcp,domain='int')
    assert conn.connect(socket.gethostbyname(server_name),port) 
    smb_logger = logging.getLogger('SMB.SMBConnection')
    smb_logger.setLevel(logging.WARNING)    
    return conn     

def smbget(tcp=True,port=445):        
#     file_list=conn.listPath('GroupData/ITProSup/Sharepoint/EMSCHEDULES')
#     for item in file_list:
#         print (item)
#     filename=file_url.split('/')[-1]
#     filename=filename.split('.')[0]+'.xml'co
    conn=getconn()
    services=conn.listShares(timeout=30)
    filename='good.xml'
    file_obj = open(filename,'w+')
    try:
        file_attributes, filesize = conn.retrieveFile('GroupData', '/ITProSup/Sharepoint/EMSCHEDULES/ALL_PROD_EM_SCHEDULES_160905.XLS', file_obj,timeout=60)
    except Exception as e:
        raise e
    file_obj.close()
    
def urlget():        
    director = build_opener(SMBHandler)
    fh = director.open('smb://u391812:Xumin&06@int.corp.sun/GroupData/ITProSup/Sharepoint/EMSCHEDULES/ALL_PROD_EM_SCHEDULES_160905.XLS')
    fh.close()   
    # Process fh like a file-like object and then close it.
              
def smbwalk(conn, shareddevice, top = u'/'):
    dirs , nondirs = [], []
    
    if not isinstance(conn, SMBConnection):
        raise TypeError("SMBConnection required")

    names = conn.listPath(shareddevice, top)

    for name in names:
        if name.isDirectory:
            if name.filename not in [u'.', u'..']:
                dirs.append(name.filename)
        else:
            nondirs.append(name.filename)

    yield top, dirs, nondirs

    for name in dirs:
        new_path = os.path.join(top, name)
        for x in smbwalk(conn, shareddevice, new_path):
            yield x

def smbclienttest():
    import smbclient
    smb = smbclient.SambaClient(server="int.corp.sun", share="GroupData",username='u391812', password='Xumin&06', domain='int')
                                
    print (smb.listdir("/"))
#     f = smb.open('/file1.txt')
#     data = f.read()
#     f.close()
#     smb.rename(u'/file1.txt', u'/file1.old')

if __name__ == "__main__":
    
    conn=getconn()
    ans = smbwalk(conn, 'GroupData',top= '/')
    for item in ans:
        print(item)