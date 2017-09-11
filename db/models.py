__author__ = 'min'
'''
    价格:decimal 10，4
    增长率:decimal 10，4
    周转天数，周转率：decimal 10，4
    资产:float
'''
from .base_model import ModelBase
from sqlalchemy.orm import relationship
from sqlalchemy import ForeignKey, DateTime, Boolean, Text, DECIMAL,Float,Column, Integer, String,event

def utcnow():
    import datetime
    return datetime.datetime.utcnow()

class Stock(ModelBase):
    '''
    股票基本信息
    '''
    __tablename__ = 'stock'
    __table_args__ = ()
    id=Column(Integer, primary_key=True,autoincrement=True)
    code=Column(String(20),unique=True)#代码
    name=Column(String(100))#名称
    industry=Column(String(100))#行业
    area=Column(String(100))#地区
    outstanding=Column(Float)#流通股值
    totals=Column(Float)#总股值
    company=Column(String(100))#公司
    exchange=Column(String(100))#交易所
    type=Column(String(50))#证券类别
    timeToMarket=Column(DateTime,nullable=True)#上市日期

# class Category(ModelBase):
#     '''
#     分类信息,树状结构
#     '''
#     __tablename__ = 'catree'
#     __table_args__ = ()

class StockBasicSeason(ModelBase):
    '''
    股票的基本面，按季公布
    '''
    __tablename__ = 'stockbasicseason'
    __table_args__ = ()
    id=Column(Integer, primary_key=True,autoincrement=True)
    stock_id=Column(Integer,ForeignKey("stock.id"),nullable=False)
    report_date=Column(DateTime,default=utcnow())#"发布日期"
    adratio=Column(DECIMAL(10,4))#"股东权益增长率"
    arturnover=Column(DECIMAL(10,4))#"应收账款周转率(次)"
    arturndays=Column(DECIMAL(10,4))#"应收账款周转天数(天)"
    bvps=Column(Float)#"每股净资产"
    business_income=Column(Float)#"营业收入(百万)"
    bips=Column(Float)#"每股主营业务收入(元)"
    cf_sales=Column(DECIMAL(10,4))#"经营现金净流量对销售收入比率"
    cf_nm=Column(DECIMAL(10,4))#"经营现金净流量与净利润的比率"
    cf_liabilities=Column(DECIMAL(10,4))#"经营现金净流量对负债比率"
    cashflowratio=Column(DECIMAL(10,4))#"现金流量比率"
    currentasset_turnover=Column(DECIMAL(10,4))#"流动资产周转率(次)"
    currentasset_days=Column(DECIMAL(10,4))#"流动资产周转天数(天)"
    cashratio=Column(DECIMAL(10,4))#"现金比率"
    currentratio=Column(DECIMAL(10,4))#"流动比率"
    distrib=Column(DECIMAL(10,4))#"分配方案"
    eps=Column(DECIMAL(10,4))#"每股收益"
    eps_yoy=Column(DECIMAL(10,4))#"每股收益同比"
    epcf=Column(DECIMAL(10,4))#"每股现金流量(元)"
    epsg=Column(DECIMAL(10,4))#"每股收益增长率"
    fixedAssets=Column(Float)#"固定资产"
    gross_profit_rate=Column(DECIMAL(10,4))#"毛利率(%)"
    inventory_turnover=Column(DECIMAL(10,4))#"存货周转率(次)"
    inventory_days=Column(DECIMAL(10,4))#"存货周转天数(天)"
    icratio=Column(DECIMAL(10,4))#"利息支付倍数"
    liquidAssets=Column(Float)#"流动资产"
    mbrg=Column(DECIMAL(10,4))#"主营业务收入增长率(%)"
    net_profits=Column(Float)#"净利润(万元)"
    net_profit_ratio=Column(DECIMAL(10,4))#"净利润率(%)"
    nprg=Column(DECIMAL(10,4))#"净利润增长率(%)"
    nav=Column(DECIMAL(10,4))#"净资产增长率"
    profits_yoy=Column(DECIMAL(10,4))#"净利润同比(%)"
    pe=Column(DECIMAL(10,4))#"市盈率"
    pb=Column(DECIMAL(10,4))#"市净率"
    peg=Column(DECIMAL(10,4))#市盈率相对盈利增长比率
    quickratio=Column(DECIMAL(10,4))#"速动比率"
    rateofreturn=Column(DECIMAL(10,4))#"资产的经营现金流量回报率"
    reserved=Column(Float)#"公积金"
    reservedPerShare=Column(DECIMAL(10,4))#"每股公积金"
    roe=Column(DECIMAL(10,4))#"净资产收益率(%)"
    seg=Column(DECIMAL(10,4))#"股东权益增长率"
    sheqratio=Column(DECIMAL(10,4))#"股东权益比率"
    targ=Column(DECIMAL(10,4))#"总资产增长率"
    totalAssets=Column(Float)#"总资产(万)"

class StockBasicYear(ModelBase):
    '''
    股票的基本面，按年公布
    '''
    __tablename__ = 'stockbasicyear'
    __table_args__ = ()
    id=Column(Integer, primary_key=True,autoincrement=True)
    stock_id=Column(Integer,ForeignKey("stock.id"),nullable=False)
    report_date=Column(DateTime,default=utcnow())#"发布日期"
    adratio=Column(DECIMAL(10,4))#"股东权益增长率"
    arturnover=Column(DECIMAL(10,4))#"应收账款周转率(次)"
    arturndays=Column(DECIMAL(10,4))#"应收账款周转天数(天)"
    bvps=Column(Float)#"每股净资产"
    business_income=Column(Float)#"营业收入(百万)"
    bips=Column(Float)#"每股主营业务收入(元)"
    cf_sales=Column(DECIMAL(10,4))#"经营现金净流量对销售收入比率"
    cf_nm=Column(DECIMAL(10,4))#"经营现金净流量与净利润的比率"
    cf_liabilities=Column(DECIMAL(10,4))#"经营现金净流量对负债比率"
    cashflowratio=Column(DECIMAL(10,4))#"现金流量比率"
    currentasset_turnover=Column(DECIMAL(10,4))#"流动资产周转率(次)"
    currentasset_days=Column(DECIMAL(10,4))#"流动资产周转天数(天)"
    cashratio=Column(DECIMAL(10,4))#"现金比率"
    currentratio=Column(DECIMAL(10,4))#"流动比率"
    distrib=Column(DECIMAL(10,4))#"分配方案"
    eps=Column(DECIMAL(10,4))#"每股收益"
    eps_yoy=Column(DECIMAL(10,4))#"每股收益同比"
    epcf=Column(DECIMAL(10,4))#"每股现金流量(元)"
    epsg=Column(DECIMAL(10,4))#"每股收益增长率"
    fixedAssets=Column(Float)#"固定资产"
    gross_profit_rate=Column(DECIMAL(10,4))#"毛利率(%)"
    inventory_turnover=Column(DECIMAL(10,4))#"存货周转率(次)"
    inventory_days=Column(DECIMAL(10,4))#"存货周转天数(天)"
    icratio=Column(DECIMAL(10,4))#"利息支付倍数"
    liquidAssets=Column(Float)#"流动资产"
    mbrg=Column(DECIMAL(10,4))#"主营业务收入增长率(%)"
    net_profits=Column(Float)#"净利润(万元)"
    net_profit_ratio=Column(DECIMAL(10,4))#"净利润率(%)"
    nprg=Column(DECIMAL(10,4))#"净利润增长率(%)"
    nav=Column(DECIMAL(10,4))#"净资产增长率"
    profits_yoy=Column(DECIMAL(10,4))#"净利润同比(%)"
    pe=Column(DECIMAL(10,4))#"市盈率"
    pb=Column(DECIMAL(10,4))#"市净率"
    peg=Column(DECIMAL(10,4))#市盈率相对盈利增长比率
    quickratio=Column(DECIMAL(10,4))#"速动比率"
    rateofreturn=Column(DECIMAL(10,4))#"资产的经营现金流量回报率"
    reserved=Column(Float)#"公积金"
    reservedPerShare=Column(DECIMAL(10,4))#"每股公积金"
    roe=Column(DECIMAL(10,4))#"净资产收益率(%)"
    seg=Column(DECIMAL(10,4))#"股东权益增长率"
    sheqratio=Column(DECIMAL(10,4))#"股东权益比率"
    targ=Column(DECIMAL(10,4))#"总资产增长率"
    totalAssets=Column(Float)#"总资产(万)"

class Stock_K_5(ModelBase):
    '''
    5分钟K线
    '''
    __tablename__ = 'stockk5'
    __table_args__ = ()    
    id=Column(Integer, primary_key=True,autoincrement=True)
    stock_id=Column(Integer,ForeignKey("stock.id"),nullable=False)
    ktype=Column(String(50))#类型
    record_t=Column(DateTime)
    high=Column(DECIMAL(10,4))
    low=Column(DECIMAL(10,4))
    volume=Column(Integer)

class Stock_K_15(ModelBase):
    '''
    15分钟K线
    '''
    __tablename__ = 'stockk15'
    __table_args__ = ()      
    id=Column(Integer, primary_key=True,autoincrement=True)
    stock_id=Column(Integer,ForeignKey("stock.id"),nullable=False)
    ktype=Column(String(50))#类型
    record_t=Column(DateTime)
    high=Column(DECIMAL(10,4))
    low=Column(DECIMAL(10,4))
    volume=Column(Integer)

class Stock_K_30(ModelBase):
    '''
    30分钟K线
    '''
    __tablename__ = 'stockk30'
    __table_args__ = ()      
    id=Column(Integer, primary_key=True,autoincrement=True)
    stock_id=Column(Integer,ForeignKey("stock.id"),nullable=False)
    ktype=Column(String(50))#类型
    record_t=Column(DateTime)
    high=Column(DECIMAL(10,4))
    low=Column(DECIMAL(10,4))
    volume=Column(Integer)

class Stock_K_60(ModelBase):
    '''
    60分钟K线
    '''
    __tablename__ = 'stockk60'
    __table_args__ = ()      
    id=Column(Integer, primary_key=True,autoincrement=True)
    stock_id=Column(Integer,ForeignKey("stock.id"),nullable=False)
    ktype=Column(String(50))#类型
    record_t=Column(DateTime)
    high=Column(DECIMAL(10,4))
    low=Column(DECIMAL(10,4))
    volume=Column(Integer)

class Stock_K_24(ModelBase):
    '''
    24小时K线
    '''
    __tablename__ = 'stockk24'
    __table_args__ = ()      
    id=Column(Integer, primary_key=True,autoincrement=True)
    stock_id=Column(Integer,ForeignKey("stock.id"),nullable=False)
    ktype=Column(String(50))#类型
    record_t=Column(DateTime)
    high=Column(DECIMAL(10,4))
    low=Column(DECIMAL(10,4))
    volume=Column(Integer)

class Stock_K_7(ModelBase):
    '''
    7天K线
    '''
    __tablename__ = 'stockk7'
    __table_args__ = ()      
    id=Column(Integer, primary_key=True,autoincrement=True)
    stock_id=Column(Integer,ForeignKey("stock.id"),nullable=False)
    ktype=Column(String(50))#类型
    record_t=Column(DateTime)
    high=Column(DECIMAL(10,4))
    low=Column(DECIMAL(10,4))
    volume=Column(Integer)

class Stock_K_M(ModelBase):
    '''
    月K线
    '''
    __tablename__ = 'stockkm'
    __table_args__ = ()      
    id=Column(Integer, primary_key=True,autoincrement=True)
    stock_id=Column(Integer,ForeignKey("stock.id"),nullable=False)
    ktype=Column(String(50))#类型
    record_t=Column(DateTime)
    high=Column(DECIMAL(10,4))
    low=Column(DECIMAL(10,4))
    volume=Column(Integer)

class Stock_D(ModelBase):
    '''
    股票日信息
    '''
    __tablename__ = 'stockd'
    __table_args__ = ()    
    id=Column(Integer, primary_key=True,autoincrement=True)
    stock_id=Column(Integer,ForeignKey("stock.id"),nullable=False)
    record_t=Column(DateTime)#记录时刻
    open=Column(DECIMAL(10,4))
    close=Column(DECIMAL(10,4))
    ma5=Column(DECIMAL(10,4))
    ma10=Column(DECIMAL(10,4))
    ma20=Column(DECIMAL(10,4))
    v_ma5=Column(DECIMAL(10,4))
    v_ma10=Column(DECIMAL(10,4))
    v_ma20=Column(DECIMAL(10,4))

class StockTrade():
    '''
    分笔数据
    '''
    __tablename__ = 'stocktrade'
    __table_args__ = ()      
    id=Column(Integer, primary_key=True,autoincrement=True)
    stock_id=Column(Integer,ForeignKey("stock.id"),nullable=False)
    record_t=Column(DateTime)#记录时刻
    price=Column(DECIMAL(10,4))
    change=Column(DECIMAL(10,4))
    volume=Column(Integer)
    amount=Column(Integer)
    tradetype=Column(String(50))