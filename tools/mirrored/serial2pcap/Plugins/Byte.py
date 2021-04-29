#!/usr/bin/python
"""
This plugin will identify every byte as a valid packet. It is used to store every byte into PCAP format
"""

from PluginCore import PluginCore

class Byte(PluginCore):
    ProtocolName = "byte"
    ProtocolDescription = "Single Byte - Generates A Packet For Every Byte"
    
    def Identify(self, data, capture_info):
        if len(data) > 0:
            return (PluginCore.Status.OK,1)
        else:
            return (PluginCore.Status.UNKNOWN,0)
    