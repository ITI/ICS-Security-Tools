#!/usr/bin/python

import os
import traceback
import threading
import time
import traceback
import pkgutil

from Lib.CaptureDataBuffer import CaptureDataBuffer
from Lib.CaptureDataBuffer import CaptureDataBufferSimple
from Lib.BufferOutput import BufferOutput
from Lib.SerialPortReader import SerialPortReader
from Lib.SerialPortReader import FileReader

import logging

#Definition for INFINITE - used by time out functions with in serial2pcap
INFINITE = 0xFFFFFFFF

class Serial2PCAP():
    #common buffer shared between serial2pcap and the buffer output thread
    pcapbuffer = None

    #holds the status of the running capture - either "Stopped" or "Running"
    status = "Stopped"

    #variables that hold information about the available plugins
    pluginlist = None
    pluginlist_ref = None
    pluginlisthelp = None
    #holds the configuration settings for the capture
    args = {}

    #internal data structure that helps move data between the serialportreader/filereader and the buffer output threads
    databuffer = None
    #coordination event between all threads to coordinate shut down
    StopEvent = None
    #file end event to signal the EOF when using a file
    FileEOF = None

    #handles to the different threads
    DataReaderThread = None
    bufferoutputthread = None

    #instance of this class will be created and returned by getStatistics
    class Statistics:
        def __init__(self):
            pass

        total_packets_captured = 0
        total_bytes_captured = 0
        running_time = 0.0

    def __init__(self):
        logger = logging.getLogger("serial2pcap").getChild(__name__)

        #initialize variables
        self.pcapbuffer = CaptureDataBufferSimple()

        #temporary variables related to program plugins
        pluginlist = []
        pluginlist_ref = {}
        pluginlisthelp = ""

        ##This block of code identifies all plugins then dynamically loads them
        import Plugins
        plugins = [name for _, name, _ in pkgutil.iter_modules([os.path.dirname(Plugins.__file__)])]

        for plugin in plugins:
            #load plugins inside of a try block, so that a plugin doesn't crash the program
            try:
                #plugins need to have a class named the same as the file name for this to work (eg. test.py loads class test)
                exec("from Plugins. " + plugin + " import " + plugin) in globals()
                tempplugin = eval(plugin + "()")

                #only load the plugin if the ProtocolName has been set - this prevents things like PluginCore from being loaded
                if tempplugin.ProtocolName != "":
                    #make sure that duplicate plugin names are not being loaded
                    if tempplugin.ProtocolName not in pluginlist_ref.keys():
                        #add the plugin name to the list
                        pluginlist.append(tempplugin.ProtocolName)
                        #and the LUT
                        pluginlist_ref[tempplugin.ProtocolName] = plugin
                        #also build the help string
                        pluginlisthelp = pluginlisthelp + tempplugin.ProtocolName + ": " + tempplugin.ProtocolDescription + "\n"

                    else:
                        logger.warning("Duplicate Plugin Name \"" + tempplugin.ProtocolName + "\"")
            except:
                logger.warning("Error Loading Plugin: " + plugin)
                logger.warning("Error Message Was: " + traceback.format_exc())
                pass

        #set class variables with the temp variables
        self.pluginlist = pluginlist
        self.pluginlist_ref = pluginlist_ref
        self.pluginlisthelp = pluginlisthelp

    #returns a list of the protocol plugins available
    def get_plugins(self):
        return self.pluginlist

    #accepts a plugin name and returns the help string associated with the plugin
    def get_plugin_description(self, plugin):
        logger = logging.getLogger("serial2pcap").getChild(__name__)

        #exception handling so that the plugin does not crash the program
        try:
            plugin = eval(self.pluginlist_ref[plugin] + "()")
        except:
            logger.error("Caught Exception in the plugin")
            logger.error(traceback.format_exc())
            raise RuntimeError("Caught Exception in the plugin")

        return plugin.ProtocolDescription

    #sets up the configuration for a capture from a device
    def setup_device(self, baudrate, parity, bytesize, stopbits, device):
        #acceptable inputs
        __parity = ['N', 'E', 'O', 'M', 'S']
        __bytesize = ['5', '6', '7', '8']
        __stopbits = ['1', '1.5', '2']

        if parity not in __parity:
            raise RuntimeError("Invalid Parity")

        if bytesize not in __bytesize:
            raise RuntimeError("Invlaid bytesize")

        if stopbits not in __stopbits:
            raise RuntimeError("Invlaid stopbits")

        if not isinstance(baudrate, int):
            raise RuntimeError("Invalid baudrate")

        #Cant do this check on windows
        #if not os.path.exists(device):
        #    raise RuntimeError("Device Does Not Exist")

        self.__setup(baudrate, parity, bytesize, stopbits, device, "device")

    #sets up the configuration for a capture from a file
    def setup_file(self, filename):
        if not os.path.isfile(filename):
            raise IOError("File Not Found")

        self.__setup(None, None, None, None, filename, "file")

    #private call that is used by setup_device and setup_file to implement setting the correct variables
    def __setup(self, baudrate, parity, bytesize, stopbits, target, targettype):
        self.args['baudrate'] = baudrate
        self.args['parity'] = parity
        self.args['bytesize'] = bytesize
        self.args['stopbits'] = stopbits
        self.args['target'] = target
        self.args['targettype'] = targettype

    #this function sets up and starts the entire capture pipeline
    def start(self, protocol):
        logger = logging.getLogger("serial2pcap").getChild(__name__)

        if self.get_status() == "Running":
            return False

        if protocol not in self.get_plugins():
            raise RuntimeError("Invalid Protocol")

        self.databuffer = CaptureDataBuffer()

        self.StopEvent = threading.Event()
        self.StopEvent.clear()

        self.FileEOF = threading.Event()
        self.FileEOF.clear()

        if self.args['targettype'] == "device":
            self.DataReaderThread = threading.Thread(target=SerialPortReader, args=[self.args['target'], self.args['baudrate'], self.args['bytesize'], self.args['parity'], self.args['stopbits'], self.databuffer, None, None, self.StopEvent, None])

        elif self.args['targettype'] == "file":
            self.DataReaderThread = threading.Thread(target=FileReader, args=[self.databuffer, None, None, self.args['target'], self.StopEvent, self.FileEOF])

        else:
            raise RuntimeError

        #exception handling so that the plugin does not crash the program
        try:
            plugin = eval(self.pluginlist_ref[protocol] + "()")
        except:
            logger.error("Caught Exception in the plugin")
            logger.error(traceback.format_exc())
            raise RuntimeError("Caught Exception in the plugin")

        self.bufferoutputthread = threading.Thread(target=BufferOutput, args=[self.databuffer, None, plugin, self.StopEvent, None, self.pcapbuffer])

        self.DataReaderThread.start()
        self.bufferoutputthread.start()

        self.status = "Running"

        return True

    #stops the entire capture pipeline
    def stop(self):
        self.StopEvent.set()
        self.DataReaderThread.join()
        self.bufferoutputthread.join()

        self.status = "Stopped"

        return True

    #returns the status of the capture
    def get_status(self):
        if self.status == "Running" and self.FileEOF.isSet():
            self.status = "Stopped"

        return self.status

    #returns the next available packet
    #optional parameter timeout determines how long to wait before returning - default is infinite will not return until more data is received
    #either returns the next packet or None if it times out
    def get_next_packet(self, timeout=INFINITE):
        if self.get_status() == "Stopped" and self.available_packets() == 0:
            return None

        sleeptime = 0.25
        curtime = 0.0

        packet = None

        while True:
            self.pcapbuffer.lock.acquire()

            if len(self.pcapbuffer.raw) > 0:
                packet = self.pcapbuffer.raw.pop(0)

            self.pcapbuffer.lock.release()

            if packet is not None:
                break
            else:
                time.sleep(sleeptime)
                curtime += sleeptime

                if timeout != INFINITE and curtime >= timeout:
                    break

        return packet

    #returns the current number of available packets in the buffer
    def available_packets(self):
        self.pcapbuffer.lock.acquire()
        length = len(self.pcapbuffer.raw)
        self.pcapbuffer.lock.release()

        return length

    #returns a statistics class with statistical capture information
    def get_statistics(self):
        stats = self.Statistics()

        stats.total_packets_captured = self.databuffer.total_packets_captured
        stats.total_bytes_captured = self.databuffer.total_bytes_captured
        stats.running_time = time.time() - self.databuffer.start_time

        return stats
