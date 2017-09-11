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
    # $1 subName $2 data file list $3 start time end time $4 return data_file map 
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
            
    data_file_map_list=(`db2 -x ${sql}`)
    funCode=$?     
    
    for data_file in "${data_file_list[@]}"
    do
        sourceFilesList=(`printf '%s\n' "${data_file_map_list[@]}" | awk -v pattern='^$data_file' '$0 ~ pattern {print $2}' | sort`)        
        sourceFilesStr=`printf ",'%s'" "${sourceFilesList[@]}"`
        start_time=`echo ${sourceFilesList[0]}|awk -F[.] '{print ${2:1}","${3:1}}'`                 
        end_time=`echo ${sourceFilesList[-1]}|awk -F[.] '{print ${2:1}","${3:1}}'`
        source_target_list+=("$data_file $start_time $end_time ${sourceFilesStr:1}")
    done
     
    min_time=`printf '%s\n' "${source_target_list[@]}" | sort -k 2,2 | head -n1 | awk '{print $2}'`
    max_time=`printf '%s\n' "${source_target_list[@]}" | sort -rk 3,3 | head -n1 | awk '{print $3}'`
    eval $3="$min_time $max_time"
    eval $4='("${data_file_map_list[@]}")' 
    return $funCode   
} 

function db2CheckTargetUpdate(){
    # check if this data_file_list ,the last successful datetime in this target db must be 
    # $1 subName $2 start time $3 end time
    local subName=$1
    local start_time=`echo $2|awk -F[,] '{print "D"$1".T"$2}'`
    local end_time=`echo $3|awk -F[,] '{print "D"$1".T"$2}'`    

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
    if [ ${#return_list[@]} -nq 0 ];then
        logSub "warn" $subName "data files of $end_time: ${#return_list[@]}  updated before"           
    fi 
    
    sql="SELECT T.TARGET_SERVER || ' ' || T.TARGET_DB_NAME || ' ' || T.TABLE_NAME_PREFIX
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
    if [ ${#return_list[@]} -nq 0 ]; then
        logSub "warn" $subName "data files of $end_time:${#return_list[@]} overlap with target database"        
    fi 
    
    sql="SELECT T.TARGET_SERVER || ' ' || T.TARGET_DB_NAME || ' ' || T.TABLE_NAME_PREFIX
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
    if [ ${#return_list[@]} -nq 0 ]; then 
        logSub "warn" $subName "data files of $end_time:${#return_list[@]}  have failed update before this"        
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

function db2ClearApplyLog(){
    # $1 is subName  $2 is date and timestamp
    local subName=$1
    local dateTime=`echo $2|awk '{print "D"$1".T"$2}'`

    db2 -x "delete from ${DB_SCHEMA}.CDC_FOR_DS_APPLY_LOG where SUB_NAME = '${subName}'
          and status <> 'COMPLETE' and  SRC_FILE_NAME like '%${dateTime}'"  
    return $?       
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

