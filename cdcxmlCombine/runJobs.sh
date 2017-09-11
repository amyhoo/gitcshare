#!/bin/bash
#input parameter project name
# -------
# This script acts as many cdc jobs automated running, which is called by schedule 
#
# Full Description
# ----------------
# - 
# - there is a setting file of subScription.csv define subscription name, output location and group
# - when this script is running by schedule, it will automatically check if the today's subscription output file if they are ready 
# - if subscription file is ready, will run the bulk load ,and store the result into subStateToday.csv
# - this script can be call many times a day, each time will check subStateToday.csv, find what job haven't been run,
# - and check the subscription  file ,and run it. when all states in the subStateToday.csv are finished,then the subStateToday.csv file
# - will rename as subState`date`.csv, but if the one subscription running generate error, then this job will not be call again.  
# Parameters
# ----------
# $1 Mandatory location  the location of cdc_output
#
######################################################################
# Change History
# Date        Author        Description
# --------------------------------------------------------------------
# 2017-01-16  Amy Xu     Initial version.
#
######################################################################
jobsArray=()
groupArray=()
stateArray=()
dirArray=()

_DEBUG="on"
function DEBUG()
{
	[ "$_DEBUG" == "on" ] &&  $@ 
}

function checkTodaySub(){
	checkWord=D`date +%Y%j`	
	dir_name="$1"
	all=`ls ${dir_name}`		
	if [[ -z "$all" ]]; then
	     echo "$dir_name is empty"
	     return 1
	fi	
	IFS=$'\n'	
	for i in $all;
	do		
		if ! [[ $i == *"$checkWord"* || $i == *".STOPPED"* ]]
		then
			return 1
		else 
			echo "correct file :$i"
		fi
		
	done	   
	return 0 
}
 
function writeStateToday(){
	#write current jobs state into file subStateToday.csv
	>"$stateDir/subStateToday.csv"
	for idx in ${!stateArray[@]};
	do
		echo ${jobsArray[$idx]},${stateArray[$idx]}>>"$stateDir/subStateToday.csv"
	done
}
 
function loadStateToday(){
	#load information from subStateToday.csv
	i=0 
	file="$stateDir/subStateToday.csv"
	IFS=","
	while read sub state
	do	
		stateArray[$i]=$state
		i=$i+1
	done <  $file
	file="$stateDir/subScription.csv"
	i=0
	while read sub loc group
	do	
		jobsArray[$i]=$sub
		dirArray[$i]=$loc	
		groupArray[$i]=$group
		i=$i+1
	done <  $file
	#echo  ${stateArray[*]}
	#echo  ${jobsArray[*]}
	#echo  ${dirArray[*]}
	#echo  ${groupArray[*]}
	
}

function initStateToday(){
	# if all job haven't start ,then create subStateToday.csv file
	IFS=","
	if ! [[ -e "$stateDir/subStateToday.csv" ]]
	then
		cat "$stateDir/subScription.csv" | while read sub data_location group;do	 			
			echo $sub,0>>"$stateDir/subStateToday.csv"
	 	done
	fi
} 

function mvStateToday(){
	# if all state is 1,which means all thing done rightfully, change the file name
	for idx in ${!stateArray[@]}; 
	do
		if [ ${stateArray[$idx]} != 2 ]
		then
			return 1
		fi
	done
	mv "$stateDir/subStateToday.csv" "$stateDir/subState`date +%Y%j`.csv" 
}

##main program run jobs
##parameter is subScription.csv and subStateToday.csv 's directory
DEBUG set -x
stateDir=$1
initStateToday
loadStateToda
for idx in ${!jobsArray[@]}; 
do 	

	if [ stateArray[$idx] == 0 ]
	then
		checkTodaySub ${dirArray[$idx]}${jobsArray[$idx]} ; 
  		ret=$?
  		if [[ret == 0]]
  		then 
  			stateArray[$idx]=1
  		fi
  	fi
  	
  	if [ stateArray[$idx] == 1 ] 
  	then
	  	stateArray[$idx]=1 
	  	cd /datawarehouse/production/dstage_nz/scripts/
	  	./process_cdc_for_ds.sh -g ${groupArray[$idx]} -s ${jobsArray[$idx]}
	  	ret=$?
	  	if [[ret == 0]] 
	  	then
	  		stateArray[$idx]=2	  		  
  		else 
  			stateArray[$idx]=3  	
  		fi
  	fi
done
writeStateToday 
mvStateToday
DEBUG set +x