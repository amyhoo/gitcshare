__author__ = 'yamin'
from sqlalchemy.ext.declarative import declarative_base

ModelBase = declarative_base()

class ModelMixin(object):
    '''
    基类
    '''
