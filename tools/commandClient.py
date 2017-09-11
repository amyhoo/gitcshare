'''
Created on 18 Aug 2016

@author: u391812
'''
try:
    import paramiko
except :
    pass
import subprocess
class CommandClient:
    
    def __init__(self,server=None,port=0,user=None,pwd=None):
        '''
        if server==None, then it is local machine
        '''
        self.server=server
        self.port=port
        self.user=user
        self.pwd=pwd
        
    def __enter__(self):
        if self.server and self.port and self.user and self.pwd:
            self.client=paramiko.SSHClient()  
            self.client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
            self.client.connect(self.server, self.port, self.user, self.pwd)
            self.session=self.client.open_sftp()
        return self
    
    def __exit__(self,exc_type,exc_val,exc_tb):
        if self.server:
            self.session.close()
            self.client.close()
                
    def exec_command(self,*args,**kwargs):
        if self.server:
            stdin, stdout, stderr = self.client.exec_command(*args,**kwargs)
            stdout=stdout.read() if stdout else ''
            stderr=stderr.read() if stderr else ''
        else:
            pipe=subprocess.Popen(*args,shell=True,stdin=subprocess.PIPE,stdout=subprocess.PIPE, stderr=subprocess.STDOUT,**kwargs)                
            stdout, stderr = pipe.communicate()
        return (stdout,stderr)
    
    def __getattr__(self,attr):
        if self.client:
            if attr=='file':
                return getattr(self.session,attr)
            return getattr(self.client, attr)
        else:
            if attr=='file':
                return open
            
class  OpenFile:
    
    def __init__(self,filename,mode='r',client=None):
        self.client=client       
        self.filename=filename
        self.mode=mode   
              
    def __enter__(self):
        if self.client:            
            try:                
                self.file=self.client.file(self.filename,self.mode)
            except Exception as e:
                self.file=str(e)
        else:#local
            try:
                self.file=open(self.filename,self.mode)
            except Exception as e:
                self.file=str(e)
        return self.file
    
    def __exit__(self,exc_type,exc_val,exc_tb):
        if type(self.file)!=str:
            self.file.close()