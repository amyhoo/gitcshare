__author__ = 'min'
from collect.basecls import WebDataFrameBasic,TushareBase
import tushare as ts

class StockData(WebDataFrameBasic):
    def hanlde_data(self,data_gen):
        '''
        对网络数据进行特别清洗
        :param data_gen:
        :return:
        '''
        for record in data_gen:
            date=str(record.timeToMarket)
            if len(date)!=8:
                date=None
            yield record._replace(timeToMarket=date)
    def start(self,queue=None):
        df=ts.get_stock_basics()
        from utils.df_trans import df2tuple_gen
        data=self.hanlde_data(df2tuple_gen(df,"Stock",
                          {'code':'index','name':'name','industry':'industry',
                           'area':'area','outstanding':'outstanding','totals':'totals',
                           'timeToMarket':'timeToMarket'}))
        queue.put({"state":"end","data":data,"label":self.unique()})
    def unique(self):
        return "get_stock_basics"

def main():
    task=TushareBase(StockData)
    task.transport()
if __name__ == "__main__":
    main()


