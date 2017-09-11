'''
Created on 12 Aug 2016

@author: u391812 no use any more ,plz refer commandClient
'''
import paramiko  
from test.test_pep277 import filenames
class SSHClient:
    def __init__(self,server='Nzarp2',port=22,user='u391812',pwd='Xumin&06'):
        self.server=server
        self.port=port
        self.user=user
        self.pwd=pwd
        
    def __enter__(self):
        self.client=paramiko.SSHClient()  
        self.client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        self.client.connect(self.server, self.port, self.user, self.pwd)
        return self.client
    def __exit__(self,exc_type,exc_val,exc_tb):
        self.client.close()

class SFTPSession:
    def __init__(self,client):
        self.client=client        
    def __enter__(self):
        self.session=self.client.open_sftp()
        return self.session
    def __exit__(self,exc_type,exc_val,exc_tb):
        self.session.close()   
                
class SSHFile:
    def __init__(self,session,filename,mode='rb'):
        self.session=session
        self.filename=filename
        self.mode=mode
    def __enter__(self):
        try:
            self.file=self.session.file(self.filename,self.mode)
        except Exception as e:
            self.file=str(e)
        return self.file
    def __exit__(self,exc_type,exc_val,exc_tb):
        if type(self.file)!=str:
            self.file.close()
        