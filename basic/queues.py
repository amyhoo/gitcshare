__author__ = 'yamin'
from collections import deque
class QueProxy(deque):
    '''
    跟线程队列Queue接口相同
    '''
    def put(self,element):
        self.append(element)
    def get(self):
        return self.pop()