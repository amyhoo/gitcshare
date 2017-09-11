__author__ = 'yamin'
import sqlalchemy
import time
from  sqlalchemy.orm.session import Session as Session
from  sqlalchemy.orm.query import Query as Query
from conf_import import *
LOG = logging.getLogger(__name__)

def _is_db_connection_error(args):
    """Return True if error in connecting to db."""
    # NOTE(adam_g): This is currently MySQL specific and needs to be extended
    #               to support Postgres and others.
    conn_err_codes = ('2002', '2003', '2006')
    for err_code in conn_err_codes:
        if args.find(err_code) != -1:
            return True
    return False

from sqlalchemy.interfaces import PoolListener
class MysqlListener(PoolListener):
    '''
    mysql数据库连接池事件监听
    '''

class SqliteListener(PoolListener):
    '''
    sqlite数据库连接池事件监听
    '''

class PsqlListener(PoolListener):
    '''
    postgresql  数据库连接池事件监听
    '''

def create_engine():
    '''
    根据配置文件创建引擎
    :return:
    '''
    engine_kwargs={
        "echo": False,
        'convert_unicode': True,
        "pool_size":SQLALCHEMY_DATABASE["pool_size"],
        "encoding":SQLALCHEMY_DATABASE["encoding"],
    }
    if SQLALCHEMY_DATABASE["type"]=="mysql":
        engine_kwargs["listeners"]=[MysqlListener()]
    elif SQLALCHEMY_DATABASE["type"]=="sqlite":
        engine_kwargs["listeners"]=[SqliteListener()]
    elif SQLALCHEMY_DATABASE["type"]=="postgresql":
        engine_kwargs["listeners"]=[PsqlListener()]
    engine = sqlalchemy.create_engine(SQLALCHEMY_DATABASE["connection_uri"], **engine_kwargs)
    try:
        engine.connect()
    except Exception as e:
        if not _is_db_connection_error(e.args[0]):
            raise
        remaining = SQLALCHEMY_DATABASE["max_retry"]
        if remaining == -1:
            remaining = 'infinite'
        while True:
            msg = 'SQL connection failed. %s attempts left.'
            LOG.info(msg % remaining)
            if remaining != 'infinite':
                remaining -= 1
            time.sleep(SQLALCHEMY_DATABASE["retry_interval"])
            try:
                engine.connect()
                break
            except Exception as e:
                if (remaining != 'infinite' and remaining == 0) or not _is_db_connection_error(e.args[0]):
                    raise
    return engine

_engine=create_engine()

def get_session():
    '''
    自动跟踪对象的改变，并可以使用 flush 立即保存结果。
    :return:
    '''
    session=sqlalchemy.orm.sessionmaker(bind=_engine,class_=Session,autocommit=True,expire_on_commit=True,query_cls=Query)()
    return session
