__author__ = 'yamin'
'''
本模块功能:
    定义数据包
    定义数据集，数据集由连续的一系列数据包构成，该包生成在时间上可能不连续
'''
from db.operater import BaseOperate
from concurrent.futures import ThreadPoolExecutor
from queue import Queue
import logging
import traceback
LOG = logging.getLogger(__name__)
from basic.queues import QueProxy
class WebDataFrameBasic():
    '''
    数据包，一个web数据的片段
    '''
    def __init__(self,query_info={}):
        '''
        '''
        self.query_info=query_info

    def start(self,queue=None):
        '''
        开始获取数据,数据存放到queue中
        queue中item的格式：{state:,label:,data:}
        label:unique
        state:end.finish,fail
        :return:
        '''

    def unique(self):
        '''
        唯一标志该数据片段的关键字：数据集通过查询该片段保证是否已经获得，是重复片段
        '''
    def _is_finished(self):
        '''
        该片段是否成功传输
        '''

    def _is_end(self):
        '''
        是否是在后一个数据片段
        '''

class WebDataSeqBasic():
    '''
    数据集，或者数据流
    '''
    STATE_TYPE={
        "START":0,
        "RUNNING":1,
        "FINISH":2,
        "END":3
    }
    def __init__(self,thread_num=1,info={},id=None):
        '''
        如果id为空，则自动创建一个数据集的id，并保存为一个记录
        如果不为空，则在数据库或者文件中查询看是否有相应的记录，如果有则获取信息，创建实例
        :param name:数据集的名称
        :param id:唯一标志一个数据集
        :return:
        '''

        if thread_num>1:#开启多线程模式

            self.thread_mode=True
            self.pool=ThreadPoolExecutor(thread_num)
            self.queue=Queue()

        else:#单线程模式

            self.thread_mode=False
            self.queue=QueProxy()

        if id:
            self._load(id)
        else:
            max_id=WebDataSeqBasic._load_max_id()
            self.id=max_id+1
            self._save(self.id)
            self.frame_state={} #该字典中填写数据片段的 STATE_TYPE
            self.frame_try={}#该数据段的尝试次数
            self.end_flags=False #当收到的数据片段标记_is_end为True时候会为真
            self.info=info

    def _load(self,id):
        '''
        从外部载入数据集
        :param id:
        :return:
        '''

    def _save(self,id):
        '''
        将该数据集保存到外部
        :param id:
        :return:
        '''

    @classmethod
    def _load_max_id(cls):
        return 0

    def is_finished(self):
        '''
        是否完成
        :return;
        '''
        finish=[WebDataSeqBasic.STATE_TYPE["FINISH"],WebDataSeqBasic.STATE_TYPE["END"]]
        if self.end_flags and all([item in finish for item in self.frame_state.values()]):
            return True
        else:
            return False

    def task_gen(self):
        '''
        获取下一个任务的WebDataFrame实例
        :return:
        '''
        raise NotImplementedError

    def transport(self):
        '''
        传输
        :return:
        '''
        for webdata in self.task_gen():
            label=webdata.unique()
            self.frame_state[label]=WebDataSeqBasic.STATE_TYPE["RUNNING"]
            if label not in self.frame_try:
                self.frame_try[label]=1
            else:
                self.frame_try[label]+=1
            if self.thread_mode:
                self.pool.submit(webdata.start,self.queue)
            else:
                webdata.start(self.queue)
        while 1:
            self.collect()
            if self.is_finished():
                break
    def collect(self):
        '''
        从queue中获取信息，并更新状态
        :return:
        '''
        item=self.queue.pop()
        if item["state"]=="end":#end状态表示这是最后一帧，且完成了该帧
            self.end_flags=True
        if item["state"]in ["finish","end"]:
            self.frame_state[item["label"]]=WebDataSeqBasic.STATE_TYPE["FINISH"]
            self.handle_data(item["data"])
        else:#
            self.frame_state[item["label"]]=WebDataSeqBasic.STATE_TYPE["START"]

    def handle_data(self,data):
        raise NotImplementedError


class TushareBase(WebDataSeqBasic):
    '''
    仅仅只有单个请求的接口
    通过tushare接口来操作数据
    '''
    def __init__(self,data_cls,thread_num=1,info={},id=None):
        super(TushareBase,self).__init__(thread_num,info,id)
        self.data_cls=data_cls

    def task_gen(self):
        yield self.data_cls(self.info)

    def handle_data(self,data):
        '''
        调用数据库接口，存储数据
        :param data:
        :return:
        '''
        op=BaseOperate()
        for record in data:
            try:
                op.createfrtuple(record)
            except Exception as e:
                LOG.error([key+":"+getattr(record,key) for key in record.__dict__])
                LOG.error(traceback.format_exc())
        op.close()