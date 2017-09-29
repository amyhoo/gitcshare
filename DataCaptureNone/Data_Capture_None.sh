#!/bin/bash
PATH=$PATH:/apps/db2/bin
uid=$1
password=$2
email_list=$3   

title="changes of data capture tables" 

function sendEmail(){    
    printf "$html_body">email_data_capture.html
    ./send_email.sh "CDTOServiceManagementOffice@suncorp.com.au" "$email_list" "$title" email_data_capture.html 
}

function getHtml(){
    local temp_title=$1
    local temp_content=$2    
    local temp_html="<html><head><style type=\"text/css\">body{font-family:Calibri;font-size:14px} table .td_oneline{text-overflow:ellipsis;word-break:keep-all; white-space:nowrap;}</style></head>\n<body>"
    temp_html+="<h1 style='text-align:center'>$temp_title</h1>\n<p style='text-align:center'>send at `date +%Y-%m-%d`</p>\n"
    temp_html+="<p>$temp_content</p></body></html>"    
    echo $temp_html
}

function getTable(){
    local tempRecords=("${!1}")         
    local table_head="\n<tr bgcolor='#d5d5d5' style='text-align:center'>\n<td><p><b>SUBSCRIPTION</b></p></td>\n<td><p><b>SOURCE_TABLE</b></p></td>\n<td><p><b>TARGET_TABLE</b></p></td>\n<td><p><b>METHOD</b></p></td>\n<td><p><b>STATUS</b></p></td>\n</tr>\n"
    if [ ${#tempRecords[@]} -eq 0 ]; then         
         local temp_table="<table border='1' width='100%%' cellspacing='0' cellpadding='2' style='text-align:center;border-collapse:collapse;'>$table_head</table>\n"
    else 
        local table_body=`printf '%s\n' "${tempRecords[@]}" | awk -F ',' '{print "<tr style='text-align:left'>\n<td class='td_oneline'><p>"$1"  </p></td>\n<td class='td_oneline'><p>" $2"  </p></td>\n<td class='td_oneline'><p>"$3"  </p></td>\n<td class='td_oneline'><p>"$4"  </p></td>\n<td class='td_oneline'><p>"$5"  </p></td>\n</tr>"}'`
        local temp_table="<table border='1' width='100%%' cellspacing='0' cellpadding='2' style='text-align:center;border-collapse:collapse;'>$table_head$table_body</table>\n"
    fi  
    echo $temp_table               
}

html_body=`getHtml "$title"`
trap "sendEmail" EXIT

sql="select trim(TABSCHEMA)||'.'||trim(TABNAME) as tableName from SYSCAT.TABLES where DATACAPTURE='N' and (CREATE_TIME > CURRENT date -1 day or ALTER_TIME > CURRENT date -1 day)"
#
db2 "connect to SOREWP01 user $uid using $password"
return_code=$?
if [[ $return_code -ne 0 ]]; then
    echo "db2 connect failed in connecting SOREWP01,please input right user name and password"
    title="Error:changes of data capture tables"
    html_body=`getHtml "$title" "db2 connect failed in connecting SOREWP01,please input right user name and password"`
    exit $return_code    
fi
record_list1=(`db2 -x ${sql}`)
status1=$?

db2 "connect to SORHSTP1 user $uid using $password"
return_code=$?
if [[ $return_code -ne 0 ]]; then
    echo "db2 connect failed,please input right user name and password"      
    title="Error:changes of data capture tables"
    html_body=`getHtml "$title" "db2 connect failed in connecting SORHSTP1,please input right user name and password"`
    exit $return_code  
fi
record_list2=(`db2 -x ${sql}`)
status2=$?

db2 "connect to SOREWP02 user $uid using $password"
return_code=$?
if [[ $return_code -ne 0 ]]; then
    echo "db2 connect failed,please input right user name and password"     
    title="Error:changes of data capture tables"
    html_body=`getHtml "$title" "db2 connect failed in connecting SOREWP02,please input right user name and password"`
    exit $return_code     
fi
record_list3=(`db2 -x ${sql}`)
status3=$?      
      
if [[ $status1 > 1 && $status2 > 1 && $status3 > 1 ]];then
    echo "Error:db2 query failed:SOREWP01 $status1; SORHSTP1 $status2;SOREWP02 $status3"  
    title="Error:changes of data capture tables"    
    html_body=`getHtml "$title" "db2 query failed:SOREWP01 $status1; SORHSTP1 $status2;SOREWP02 $status3"`
    exit 1      
fi

tables_str=`printf ",('%s')" "${record_list1[@]}${record_list2[@]}${record_list3[@]}"`


create_temp_table=$"DECLARE GLOBAL TEMPORARY TABLE SESSION.TEMP_TABLE (TEMPNAME CHAR(200) NOT NULL) WITH REPLACE ON COMMIT PRESERVE ROWS"
insert_temp_table=$"insert into SESSION.TEMP_TABLE (TEMPNAME) values ${tables_str:1};"

sql_tables="select subscription||','||source_table||','||target_table||','||method||','||STATUS from sor_cdc.cdc_table as A
    join SESSION.TEMP_TABLE as B on A.source_table=B.TEMPNAME
    where method = 'Mirror' 
    and source_table like 'SOR%' 
    and LOAD_ID = (select max(LOAD_ID) from sor_cdc.cdc_table)"
db2 -x  $create_temp_table
echo "$insert_temp_table">sql_tables.sql   
db2 -xtf sql_tables.sql
record_list=(`db2 -x $sql_tables`)
status=$?

if [[ $status = 0 ]];then
    num=`echo "${#record_list[@]}"`
    title="Warning: There are $num new changed data capture tables, please help check!"
    content=`getTable record_list[@]`        
    html_body=`getHtml "$title" "$content"`
elif [[ $status = 1 ]]; then
    title="Notification: There are no new changed data capture tables"
    content=`getTable record_list[@]`
    html_body=`getHtml "$title" "$content"`        
else
    title="Error: db2 query SOR_CDC on SOREWP02 failed"
    html_body=`getHtml "$title" "failcode $status;  $sql_tables"`
    exit $status
fi

sendEmail