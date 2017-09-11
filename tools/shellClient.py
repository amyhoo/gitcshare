'''
Created on 17 Aug 2016

@author: u391812 no use any more ,plz refer commandClient
'''
import subprocess
class ShellClient:        
    def __enter__(self):        
        return self
    def __exit__(self,exc_type,exc_val,exc_tb):
        self.p.terminate()
    def exec_command(self,command):
        self.p= subprocess.Popen(command, shell=True,stdin=subprocess.PIPE,stdout=subprocess.PIPE, stderr=subprocess.STDOUT)   
        stdout,stderr=self.p.communicate(' baby', 10)   
        return (None,stdout,stderr)         
        return (self.p.stdin,self.p.stdout,self.p.stderr)
if __name__ == "__main__":
    with ShellClient() as client:
        stdin,stdout,stderr=client.exec_command('dir')        
        print (stdin,stdout,stderr)