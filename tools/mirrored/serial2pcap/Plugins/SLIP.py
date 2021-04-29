#!/usr/bin/python
"""
This plugin will identify the SLIP (Serial Line IP) Protocol
"""

from PluginCore import PluginCore
import struct

class SLIP(PluginCore):
    ProtocolName = "slip"
    ProtocolDescription = "Serial Line IP"
    
    PluginDataLinkType = 8 #DLT_SLIP
    
    def OutputCallback(self, seconds, mseconds, data, Unused):
        #+----------------------+
        #|	Direction	|
        #|	(1 Octet)	|
        #+----------------------+
        #|	Packet Type	|
        #|	(1 Octet)	|
        #+----------------------+
        #|Compression Info	|
        #|	(14 Octets)	|
        #+----------------------|
        #|	Payload		|
        #.			.
        #.			.
        #.			.
    
        return struct.pack("BBHIII", 0,int("40",16),0,0,0,0) #2 CHRS + 14 Octets (96 bits) - \x40 for an unmodified IP Diagram
    
    def Identify(self, data, capture_info):
        ESC = "\xDB"
        END =  "\xC0"
    
        if len(data) == 0:
            return (PluginCore.Status.UNKNOWN,0)
    
        #Scan to find the END character
        for i in range(0, len(data)-1):
            if data[i] == END:
                if i > 0 and data[i-1] != ESC:
                    return (PluginCore.Status.OK,i+1)
        
        return (PluginCore.Status.UNKNOWN,0)
    