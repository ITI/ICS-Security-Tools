#!/usr/bin/python
"""
This is the base class for all plugins. The class name that implements PluginCore should be stored in a file named the exact same. This means that if implementing class is TestPlugin,
then it should be in TestPlugin.py. PluginCore defines the minimum functions and variables that any other plugin should implement:
    ProtocolName: The short name of the protocol that the plugin parses (ex. testplugin)
    ProtocolDescription: A description of the protocol that the plugin parses. (ex. the test protocol is used with test devices)
    Identify(self, data): This is the main function that serial2pcap will call in the plugin to identify the protocol.

The identify function is called whenever serial2pcap needs to determine if a data stream is a valid packet for a specific protocol. The variable data is a python string byte array.
This means that each element in the array is a string representation of a single octet. Ex. data may look like this: data = ["\x01", "\x02", "\x03"]. The identify function should determine
if data[0] + ... + data[n] is a valid packet for the protocl it is parsing. If it is a valid packet then it should return the length of the packet (n) starting from data[0] and the status
code OK. If data[0] + ... + data[n] is not a packet then Identify should return a 0 length and the status code UNKNOWN. If the identify function determines that the data stream might be
a valid packet but the buffer is too short to make a determination then it should return the status code TOOSHORT and a 0 length.

Status.UNKNOWN - Signals to serial2pcap that data at index 0 does not represent a packet from the implemented protocol
Status.INVALID - Signals to serial2pcap that the data is a packet from the implemented protocol but the packet is invalid. This is treated the same was as signalling OK
Status.OK - Signals to serial2pcap that data at index 0 plus some number of bytes is a packet from the implemented protocol.
Status.TOOSHORT - Signals to serial2pcap that there is not enough data to make a determination. serial2pcap will wait until there is more data before calling identify again.

The identify function should return status messages and lengths in the following format:

    return(PluginCore.Status.UNKNOWN, 0)
            or
    return(PluginCore.Status.INVALID, packet_length)
            or
    return(PluginCore.Status.OK, packet_length)
            or
    return(PluginCore.Status.TOOSHORT, 0)

Plugins can override the PluginDataLinkType and the OutputCallBack function. The plugin data link type variable is used to set the data link type within the PCAP header. The default
is to use  250 or LINKTYPE_RTAC_SERIAL. Plugins can override the OutputCallback function to have data added to a pcap entry. The default is to have the RTAC Serial data structure added to the
pcap entry. The function is passed in the seconds and milliseconds of when the packet was captured as well as access to the packet data. There is an unused variable in the function call that
is there for forward compatibility. Anything the function returns is added to the front of the pcap entry, unless it returns None, then nothing is written to disk.
"""

import struct

class PluginCore():
    class Status:
        UNKNOWN = 0
        INVALID = 1
        OK = 2
        TOOSHORT = 3
    
    ProtocolName = ""
    ProtocolDescription = ""
    
    def Identify(self, data, capture_info):
        return (PluginCore.Status.UNKNOWN,0)
    
    #This can be overridden
    PluginDataLinkType = 250 #LINKTYPE_RTAC_SERIAL
    
    #This can be overridden
    #Defines the RTAC Serial Data Header Needed For RTAC Serial PCAP Packets
    def OutputCallback(self, seconds, mseconds, data, Unused):
        #build the SERIAL_RTAC Header - This has to be big endian (>) in the pack function
        #>IIBBH - seconds, mseconds, Serial Event Type = 4 = DATA_RX_END, UART Control Line State, Footer
        padding = struct.pack(">IIBBH", seconds,mseconds,4,0,0)
    
        return padding
    