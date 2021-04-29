#!/usr/bin/python

import time
import struct
import logging
import traceback

from CaptureDataBuffer import FileNameTracking

def MakePCAPHeader(filehandle, datalinktype):
    magic_number = struct.pack("I", int(0xa1b2c3d4))
    version_major = struct.pack("H", 2)
    version_minor = struct.pack("H", 4)
    thiszone = struct.pack("I", 0)
    sigfigs = struct.pack("I", 0)
    snaplen = struct.pack("I", 65535)
    network = struct.pack("I", datalinktype)
    
    filehandle.write(magic_number)
    filehandle.write(version_major)
    filehandle.write(version_minor)
    filehandle.write(thiszone)
    filehandle.write(sigfigs)
    filehandle.write(snaplen)
    filehandle.write(network)
    
    filehandle.flush()
    
#This function takes some data and creates a packet entry within a PCAP file.
#filehandle is a file handle to an open PCAP file
#data is the data to be written
#CallBackFunction is a function that can intercept the data and return data to be added to the packet - it can also modify the data if it wants
def MakePCAPEntry(filehandle, data, CallBackFunction):
    databuffer = ""
    
    for byte in data:
        databuffer += byte
    
    #retrieve the capture time from the last byte in the array
    ftime = data[len(data)-1].GetTime()
    
    #do timing calculations to separate seconds from ms
    seconds = int(ftime)
    mseconds = int((ftime - int(ftime)) * 1000000)
    
    #run the callback function - this allows programs to intercept the output before its written to disk
    callbackdata = CallBackFunction(seconds, mseconds, databuffer, None)
    
    #Build the packet header within the PCAP file - this includes the time stamp and length
    ts_sec = struct.pack("I", seconds)
    ts_usec = struct.pack("I", mseconds)
    incl_len = struct.pack("I", len(data) + len(callbackdata))
    orig_len = struct.pack("I", len(data) + len(callbackdata))
    
    #write the packet header to disk
    filehandle.write(ts_sec)
    filehandle.write(ts_usec)
    filehandle.write(incl_len)
    filehandle.write(orig_len)
    
    #if there is any callback data then write it to disk
    if callbackdata is not None:
        filehandle.write(callbackdata)
    
    #finally write the actual data to disk
    filehandle.write(databuffer)
    
    #flush the data to make sure it hits disk
    filehandle.flush()
    
#this function is used when used with libserial2pcap - it is responsible for taking identified packets from BufferOutput and storing them in a shared PCAP buffer
#pcapbuffer - internal buffer used to store processed packets and header information
#data - actual packet data that will be stored in the pcapbuffer
#CallBackFunction - function pointer from a processing plugin that needs to be called before storing data
#datalinktype - the data link type as provided from the processing plugin
def AddToPCAPBuffer(pcapbuffer, data, CallBackFunction, datalinktype):
    databuffer = ""
    
    for byte in data:
        databuffer += byte
    
    #retrieve the capture time from the last byte in the array
    ftime = data[len(data)-1].GetTime()
    
    #do timing calculations to separate seconds from ms
    seconds = int(ftime)
    mseconds = int((ftime - int(ftime)) * 1000000)
    
    #run the callback function - this allows programs to intercept the output before its written to disk
    callbackdata = CallBackFunction(seconds, mseconds, databuffer, None)
    
    #create the packetdata structure
    packetdata = {}

    packetdata["timestamp_s"] = seconds
    packetdata["timestamp_ms"] = mseconds
    packetdata["length"] = len(callbackdata)
    packetdata["dlt"] = datalinktype

    pcapbuffer.lock.acquire()
    pcapbuffer.raw.append({"pkthdr": packetdata, "packet": callbackdata + databuffer})
    pcapbuffer.lock.release()
    
def BufferOutput(datastorage, outfilename, plugin, stopevent, debug, pcapbuffer):
    logger = logging.getLogger("serial2pcap").getChild(__name__)
    if debug:
        logger.setLevel(logging.DEBUG)
    
    #this should check to make sure that either outfilename or (xor) pcap buffer are used
    #logic further down relies on one or the other being set
    if outfilename is not None and pcapbuffer is not None:
        raise RuntimeError("Can Not Use Output File And PCAP Buffer Together")
    elif outfilename is None and pcapbuffer is None:
        raise RuntimeError("Must Use Either Output File Or PCAP Buffer")
    
    fnt = FileNameTracking()
    
    unknownbuffer = []
    
    logger.info("Starting to Process Data")
    
    #this only needs to be done if using an outfile
    if outfilename is not None:
        try:
            filehandle = open(outfilename, "wb")
        except IOError:
            logger.error("Could Not Open Output File")
            stopevent.set()
            return
        MakePCAPHeader(filehandle, plugin.PluginDataLinkType)
    
    while not stopevent.isSet():
        if datastorage.newdata.isSet():
            datastorage.lock.acquire()
    
            try:
                (pluginresult,packetlength) = plugin.Identify(datastorage.raw, datastorage.capture_info)
            except Exception as e:
                logger.error("Caught Exception in the plugin, Shutting Down")
                logger.error("Exception Was: " + traceback.format_exc())
                stopevent.set()
                datastorage.lock.release()
                return
            
            if pluginresult == plugin.Status.UNKNOWN:
                logger.debug("Got Status.UNKNOWN, data was: " + datastorage.raw[0])
                unknownbuffer.append(datastorage.raw[0])
                datastorage.pop_front(1)
            
            elif pluginresult == plugin.Status.OK or pluginresult == plugin.Status.INVALID:
                #if packet length is 0 then this is probably an error. Treat it the same as an UNKNOWN event otherwise a deadlock may occur				
                if packetlength <= 0:
                    logger.debug("Got Status.OK | Status.INVALID, but the packet length was <= 0. Treating as Status.UNKNOWN")
                    unknownbuffer.append(datastorage.raw[0])
                    datastorage.pop_front(1)
    
                #otherwise process it as normal
                else:
                    if len(unknownbuffer) > 0:
                        logger.debug("Flushing Unknown Buffer To PCAP:")
                        logger.debug(unknownbuffer)
    
                        #this if/else checks to see if output needs to go to a file or the pcapbuffer
                        if outfilename is not None:
                            #check to see if the output file needs to be split
                            if datastorage.DoSplit(filehandle) == True:
                                #if so then close the original file
                                filehandle.close()
                                #get a new file handle with the new naming convention
                                filehandle = fnt.GetNewFileHandle(outfilename)
                                #make sure it gets the pcap header
                                MakePCAPHeader(filehandle, plugin.PluginDataLinkType)
    
                            MakePCAPEntry(filehandle, unknownbuffer, plugin.OutputCallback)
    
                        else:
                            #adds the unknown buffer to the pcap buffer
                            AddToPCAPBuffer(pcapbuffer, unknownbuffer, plugin.OutputCallback, plugin.PluginDataLinkType)
    
                        datastorage.inc_packet_counter()
    
                        unknownbuffer = []
                    
                    logger.debug("Got Status.OK | Status.INVALID, Packet Length: " + str(packetlength) + " flushing packet to PCAP")
                    logger.debug(datastorage.raw[:packetlength])
    
                    #this if/else checks to see if output needs to go to a file or the pcapbuffer
                    if outfilename is not None:
                        #check to see if the output file needs to be split
                        if datastorage.DoSplit(filehandle) == True:
                            #if so then close the original file
                            filehandle.close()
                            #get a new file handle with the new naming convention
                            filehandle = fnt.GetNewFileHandle(outfilename)
                            #make sure it gets the pcap header
                            MakePCAPHeader(filehandle, plugin.PluginDataLinkType)
    
                        MakePCAPEntry(filehandle, datastorage.raw[:packetlength], plugin.OutputCallback)
    
                    else:
                        #adds the identified packet to the pcap buffer
                        AddToPCAPBuffer(pcapbuffer, datastorage.raw[:packetlength], plugin.OutputCallback, plugin.PluginDataLinkType)
    
                    datastorage.inc_packet_counter()
    
                    datastorage.pop_front(packetlength)
            
            elif pluginresult == plugin.Status.TOOSHORT:
                logger.debug("Got Status.TOOSHORT")
                datastorage.newdata.clear()
                
            else:
                logger.warning("Unknown Response Code From Plugin - This Will Be Ignored") 
            
            if len(datastorage.raw) == 0:
                datastorage.newdata.clear()
    
            datastorage.lock.release()
    
        else:
            time.sleep(.1)
    
    #end of while loop
    
    #if there is any data still left after the main while loop then move it over to the unknown buffer
    for i in range(0,len(datastorage.raw)):
        unknownbuffer.append(datastorage.raw[i])
    
    #dump anything in the unknown buffer to a single packet
    if len(unknownbuffer) > 0:
        logger.debug("Flushing Unknown Buffer To PCAP:")
        logger.debug(unknownbuffer)
        #this if/else checks to see if output needs to go to a file or the pcapbuffer
        if outfilename is not None:
            #for now not splitting files at this code since the code writes one big packet and there is not fine grained control over the size
            MakePCAPEntry(filehandle, unknownbuffer, plugin.OutputCallback)
        else:
            #adds the unknown buffer to the pcap buffer
            AddToPCAPBuffer(pcapbuffer, unknownbuffer, plugin.OutputCallback, plugin.PluginDataLinkType)
        datastorage.inc_packet_counter()
        logger.info("Done Processing")
        
    