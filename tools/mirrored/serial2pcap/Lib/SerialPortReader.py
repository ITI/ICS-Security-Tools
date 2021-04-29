#!/usr/bin/python

import time
import serial
import sys
import threading
import struct
import logging

from CaptureDataBuffer import CaptureDataBuffer
from CaptureDataBuffer import FileNameTracking
from DumpTimeFile import DumpTimeFile

#class that inherits str and adds embedded timing information. This will be used in the output buffer thread.
class strwtime(str):
    __raw_time = None
    
    def GetTime(self):
        return self.__raw_time
    def SetTime(self, ltime):
        self.__raw_time = ltime
    
#this function serves as an intermediate between the SerialPortReader and the BufferOutput thread. This is because of the locking mechanisms that caused a performance coupling between the plugins
#and the performance of the SerialPortReader. E.g. if a plugin is slow it could acquire the lock and prevent the serial port reader from adding new data to the capture data buffer. Since this data
#has timing info added to it this delay could cause a skew in the timing information presented. This function solves this so that the serialportreader only has to compete with an optimized function 
#for run time
#   Thread1:
#   Data - > SerialPortReader -> localdatabuffer
#   Thread2:
#   localdatabuffer -> CopyHandler -> remotedatabuffer
#   					Slowdowns that happen in the remote data buffer wont affect the serial port reader
def CopyHandler(stopevent, localdatabuffer, remotedatabuffer, printtoscreen, dumpfilename, addtimestamp):
    fnt = FileNameTracking()	
    
    dumpfile = None
    dtf = None
    if dumpfilename is not None:
        dumpfile = open(dumpfilename, "wb")
        if addtimestamp == True:
            dtf = DumpTimeFile(dumpfile, localdatabuffer.capture_info)
    
    #temporary byte storage variable
    tempbyte = None
    
    while not stopevent.isSet():
        if localdatabuffer.newdata.isSet():
            #get data from the local data buffer
            localdatabuffer.lock.acquire()
            if len(localdatabuffer.raw) > 0:
                tempbyte = localdatabuffer.raw[0]
                localdatabuffer.pop_front(1)
                if len(localdatabuffer.raw) == 0:
                    localdatabuffer.newdata.clear()
            else:
                tempbyte = None
            localdatabuffer.lock.release()
    
            #move it to the remote data buffer
            if tempbyte is not None:
                remotedatabuffer.lock.acquire()
                #increment the internal counters - this controls things like statistics monitoring and file size splitting
                remotedatabuffer.inc_bytes_counter()
                remotedatabuffer.raw.append(tempbyte)
                remotedatabuffer.newdata.set()	
                remotedatabuffer.lock.release()
    
            if printtoscreen:
                sys.stdout.write(tempbyte)
                sys.stdout.flush()
    
            if dumpfile is not None:
                dumpfile.write(tempbyte)
                if addtimestamp == True:
                    dtf.AddTime2File(tempbyte.GetTime())
    
                #check to see if the output file needs to be split - if so then close the old dump file and get a new one
                if remotedatabuffer.DoSplit(dumpfile):
                    dumpfile.close()
                    dumpfile = fnt.GetNewFileHandle(dumpfilename)
    
                    #if using dump time files then reinitialize the file 
                    if addtimestamp == True:
                        dtf.Init(dumpfile, remotedatabuffer.capture_info)
    
        else:
            time.sleep(.1)
    
    if dumpfile is not None:
        dumpfile.flush()
        dumpfile.close()
    
def FileReader(databuffer, printtoscreen, dumpfilename, inputfile, stopevent, fileeof):
    count = 1

    logger = logging.getLogger("serial2pcap").getChild(__name__)
    
    fnt = FileNameTracking()
    
    dumpfile = None
    #don't need to worry about handling time stamp information here because the parent function should not allow this option
    #but this will be checked in a few lines
    if dumpfilename is not None:
        dumpfile = open(dumpfilename, "wb")	
    
    filehandle = open(inputfile, "rb")
    
    #flag to determine if the raw dump file contains time stamp information
    decode_timestamps = False
    #read the first 16 bytes of the file to see if has the special marker
    magic_data = filehandle.read(16)
    
    #check to see if the data read is the magic_data
    if magic_data is not None and len(magic_data) >= 16 and magic_data == "0123456789987654":
        decode_timestamps = True
    
        #attempt to read the capture information from the dump file
        ci = filehandle.read(8)
        #make sure that enough data was read - if  not the quit
        if len(ci) < 8:
            logger.error("Dump File With Timestamps Does Not Contain Properly Formatted Capture Information. Can Not Continue")
            stopevent.set()
            return
        else:
            #unpack the data structure
            __ci = struct.unpack("IBBBB", ci)
            __baudrate = __ci[0]
            __bytesize = __ci[1]
            __parity = __ci[2]
            __stopbits = __ci[3]
            __zero = __ci[4]
    
            #need to process parity since its encoded within the dump file
            parity = 0
            if __parity == 0:
                parity = "N"
            elif __parity == 1:
                parity = "E"
            elif __parity == 2:
                parity = "O"
            elif __parity == 3:
                parity = "M"
            elif __parity == 4:
                parity = "S"
            else:
                logger.error("Dump File With Timestamps Contains Invalid Data (Parity). Can Not Continue")
                stopevent.set()
                return
    
            #need to process stopbits since its encoded within the dump file
            stopbits = "1"
            if __stopbits == 1:
                stopbits = "1"
            elif __stopbits == 2:
                stopbits = "2"
            elif __stopbits == 3:
                stopbits = "1.5"
            else:
                logger.error("Dump File With Timestamps Contains Invalid Data (stopbits). Can Not Continue")
                stopevent.set()
                return
    
            #need to convert byte size to string
            bytesize = str(__bytesize)
            
            #set the capture information	
            databuffer.capture_info.backing = "file_timestamps"
            databuffer.capture_info.baudrate = __baudrate
            databuffer.capture_info.bytesize = bytesize
            databuffer.capture_info.parity = parity
            databuffer.capture_info.stopbits = stopbits
    
            logger.info("Raw Capture File Contains Timestamps - Using Timestamped Raw Data")
    
    #otherwise (file does not contain timestamped data) reset the file to the beginning (because the beginning contains valid data)
    #and update the capture information
    else:
        databuffer.capture_info.backing = "file"
        databuffer.capture_info.baudrate = None
        databuffer.capture_info.bytesize = None
        databuffer.capture_info.parity = None
        databuffer.capture_info.stopbits = None
    
        filehandle.seek(0)
        
    
    while True:
        #create a variable to hold the byte information
        byte = ""
    
        #read in the raw byte
        __byte = filehandle.read(1)
        #check to make sure that it actually read something (may have gotten to the end of the file)
        if __byte is None or __byte == "":
            break
    
        if decode_timestamps == True:
            #if the file has embedded timestamps then read the time information from the file
            __time = filehandle.read(8)
            #check to make sure that it actually read something (may have gotten to the end of the file)
            if __time is None or __time == "":
                break
            #convert to a datatype that can hold the time information
            byte = strwtime(__byte)
            #add the time information
            if len(str(__time)) >= 8:
                byte.SetTime(struct.unpack("d", str(__time))[0])
            else:
                break
        #file does not have embedded time stamps
        else:
            byte = strwtime(__byte)
            byte.SetTime(time.time())
    
        databuffer.raw.append(byte)
        #increment the internal counters - this controls things like statistics monitoring and file size splitting
        databuffer.inc_bytes_counter()
    
        if printtoscreen == True:
            sys.stdout.write(byte)
            sys.stdout.flush()
    
        if dumpfile is not None:
            dumpfile.write(byte)
    
            #check to see if the output file needs to be split - if so then close the old dump file and get a new one
            if databuffer.DoSplit(dumpfile):
                dumpfile.close()
                dumpfile = fnt.GetNewFileHandle(dumpfilename)
                #no need to add dump time file header information since you can't do that while reading from a file
    
    databuffer.newdata.set()
    
    #signal end of file
    fileeof.set()
    
    while not stopevent.isSet():
        if not databuffer.newdata.isSet():
            stopevent.set()
        time.sleep(.5)
    
    if dumpfile is not None:
        dumpfile.flush()
        dumpfile.close()
    
def SerialPortReader(Device, Baudrate, ByteSize, Parity, StopBits, DataStorage, printtoscreen, dumpfilename, StopEvent, addtimestamp):
    logger = logging.getLogger("serial2pcap").getChild(__name__)
    
    localParity = serial.PARITY_NONE
    localByteSize = serial.EIGHTBITS
    localStopBits = serial.STOPBITS_ONE
    
    if ByteSize == '5':
        localByteSize = serial.FIVEBITS
    elif ByteSize == '6':
        localByteSize = serial.SIXBITS
    elif ByteSize == '7':
        localByteSize = serial.SEVENBITS
    elif ByteSize == '8':
        localByteSize = serial.EIGHTBITS
    
    if Parity == 'N':
        localParity = serial.PARITY_NONE
    elif Parity == 'E':
        localParity = serial.PARITY_EVEN
    elif Parity == 'O':
        localParity = serial.PARITY_ODD
    elif Parity == 'M':
        localParity = serial.PARITY_MARK
    elif Parity == 'S':
        localParity = serial.PARITY_SPACE
    
    if StopBits == '1':
        localStopBits = serial.STOPBITS_ONE
    elif StopBits == '1.5':
        localStopBits = serial.STOPBITS_ONE_POINT_FIVE
    elif StopBits == '2':
        localStopBits = serial.STOPBITS_TWO
    
    port = serial.Serial(port=Device, baudrate=Baudrate, bytesize=localByteSize, parity=localParity, stopbits=localStopBits, interCharTimeout=None, xonxoff=False, rtscts=False, dsrdtr=False, timeout=1)
    logger.info("Waiting 3 Seconds For Serial Port To Be Ready")
    time.sleep(3)
    port.flushInput()
    logger.info("Starting Capture")
    
    #Create the local data storage instance of the CpatureDataBuffer - the CopyHandler thread will be responsible for copying data over to the real CaptureDataBuffer instance
    localdatastorage = CaptureDataBuffer()
    
    localdatastorage.capture_info.backing = "device"
    localdatastorage.capture_info.baudrate = Baudrate
    localdatastorage.capture_info.bytesize = ByteSize
    localdatastorage.capture_info.parity = Parity
    localdatastorage.capture_info.stopbits = StopBits
    
    #ensure that things downstream have access to the capture information
    DataStorage.capture_info = localdatastorage.capture_info	
    
    #fire up the copy handler thread
    copythread = threading.Thread(target=CopyHandler, args=[StopEvent, localdatastorage, DataStorage, printtoscreen, dumpfilename, addtimestamp])
    copythread.start()
    
    while not StopEvent.isSet():
        
        __byte = port.read(1)
        
        if __byte is not None and __byte != '':
            byte = strwtime(__byte)
            byte.SetTime(time.time())
    
            localdatastorage.lock.acquire()
            localdatastorage.raw.append(byte)
            #do not need to increment any counters here since this is a local copy of the data - the copy handler thread will handle the statistics
            localdatastorage.newdata.set()
            localdatastorage.lock.release()	
    
    