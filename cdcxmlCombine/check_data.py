'''
Created on 19 Jan 2017

@author: u391812
'''
import csv,sys,os,urllib,ast
file1=open('H:/projects/CDC/cdc_compare/result1.csv')
file2=open('H:/projects/CDC/cdc_compare/result11.csv')
table1=csv.DictReader(file1)
table2=csv.DictReader(file2)
id_dict1={item ['ID']:item for item in table1}
id_dict2={item ['ID']:item for item in table2}
for id in  (set(id_dict1.keys())-set(id_dict2.keys())):
    print (id_dict1[id])