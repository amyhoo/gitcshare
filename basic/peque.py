__author__ = 'min'
'''
优先级队列
'''

import heapq

class Peque:
    def __init__(self):
        self._queue = []
        self._index = 0

    def push(self, item, priority):
        '''
        :param item:
        :param priority: 越高越优先；如果优先级一样，则根据插入顺序_index来排序；
        :return:
        '''
        heapq.heappush(self._queue, (-priority, self._index, item))
        self._index += 1

    def pop(self):
        return heapq.heappop(self._queue)[-1]


