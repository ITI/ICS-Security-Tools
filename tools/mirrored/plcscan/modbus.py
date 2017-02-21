"""
File: modbus.py
Desc: partial implementation of modbus protocol
Version: 0.1

Copyright (c) 2012 Dmitry Efanov (Positive Research)
"""

__author__ = 'defanov'

from struct import pack,unpack
import socket

from optparse import OptionGroup

import string

__FILTER = "".join([' '] + [' ' if chr(x) not in string.printable or chr(x) in string.whitespace else chr(x) for x in range(1,256)])
def StripUnprintable(msg):
    return msg.translate(__FILTER)

class ModbusProtocolError(Exception):
    def __init__(self, message, packet=''):
        self.message = message
        self.packet = packet
    def __str__(self):
        return "[Error][ModbusProtocol] %s" % self.message

class ModbusError(Exception):
    _errors = {
        0:      'No reply',
        # Modbus errors
        1:      'ILLEGAL FUNCTION',
        2:      'ILLEGAL DATA ADDRESS',
        3:      'ILLEGAL DATA VALUE',
        4:      'SLAVE DEVICE FAILURE',
        5:      'ACKNOWLEDGE',
        6:      'SLAVE DEVICE BUSY',
        8:      'MEMORY PARITY ERROR',
        0x0A:   'GATEWAY PATH UNAVAILABLE',
        0x0B:   'GATEWAY TARGET DEVICE FAILED TO RESPOND'
    }
    def __init__(self,  code):
        self.code = code
        self.message = ModbusError._errors[code] if ModbusError._errors.has_key(code) else 'Unknown Error'
    def __str__(self):
        return "[Error][Modbus][%d] %s" % (self.code, self.message)


class ModbusPacket:
    def __init__(self, transactionId=0, unitId=0, functionId=0, data=''):
        self.transactionId = transactionId
        self.unitId = unitId
        self.functionId = functionId
        self.data = data

    def pack(self):
        return pack('!HHHBB',
            self.transactionId,          # transaction id
            0,                           # protocol identifier (reserved 0)
            len(self.data)+2,            # remaining length
            self.unitId,                 # unit id
            self.functionId              # function id
        ) + self.data                    # data

    def unpack(self,packet):
        if len(packet)<8:
            raise ModbusProtocolError('Response too short', packet)

        self.transactionId, self.protocolId, length, self.unitId, self.functionId = unpack('!HHHBB',packet[:8])
        if len(packet) < 6+length:
            raise ModbusProtocolError('Response too short', packet)

        self.data = packet[8:]

        return self

class Modbus:
    def __init__(self, ip, port=502, uid=0, timeout=8):
        self.ip = ip
        self.port = port
        self.uid = uid
        self.timeout = timeout

    def Request(self, functionId, data=''):
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(self.timeout)

        sock.connect((self.ip,self.port))

        sock.send(ModbusPacket(0, self.uid, functionId, data).pack())

        reply = sock.recv(1024)

        if not reply:
            raise ModbusError(0)

        response = ModbusPacket().unpack(reply)

        if response.unitId != self.uid:
            raise ModbusProtocolError('Unexpected unit ID or incorrect packet', reply)

        if response.functionId != functionId:
            raise ModbusError(ord(response.data[0]))

        return response.data

    def DeviceInfo(self):
        res = self.Request(0x2b, '\x0e\x01\00')

        if res and len(res)>5:
            objectsCount = ord(res[5])
            data = res[6:]
            info = ''
            for i in range(0, objectsCount):
                info += data[2:2+ord(data[1])]
                info += ' '
                data = data[2+ord(data[1]):]
            return info
        else:
            raise ModbusProtocolError('Packet format (reply for device info) wrong', res)

def ScanUnit(ip, port, uid, timeout, function=None, data=''):
    con = Modbus(ip, port, uid, timeout)

    unitInfo = []
    if function:
        try:
            response = con.Request(function, data)
            unitInfo.append("Response: %s\t(%s)" % (StripUnprintable(response), response.encode('hex')))
        except ModbusError as e:
            if e.code:
                unitInfo.append("Response error: %s" % e.message)
            else:
                return unitInfo

    try:
        deviceInfo = con.DeviceInfo()
        unitInfo.append("Device: %s" % deviceInfo)
    except ModbusError as e:
        if e.code:
            unitInfo.append("Device info error: %s" % e.message)
        else:
            return unitInfo

    return unitInfo

def Scan(ip, port, options):
    res = False
    try:
        data = options.modbus_data.decode('string-escape') if options.modbus_data else ''

        if options.brute_uid:
            uids = [0,255] + range(1,255)
        elif options.modbus_uid:
            uids = [int(uid.strip()) for uid in options.modbus_uid.split(',')]
        else:
            uids = [0,255]

        for uid in uids:
            unitInfo = ScanUnit(ip, port, uid, options.modbus_timeout, options.modbus_function, data)

            if unitInfo:
                if not res:
                    print "%s:%d Modbus/TCP" % (ip, port)
                    res = True
                print "  Unit ID: %d" % uid
                for line in unitInfo:
                    print "    %s" % line

        return res

    except ModbusProtocolError as e:
        print "%s:%d Modbus protocol error: %s (packet: %s)" % (ip, port, e.message, e.packet.encode('hex'))
        return res
    except socket.error as e:
        print "%s:%d %s" % (ip, port, e)
        return res

def AddOptions(parser):
    group = OptionGroup(parser, "Modbus scanner")
    group.add_option("--brute-uid", action="store_true", help="Brute units ID", default=False)
    group.add_option("--modbus-uid", help="Use uids from list", type="string", metavar="UID")
    group.add_option("--modbus-function", help="Use modbus function NOM for discover units", type="int", metavar="NOM")
    group.add_option("--modbus-data", help="Use data for for modbus function", default="", metavar="DATA")
    group.add_option("--modbus-timeout", help="Timeout for modbus protocol (seconds)", default=8, type="float", metavar="TIMEOUT")
    parser.add_option_group(group)
