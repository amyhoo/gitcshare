__author__ = 'yamin'
from .session import get_session
from .base_model import ModelBase
from collections import deque
import logging
import weakref
from .models import *
LOG = logging.getLogger(__name__)

class BaseOperate():
    _session=None
    _model=None

    def __init__(self,length=100):
        '''
        length为操作次数满后进行session的flush操作
        :param length:
        :return:
        '''
        self.count=0#记录操作次数,从零开始计数
        self.length=length #达到length的时候进行flush

    def init_cache(self,key=None,maxlen=None):
        '''
        初始化数据库缓存
        key必须是model的unique非空字段
        :return:
        '''
        self.key=key
        self.cache_keys={}#存放缓存的记录的唯一非空关键字,凭借该key可以找到并唯一找到一条记录，必须对象的全部keys
        if maxlen:
            self.cache=deque(maxlen=maxlen)#长度为maxlen的队列
        else:
            self.cache=deque()#长度无限的队列
        for index ,record in enumerate(self.query.all()):
            self.cache_keys[getattr(record,'code')]=weakref.ref(record)
            if not maxlen or index <maxlen:
                self.cache.append(record)

    def is_hit(self,record):
        '''
        是否在keys中查到值
        :return:
        '''
        key_value=getattr(record,self.key)
        if key_value in self.cache_keys:
            return self.cache_keys[key_value]
        else:#当没有该记录则将该记录添加到缓存当中，并返回False
            self.cache_keys[key_value]=weakref.ref(record)
            self.cache.append(record)
            return False

    @property
    def session(self):
        if not self._session:
            self._session=get_session()
        return self._session

    @property
    def query(self):
        return self.session.query(self._model)

    @property
    def model(self):
        return self._model

    def close(self):
        self.session.flush()
        self.session.close()

    def get_or_create(self, model,**kwargs):
        instance = self.session.query(model).filter_by(**kwargs).first()
        if instance:
            return instance
        else:
            instance = model(**kwargs)
            return instance

    def flush_wrapper(func=None):
        '''
        func函数被调用length次数时候，才会进行flush
        :param func:
        :return:
        '''
        def wrap(self,*arg,**kwargs):
            try:
                func(self,*arg,**kwargs)
                if self.count==self.length:
                    self.session.flush()
                    self.count=0
                else:
                    self.count+=1
            except Exception as e:
                self.session.close()
                LOG.error(str(e))
                raise e
        return wrap

    @flush_wrapper
    def createfrtuple(self,record,find_cache=False):
        '''
        如果record是已有记录,则不做任何事情，如果没有记录，创建新记录，
        :param record:nametupe,如果_model为None,则从nametuple的表名默认为model名
        :param find_cache:
        :return:
        '''
        if self._model==None:
            from . import models as mod
            model=getattr(mod,type(record).__name__)
        else:
            model=self._model

        if find_cache:#支持cache
            cached_record=self.is_hit(record)
            if cached_record==False:#仅仅在数据库中没有该记录时候创建新记录
                new_record=model(**dict(record.__dict__))
                self.session.add(new_record)

        else:#不支持cache，对每条记录进行get_or_create判断
            record=self.get_or_create(model,**dict(record.__dict__))
            self.session.add(record)

    @flush_wrapper
    def createupdatefrtuple(self,record,find_cache=False):
        '''
        创建或者更新记录
        如果record是已有记录,则更新，如果没有记录，创建新记录，
        :param record:nametupe,如果_model为None,则从nametuple的表名默认为model名
        :param find_cache:
        :return:
        '''
        if self._model==None:
            from . import models as mod
            model=getattr(mod,type(record).__name__)
        else:
            model=self._model

        if find_cache:#支持cache
            cached_record=self.is_hit(record)
            if cached_record==False:#该记录为新记录，数据库中不存在，无需读数据库
                new_record=model(**dict(record.__dict__))
                self.session.add(new_record)
            elif cached_record==None:#cached_record不在内存，在数据库中,需要先读数据库
                new_record=self.get_or_create(model,**dict(record.__dict__))
                self.session.add(new_record)
            else:#在cache中，无需读数据库
                cached_record().update(**record.__dict__)
                self.session.add(cached_record())

        else:#不支持cache
            record=self.get_or_create(model,**dict(record.__dict__))
            self.session.add(record)

    def getObjFromDict(self,obj, jsonDict):
        '''
        将一个字典对象转换为Modelbase的对象，且如果该字典key为外键，则外键格式为  外键名.外键对象.对象属性
        :param obj:
        :param jsonDict:
        :return:
        '''
        if jsonDict and isinstance(obj,ModelBase):
            for (key, value) in jsonDict.items():
                if hasattr(obj, key):
                    obj[key] = value
                else:#是一个外键
                    if len(key.split("."))>2:
                        current_key=key.split(".")[0]
                        klass=eval(key.split(".")[1])
                        innerkey=key.split(".")[2]
                        foreign_obj=self.session.query(klass).filter_by(**{innerkey:value}).first()
                        if hasattr(obj,current_key):
                            obj[current_key]=foreign_obj.id
                        else:
                            return False
                    else:
                        return False
            return obj

    def getDictFromObj_nr(self,obj):
        '''
        obj为ModelBase的实例，取obj中所有列的值，relation除外
        :param obj:
        :return:
        '''
        return_dict={}
        if isinstance(obj,ModelBase):
            for key in obj.__dict__ :
                if key.startswith('_'):continue
                return_dict[key]=getattr(obj,key)
        return return_dict

    def getDictFromObj_rp(self,obj,rp_list={}):
        '''
        obj为ModelBase的实例，取obj中所有列的值，relation除外,rp_list里面的对象替换为对象中的某个值
        :param obj:
        :param rp_list: 为relation关系所获取的唯一对象，一般为外键对应的对象
        :return:
        '''
        return_dict=self.getDictFromObj_nr(obj)
        for key in rp_list:
            if hasattr(obj,key):
                sub_obj=getattr(obj,key)
                if isinstance(sub_obj,ModelBase):
                    if hasattr(sub_obj,rp_list[key]):
                        return_dict[key]=getattr(sub_obj,rp_list[key])
        return return_dict

    def getDictFromObj(self,obj):
        '''
        obj为ModelBase的实例，取obj中所有列的值，包括relation所获取的对象或者对象列表
        :param obj:
        :return:
        '''
        return_dict={}
        if isinstance(obj,ModelBase):
            for key in [x for x in dir(obj) if not x.startswith('_') and x not in ["get", "iteritems", "metadata", "next", "save", "update"]]:
                value=getattr(obj,key)
                if isinstance(value,list):#如果是对象列表
                    return_dict[key]=[]
                    for item in value:
                        if isinstance(item,ModelBase):
                            return_dict[key].append(self.getDictFromObj_nr(item))
                        else:
                            return_dict[key].append(item)
                elif isinstance(value,ModelBase):#如果是对象
                    return_dict[key]=self.getDictFromObj_nr(value)
                else:
                    return_dict[key]=getattr(obj,key)
            return return_dict
        else:
            return obj

    def create(self,info):
        '''
        通过info获取ModelBase的对象并保存
        :param info:字典信息，描述了 ModelBase对象的内容
        :return:
        '''
        try:
            # obj=self.__class__()
            # record=self.getObjFromDict(obj,info)
            record=self.model(**info)
            self.session.add(record)
            self.session.flush()
            return self.getDictFromObj(record)
        except Exception as e:
            self.session.close()
            LOG.error(str(e))
            raise e

    def bulk_create(self):
        '''
        批量创建
        :return:
        '''
    def update(self,info):
        '''
        通过info获取ModelBase的对象并更新
        :param info: 字典信息，拥有key字典，以及其他信息描述了需要更新的ModelBase对象的内容
        :return:
        '''
        try:
            key_params=info.pop("key")
            self.session.begin()
            record=self.query.filter_by(**key_params).first()
            record.update(info)
            self.session.commit()
            return self.getDictFromObj(record)
        except Exception as e:
            self.session.close()
            LOG.error(str(e))
            raise e

    def bulk_update(self):
        '''
        批量更新
        :return:
        '''

    def detail(self,info):
        '''
        通过info获取ModelBase的对象
        :param info: 拥有key信息
        :return:
        '''
        try:
            key_params=info.pop("key")
            record=self.query(self._model).filter_by(**key_params).first()
            return self.getDictFromObj(record)
        except Exception as e:
            self.session.close()
            LOG.error(str(e))
            raise e

    def list(self,info):
        '''
        通过info中的过滤信息，获取列表
        :param info:包括过滤信息
        :return:
        '''
        try:
            query=self.query(self._model)
            query=self._list_filter(query,info)
            total=query.count()
            if "page" in info:
                query=self._page(query,info)
            records=query.all()
            return [self.getDictFromObj_nr(item) for item in records],total
        except Exception as e:
            LOG.error(str(e))
            raise e

    def _list_filter(self,query,info):
        try:
            if "join" in info:
                key,value=info["join"].items()[0]
                query=query.join(key).filter(*value)
            if "filter_and" in info:#使用语句来过滤
                sql_word=" and ".join([key for key in info["filter_and"]])
                params=dict([value for key,value in info["filter_and"].items() if value !=""])
                query=query.filter(sql_word).params(**params)
            if "filter_expression" in info:#使用表字段表达式过滤
                query=query.filter(*info["filter_expression"])
            if "order_by" in info:
                query=query.order_by(*info["order_by"])
            return query
        except Exception as e:
            LOG.error(str(e))
            raise e

    def _page(self,query,info):
        if "page" in info:#使用分页
            offset=info["page"]["offset"]
            limit=info["limit"]["limit"]
            query=query.offset(offset).limit(limit)
        return query

class StockOperate(BaseOperate):
    '''
    股票数据
    '''

    _model=Stock
    def __init__(self,length=100):
        super(StockOperate,self).__init__(length)
        self.init_cache()
    def init_cache(self,key='code',maxlen=None):
        super(StockOperate,self).init_cache(key)

    def createfrtuple(self,record,find_cache=True):
        super(StockOperate,self).createfrtuple(record,find_cache)

class WorkOrderRecordOperate(BaseOperate):
    '''
    订单记录操作
    '''

class WorkOrderTypeOperate(BaseOperate):
    '''
    订单类型操作
    '''