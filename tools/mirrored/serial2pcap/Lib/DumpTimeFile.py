#!/usr/bin/python

import struct

class DumpTimeFile:
    filehandle = None
    setup = False
    
    def __init__(self, filehandle=None, capture_info=None):
        if filehandle is not None and capture_info is not None:
            self.Init(filehandle, capture_info)
    
    def Init(self, filehandle, capture_info):
        self.filehandle = filehandle
    
        #raw dump with time stamp file format:
        #0x00 header 0x01234567
        #0x08 header 0x89987654
        #0x10 Baud Rate [0-115200]
        #0x14 Byte Size [5,6,7,8]
        #0x15 Parity [None - 0, Even - 1, Odd - 2, Mark - 3, Space - 4]
        #0x16 StopBits [1 - 1, 1.5 - 3, 2 - 2]
        #0x17 Padding (0)
        #add the magic value so that the file reader can determine if the file contains time stamp information
        self.filehandle.write("0123456789987654")
        if capture_info.baudrate is not None:
            self.filehandle.write(struct.pack("I", int(capture_info.baudrate)))
        else:	
            self.filehandle.write("\xFF\xFF\xFF\xFF")
        if capture_info.bytesize is not None:
            self.filehandle.write(struct.pack("B", int(capture_info.bytesize)))
        else:
            self.filehandle.write("\xFF")
        __parity = None
        if capture_info.parity == "N":
            __parity = 0
        elif capture_info.parity == "E":
            __parity = 1
        elif capture_info.parity == "O":
            __parity = 2
        elif capture_info.parity == "M":
            __parity = 3
        elif capture_info.parity == "S":
            __parity = 4
    
        if __parity is not None:
            self.filehandle.write(struct.pack("B", __parity))
        else:
            self.filehandle.write("\xFF")
    
        __stopbits = None
        if capture_info.stopbits == "1":
            __stopbits = 1
        elif capture_info.stopbits == "1.5":
            __stopbits = 3
        elif capture_info.stopbits == "2":
            __stopbits = 2
    
        if __stopbits is not None:
            self.filehandle.write(struct.pack("B", __stopbits))
        else:
            self.filehandle.write("\xFF")
    
        self.filehandle.write(struct.pack("B",0)) 
    
        self.filehandle.flush()
    
        self.setup = True
    
    def AddTime2File(self, time):
        if not self.setup:
            raise RuntimeError("The Dump Time File Has Not Been Initialized")
    
        self.filehandle.write(struct.pack("d", time))
    
    def IsReady(self):
        return self.setup
    
    def GetFileHandle(self):
        return self.filehandle
    
    