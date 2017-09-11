#!/bin/ksh
#=============================================================================
to=${1} 
from=${2}
sub=${3}

thisdir=$(dirname $0)





#Note, Body specifies a file.  The contents of the file will be used for the body of the email
body=${4}

attachFlag=N
if [ $# -eq 5 ]
then
  attachFlag="Y"
  attach=${5}
fi

if [ $attachFlag = "Y" ] 
then
     ${thisdir}/emailit.sh -t "${to}" -a "${from}" -s "${sub}" -A "${attach}" -b "${body}"
     rc=$?
     if [[ $rc != 0 ]] ; then
        echo [ERROR] error sending email ${rc} 
        exit -1
     fi
else
     ${thisdir}/emailit.sh -t "${to}" -a "${from}" -s "${sub}" -b "${body}"
     rc=$?
     if [[ $rc != 0 ]] ; then
        echo [ERROR] error sending email ${rc}
        exit -1
     fi
     
     
fi

echo [OK] email sent
exit 0

