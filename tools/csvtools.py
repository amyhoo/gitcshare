'''
Created on 23 Sep 2016

@author: u391812
'''

def getfields(buffer):
    '''
    for records (dict format) in buffer get all fields of all records  
    '''
    fields=reduce(set.union,[set(record.keys()) for record in buffer])
    return list(fields)
    
