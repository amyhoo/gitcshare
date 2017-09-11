'''
Created on 10 Aug 2016

@author: u391812
'''
RE_DEFINE={
           'command':r'^[a-z]+$',#linux,windows inner command
           'filename':r'[-\.\w_]+',#recommend  file name in linux windows,without space * ? 
           'file-path':'[-\.\w_]*(/+[-\.\w_]+)*', #path name with file,the separator is /, // ,/// and so on
           'net-file-path':'\\\\[-\.\w_]*(\\[-\.\w_]+)*', #net location path with file ,the separator is /  
           'path':'[-\.\w_]*(/+[-\.\w_]+)*/', #file path
           'file-path-sh':'[-\.\w_]*(/+[-\.\w_]+)*\.sh',#sh file with path
           'file-path-bsh':'[-\.\w_]*(/+[-\.\w_]+)*\.bsh',#bsh file with path
           'file-path-cmd':'[-\.\w_]*(/+[-\.\w_]+)*\.cmd',#cmd file with path
           'file-path-bat':'[-\.\w_]*(/+[-\.\w_]+)*\.bat',#bat file with path
           'file-path-no-suffix':'^[-\.\w_]*(/+[-\.\w_]+)*/[-\w_]+$', #path and file without extension 
           'file-path-suffix':'^[-\.\w_]*(/+[-\.\w_]+)*/[-\w_]+\.[-\w_]+$', #path and file with extension
           'assign_line':'=[^=#\n]+',#variable assignment line 
           'var':'\$\{[^ \n"\'\\/=`#|(.]+\}|\$[^ \n"\'\\/=`#|(.]+',#variable
           'log_var':'\$\{log[^ \n"\'\\/=`#|(.]+\}|\$log[^ \n"\'\\/=`#|(.]+',#variable about log 
           }

if __name__ == '__main__':
    pass