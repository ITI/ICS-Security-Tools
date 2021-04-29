#!/usr/bin/python

import threading
import time

class FileNameTracking:
    def __init__(self):
        pass

    filecount = 0
    
    def GetNewFileHandle(self, OriginalFileName, mode="wb"):
        self.filecount += 1
    
        return open(OriginalFileName + "." + str(self.filecount), mode)
        
    
    
class CaptureInformation:
    def __init__(self):
        pass

    backing = None
    baudrate = None
    bytesize = None
    parity = None
    stopbits = None
    
class CaptureDataBuffer:
    lock = None
    newdata = None
    raw = None
    
    total_packets_captured = 0
    total_bytes_captured = 0
    start_time = 0.0
    file_split_size = 0
    
    capture_info = None
    
    def __init__(self):
        self.lock = threading.Lock()
        self.newdata = threading.Event()
        self.raw = []
    
        self.capture_info = CaptureInformation()
    
        self.start_time = time.time()
    
    def pop_front(self, count):
        if count >= len(self.raw):
            self.raw = []
    
        else:
            for i in range(0, count):
                self.raw.pop(0)
    
    def inc_packet_counter(self, count=1):
        self.total_packets_captured += count
    
    def inc_bytes_counter(self, count=1):
        self.total_bytes_captured += count
    
    def DoSplit(self, filehandle):
        if self.file_split_size != 0 and filehandle.tell() >= self.file_split_size:
            return True
        else:
            return False
    
class CaptureDataBufferSimple:
    lock = None
    raw = None
    
    def __init__(self):
        self.lock = threading.Lock()
        self.raw = []
    
    