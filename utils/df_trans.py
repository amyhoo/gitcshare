__author__ = 'min'
from collections import namedtuple
from numpy import  float64
def numpy2type(data):
    '''
    将numpy格式的data转为普通的python格式
    :return:
    '''
    import numpy
    if "numpy" in str(type(data)):
        if "float" in str(type(data)):
            return float(data)
        elif "int" in str(type(data)):
            return int(data)
    else:
        return data

def df2tuple_gen(df,table_name,map={}):
    '''
    dataframe转为nametuple的生成器
    map为记录的列名，df的列名的对应关系
    '''
    record=namedtuple(table_name,map.keys())
    assert set(map.values())-{'index'} < set(df.columns)
    rows,columns=df.shape
    for row in range(rows):
        info={}
        for key in map:
            if map[key]=="index":
                info[key]=numpy2type(df.index[row])
                continue
            info[key]=numpy2type(df.iloc[row][map[key]])
        yield record(**info)

def tuple2dict():
    '''
    nametuple转为字典
    :return:
    '''