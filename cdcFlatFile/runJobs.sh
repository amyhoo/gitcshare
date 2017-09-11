#!/bin/bash
#input parameter project name
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
	if  ! [[ -e "$stateDir/subStateToday.csv" ]]
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
DEBUG set -x
stateDir=$1
initStateToday
loadStateToday
echo "the arrays:${#jobsArray[@]},${#stateArray[@]},${#groupArray[@]},${#dirArray[@]}"
for idx in ${!jobsArray[@]}; 
do 	
	checkTodaySub ${dirArray[$idx]}${jobsArray[$idx]} ; 
  	ret=$?
	stateArray[$idx]=2
done
writeStateToday 
mvStateToday
DEBUG set +x
