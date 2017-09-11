import ibm_db,sys,os
print(os.getenv("INCLUDE"))
print(os.getenv("LIB"))
os.environ["INCLUDE"]="C:\Program Files (x86)\Quest Software\Toad for DB2 5.0\SQLLIB\include"
os.environ["LIB"]="C:\Program Files (x86)\Quest Software\Toad for DB2 5.0\SQLLIB"

try:
    sys.path.append("C:\Program Files (x86)\Quest Software\Toad for DB2 5.0\SQLLIB\BIN")
    dsn = """ DATABASE=SOREWD02;HOSTNAME=DB2AT5;PORT=60020;PROTOCOL=TCPIP;UID=u391812;PWD=Xumin$06;"""
        
    connection = ibm_db.connect("DATABASE=SOREWD02;HOSTNAME=DB2AT5;PORT=60020;PROTOCOL=TCPIP;UID=u391812;PWD=Xumin$06;","","")
except Exception as e:
    pass
