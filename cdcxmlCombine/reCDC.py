'''
Created on 21 Mar 2017

@author: u391812
'''
import csv,sys,os,urllib,ast
file1=open('H:\projects\CDC\CDC_latency\subs.csv')
file2=open('H:\projects\CDC\CDC_latency\critical_subs.csv')
table1=csv.DictReader(file1,dialect="excel-tab")
table2=csv.DictReader(file2,dialect="excel-tab")
prefixDBSET={(record['SUBSCRIPTION'][:2],record['SOURCE_TABLE'].split('.')[0],record['TARGET_TABLE'].split('.')[0]) for record in table1}
prefixDBList=sorted(list(prefixDBSET))
for item in  prefixDBList:
    print (item)
    