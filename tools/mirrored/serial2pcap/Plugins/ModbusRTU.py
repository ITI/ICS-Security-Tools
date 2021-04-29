#!/usr/bin/python
"""
This plugin implements identifying the modbusRTU protocol for serial2pcap.

Modbus RTU Frame Format:
    Name		Length (bits)	Function
    Start		28		At least 3.5 (28 bits) character times of silence
    Address		8
    Function	8
    Data		n*8
    CRC		16
    End		28		At Least 3.5 (28 bits) character times of silence between frames

This plugin identifies ModbusRTU frames by matching data to CRC's. The plugin forward slices through received data (up to 256 bytes - max RTU ADU size) and computes the data so far to the next two bytes. If a CRC match is found then the plugin assumes that it has found a valid RTU frame.
"""

from PluginCore import PluginCore
from ctypes import c_ushort

class ModbusRTU(PluginCore):
    ProtocolName = "modbusRTU"
    ProtocolDescription = "Modbus RTU Frame Format Serial Protocol"
    
    crc16_tab = []
    
    crc16_constant = 0xA001
    
    def __init__(self):
        if not len(self.crc16_tab):
            self.init_crc16()
    
    #CRC code derived and modified from PyCRC - Github cristianav/PyCRC - GPLv3 license
    #https://github.com/cristianav/PyCRC/blob/master/PyCRC/CRC16.py
    def calculate(self, input_data):
        is_string = isinstance(input_data, str)
        is_bytes = isinstance(input_data, (bytes, bytearray))
    
        #if not is_string and not is_bytes:
        #	raise Exception("input data type is not supported")
    
        crc_value = 0xFFFF
    
        for c in input_data:
            d = ord(c)
            tmp = crc_value ^ d
            rotated = crc_value >> 8
            crc_value = rotated ^ self.crc16_tab[(tmp & 0x00ff)]
    
        #added this to rotate the bytes. RTU transmits CRC in a different endian
        crc_low = crc_value & 255
        crc_high = crc_value >> 8
    
        return (crc_low << 8) ^ crc_high
    
    def init_crc16(self):
        for i in range(0,256):
            crc = c_ushort(i).value
            for j in range(0,8):
                if crc & 0x0001:
                    crc = c_ushort(crc >> 1).value ^ self.crc16_constant
                else:
                    crc = c_ushort(crc >> 1).value
            self.crc16_tab.append(crc)
    #end derived code
    
    def Identify(self, data, capture_info):	
        #sizes do not include 2 byte checksum	
        LOWER_SLICE_LIMIT = 6 #min Modbus RTU Size 8
        UPPER_SLICE_LIMIT = 254 #max Modbus RTU Size 256
    
        #if not enough data then wait
        if len(data) <= LOWER_SLICE_LIMIT:
            return (PluginCore.Status.TOOSHORT,0)
    
        sliceat = LOWER_SLICE_LIMIT
        while sliceat <= UPPER_SLICE_LIMIT:
            #make sure there is enough data
            if len(data) < sliceat + 2:
                return (PluginCore.Status.TOOSHORT,0)
    
            #calculate CRC at slice
            calc_crc = self.calculate(data[:sliceat])
            #get test CRC from data
            recv_crc = (ord(data[sliceat]) << 8) ^ ord(data[sliceat + 1])
    
            #check to see if calculated and received CRC match - if so then assume good packet
            if calc_crc == recv_crc:
                return (PluginCore.Status.OK,sliceat+2)

            sliceat += 1
    
        #if no packet was found then signal unknown
        return (PluginCore.Status.UNKNOWN,0)
    