#!/usr/bin/python

import time
import os
import signal

def BlankConsumer(datastorage, stopevent):
    while not stopevent.isSet():
        if len(datastorage.raw) > 1:
            datastorage.pop_front(1)
        else:
            datastorage.newdata.clear()
            #let other threads exit
            time.sleep(1)
            #simulate a ctrl-c event so that the higher level thread will exit
            os.kill(os.getpid(), signal.SIGINT)
            #need to break so that this thread doesn't try to catch the keyboard interrupt
            break
    