__author__ = 'min'
try:
    import sys,logging
    sys.path.append("/etc/cshare")
    from local_settings import *
    ImportFromEtcSuccess=True
except ImportError:
    ImportFromEtcSuccess=False
    logging.warning("Error import local_settings from /etc/cshare")

try:
    if not ImportFromEtcSuccess:
        from local.local_settings import *
except ImportError:
    logging.warning("No local_settings file found.")