# -*- coding:utf-8 -*-
# CACHES = {
#     'default': {
#         'BACKEND': 'django.core.cache.backends.memcached.MemcachedCache',
#         'LOCATION': ['192.168.100.150:11211'],
#     }
# }
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql_psycopg2',
        'NAME': "",
        'USER': '',  #mysql用户名，留空则默认为当前linux用户名
        'PASSWORD': '',   #mysql密码
        'HOST': '',  #留空默认为localhost
        'PORT': '5432',  #留空默认为3306端口
        #'CONN_MAX_AGE':3600
    }
}
SQLALCHEMY_DATABASE={
    "type":"postgres",
    "connection_uri":"postgres://",#数据库连接字符串
    "encoding":"utf-8",#数据库编码字符串
    "pool_size":5, #连接线程池
    "max_retry":10, #最大重试次数
    "retry_interval":10, #重试间隔
}

SESSION_EXPIRE_AT_BROWSER_CLOSE=True
SESSION_COOKIE_AGE=60*30
SESSION_COOKIE_NAME = 'cshare_session_id'

ADMINUSER={'name':'admin','password':'password'}

LOG_LEVEL="info"
debug=True