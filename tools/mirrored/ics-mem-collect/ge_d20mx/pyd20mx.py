import binascii
import collections
import serial
import socket
import struct
import time
import re

page_size = 0x1000
MEM_SIZE = 0x40000000


def page_align(addr):
    return addr & 0xFFFFF000

re_prompt = re.compile("^D20M>", re.MULTILINE)

class fd_d20mx_cache(object):
    def __init__(self):
        self.f_cache = open("d20mx.cache", "r+b", 0)
        self.f_cache.seek(0)
        self.f_cache.truncate(MEM_SIZE)
        self.f_status = open("d20mx.cache.status", "r+b", 0)
        self.f_status.seek(0)
        self.f_status.truncate(MEM_SIZE / page_size)

    def _page_num(self, addr):
        return page_align(addr) / page_size

    def set_page_cached(self, addr, status):
        if page_align(addr) != addr:
            print "page unaligned addr"
            return False
        if status not in [True, False]:
            print "status error, must be true or false"
            return False
        else:
            # print "writing status for %08x to %s" % (addr, status)
            self.f_status.seek(self._page_num(addr))
            b = struct.pack("B", 1 if status == True else 0)
            self.f_status.write(b)
            self.f_status.flush()
            return True

    def is_page_cached(self, addr):
        self.f_status.seek(self._page_num(addr))
        # print "Checking page %08x located at %08x" % (self.f_status.tell(), addr)
        data = self.f_status.read(1)
        status = True if data == '\x01' else False
        return status

    def cache_page(self, addr, bytez):
        if len(bytez) != page_size:
            print "Not enough bytez, expected 0x%08x got 0x%08x" % (page_size, len(bytez))
            return
        if page_align(addr) != addr:
            print "addr was not page aligned"
            return
        tell = self.tell()
        self.seek(addr)
        self.f_cache.write(bytez)
        self.seek(tell)
        self.set_page_cached(addr, True)
        return True

    def read(self, n=-1):
        if self.is_page_cached(page_align(self.f_cache.tell())):
            return self.f_cache.read(n)
        else:
            print "ATTEMPT TO READ UNPAGED MEM"

    def write(self, b):
        self.f_cache.write(b)

    def seek(self, offset, whence=0):
        return self.f_cache.seek(offset, whence)

    def tell(self):
        return self.f_cache.tell()


class fd_d20mx_transport(object):
    def __init__(self):
        self.mem = fd_d20mx_cache()

    def seek(self, offset, whence=0):
        self.mem.seek(offset, whence)

    def tell(self):
        return self.mem.tell()

    def read(self, length):
        pages = [x for x in range(page_align(self.tell()), self.tell() + length, page_size)]
        for page in pages:
            if self.mem.is_page_cached(page):
                pass
                #print "[!] [%08x] PAGE CACHED" % page_align(self.tell())
            else:
                #print "[X] [%08x} PAGE NOT CACHED" % page_align(self.tell())
                self._cache_page(page)
        data = self.mem.read(length)
        return data
    def write(self, data):
        self.mem.write(data)

class fd_d20mx_serial(fd_d20mx_transport):
    def __init__(self, **parameters):
        super(fd_d20mx_serial, self).__init__()
        parameters['port'] = None  # "/dev/ttyUSB0"
        parameters['baudrate'] = 115200
        parameters['xonxoff'] = True

        # Initalize the serial device
        self.ser = serial.Serial(**parameters)
        self.ser.write("\r\n")
        time.sleep(1)
        prompt = self.ser.read(self.ser.in_waiting)
        print prompt
        if "D20M>" not in prompt:
            raise Exception("Invalid prompt")
            # Create cache

    def _cache_page(self, addr):
        self.ser.write("d %08x %08x\r\n" % (page_align(self.mem.tell()), page_align(self.mem.tell()) + page_size))
        self.ser.read_until("\r\n")
        udata = self.ser.read_until("D20M>")
        bdata = ''
        print udata
        if "data access" in udata:
            print "GUARD PAGE"
        else:
            for line in udata.splitlines():
                if line.startswith("value"): continue
                if line.startswith("D20M>"): continue
                if line == "": continue
                bdata += line[9:9 + 2 + 3 * 16].replace(" ", "")
            bytez = binascii.unhexlify(bdata)
            self.mem.cache_page(addr, bytez)
            # print "CACHING PAGE"


class fd_d20mx_tcp(fd_d20mx_transport):
    def __init__(self, s=None):
        super(fd_d20mx_tcp, self).__init__()
        self.s = s
        if not s:
            self.s = socket.socket()
        self.s.settimeout(60)
        if not s:
            self.s.connect(("127.0.0.1", 4444))
            print "Connected to loopback:4444"
        self.s.send("\r\n")
        time.sleep(1)
        data = ''
        for i in range(10):
            self.s.send("\r\n")
            try:
                data += self.s.recv(1000)
            except socket.timeout as e:
                print e.message
            if "D20M>" in data:
                break
        else:
            raise Exception("Invalid prompt")

        self.mem = fd_d20mx_cache()

    def _cache_page(self, addr):
        print "[X] [%08x] Caching page" % page_align(addr)
        #print "d %08x %08x\r\n" % (page_align(self.mem.tell()), page_align(self.mem.tell()) + page_size)
        self.s.send("d %08x %08x" % (page_align(self.mem.tell()), page_align(self.mem.tell()) + page_size))
        time.sleep(1)
        self.s.recv(1000)
        self.s.send("\r\n")

        udata = ''
        #udata = self.s.recv(1000)
        #udata = udata[udata.index("\r\n"):]
        while True:
            udata += self.s.recv(1000)
            if re_prompt.search(udata):
                #udata = udata[:udata.index("D20M>")]
                break
        bdata = ''
        if "data access" in udata:
            print "GUARD PAGE"
        else:
            for line in udata.splitlines():
                if line.startswith("value"): continue
                if line.startswith("d "): continue
                if line.startswith("D20M>"): continue
                if line == "": continue
                if len(bdata) % 2: break
                match = re.match("[0-9A-F]{8} (([0-9a-f]{2} ( )?){16})", line)
                if match:
                    print "GROUP", match.group(1)
                bdata += line[9:9 + 2 + 3 * 16].replace(" ", "")
            bytez = binascii.unhexlify(bdata)
            self.mem.cache_page(addr, bytez)
    def _write(self, data):
        print "[X] [%08x] Edit mem" % addr
        #print "d %08x %08x\r\n" % (page_align(self.mem.tell()), page_align(self.mem.tell()) + page_size)
        #self.s.send("eds\r\n")
        #self.s.send("c\r\n")
        #self.s.recv(1000)
        #self.s.send("f %08x,1" % (page_align(self.mem.tell()), page_align(self.mem.tell()) + page_size))
        #self.s.send("\r\n")
        #send_data = '\r\n'.join(["%X" % x for x in data])
        #print send_data
        #print "."
        #self.s.send(".")
        #self.s.send("quit")
        #self.s.send("3")
        #try:
        #    while True:
        #        self.s.recv(1000)
        #except:
        #    print "recvd all"
        #    pass
        #
        #self.s.send("\r\n")
        #rdata = self.s.recv(1000)
        #groups = prompt.match(rdata)
        #if groups is None:
        #    raise Exception("Bad prompt returning from write")
        self.mem.write(data)



