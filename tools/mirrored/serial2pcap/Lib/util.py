#!/usr/bin/python

import logging
import time

def SetUpLogging(defaultLevel):
    logger = logging.getLogger("serial2pcap")
    
    logging.Formatter.converter = time.gmtime
    logging.basicConfig(format='[%(levelname)s] %(message)s', datefmt='%m/%d/%Y %H:%M:%S UTC', level=defaultLevel)
    
    logger.debug("Set Logging Level To: " + repr(defaultLevel))
    