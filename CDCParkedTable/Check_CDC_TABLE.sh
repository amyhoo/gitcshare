#!/bin/sh
PATH=$PATH:/apps/db2/bin

db2 "connect to SOREWP02 user u391812 using Xumin&09"
if [[ $? -ne 0 ]]; then
	echo "db2 connect failed,please input right user name and password"
	exit 1
fi   
sql="(
SELECT   subscription, source_table, target_table, method, STATUS
FROM     SOR_CDC.CDC_TABLE
WHERE    METHOD = 'Mirror' AND STATUS = 'Parked' AND load_id = (SELECT max(load_id)
                                          FROM   SOR_CDC.CDC_TABLE
                                          WHERE  DATA_DATE < CURRENT TIMESTAMP - 1 DAYS)
GROUP BY subscription, source_table, target_table, method, STATUS
)
MINUS

(
SELECT   subscription, source_table, target_table, method, STATUS
FROM     SOR_CDC.CDC_TABLE
WHERE    METHOD = 'Mirror' AND STATUS = 'Parked' AND load_id = (SELECT max(load_id)
                                          FROM   SOR_CDC.CDC_TABLE
                                          WHERE  DATA_DATE < CURRENT TIMESTAMP - 2 DAYS)
GROUP BY subscription, source_table, target_table, method, STATUS
)
ORDER BY SUBSCRIPTION,SOURCE_TABLE,TARGET_TABLE"

html_body="<html><head><style type=\"text/css\">td{border-width:1px;border-left-style:none;border-right-style:solid;border-top-style:none;border-bottom-style:solid}</style></head><body>"
record_str=`db2 ${sql}` 

if [[ $? -ne 0 ]]; then
    if [[ -z "$record_str" ]]; then
        num=0        
        title="Notification: There is no new added parked table last night"
        html_body="$html_body<h1 style='text-align:center'><span style='mso-fareast-font-family:\"Times New Roman\"'>$title</span></h1><p style='text-align:center'>send at `date +%Y-%m-%d`</p>"                
        html_body="$html_body</body></html>"
        
    else
        echo "Error:db2 query failed"  
        title="Error:db2 query failed last night"
        html_body="$html_body<h1 style='text-align:center'><span style='mso-fareast-font-family:\"Times New Roman\"'>$title</span></h1><p style='text-align:center'>send at `date +%Y-%m-%d`</p>"
        html_body="$html_body</body></html>"                                   
    fi
else 
    num=`echo "${record_str}" | wc -l`
    title="Warning: There are  $num new added parked tables last night, please help check!"
    html_body="$html_body<h1 style='text-align:center'><span style='mso-fareast-font-family:\"Times New Roman\"'>$title</span></h1><p style='text-align:center'>send at `date +%Y-%m-%d`</p>"
    temp=`printf "$record_str" | awk '{print "<tr>\n<td>"$1"  </td><td>" $2"  </td><td>"$3"  </td><td>"$4"  </td><td>"$5"  </td>\n</tr>"}'`
    #`printf "${record_str}" | sed -e 's/[ ]\+/|/g'`
    html_body="$html_body<table border='1' width='100%' cellspacing='0' cellpadding='0' style='text-align:center'>$temp</table>"
    html_body="$html_body </body></html>"    
fi  
echo $html_body >email_content.txt
./send_email.sh "do_not_reply@bidrd1dbfit.int.corp.sun" "$1" "$title" email_content.txt 
