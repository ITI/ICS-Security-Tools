#!/usr/bin/python

from PluginCore import PluginCore

import struct

class SELFM(PluginCore):
    
    ProtocolName = "selfm"
    ProtocolDescription = "SEL Fast Message"
    
    selfm_codes = [
    "\xA5\x46", #??? Fast SER Block ???
    "\xA5\xC0",
    "\xA5\xC1",
    "\xA5\xC2",
    "\xA5\xC3", #???
    "\xA5\xCE",
    "\xA5\xCF",
    "\xA5\xDC", #Old Standard Fast Meter
    "\xA5\xDA", #Old Extended Fast Meter
    "\xA5\xD1", #Regular Fast Meter
    "\xA5\xD2", #Demand Fast Meter
    "\xA5\xD3", #Peak Demand Fast Meter
    "\xA5\xE0",
    "\xA5\xE3",
    "\xA5\xE5",
    "\xA5\xE6",
    "\xA5\xE7",
    "\xA5\xE8",
    "\xA5\xE9",
    "\xA5\xB2",
    "\xA5\xB5",
    "\xA5\xB9",
    "\xA5\x60",
    #Other codes added in __init__
    ]
    
    data_stream_msg = [
    "ID", #FID and TRMID
    "ENA", #Short Event Packet Data
    "DNA", # Digital I\O
    "BNA", #Status Bits
    "CST", #???
    "SNS",
    "ACC",
    "2AC",
    "CAL",
    "BAC",
    "MET",
    "OPE",
    "CLO",
    "SET",
    "SHO",
    "SER",
    "HIS",
    "PAS",
    "PUL",
    "TAR",
    "PORT",
    "VER",
    ]
    
    #init function fills out the rest of the selfm_codes array. 
    def __init__(self):
        #add codes 0xA561 - 0xA56C - Previous Event Reports - 0xA561 is the previous event report 0xA56C is the 12th oldest report
        for i in range(97,108+1):
            self.selfm_codes.append(struct.pack("BB", 165, i))
    
        #add codes 0xA56D - 0xA59F - Previous Event Reports for relays that save up to 64 event reports
        for i in range(109,159+1):
            self.selfm_codes.append(struct.pack("BB", 165, i))
    
    #Merge function is used to take an array of bytes (eg. Data = ["\x00", "\x01", "\x02"]) and turn it into a block of that data (eg. Data = "\x00\x01\x02")
    def Merge(self, inbytes):
        outbytes = ""
        for inbyte in inbytes:
            outbytes = outbytes + inbyte
    
        return outbytes
    
    #scans through the selfm_codes and determines if the passed in bytes are a recognized code
    def IsSelfmCode(self, byte0, byte1):
        __bytes = byte0 + byte1
    
        for code in self.selfm_codes:
            if code == __bytes:
                return True
        return False
    
    #scans through the data_stream_msg and determines if the bytes passed in are a valid data stream message
    def IsDSM(self, inbytes):
        if len(inbytes) < 4:
            return False
    
        for msg in self.data_stream_msg:
            if self.Merge(inbytes[:3]) == msg:
                return True
    
        return False
    
    #convert from a sel length byte to an integer representation of that length
    def SelfmLength(self, inbyte):
        return int(struct.unpack("B", inbyte)[0])
    
    def Identify(self, data, capture_info):
        if len(data) < 4:
            return (PluginCore.Status.TOOSHORT,0)
    
        #try to identify ASCII Data Stream Messages
        if data[3] == "\x0D" and self.IsDSM(data): #\x0D == Carriage Return
            return (PluginCore.Status.OK,4)
    
        if self.IsSelfmCode(data[0], data[1]):
            #see if the next set of bytes is also a selfm code, if it is then the first two bytes are a packet
            if self.IsSelfmCode(data[2], data[3]) or self.IsDSM(data[2:]):
                return (PluginCore.Status.OK,2)
    
            #try to determine the length
            else:
                pack_len = self.SelfmLength(data[2])
    
                #if there's not enough data then return tooshort
                if len(data) < pack_len:
                    return (PluginCore.Status.TOOSHORT,0)
    
                #scan to see if there's another selfm status code in the packet
                for i in range(3, pack_len-1):
                    if self.IsSelfmCode(data[i], data[i+1]) or self.IsDSM(data[i:]):
                        return (PluginCore.Status.INVALID,i)
    
                #otherwise return the length
                if pack_len != 0:
                    return (PluginCore.Status.OK,pack_len)
    
        #implied else
        return (PluginCore.Status.UNKNOWN,0)
    