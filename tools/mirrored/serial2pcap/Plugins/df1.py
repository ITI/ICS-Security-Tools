#!/usr/bin/python
"""
This plugin implements identifying the df1 protocol for serial2pcap. 
Reference Document Used: literature.rockwellautomation.com/idc/groups/literature/documents/rm/1770-rm516_-.en-p.pdf

DF1 Structure:
STX 02
SOH 01
ETX 03
EOT 04
ENQ 05
ACK 06
DLE 10
NAK 0F

All packets begin with DLE then are followed by another code. E.g: DLE ACK.
Naturally occurring 0x10's are escaped with DLE and look like: DLE DLE
"""

from PluginCore import PluginCore

class df1(PluginCore):
    ProtocolName = "df1"
    ProtocolDescription = "Allen Bradly DF1 Serial Protocol"
    
    df1_codes = [
    "\x02", #STX 
    "\x01", #SHO
    "\x03", #ETX
    "\x04", #EOT
    "\x05", #ENQ
    "\x06", #ACK
    "\x10", #DLE
    "\x0F", #NAK
    ]
    
    def Identify(self, data, capture_info):
        #Check to see if there is enough data to even process
        if len(data) <= 2:
            return (PluginCore.Status.TOOSHORT,0)
    
        #if the data does not start with DLE then it is not the start of a packet so return unknown
        if data[0] != "\x10":
            return (PluginCore.Status.UNKNOWN,0)
    
        #if the next bit is also a DLE then it is a DLE DLE so just ignore it
        elif data[1] == "\x10":
            return (PluginCore.Status.UNKNOWN,0)
    
        #this assumes that DLE XXX has been found (where XXX is not DLE)- check to see if it a valid DF1 code
        elif data[1] in self.df1_codes:
            #scan to find the next DLE XXX
            for i in range(2,len(data)-1):
                if data[i] == "\x10" and data[i+1] != "\x10" and data[i+1] in self.df1_codes:
                    return (PluginCore.Status.OK,i)
            
            #if a DLE XXX is not found then return too short
            return (PluginCore.Status.TOOSHORT,0)
    