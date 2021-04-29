#!/usr/bin/python
"""
This plugin implements identifying the modbusASCII protocol for serial2pcap.
"""

from PluginCore import PluginCore

class ModbusASCII(PluginCore):
    ProtocolName = "modbusASCII"
    ProtocolDescription = "Plugin to detect serial modbusASCII"
    
    def Identify(self, data, capture_info):
        #first, check to make sure that there are at least 4 bytes in the array, if there is not then return TOOSHORT
        if len(data) <= 6:
            return (PluginCore.Status.TOOSHORT,0)	
        
        #next check to see if the first byte is ":", which is the header for a Modbus ASCII packet
        if data[0] == ":":
    
            #if the first byte is ":" then scan the rest of the array to find the next ":". This is how the plugin determines the length of the current Modbus ASCII packet
            #calculate how much data to scan
            for i in range(6, len(data)-1):
                #scan for ":"
                if data[i] == ":":
                    #if its found then return OK with the position of the second ":" which is also the length of the first packet
                    return (PluginCore.Status.OK,i)		
    
            #if another ":" cant be found then there is not enough data
            return (PluginCore.Status.TOOSHORT,0)			
    
        #if the first byte is not ":" then return UNKNOWN
        return (PluginCore.Status.UNKNOWN,0)
    