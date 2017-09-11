#!/bin/bash
###########################################################################################################################################
# Purpose
# -------
# This script acts as cdc building block, is to be called by other schedule tools to syncronise data from db2 database to Netezza databse.
#
# Full Description
# ----------------
# - Takes group name as parameter
# - Log into file in log directory including datetime that the script started, and the group name.
# - Retrieve DB credentials from  configration file and connect to DB2
# - Retrieve records from the subscription config table in DB2 for the passed in group name.
# - Loop through each subscription record returned:
#     - Call createSubDirectories function passing the sub name.  Exit with error if this fails.
#     - Call mergeSubscription function passing the sub name, source DB name, source DB schema.  Continue to next iteration of the loop if this fails (ie don�t run apply)
#     - Call apply function, passing the sub name.  Continue to next iteration of the loop if this fails.
# - return 0 if no errors, else 1
#
# Parameters
# ----------
# $1 Mandatory -g {group name}  which can be retrieved from subscription config table.
# $2 Optionally -s {subname} which is subscription name, to restrict to a single subscription.
# $3 Optionally -m merge only flag, which indicates to run merge only(default is to run both merge and apply).
# $4 Optionally -a apply only flag, which indicates to run apply only(default is to run both merge and apply).
#
###########################################################################################################################################
# Change History
# Date        Author        Description
# --------------------------------------------------------------------
# 2016-08-03  Joy Zhang     Initial version.
# 2017-02-09  Amy Xu        Fix
#
######################################################################
function initGlobalVar(){
    #this program init Global variable or environment variable error func_code 1
    local func_code=0
    
    #check environment variable in account profile
    if [[ ${SCRIPTS_HOME}  = "" ]] ; then
        func_code=1
        echo "Environment variable, SCRIPTS_HOME, was not set, please check and set it in user's profile"      
    fi    
    
    if [[ ${DATA_USER_HOME}  = "" ]] ; then
        func_code=1
        echo "Environment variable, DATA_USER_HOME, was not set, please check and set it in user's profile"      
    fi    
        
        
    if [[ ${SQL_USER_HOME}  = "" ]] ; then
        func_code=1
        echo "Environment variable, SQL_USER_HOME, was not set, please check and set it in user's profile"      
    fi
    
    if [[ ${LOG_HOME}  = "" ]] ; then
        func_code=1
        echo "Environment variable, LOG_HOME, was not set, please check and set it in user's profile"      
    fi
    
    if [[ ${ADMIN_HOME}  = "" ]] ; then
        func_code=1
        echo "Environment variable, ADMIN_HOME, was not set, please check and set it in user's profile"     
    fi
    
    if [[ ${NZ_PWD_FILE}  = "" ]] ; then
        func_code=1
        echo "Environment variable, NZ_PWD_FILE, was not set, please check and set it in user's profile"      
    fi
    
    if [[ ${DB2_PWD_FILE}  = "" ]] ; then
        func_code=1
        echo "Environment variable, DB2_PWD_FILE, was not set, please check and set it in user's profile"      
    fi    
    
    
    # intialize some local variables    
    NOW='date +%Y"-"%m"-"%d" "%H":"%M":"%S'
    SCRIPT_START_TIME=$(date +%Y%m%d%H%M%S)
    SCRIPT_FULL_NAME=$(basename ${0})
    SCRIPT_NAME=${script_full_name%.*}       
    
    # get the directory of this script
    script_source="${BASH_SOURCE[0]}"
    while [ -h "${script_source}" ]; do # resolve ${script_source} until the file is no longer a symlink
      {script_dir}="$( cd -P "$( dirname "${script_source}" )" && pwd )"
      script_source="$(readlink "${script_source}")"
      [[ ${script_source} != /* ]] && ${script_source}="${script_dir}/${script_source}" # if $script_source was a relative symlink, we need to resolve it relative to the path where the symlink file was located
    done
    SCRIPT_DIR="$( cd -P "$( dirname "${script_source}" )" && pwd )"    
    BASE_DIR="$(dirname "${SCRIPT_DIR}")"
            
    #the shell script's parameters       
    while [[ $# -ge 1 ]]
    do
        key="${1}"
        
        case $key in
            -g|--group)
            GROUP_NAME="${2}"
            shift 2 # past argument
            ;;
            -s|--subscription)
            SUB_NAME="${2}"
            shift 2 # past argument
            ;;
            -l|--logdir)
            temp_log_dir="${2}"
            shift 2 # past argument
            ;;
            -m|--merge)
            MERGE_ONLY="Y"
            shift # past argument
            ;;
            -a|--apply)
            APPLY_ONLY="Y"
            shift # past argument
            ;;
            *) # unknown option
            ;;
        esac
    
    done    
    
    # assign log file's directory
    if [[ ${temp_log_dir}  != "" ]] ; then    
        LOG_DIR="${temp_log_dir}"     
    else    
        LOG_DIR=$LOG_HOME      
    fi    
    SUB_COUNT=0    
    return $func_code
}

function  checkPremise(){
    #check is all premise arrived ,error func_code=2
    local func_code=0
    
    # Check folders 
    if [ ! -d "${SCRIPTS_HOME}" ]; then
        func_code=2
        echo "The directory ${SCRIPTS_HOME}, mentioned by environment variable SCRIPTS_HOME, was not valid, please check and make sure it is valid!"      
    fi
    
    if [ ! -d "${DATA_USER_HOME}" ]; then
        func_code=2
        echo "The directory ${DATA_USER_HOME}, mentioned by environment variable DATA_USER_HOME, was not valid, please check and make sure it is valid!"      
    fi
    
    if [ ! -d "${SQL_USER_HOME}" ]; then
        func_code=2
        echo "The directory ${SQL_USER_HOME}, mentioned by environment variable SQL_USER_HOME, was not valid, please check and make sure it is valid!"      
    fi
    
    if [ ! -d "${LOG_HOME}" ]; then
        func_code=2
        echo "The directory ${LOG_HOME}, mentioned by environment variable LOG_HOME, was not valid, please check and make sure it is valid!"     
    fi
    
    if [ ! -d "${ADMIN_HOME}" ]; then
        func_code=2
        echo "The directory ${ADMIN_HOME}, mentioned by environment variable ADMIN_HOME, was not valid, please check and make sure it is valid!"      
    fi
    
    # check the password files 
    if [ ! -f "${ADMIN_HOME}/${DB2_PWD_FILE}" ]; then
        func_code=2
        echo "The db2 password file ${ADMIN_HOME}/${DB2_PWD_FILE} was not valid, please check and make sure it is valid!"      
    fi
    
    if [ ! -f "${ADMIN_HOME}/${NZ_PWD_FILE}" ]; then
        func_code=2
        echo "The db2 password file ${ADMIN_HOME}/${NZ_PWD_FILE} was not valid, please check and make sure it is valid!"      
    fi    
    
    #check parameters
    if [[ ${GROUP_NAME}  = "" ]] ; then
        func_code=2    
        logMsg "error" "${FUNCNAME[0]} : A required parameter, group name, was missing!"       
    fi
    
    if [[ ${MERGE_ONLY}  = "Y" &&  ${APPLY_ONLY}  = "Y" ]] ; then
        func_code=2
        logMsg "error" "${FUNCNAME[0]} : merge only and apply only can't be both set!"                
    fi    
    
    if [ ! -d "${LOG_DIR}" ]; then      
        func_code=2
        echo "The directory ${LOG_DIR} was not valid, please check and make sure it is valid!"      
    fi    

    # init DB2
    . ${ADMIN_HOME}/${DB2_PWD_FILE}
    if [[ ${DB_NAME}  = "" || ${DB_SCHEMA}  = "" || ${DB_USER}  = "" || ${DB_PWD}  = "" ]] ; then
        func_code=2
        echo "the db2 connection string or credential, were not set in the db2 password file, please check and set it properly!"      
    fi
    # init NZ
    . ${ADMIN_HOME}/${NZ_PWD_FILE}
    if [[ ${NZ_USER}  = "" || ${NZ_PASSWORD}  = "" ]] ; then
        func_code=2
        echo "the NZ connection string or credential, were not set in the NZ password file, please check and set it properly!"     
    fi    
    return $func_code
}

function createSubDirectories(){
    # create sub directory for each subscription, if the directories do not exist ,error func_code 3    
      
    mkdir -p -m 770 "${DATA_USER_HOME}/cdc_output/${1}"
    mkdir -p -m 770 "${DATA_USER_HOME}/cdc_hardened/${1}"
    mkdir -p -m 770 "${DATA_USER_HOME}/working/${1}"
    mkdir -p -m 770 "${DATA_USER_HOME}/nz_input/${1}"
    mkdir -p -m 770 "${DATA_USER_HOME}/archive/cdc_output/${1}"
    mkdir -p -m 770 "${DATA_USER_HOME}/archive/nz_input_superseded/${1}"  
    mkdir -p -m 770 "${DATA_USER_HOME}/archive/nz_input/${1}"
    mkdir -p -m 770 "${SQL_USER_HOME}/${1}"  
    mkdir -p -m 775 "${LOG_DIR}/${1}"  
}

function dataFilesFolder(){
    # $1 is folder contains nz_input data files $2 all table name list $3 insert file list $4 delete file list      
    local __table_name
    local __file_name    
    local __all_table_list=() #tableName
    local __insert_file_list=() #fileName tableName date time
    local __delete_file_list=() #fileName tableName date time
    local idx1=0 
    local idx2=0
    local idx3=0    
     
    for __file_path in $1/*.*
    do        
        __file_name=$(basename $__file_path)   
        if [[ $__file_name =~ ([a-zA-Z_\d]{1,128})\.LOAD_READY\.I\.D([0-9]{7})\.T([0-9]{9})  ]]; then
            __table_name=${__file_name%%.*}            
            __all_table_list[$idx1]=$__table_name
            idx1=$idx1+1
            __insert_file_list[$idx2]="$__file_name $__table_name ${BASH_REMATCH[2]} ${BASH_REMATCH[3]}"            
            idx2=$idx2+1            
        elif [[ $__file_name =~ ([a-zA-Z_\d]{1,128})\.LOAD_READY\.D\.D([0-9]{7})\.T([0-9]{9})  ]]; then
            __table_name=${__file_name%%.*}
            __all_table_list[$idx1]=$__table_name
            idx1=$idx1+1
            __delete_file_list[$idx3]="$__file_name $__table_name ${BASH_REMATCH[2]} ${BASH_REMATCH[3]}"
            idx3=$idx3+1                    
        fi
    done
    IFS=$'\n'
    local returnVar=$(printf "%s\n" "${__all_table_list[@]}" | sort -ur)
    unset IFS
    eval $2='("${returnVar[@]}")'    
    eval $3='("${__insert_file_list[@]}")'
    eval $4='("${__delete_file_list[@]}")'    
        
    return $func_state                
}

function cdcFilesFolder(){
    # $1 is folder contains cdc output files $2 all table name list $3 finish file list $4 unfinished file list $5 max day timestamp   
    local __all_table_list=()  #format: tableName  
    local __finish_file_list=() #format: fileName tableName date time recordCount
    local __unfinish_file_list=() #format: fileName tableName date time recordCount
    local __time_sort_key
    local __table_name
    local __file_name
    local idx1=0 
    local idx2=0
    local idx3=0
        
    for __file_path in $1/*.*
    do          
        __file_name=$(basename $__file_path)               
        if [[ $__file_name =~ ([a-zA-Z_\d]{1,128})\.D([0-9]{7})\.T([0-9]{9})\.R([0-9]*)  ]]; then
            __table_name=${__file_name%%.*}            
            __all_table_list[$idx1]=$__table_name
            idx1=$idx1+1
            __finish_file_list[$idx2]="$__file_name $__table_name ${BASH_REMATCH[2]} ${BASH_REMATCH[3]} ${BASH_REMATCH[4]}"
            __time_sort_key[$idx2]="${BASH_REMATCH[2]} ${BASH_REMATCH[3]}"
            idx2=$idx2+1
        elif [[ $__file_name =~ ([a-zA-Z_\d]{1,128})\.@([0-9]{7})\.T([0-9]{9})\.R([0-9]*)  ]]; then
            __table_name=${__file_name%%.*}            
            __all_table_list[$idx1]=$__table_name            
            idx1=$idx1+1   
            __unfinish_file_list[$idx3]="$__file_name $__table_name ${BASH_REMATCH[2]} ${BASH_REMATCH[3]} ${BASH_REMATCH[4]}"
            idx3=$idx3+1
        elif [[ $__file_name =~ ([a-zA-Z_\d]{1,128})\.STOPPED  ]]; then
            __table_name=${__file_name%%.*}
            __all_table_list[$idx1]=$__table_name
            idx1=$idx1+1           
        fi                                                                   
    done    
    IFS=$'\n'
    local returnVar=$(printf "%s\n" "${__all_table_list[@]}" | sort -ur)
    unset IFS    
    eval $2='("${returnVar[@]}")'         
    eval $3='("${__finish_file_list[@]}")'    
    eval $4='("${__unfinish_file_list[@]}")'
    IFS=$'\n' 
    local sorted=($(sort -r<<<"${__time_sort_key[*]}"))      
    unset IFS    
    returnVar=${sorted[0]}    
    eval "$5='$returnVar'"
}
  
function db2Init(){    
    # init db2 config database,error func_code 4
    local func_code=0
    db2 "connect to ${DB_NAME} user ${DB_USER} using '${DB_PWD}'"     
     
    if [[ $? -ne 0 ]]; then
        logMsg "error" "${FUNCNAME[0]} : ERROR: failed to connect to DB2 database ${DB_NAME} as user ${DB_USER}"          
        func_code=4
    fi            
    db2 "set schema ${DB_SCHEMA}"
    logMsg "info" "${FUNCNAME[0]}: Connected to DB2 database ${DB_NAME} as user ${DB_USER},set schema as ${DB_SCHEMA}"
        
    return $func_code
}

function db2GetSubList(){
    # $1 the return value is an array    
    local sub_list_sql="SELECT SUB_NAME || ' ' || SRC_DB_NAME || ' ' || SRC_SCHEMA_NAME FROM CDC_FOR_DS_SUB WHERE GROUP_NAME = '${GROUP_NAME}'"
    if [[ ${SUB_NAME} != "" ]]; then
      sub_list_sql="${sub_list_sql} AND SUB_NAME='${SUB_NAME}'"
    fi    
    
    local returnVar=`db2 -x ${sub_list_sql}`    
    funCode=$?   
    eval $1='("${returnVar[@]}")'                
    return $funCode
}

function db2GetSubFilesMap(){
    # $1 subName $2 data file list $3 start time $4 end time $5 return data_file map 
    local subName=$1
    local data_file_list=("${!2}")    
    local source_target_list=()
    local dataFileStr=`printf ",'%s'" "${data_file_list[@]}"`
    local sql="SELECT TARGET_INS_FILE_NAME || ' ' || SRC_FILE_NAME FROM CDC_FOR_DS_MERGE_LOG  AS T1
            WHERE T1.SUB_NAME = '${subName}' and TARGET_INS_FILE_NAME!= ''
            AND T1.TARGET_INS_FILE_NAME in (${dataFileStr:1})
            union
            SELECT TARGET_DEL_FILE_NAME || ' ' || SRC_FILE_NAME FROM CDC_FOR_DS_MERGE_LOG  AS T1
            WHERE T1.SUB_NAME = '${subName}' AND TARGET_DEL_FILE_NAME != ''
            AND T1.TARGET_DEL_FILE_NAME in (${dataFileStr:1})"                      
    IFS=$'\n'   
    data_file_map_list=(`db2 -x ${sql}`)
    unset IFS
    funCode=$?     
    
    for data_file in "${data_file_list[@]}"
    do        
        sourceFilesList=(`printf '%s\n' "${data_file_map_list[@]}" | awk -v pattern="^$data_file" '$0 ~ pattern {print $2}'|sort`)       
        sourceFilesStr=`printf ",%s" "${sourceFilesList[@]}"`
        start_time=`echo ${sourceFilesList[0]}|awk -F[.] '{print substr($2,2)","substr($3,2)}'`                 
        end_time=`echo ${sourceFilesList[-1]}|awk -F[.] '{print substr($2,2)","substr($3,2)}'`
        source_target_list+=("$data_file $start_time $end_time ${sourceFilesStr:1}")
    done     
    min_time=`printf '%s\n' "${source_target_list[@]}" | sort -k 2,2 | head -n1 | awk '{print $2}'`
    max_time=`printf '%s\n' "${source_target_list[@]}" | sort -rk 3,3 | head -n1 | awk '{print $3}'`    
    eval $3=$min_time 
    eval $4=$max_time
    eval $5='("${source_target_list[@]}")' 
    return $funCode   
} 

function db2CheckTargetUpdate(){
    # check if this data_file_list ,the last successful datetime in this target db must be 
    # $1 subName $2 start time $3 end time
    local subName=$1
    local start_time=`echo $2|awk -F[,] '{print "D"$1".T"$2}'`
    local end_time=`echo $3|awk -F[,] '{print "D"$1".T"$2}'`    
    local sql=""
    sql="SELECT T.TARGET_SERVER || ':' || T.TARGET_DB_NAME
                 FROM CDC_FOR_DS_SUB_TARGET T
                WHERE SUB_NAME = '${subName}' 
                  AND EXISTS (
                      SELECT  SUB_NAME
                      FROM CDC_FOR_DS_APPLY_LOG L
                      WHERE L.SUB_NAME = T.SUB_NAME   
                        AND L.TARGET_SERVER = T.TARGET_SERVER AND L.TARGET_DB_NAME = T.TARGET_DB_NAME                    
                        AND L.STATUS = 'COMPLETE'                         
                        AND RIGHT(L.SRC_FILE_NAME,19) > '${end_time}'                        
                      )"
    return_list=(`db2 -x $sql`)
    if [[ ${#return_list[@]} != 0 ]];then
        logSub "warn" $subName "data files of $end_time to target ${#return_list[@]}  updated before"           
    fi 
    
    sql="SELECT T.TARGET_SERVER || ':' || T.TARGET_DB_NAME
                 FROM CDC_FOR_DS_SUB_TARGET T
                WHERE SUB_NAME = '${subName}' 
                  AND EXISTS (
                      SELECT  SUB_NAME
                      FROM CDC_FOR_DS_APPLY_LOG L
                      WHERE L.SUB_NAME = T.SUB_NAME   
                        AND L.TARGET_SERVER = T.TARGET_SERVER AND L.TARGET_DB_NAME = T.TARGET_DB_NAME                    
                        AND L.STATUS = 'COMPLETE'                         
                        AND RIGHT(L.SRC_FILE_NAME,19) > '${start_time}'
                        AND RIGHT(L.SRC_FILE_NAME,19) < '${end_time}'
                      )"
    return_list=(`db2 -x $sql`)
    if [[ ${#return_list[@]} != 0 ]]; then
        logSub "warn" $subName "data files of $end_time to target ${#return_list[@]} overlap with before update"        
    fi 
    
    sql="SELECT T.TARGET_SERVER || ':' || T.TARGET_DB_NAME
                 FROM CDC_FOR_DS_SUB_TARGET T
                WHERE SUB_NAME = '${subName}' 
                  AND EXISTS (
                      SELECT  SUB_NAME
                      FROM CDC_FOR_DS_APPLY_LOG L
                      WHERE L.SUB_NAME = T.SUB_NAME                        
                        AND L.TARGET_SERVER = T.TARGET_SERVER AND L.TARGET_DB_NAME = T.TARGET_DB_NAME
                        AND L.STATUS = 'FAILED'                        
                        AND RIGHT(L.SRC_FILE_NAME,19) < '${end_time}'
                      )"
                     
    return_list=(`db2 -x $sql`)
    if [[ ${#return_list[@]} != 0 ]]; then 
        logSub "warn" $subName "data files of $end_time to target ${#return_list[@]}  have failed before"        
    fi         
}

function db2GetTargetDb(){
    # $1 is sub ,$2  start_time,$3 end_time   $4 return target db of sub
    local subName=$1    
    local start_time=`echo $2|awk -F[,] '{print "D"$1".T"$2}'`
    local end_time=`echo $3|awk -F[,] '{print "D"$1".T"$2}'`
    
    #select all target db that not complete and not failed before    
    local __sql="SELECT T.TARGET_SERVER || ' ' || T.TARGET_DB_NAME || ' ' || T.TABLE_NAME_PREFIX
                 FROM CDC_FOR_DS_SUB_TARGET T
                WHERE SUB_NAME = '${subName}' 
                  AND NOT EXISTS (
                      SELECT  SUB_NAME
                      FROM CDC_FOR_DS_APPLY_LOG L
                      WHERE L.SUB_NAME = T.SUB_NAME   
                        AND L.TARGET_SERVER = T.TARGET_SERVER AND L.TARGET_DB_NAME = T.TARGET_DB_NAME                    
                        AND L.STATUS = 'COMPLETE' 
                        AND RIGHT(L.SRC_FILE_NAME,19) > '${start_time}'
                      )
                  AND NOT EXISTS (
                      SELECT  SUB_NAME
                      FROM CDC_FOR_DS_APPLY_LOG L
                      WHERE L.SUB_NAME = T.SUB_NAME                        
                        AND L.TARGET_SERVER = T.TARGET_SERVER AND L.TARGET_DB_NAME = T.TARGET_DB_NAME
                        AND L.STATUS = 'FAILED'                        
                        AND RIGHT(L.SRC_FILE_NAME,19) < '${end_time}'
                      )"    
    local returnVar=("`db2 -x $__sql`") 
    funCode=$?  
    eval $4='("${returnVar[@]}")'
    return $funCode    
}

function db2GetTargetTable(){
    # $1 subName $2 target db info, $3 file _list $4 return value
    #select this sub map table 
    local subName=$1   
    local targetServer=`echo $2|awk '{print $1}'`    
    local targetDb=`echo $2|awk '{print $2}'`
    local targetPre=`echo $2|awk '{print $3}'`
    declare -a __file_info_list=("${!3}")     
    local __file_target_info_list    
    __sql="select T2.SOURCE_TABLE|| ' ' ||T2.TARGET_SERVER ||' '|| T2.TARGET_DB || ' ' || T2.TARGET_TABLE 
            from CDC_FOR_DS_SUB T1 JOIN  CDC_FOR_DS_SOURCE_TARGET_MAPPING T2 on T1.SRC_DB_NAME=T2.SOURCE_DB and T1.SRC_SCHEMA_NAME=T2.SOURCE_SCHEMA
            where T1.SUB_NAME='$subName'
            and T2.TARGET_SERVER='${targetServer}'
            and T2.TARGET_DB='$targetDb'"   
    
    local __target_db_table_list=(`db2 -x $__sql`)    
    funCode=$?
    return $funCode
    
    #for each file get target server, target db, target table
    for idx in ${!__file_info_list[@]};
    do
        local __temp1=${__file_info_list[$idx]}
        local __sourceTable=`echo ${__temp1}|awk '{print $2}'`
        IFS=$'\n'        
        local __temp2=( `printf '%s\n' "${__target_db_table_list[@]}"|awk -v pattern="^$__sourceTable" '$0 ~ pattern {print $2" "$3" "$4}'` )        
        unset IFS        
        if [ "$__temp2" != "" ];then                       
            __file_target_info_list[$idx]="${__temp1} ${__temp2}"
        else            
            __file_target_info_list[$idx]="${__temp1} ${targetServer} ${targetDb} ${targetPre}${__sourceTable}"
        fi
    done    
    eval $4='("${__file_target_info_list[@]}")'
    return $funCode
}

function db2MergeLog(){
    local subName=$1
    for file_info in  "${finish_file_list[@]}"
    do  
        local record_number=$((`echo $file_info|awk '{print $5}'`))
        local __tableName=`echo $file_info|awk '{print $2}'`
        local source_file=`echo $file_info|awk '{print $1}'`
        local __insertFile=${filesInsertMap[$__tableName]}
        local __deleteFile=${filesDeleteMap[$__tableName]}        
        __success_sql="MERGE INTO CDC_FOR_DS_MERGE_LOG AT
                           USING ( SELECT '${subName}' AS SUB_NAME, '${source_file}' AS SRC_FILE_NAME, $record_number AS RECORD_COUNT, '${__deleteFile}' AS TARGET_DEL_FILE_NAME, '${__insertFile}' AS TARGET_INS_FILE_NAME, CURRENT_TIMESTAMP AS MERGE_TIMESTAMP FROM SYSIBM.SYSDUMMY1) AS BS
                        ON AT.SUB_NAME=BS.SUB_NAME AND AT.SRC_FILE_NAME=BS.SRC_FILE_NAME
                     WHEN MATCHED
                     THEN UPDATE SET RECORD_COUNT=BS.RECORD_COUNT, TARGET_DEL_FILE_NAME=BS.TARGET_DEL_FILE_NAME, TARGET_INS_FILE_NAME=BS.TARGET_INS_FILE_NAME, MERGE_TIMESTAMP=BS.MERGE_TIMESTAMP
                     WHEN NOT MATCHED
                     THEN INSERT VALUES ('${subName}', '${source_file}', $record_number, '${__deleteFile}', '${__insertFile}', CURRENT_TIMESTAMP )"             
        db2 ${__success_sql}
        exitSub $? $subName "${FUNCNAME[0]}:fail" "${FUNCNAME[0]}:success"
    done

}

function db2ClearApplyLog(){
    # $1 is subName  $2 is date and timestamp
    local subName=$1
    local dateTime=`echo $2|awk '{print "D"$1".T"$2}'`
    db2 -x "delete from ${DB_SCHEMA}.CDC_FOR_DS_APPLY_LOG where SUB_NAME = '${subName}'
          and status <> 'COMPLETE' and  SRC_FILE_NAME like '%${dateTime}'"  
    return $?       
}

function db2ApplyLog(){
    # $1 start /end  $2 COMPLETE/FAILED $3 subName $4 targetDbInfo $5 data_file_list
    local subName=$3
    local targetServer=`echo $4|awk '{print $1}'`
    local targetDb=`echo $4|awk '{print $2}'`
    declare -a data_file_list=("${!5}")      
    
    if [ "$1" = "start" ];then
        for data_file_name in ${data_file_list[@]}
        do
            sql="insert into ${DB_SCHEMA}.CDC_FOR_DS_APPLY_LOG values 
                ('${subName}','${data_file_name}','${targetServer}','${targetDb}',
                'STARTING',current_timestamp,current_timestamp)"                   
            db2 -x ${sql}                                        
        done
    elif [[ "$1" = "end" && "$2" = "COMPLETE" ]];then
        dataFileStr=`printf ",'%s'" "${data_file_list[@]}"`
        sql="update ${DB_SCHEMA}.CDC_FOR_DS_APPLY_LOG set status = 'COMPLETE'                 
                   where TARGET_SERVER = '${targetServer}' and
                         TARGET_DB_NAME = '${targetDb}' and
                         sub_name = '${subName}' and
                         SRC_FILE_NAME in (${dataFileStr:1})"
        db2 -x ${sql}                
    elif [[ "$1" = "end" && "$2" = "FAILED" ]];then
        dataFileStr=`printf ",'%s'" "${data_file_list[@]}"`        
        sql="update SOR_CDC.CDC_FOR_DS_APPLY_LOG set status = 'FAILED'
                  where TARGET_SERVER = '${targetServer}' and
                        TARGET_DB_NAME = '${targetDb}' and
                        sub_name = '${subName}' and
                        SRC_FILE_NAME in (${dataFileStr:1})"
        db2 -x ${sql}        
    fi    
    return $?    
}

function db2GetTablePK(){  
  local sSourceDBName="${1}"
  local sSourceSchemaName="${2}"
  local sDBUser="${3}"
  local sDBPassword="${4}"
  local sTableName="${5}"  
  local out_delim="${6}"
  
  local pk_list_sql=""
  local columns_list_sql=""
  
  declare -a pk_column_seq
  declare -a table_column_seq
  
  local pk_format=""
  local pk_string=""
  
  pk_column_count=0
  table_column_count=0
  
  if [[ ${out_delim} = "" ]]; 
  then
    out_delim=","
  fi
  
  pk_list_sql="SELECT B.COLSEQ+4
               FROM SYSCAT.TABCONST A , SYSCAT.KEYCOLUSE B
               WHERE A.TABNAME    = B.TABNAME
                 AND A.TABSCHEMA  = B.TABSCHEMA
                 AND A.CONSTNAME  = B.CONSTNAME
                 AND A.TABSCHEMA  = '${sSourceSchemaName}'
                 AND A.TABNAME    = '${sTableName}'
               ORDER BY B.COLSEQ
              "
  
  for col_seq in `db2 -x ${pk_list_sql}` ; do
    pk_column_seq[pk_column_count]=${col_seq}
    pk_column_count=$(($pk_column_count+1))
  done
    
  if [[ pk_column_count -eq 0 ]] ;then      
    columns_list_sql="SELECT COLNO+5 FROM SYSCAT.COLUMNS
                      WHERE TABSCHEMA = '${sSourceSchemaName}'
                        AND TABNAME = '${sTableName}'
                      ORDER BY COLNO
                     "
    
    for col_seq in `db2 -x ${columns_list_sql}` ; do
      table_column_seq[table_column_count]=${col_seq}
      table_column_count=$(($table_column_count+1))
    done
  fi
  
  if [[ pk_column_count -ne 0 ]] ;
  then
    for pk_seq in ${pk_column_seq[@]} ;
    do
      if [[ ${pk_string} = "" ]]; 
      then
        pk_format+="%s"
        pk_string="$pk_seq"
        sort_key_string="-k $pk_seq,$pk_seq"
      else
        pk_format+="%s%s"
        pk_string+="${out_delim}$pk_seq"
        sort_key_string+=" -k $pk_seq,$pk_seq"
      fi
    done
  elif [[ table_column_count -ne 0 ]] ;then
    for col_seq in ${table_column_seq[@]} ;
    do
      if [[ ${pk_string} = "" ]]; 
      then
        pk_format+="%s"
        pk_string="$col_seq"
        sort_key_string="-k $col_seq,$col_seq"
      else
        pk_format+="%s%s"
        pk_string+="${out_delim}$col_seq"
        sort_key_string+=" -k $col_seq,$col_seq"
      fi
    done  
  fi
    
  if [[ ${pk_string} != "" ]]; 
  then
    sort_key_string+=" -k 1,1 -k 3,3r"
  fi
  
  pk_cmd_string=$pk_string  
  return 0
}

function nzGetDbColumns(){
    # $1 targetDBinfo $2 targetTableList  $3 the return value
    local _dbServer=`echo $1|awk '{print $1}'`
    local _db=`echo $1|awk '{print $2}'`    
    declare -a targetTableList=("${!2}")      
    local tableListStr=`printf ",'%s'" "${targetTableList[@]}"`        
    local __sql="SELECT NAME || ' ' || ATTNAME AS TAB_COL_NAME
                FROM _V_RELATION_COLUMN
                WHERE DATABASE = '$_db'
                  AND NAME IN (${tableListStr:1})
                  AND ATTNAME NOT IN ('CDC_SOURCE_SYSTEM_CODE', 'CDC_RECORD_TYPE', 'CDC_COMMIT_TIMESTAMP', 'CDC_EXTRACT_TIMESTAMP')
                ORDER BY NAME, ATTNUM"  
    IFS=$'\n'       
    local returnVal=( `nzsql -A -t -c "${__sql}" -host ${_dbServer} -d ${_db} -u ${NZ_USER} -pw ${NZ_PASSWORD} ` ) 
    funCode=$?
    unset IFS
    eval $3='("${returnVal[@]}")'
    return $funCode    
}

function nzCreateTempTables(){
    # $1 targetDbInfo, $2 the sqls_file_info    
    local targetServer=`echo $1|awk '{print $1}'`
    local targetDb=`echo $1|awk '{print $2}'`
    local create_sql=`echo $2|awk '{print $1}'`      
    nzsql -host $targetServer -d $targetDb -u ${NZ_USER} -pw ${NZ_PASSWORD}  -f $create_sql
    return $?
}

function nzDropTempTables(){
    # $1 targetDbInfo, $2 the sqls_file_info  
    local targetServer=`echo $1|awk '{print $1}'`
    local targetDb=`echo $1|awk '{print $2}'`    
    local delete_sql=`echo $2|awk '{print $3}'`    
    nzsql -host $targetServer -d $targetDb -u ${NZ_USER} -pw ${NZ_PASSWORD} -f $delete_sql 
    return $?
}

function nzExecTempTables(){
    # $1 targetDbInfo, $2 the sqls_file_info  
    local targetServer=`echo $1|awk '{print $1}'`
    local targetDb=`echo $1|awk '{print $2}'`    
    local exec_sql=`echo $2|awk '{print $2}'`
    nzsql -host ${targetServer} -d ${targetDb} -u ${NZ_USER} -pw ${NZ_PASSWORD}  -f ${exec_sql}    
    return $?
}    

function logFolderStatus(){
    # $1 is sub name 
    local subName=$1
    folder_info=`ls -l ${DATA_USER_HOME}/cdc_output/$subName`
    logSub "info" $subName "$subName ${FUNCNAME[0]}: cdc_output:$folder_info"
    
    folder_info=`ls -l ${DATA_USER_HOME}/cdc_hardened/$subName`
    logSub "info" $subName "$subName ${FUNCNAME[0]}: cdc_hardened:$folder_info"
        
    local folder_info=`ls -l ${DATA_USER_HOME}/working/$subName`
    logSub "info" $subName "$subName ${FUNCNAME[0]}: working:$folder_info"

    folder_info=`ls -l ${DATA_USER_HOME}/nz_input/$subName`
    logSub "info" $subName "$subName ${FUNCNAME[0]}: nz_input:$folder_info"    
}

function logHistory(){
    local func_state=0
    local log_file="${LOG_DIR}/execution_history.log"
    local level=$1
    local msg=$2
    
    if [[ "$msg" == "" ]]; then
        return 1
    fi   
    case "${level}" in
    "debug")          
        echo "debug:$msg" | tee -a $log_file
    ;;
    "info")      
        echo "info:$msg" | tee -a $log_file
      ;;
    "warn")      
        echo "warn:$msg" | tee -a $log_file
      ;;      
    "error")
        echo "error:$msg" | tee -a $log_file
      ;;
    "exit")
        echo "exit:$msg" | tee -a $log_file
      ;;       
    esac    
    return $?                
}
    
function logMsg(){
    #error func_code 7
    # input msg into LOG_DIR    
    local level=$1
    local msg=$2
    local log_file="${LOG_DIR}/${SCRIPT_START_TIME}_$$.log"
    
    if [[ "$msg" == "" ]]; then
        return 1
    fi      
    case "${level}" in
    "debug")          
        echo "debug:$msg" | tee -a $log_file
    ;;
    "info")      
        echo "info:$msg" | tee -a $log_file
      ;;
    "warn")      
        echo "warn:$msg" | tee -a $log_file
      ;;      
    "error")
        echo "error:$msg" | tee -a $log_file
      ;;
    "exit")
        echo "exit:$msg" | tee -a $log_file
      ;;       
    esac
    return $?    
}    

function logSub(){    
    #input msg into LOG_DIR/sub  error func_code 8
    local func_state=0
    local level=$1
    local sub=$2
    local msg=$3    
    local log_file="${LOG_DIR}/${sub}/${SCRIPT_START_TIME}.log"
    
    if [[ "$msg" == "" ]]; then
        return 1
    fi
    case "${level}" in
    "debug")          
        echo "debug:$msg" | tee -a $log_file
    ;;
    "info")      
        echo "info:$msg" | tee -a $log_file
      ;;
    "warn")      
        echo "warn:$msg" | tee -a $log_file
      ;;      
    "error")
        echo "error:$msg" | tee -a $log_file
      ;;
    "exit")
        echo "exit:$msg" | tee -a $log_file
      ;;       
    esac    
    return $?
}

function exitIf(){
    # $1 is return status ,$2 is error_msg, $3 is correct_msg          
    if [[ $1 != 0 ]]; then        
        logMsg "exit" "$2 at `eval $NOW`"
        logHistory "exit" "$2 at `eval $NOW`"
        exit $1
    elif [[ ! -z $3 ]];then
        logMsg "info" "$3 at `eval $NOW`"                 
    fi    
}

function exitSub(){
    # $1 is the return status, $2 is sub name, $3 is error_msg, $4 is correct_msg           
    if [[ $1 != 0 ]]; then        
        logSub "exit" $2 "$3 at `eval $NOW`"
        logMsg "exit" "$3 at `eval $NOW`"
        logHistory "exit" "$3 at `eval $NOW`"
        exit $1
    elif [[ ! -z $4 ]]; then
        logSub "info" $2 "$4 at `eval $NOW`"
        logMsg "info" "$4 at `eval $NOW`"
    fi    
}

function mergeCheck(){
    #check if there exist error file in sub working folder error func_state 101
    local subName=$1         
    local funCode=0
    if [ -e ${DATA_USER_HOME}/working/${subName}/MERGE_ERROR.txt ];then 
         logSub "error" ${subName} "${subName} ${FUNCNAME[0]}: MERGE_ERROR.txt exist"
         funCode=101
    fi
              
    #check if cdc_output files are all hardened and match to the tables of subscription    
    if [ ${#unfinished_file_list[@]} -eq 0 ];then
        :
    else       
        logSub "warn" ${subName} "${subName} ${FUNCNAME[0]}: some table is still in process"
        funCode=0
    fi
    
    return $funCode       
}

function mergeMove2Harden(){
    #move files into harden folder error func_code 102,and get maxtime,table_list of hardened files        
    local subName=$1
    
    #clear working cdc_hardened, nz_input folders
    if [ -f ${DATA_USER_HOME}/working/$subName/*.* ]; then
        logSub "warn" $subName "$subName ${FUNCNAME[0]}: working folder have before files,shouldn't"
        rm -f ${DATA_USER_HOME}/working/$subName/*
    fi
    if [ -f ${DATA_USER_HOME}/cdc_hardened/$subName/*.* ];then
        logSub "warn" $subName "$subName ${FUNCNAME[0]}: cdc_hardened have before files,shouldn't"
        rm -f ${DATA_USER_HOME}/cdc_hardened/$subName/*
    fi
    if [ -f ${DATA_USER_HOME}/nz_input/$subName/*.* ]; then
        logSub "warn" $subName "$subName ${FUNCNAME[0]}: nz_input have before files,shouldn't"
        mv ${DATA_USER_HOME}/nz_input/$subName/*  ${DATA_USER_HOME}/archive/nz_input_superseded/$subName/ 
    fi
    
    #back up the cdc_output file first,and move it to hardened file folder
    for file in ${DATA_USER_HOME}/cdc_output/$subName/*.D[0-9]*.T[0-9]*
    do
        cp -f $file ${DATA_USER_HOME}/archive/cdc_output/$subName/
        mv $file ${DATA_USER_HOME}/cdc_hardened/$subName/
    done        
}

function mergeTable(){
    # merge files with different datetime, the target file have the name with last datetime
    local subName=$1
    local __dbName=$2
    local __shemaName=$3
    local __tableName=$4
    local __source_dir=$5
    local __target_dir=$6
    local __splitter=$7       
    local func_code=0        
    db2GetTablePK ${__dbName} ${__shemaName} ${DB_USER} ${DB_PWD} ${__tableName} 
    mergeErrorExit  $? $subName  "$subName ${FUNCNAME[0]} db2GetTablePK:fail"  "$subName ${FUNCNAME[0]} db2GetTablePK :success"                
    IFS=$'\n' 
    local table_file_list=(`printf '%s\n' "${finish_file_list[@]}" | awk -v pattern="^$__tableName" '$0 ~ pattern {print $0}' |sort -rk 3,4`)                          
    unset IFS    
    local file_datetime_suffix=`echo "${table_file_list[0]}" | awk '{print "D"$3".T"$4}'`       
    local dataFileNameInsert="$__tableName.LOAD_READY.I.$file_datetime_suffix"
    local dataFileNameDelete="$__tableName.LOAD_READY.D.$file_datetime_suffix"

    # Columns with less than 32 primary key ( Majority of the case 99.99%) - Current script handles this
    # Columns with no primary key but total columns less than 32 - Current script handles this
    # Columns with no primary key but total columns more than 32 - Apply the logic of using GS to break the file into 2 colums and then use the later column as 1 single entity to sort
    # Columns with more than 32 primary keys - Error it out.
    if [[ ${pk_column_count} -gt 0 ]] ; 
    then
      if [[ ${pk_column_count} -gt 28 ]] ; 
      then
        # Columns with more than 31 primary keys - Error it out.        
        mergeErrorExit 1 $subName "$subName ${FUNCNAME[0]} :${__tableName}'s Columns with more than 31 primary keys - Error it out."       
      else
        # Columns with less than 32 primary key ( Majority of the case 99.99%)        
        find $__source_dir -type f -name $__tableName.D[0-9]*.T[0-9]* | xargs cat | sort -t  ${sort_key_string} | awk 'BEGIN {FS="\034"} { gsub(/"/,"\"\"",$0); print $0;}' | awk -v output_dir="$__target_dir" -v ouput_table="$__tableName" -v key_string="${pk_cmd_string}" -v date_timestamp="${file_datetime_suffix}" -f mergeSource.awk
        func_code=$?
      fi
    else
      if [[ ${table_column_count} -gt 28 ]] ; 
      then
        # Columns with no primary key but total columns more than 31        
        #find ${sDataUserHome}/cdc_hardened/${sSubName} -type f -name ${sTableName}.D[0-9]*.T[0-9]* | xargs cat | sort -t  -k 2,2 | awk -v output_dir="${sDataUserHome}/working/${sSubName}" -v ouput_table="${sTableName}" -v key_string="${pk_cmd_string}" -v date_timestamp="${file_datetime_suffix}" -f mergeSource.awk
        find $__source_dir -type f -name $__tableName.D[0-9]*.T[0-9]* | xargs cat | sort -t  -k 2,2 | awk 'BEGIN {FS="\034"} { gsub(/"/,"\"\"",$0); print $0;}' | awk -v output_dir="$__target_dir" -v ouput_table="$__tableName" -v key_string="${pk_cmd_string}" -v date_timestamp="${file_datetime_suffix}" -f mergeSource.awk
        func_code=$?
      else
        # Columns with no primary key but total columns less than 31                
        find $__source_dir -type f -name $__tableName.D[0-9]*.T[0-9]* | xargs cat | sort -t  ${sort_key_string} | awk 'BEGIN {FS="\034"} { gsub(/"/,"\"\"",$0); print $0;}' | awk -v output_dir="$__target_dir" -v ouput_table="$__tableName" -v key_string="${pk_cmd_string}" -v date_timestamp="${file_datetime_suffix}" -f mergeSource.awk
        func_code=$?
      fi
    fi   
    mergeErrorExit $func_code $subName "$subName ${FUNCNAME[0]}: $__tableName merge failed" "$subName ${FUNCNAME[0]}: $__tableName merge success"    
    
    if [ -e "$__target_dir/$dataFileNameInsert" ]; then
        filesInsertMap[$__tableName]=$dataFileNameInsert        
    fi
    if [ -e "$__target_dir/$dataFileNameDelete" ]; then  
        filesDeleteMap[$__tableName]=$dataFileNameDelete       
    fi  
    
    return $?    
}

function mergeFinal(){
    local subName=$1
    local __success_sql  
    local funCode=0
    logFolderStatus $subName
          
    if [ -e ${DATA_USER_HOME}/working/${subname}/MERGE_ERROR.txt ] ; then        
        :
    else    
        db2MergeLog $subName
        funCode=$?
        mv ${DATA_USER_HOME}/working/${subName}/* ${DATA_USER_HOME}/nz_input/${subName} 
    fi
    
    #clear cdc_hardened folder
    rm -f ${DATA_USER_HOME}/cdc_hardened/${subName}/*
    
    #Delete any files that are more than x days old from: 1.data/archive/cdc_output/{subname}/  (x=3)       2.data/archive/nz_input_superseded/{subname}/  (x=3)   3.log/{subname}  (x=30)
    find ${DATA_USER_HOME}/archive/cdc_output/${subName} -type f -mtime +3 -exec rm -f {} \;
    find ${DATA_USER_HOME}/archive/nz_input_superseded/${subName} -type f -mtime +3 -exec rm -f {} \;
    find ${LOG_DIR}/$subName -type f -mtime +30 -exec rm -f {} \;
  
    #Compress any files in the data/archive/cdc_output/{subname}/ directory that arent already compressed  and  data/archive/nz_input_superseded/{subname}/  directory that arent already compressed
    find ${DATA_USER_HOME}/archive/cdc_output/${subName} -name '*.D[0-9]*.T[0-9]*' -exec tar -cf ${DATA_USER_HOME}/archive/cdc_output/${subName}/${subName}_cdc_output_${SCRIPT_START_TIME}_$$.tar {} \;
    find ${DATA_USER_HOME}/archive/nz_input_superseded/${subName} -name '*.D[0-9]*.T[0-9]*' -exec tar -cf ${DATA_USER_HOME}/archive/nz_input_superseded/${subName}/${subName}_nz_input_superseded_${SCRIPT_START_TIME}_$$.tar {} \;

    return $funCode
}

function mergeSubscription(){
    # $1 is sub name    
    local subName=$1
    local dbName=$2
    local schemaName=$3
    declare -g sub_table_list
    declare -g finish_file_list
    declare -g unfinished_file_list
    declare -g max_day_stamp
    declare -gA filesInsertMap #the source file mapping with the final insert data file
    declare -gA filesDeleteMap #the source file mapping with the final delete data file

    cdcFilesFolder ${DATA_USER_HOME}/cdc_output/${subName} sub_table_list finish_file_list unfinished_file_list max_day_stamp        
    logSub "info" $subName "${subName} ${FUNCNAME[0]} cdcFilesFolder tables:${sub_table_list[@]}"
    
    mergeCheck $subName
    exitSub $? $subName "$subName ${FUNCNAME[0]} mergeCheck:fail" "$subName ${FUNCNAME[0]} mergeCheck:success"
    
    #if there is no hardened files then return 0
    ls -l  ${DATA_USER_HOME}/cdc_output/${subName}/*.D[0-9]*.T[0-9]* > /dev/null 2>&1      
    if [ $? != 0 ]; then
        logSub "info" ${subName} "${subName} ${FUNCNAME[0]}:there are no hardened files"
        return 0
    fi
    
    mergeMove2Harden $subName                   
    logSub "info" $subName "$subName ${FUNCNAME[0]} mergeMove2Harden:success"    
    
    cdcFilesFolder ${DATA_USER_HOME}/cdc_hardened/${subName} sub_table_list finish_file_list unfinished_file_list max_day_stamp           
    
    for tableName in "${sub_table_list[@]}"
    do
        source_dir=${DATA_USER_HOME}/cdc_hardened/${subName}
        target_dir=${DATA_USER_HOME}/working/${subName}
        
        mergeTable $subName $dbName $schemaName ${tableName} ${source_dir} ${target_dir} ','
        mergeErrorExit $? $subName "$subName ${FUNCNAME[0]} mergeTable $tableName:fail" "$subName ${FUNCNAME[0]} mergeTable $tableName:success"
    done
    
    mergeFinal $subName
    exitSub $? $subName "$subName ${FUNCNAME[0]} mergeFinal:failed" "$subName ${FUNCNAME[0]} mergeFinal:ok"

    return $func_state    
}

function applyCheck(){
    #check premise
    local subName=$1
    local targetDbInfo=`echo $2 |awk '{print $1"_"$2}'`
    local funCode=0
    #check if there is error file
    if [ -e ${DATA_USER_HOME}/working/${subname}/APPLY_ERROR_${targetDbInfo}.txt ];then 
        logSub "error" ${subname} "$subName ${FUNCNAME[0]}: APPLY_ERROR_${targetDbInfo}.txt exist";
        funCode=1               
    fi
    #check if there is any file in nz_input
    return $funCode
}

function recordsGroup(){
    # $1 record  first column is group, last columns is info  $2 return Value
    local recordList=("${!1}")
    declare -A tempList
    IFS=$'\n'
    
    #groups is tablename list
    local groups=(`printf '%s\n' "${recordList[@]}"|awk '{print $1}'|sort -u`)    
    
    for key in  ${groups[@]}
    do
        tempList[$key]=`printf '%s\n' "${recordList[@]}"|awk -v pattern="$key" '$1 ~ pattern {print $2}' ORS=' '`                
    done         
    assoc_array_string=`declare -p  tempList`
    eval $2=${assoc_array_string#*=}
}

function applyGetSql(){
    #generate nz_crt_ext_tbls_{target}_{datetime}.sql ,nz_exec_apply_{target}_{datetime}.sql nz_drop_ext_tbls_{target}_{datetime}.sql
    # $1 subName $2 datetime $3 targetDbInfo $4 file info  $5 the return value
    local funCode=0
    local subName=$1
    local __dateTime=D${2% *}_T${2#* } 
    local targetDbInfo=$3
    local __targetDbServer=`echo $3|awk '{print $1}'`
    local __targetDb=`echo $3|awk '{print $2}'` 
    declare -a file_info_list=("${!4}")        
    
    IFS=$'\n'
    local __targetTable_list=( `printf '%s\n' "${file_info_list[@]}"|awk '{print $7}'` )
    unset IFS
            
    local __create_sql_path="$SQL_USER_HOME/$subName/nz_create_ext_tables_${__targetDbServer}_${__targetDb}_${__dateTime}.sql"    
    local __exec_sql_path="$SQL_USER_HOME/$subName/nz_exec_apply_${__targetDbServer}_${__targetDb}_${__dateTime}.sql"    
    local __delete_sql_path="$SQL_USER_HOME/$subName/nz_drop_ext_tables_${__targetDbServer}_${__targetDb}_${__dateTime}.sql"
    local nzDbColumns    
    local tableCreateStr
    
    #get table columns of db
    nzGetDbColumns "$targetDbInfo" __targetTable_list[@] nzDbColumns
    funCode=$?          
    
    recordsGroup nzDbColumns[@] tableCreateStr         
    eval "declare -gA arrayTableColumnList="$tableCreateStr
    rm ${__create_sql_path}
    rm ${__delete_sql_path}
    echo "BEGIN WORK;"> ${__exec_sql_path}
    chmod 775 ${__exec_sql_path}
    
    for file_info in "${file_info_list[@]}"
    do        
        local file_name=`echo $file_info|awk '{print $1}'`
        local file_type=`echo $file_name|awk  -F'[.]' '{print $3}'`
        local targetTableName=`echo $file_info|awk '{print $7}'`
        local tableColumnsList=(${arrayTableColumnList[$targetTableName]})   
        local tableColumnStr=`printf ",'%s'" "${tableColumnsList[@]}"`
        local sNZTmpTableName=""
        local sScriptStatement=""
        echo tableColumnStr "%{tableColumnStr[@]}"
        
        # generate create table script
        if [[ ${file_type} == "I" ]] ; then
          sNZTmpTableName="SRC_${targetTableName}_${__dateTime}_INS"          
          sScriptStatement="CREATE TABLE ${sNZTmpTableName} AS SELECT * FROM ${targetTableName} LIMIT 0;"
        fi
        if [[ ${file_type} == "D" ]] ; then
          sNZTmpTableName="SRC_${targetTableName}_${__dateTime}_DEL"
          sScriptStatement="CREATE TABLE ${sNZTmpTableName} AS SELECT ${tableColumnStr:1} FROM ${targetTableName} LIMIT 0;"
        fi
        echo "${sScriptStatement}" >> ${__create_sql_path}
        
        # generate apply script
        if [[ ${file_type} == 'D' ]] ; then
          sScriptStatement="DELETE FROM ${targetTableName} WHERE (${tableColumnStr:1}) IN (SELECT DISTINCT ${tableColumnStr:1} FROM ${sNZTmpTableName} );"          
        fi
        if [[ ${file_type} == 'I' ]] ; then
          sScriptStatement="INSERT INTO ${targetTableName} SELECT * FROM ${sNZTmpTableName};"          
        fi
        echo "${sScriptStatement}" >> ${__exec_sql_path}
        echo "COMMIT WORK;" >> ${__exec_sql_path}
                
        # generate drop table script
        sScriptStatement="DROP TABLE ${sNZTmpTableName};"
        echo "${sScriptStatement}" >> ${__delete_sql_path}
        
        chmod 775 ${__create_sql_path}
        chmod 775 ${__delete_sql_path}
                               
    done
    IFS=$'\n'
    local data_file_list=(`printf '%s\n' "${file_info_list[@]}"|awk '{print $1}'`)
    unset IFS
               
    db2ApplyLog "start" "STARTING" ${subName} "${targetDbInfo}" data_file_list[@]
    temp=$?;[[ $funCode==0 ]] && funCode=$temp                
    
    eval $5=\"$__create_sql_path $__exec_sql_path $__delete_sql_path\"
    
    return $funCode
}

function applyLoadData(){
    # load data from data file $subName $targetDbInfo $dateTime_Db_file_info_list
    local subName=$1
    local dateTime="$2"
    local targetServer=`echo $3|awk '{print $1}'`
    local targetDB=`echo $3|awk '{print $2}'`    
    declare -a file_info_list=("${!4}")
    for file_info in "${file_info_list[@]}"
    do
        local fileName=`echo $file_info|awk '{print $1}'`
        local targetTable=`echo $file_info|awk '{print $2}'`
        local file_type=`echo $fileName|awk '{print $3}'`

        if [[ ${file_type} == "I" ]] ; then
          sNZTmpTableName="SRC_${targetTable}_D${dateTime% *}_T${dateTime#* }_INS"                    
        fi
        if [[ ${file_type} == "D" ]] ; then
          sNZTmpTableName="SRC_${targetTable}_D${dateTime% *}_T${dateTime#* }_DEL"          
        fi            
        # load the data into this table which is just created_day by using nzload
        nzload -host ${targetServer} -db ${targetDB} -u ${NZ_USER} -pw ${NZ_PASSWORD}  -t ${sNZTmpTableName} -delim 29 -quotedValue DOUBLE -nullValue "" -df ${DATA_USER_HOME}/nz_input/$subName/${fileName} -fillRecord
#        nzload_pid=$!
#        echo "-- waiting for nzload to finish"
#        while ps | grep ${nzload_pid} | grep -v grep
#        do
#            sleep 10
#        done
#        wait ${nzload_pid}
        # check whether nz load is executed successfully or not                     
        local funCode=$?
        if [ $funCode -ne 0 ]; then            
            return $funCode        
        fi      
    done
}

function applySubscription(){    
    local subName=$1
    local func_state=0
    declare -g sub_table_list
    declare -g insert_file_list
    declare -g delete_file_list
    declare -g data_file_list    
    declare -gA arrayTableColumnList #target table and its columns
     
    ls -l  ${DATA_USER_HOME}/nz_input/${subName}/*.D[0-9]*.T[0-9]* > /dev/null 2>&1                  
    if [ $? != 0 ]; then
        logSub "info" ${subName} "${subName} ${FUNCNAME[0]}:there are no nz_input files"
        return $func_state
    fi
         
    dataFilesFolder ${DATA_USER_HOME}/nz_input/${subName} sub_table_list insert_file_list delete_file_list
    data_file_list=("${insert_file_list[@]}" "${delete_file_list[@]}")
    
    IFS=$'\n'
    #datetime_list list from early to late
    local datetime_list=( `printf '%s\n' "${data_file_list[@]}" | awk '{print $3" "$4}' |sort -u` )    
    unset IFS  
      
    #levelup, what is the datetime's source files batch is not the same as the before datetime's source files's batch
    for idx1 in ${!datetime_list[@]}
    do
        local dateTime=${datetime_list[$idx1]}

        IFS=$'\n'
        #get files of this dateTime levelup
        local dateTime_file_info_list=( `printf '%s\n' "${data_file_list[@]}" | awk -v pattern="$dateTime" '$0 ~ pattern {print $0}'` ) #
        unset IFS 
                  
        local targetDbList 
        local dateTime_file_list=(`printf '%s\n' "${dateTime_file_info_list[@]}"|awk '{print $1}'`)
        local start_time=""
        local end_time=""
        local targetSourceMapList=()
        db2GetSubFilesMap $subName dateTime_file_list[@] start_time end_time targetSourceMapList        
        logSub "info" $subName "$subName from $start_time to $end_time files: ${targetSourceMapList[@]}"       
        
        #levelup ,what if the following two failed 
        db2ClearApplyLog $subName "$dateTime" 
         
        db2CheckTargetUpdate $subName $start_time $end_time
        
        db2GetTargetDb $subName $start_time $end_time targetDbList
        exitSub $?  ${subName} "${subname} ${FUNCNAME[0]}:db2GetTargetDb failed" "${subname} ${FUNCNAME[0]}:db2GetTargetDb success"  
                                                                              
#        for idx2 in ${!targetDbList[@]}     
#        do              
#            local targetDbInfo=${targetDbList[$idx2]}  #server, db,prefix 
#            local targetServer=`echo $targetDbInfo|awk '{print $1}'`
#            local targetDB=`echo $targetDbInfo|awk '{print $2}'`            
#            local dateTime_Db_file_info_list
#            
#            applyCheck $subName $targetDb                
#            applyError $? $subName "$targetDbInfo" "$subName ${FUNCNAME[0]} applyCheck ${targetServer} ${targetDB}:fail" "$subName ${FUNCNAME[0]} applyCheck ${targetServer} ${targetDB}:success" ||  continue                                                  
#            
#            #dateTime_Db_file_info_list: fileName tableName date time targetServer targetDb targetTable
#            db2GetTargetTable $subName "$targetDbInfo" dateTime_file_info_list[@] dateTime_Db_file_info_list                                     
#            applyError $? $subName "$targetDbInfo" "$subName ${FUNCNAME[0]} db2GetTargetTable ${targetServer} ${targetDB}:fail" "$subName ${FUNCNAME[0]} db2GetTargetTable ${targetServer} ${targetDB}:success" ||  continue
#
#            applyGetSql $subName "$dateTime" "$targetDbInfo" dateTime_Db_file_info_list[@] sqls_file_path
#            applyError $? $subName "$targetDbInfo" "$subName ${FUNCNAME[0]} applyGetSql ${targetServer} ${targetDB}:fail" "$subName ${FUNCNAME[0]} applyGetSql ${targetServer} ${targetDB}:success" ||  continue
#            
#            #sqls_file_path: create, exec,delete
#            nzCreateTempTables "$targetDbInfo" "$sqls_file_path"                                             
#            if [ $? -ne 0 ]; then
#                nzDropTempTables "$targetDbInfo" "$sqls_file_path"
#                db2ApplyLog 'end' 'FAILED' $subName "$targetDbInfo" "$sqls_file_path" dateTime_file_list[@]
#                applyError 1 $subName "$targetDbInfo" "$subName ${FUNCNAME[0]} nzCreateTempTables ${targetServer} ${targetDB}:fail" "$subName ${FUNCNAME[0]} nzCreateTempTables ${targetServer} ${targetDB}:success"
#                continue 
#            fi       
#            
#            #get records from temp tables to target tables
#            applyLoadData $subName "$dateTime" "$targetDbInfo" dateTime_Db_file_info_list[@]
#            if [ $? -ne 0 ]; then
#                nzDropTempTables "$targetDbInfo" "$sqls_file_path"
#                db2ApplyLog 'end' 'FAILED' $subName "$targetDbInfo" "$sqls_file_path" dateTime_file_list[@]                
#                applyErrorExit 1 $subName "$targetDbInfo" "$subName ${FUNCNAME[0]} applyLoadData ${targetServer} ${targetDB}:fail" "$subName ${FUNCNAME[0]} applyLoadData ${targetServer} ${targetDB}:success"
#                continue 
#            fi
#                                 
#            nzExecTempTables "$targetDbInfo" "$sqls_file_path"
#            if [ $? -ne 0 ]; then
#                nzDropTempTables "$targetDbInfo" "$sqls_file_path"
#                db2ApplyLog 'end' 'FAILED' $subName "$targetDbInfo" "$sqls_file_path" dateTime_file_list[@]
#                applyErrorExit 1 $subName "$targetDbInfo" "$subName ${FUNCNAME[0]} nzExecTempTables ${targetServer} ${targetDB}:fail" "$subName ${FUNCNAME[0]} nzExecTempTables ${targetServer} ${targetDB}:success"
#                continue 
#            fi                                       
#            nzDropTempTables "$targetDbInfo" "$sqls_file_path"     
#            db2ApplyLog 'end' 'COMPLETE' $subName "$targetDbInfo" "$sqls_file_path" dateTime_file_list[@]       
#        done                                            
    done     
    return $?
}

function mergeErrorExit(){
    # $1 is the return status , $2 is sub name,$3 is error_msg  $4 is correct_msg      
    if [[ $1 != 0 ]] ; then         
        echo >${DATA_USER_HOME}/working/${2}/MERGE_ERROR.txt
        mergeFinal                 
    fi         
    exitSub $1 $2 "$3" "$4"       
}

function applyError(){
    # $1 is the return status , $2 is sub name,$3 targetDbInfo $4 is error_msg   $5 is corrrect_msg
    local func_state=0    
    local targetDbInfo=`echo $3 |awk '{print $1"_"$2}'`
    if [[ $1 != 0 ]] ; then        
        echo >${DATA_USER_HOME}/working/${2}/APPLY_ERROR_${targetDbInfo}.txt   
        logSub "error" $2 "$4"
    else  
        logSub "info" $2 "$5"    
    fi          
    return $1
}

##########################################################################################################################
#main process                                                                                                            #
##########################################################################################################################

#init Global variables and environment
initGlobalVar $* 
exitIf $? "initGlobalVar: initiation of global variable fails"

logHistory "info" "$SCRIPT_START_TIME process $$ start by user $USER"
logHistory "info" "current log file is ${LOG_DIR}/${SCRIPT_START_TIME}_$$.log"

#check if the premise has arrived
checkPremise
exitIf $? "checkPremise: premise is not satisfied"

#init db2 config database
db2Init
exitIf $? "db2Init:fails" "db2Init:db2 initiation success"

#get sub list from db2 config database from parameters (group, sub)
db2GetSubList subInfoList
exitIf $? "db2GetSubList:fails" "db2GetSubList:success"

#loop through sub list for each sub execute action of merge or apply 
for idx in ${!subInfoList[@]};
do            
    sub_name=`echo ${subInfoList[$idx]}|awk '{print $1}'`    
    sub_db=`echo ${subInfoList[$idx]}|awk '{print $2}'`
    sub_shema=`echo ${subInfoList[$idx]}|awk '{print $3}'`       
    
    createSubDirectories ${sub_name}    
    
    if [[ ${APPLY_ONLY}  != "Y" ]] ; then        
        logHistory "info" "${sub_name} mergeSubscription start at `eval $NOW`"
        mergeSubscription ${sub_name} $sub_db $sub_shema
        exitSub $? ${sub_name} "${sub_name} mergeSubscription fail at `eval $NOW`" "mergeSubscription success at `eval $NOW`"
    fi
    if [[ ${MERGE_ONLY}  != "Y" ]] ; then
        logHistory "info" "${sub_name} apply process start at `eval $NOW`"
        applySubscription ${sub_name}
        exitSub $? ${sub_name} "${sub_name} applySubscription fail `eval $NOW`" "applySubscription success `eval $NOW`"
    fi
done
logHistory "info" "$SCRIPT_START_TIME process $$ end at `eval $NOW` by user $USER"

#check if the nzload subprocess has finished
#for process in process_list
#    apply3Wait
#    apply4Update        