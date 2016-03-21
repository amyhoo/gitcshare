__author__ = 'yamin'
'''
历史数据收集
'''
from django.db import transaction
import tushare as ts
from django_db.models import *

@transaction.commit_manually
def In_Stock():
    '''
    收集股票信息
    :return:
    '''
    df=ts.get_stock_basics()
    rows,columns=df.shape
    for row in range(rows):
        sd={
            "code":df.iloc[row]["code"],
            "name":df.iloc[row]["name"],
            "industry":df.iloc[row]["industry"],
            "outstanding":df.iloc[row]["outstanding"],
            "totals":df.iloc[row]["totals"],
            "exchange":df.iloc[row]["exchange"],
            "timeToMarket":df.iloc[row]["timeToMarket"],
            }

        record=Stock(sd["code"],sd["name"],sd["industry"],sd["area"],sd["outstanding"],sd["totals"],sd[""])
        record.save()
    transaction.commit()

@transaction.commit_manually
def In_StockBasic():
    '''
    收集股票基本面
    :return:
    '''
    qd=ts.get_stock_basics()
    for row in qd:
        record=StockBasic(row)
        record.save()
    transaction.commit()

@transaction.commit_manually
def In_Stock_K_5():
    '''
    收集股票基本面
    :return:
    '''
    qd=ts.get_stock_basics()
    for row in qd:
        record=Stock_K_5(row)
        record.save()
    transaction.commit()

@transaction.commit_manually
def In_Stock_K_15():
    '''
    收集股票基本面
    :return:
    '''
    qd=ts.get_stock_basics()
    for row in qd:
        record=Stock_K_15(row)
        record.save()
    transaction.commit()

@transaction.commit_manually
def In_Stock_K_30():
    '''
    收集股票基本面
    :return:
    '''
    qd=ts.get_stock_basics()
    for row in qd:
        record=Stock_K_30(row)
        record.save()
    transaction.commit()

@transaction.commit_manually
def In_Stock_K_60():
    '''
    收集股票基本面
    :return:
    '''
    qd=ts.get_stock_basics()
    for row in qd:
        record=Stock_K_60(row)
        record.save()
    transaction.commit()

@transaction.commit_manually
def In_Stock_K_24():
    '''
    收集股票基本面
    :return:
    '''
    qd=ts.get_stock_basics()
    for row in qd:
        record=Stock_K_24(row)
        record.save()
    transaction.commit()

@transaction.commit_manually
def In_Stock_K_7():
    '''
    收集股票基本面
    :return:
    '''
    qd=ts.get_stock_basics()
    for row in qd:
        record=Stock_K_7(row)
        record.save()
    transaction.commit()

@transaction.commit_manually
def In_Stock_K_M():
    '''
    收集股票基本面
    :return:
    '''
    qd=ts.get_stock_basics()
    for row in qd:
        record=Stock_K_M(row)
        record.save()
    transaction.commit()

@transaction.commit_manually
def In_Stock_D():
    '''
    收集股票基本面
    :return:
    '''
    qd=ts.get_stock_basics()
    for row in qd:
        record=Stock_D(row)
        record.save()
    transaction.commit()

@transaction.commit_manually
def In_StockTrade():
    '''
    收集股票基本面
    :return:
    '''
    qd=ts.get_stock_basics()
    for row in qd:
        record=StockTrade(row)
        record.save()
    transaction.commit()