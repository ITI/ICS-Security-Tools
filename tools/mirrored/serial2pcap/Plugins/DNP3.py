#!/usr/bin/python
"""
This plugin implements identifying the dnp3 protocol for serial2pcap.
"""

from PluginCore import PluginCore

class DNP3(PluginCore):
    ProtocolName = "dnp3"
    ProtocolDescription = "Plugin to detect serial dnp3"
    
    def Identify(self, data, capture_info):
        #first, check to make sure that there are at least 4 bytes in the array, if there is not then return TOOSHORT
        if len(data) <= 3:
            return (PluginCore.Status.TOOSHORT,0)	
        
        #next check to see if the first two bytes are 0x0564, which is the header for dnp3
        if data[0] == "\x05" and data[1] == "\x64":
    
            #if the first two bytes are 0x0564 then scan the rest of the array to find the next 0x0564. This is how the plugin determines the length of the current dnp3 packet
            #calculate how much data to scan
            for i in range(2, len(data)-1):
                #scan for 0x0564
                if data[i] == "\x05" and data[i+1] == "\x64":
                    #if its found then return OK with the position of the second 0x0564 which is also the length of the first packet
                    return (PluginCore.Status.OK,i)		
    
            #if another 0x0564 cant be found then there is not enough data
            return (PluginCore.Status.TOOSHORT,0)			
    
        #if the first two bytes are not 0x0564 then return UNKNOWN
        return (PluginCore.Status.UNKNOWN,0)
    