#!/usr/bin/python

from __future__ import print_function
import sys
if sys.version_info.major != 2 and sys.version_info.minor != 7:
    print("serial2pcap only works with python version 2.7")
    quit()
import time
import argparse
import threading
import os
import traceback
import pkgutil

from Lib.CaptureDataBuffer import CaptureDataBuffer
from Lib.BufferOutput import BufferOutput
from Lib.SerialPortReader import SerialPortReader
from Lib.SerialPortReader import FileReader
from Lib.BlankConsumer import BlankConsumer

import logging
import Lib.util

if __name__ == "__main__":
    Lib.util.SetUpLogging(logging.INFO)
    logger = logging.getLogger("serial2pcap").getChild(__name__)
    
    pluginlist = []
    pluginlist_ref = {}
    pluginlisthelp = ""
    
    ##This block of code identifies all plugins then dynamically loads them
    import Plugins
    plugins = [name for _, name, _ in pkgutil.iter_modules([os.path.dirname(Plugins.__file__)])]
    
    for plugin in plugins:
        #load plugins inside of a try block, so that a plugin doesn't crash the program
        try:
            #plug ins need to have a class named the same as the file name for this to work (e.g.. test.py loads class test)
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
            logger.warning("Error Message Was:")
            traceback.print_exc()
            print("\n")
            pass
    
    parser = argparse.ArgumentParser()
    parser.add_argument("-p", "--protocol", choices=pluginlist, help="to see a more detailed list of protocols use --list")
    parser.add_argument("--list", action="store_true", help="print a detailed list of all available protocols")
    parser.add_argument("-o", "--outfile", help="Location of the output PCAP file to use")
    parser.add_argument("-b", "--baudrate", default='9600', help="Baud rate such as 9600 or 115200 etc.")
    parser.add_argument("-P", "--parity", default='N', choices=['N', 'E', 'O', 'M', 'S'], help="Enable parity checking. Possible values: [N]one, [E]ven, [O]dd, [M]ark, [S]pace. Default is None")
    parser.add_argument("-s", "--bytesize", default='8', choices=['5', '6', '7', '8'], help="Number of data bits. Possible values: 5, 6, 7, 8. Default is 8")
    parser.add_argument("-x", "--stopbits", default='1', choices=['1', '1.5', '2'], help="Number of stop bits. Possible values: 1, 1.5, 2, Default is 1")
    parser.add_argument("-d", "--device", help="Device name or port number number")
    parser.add_argument("--view", action="store_true", default=False, help="Print the raw capture data to the screen")
    parser.add_argument("-D", "--dumpfile", help="Output raw capture data to this file")
    parser.add_argument("-t", "--timestamp", action="store_true", help="Add timing information the the raw dump file")
    parser.add_argument("-r", "--rawinput", help="raw dump input file")
    parser.add_argument("-w", "--split", help="Split output files after capturing specified number of bytes. New files will be appended with .1, .2, etc.")
    parser.add_argument("--debug", action="store_true", default=False, help="Display debugging messages")
    parser.add_argument("--version", action="store_true", default=False, help="Display Version Information and Exit")
    args = parser.parse_args()
    
    if args.version:
        print("2.0")
        quit()
    
    #print the list of available plugins if requested
    if args.list:
        print("Available Plugins:")
        print(pluginlisthelp)
        quit()
    
    #user input needs to either specify an input file or a device to use otherwise there is nothing for the program to do
    if args.device is None and args.rawinput is None:
        print("Must Specify either --device or --rawinput\n")
        parser.print_help()
        quit()
    
    if args.outfile is None and args.dumpfile is None and args.view == False:
        print("You Did Not Specify Anything For The Program To Do. This Is Probably Not What You Wanted.\n")
        parser.print_help()
        quit()
    
    if args.dumpfile is not None and args.rawinput is not None and args.timestamp == True:
        print("You Can Not Add Time Stamp Information To A Dump File When Reading From A Dump File.\n")
        parser.print_help()
        quit()

    plugin = None
    if args.protocol is None or args.outfile is None:
        plugin = None
    #load the selected plugin
    else:
        #exception handling so that the plugin does not crash the program
        try:
            plugin = eval(pluginlist_ref[args.protocol] + "()")
        except:
            traceback.print_exc()
            print("Caught Exception in the plugin, Shutting Down")
            quit()
    
    #output file without a plugin doesn't make sense
    if plugin is None and args.outfile is not None:
        print("\nYou need to specify a protocol (-p\--protocol) for the output file (-o\--output) to do anything\n")
        quit()
    
    #make sure that the output file does not already exist, if it does then quit and tell the user about it
    if (args.outfile is not None and os.path.exists(args.outfile)) or (args.dumpfile is not None and os.path.exists(args.dumpfile)):
        print("The Output File Already Exists. Pick Another File Name")
        quit()
    
    #databuffer is the shared memory location that all threads will interact with
    databuffer = CaptureDataBuffer()
    
    if args.split is not None and args.split != 0:
        databuffer.file_split_size = int(args.split)
    
    #define the event that will coordinate shutting down all of the threads
    StopEvent = threading.Event()
    StopEvent.clear()
    
    FileEOF = threading.Event()
    FileEOF.clear()
    
    #livecapture variable will be used to determine if live capture information is available 
    livecapture = False
    #select which input thread will be used - either from a serial port or a file
    if args.rawinput is None:
        DataReaderThread = threading.Thread(target=SerialPortReader, args=[args.device, args.baudrate, args.bytesize, args.parity, args.stopbits, databuffer, args.view, args.dumpfile, StopEvent, args.timestamp])
        livecapture = True
    else:
        if args.timestamp == True:
            print("You Can Not Add Time Stamp Information To A Dump File When Reading From A Dump File.\n")
            parser.print_help()
            quit()
        DataReaderThread = threading.Thread(target=FileReader, args=[databuffer, args.view, args.dumpfile, args.rawinput, StopEvent, FileEOF])
    #start the input thread	
    DataReaderThread.start()	

    bufferoutputthread = None
    #if there is a defined output file and a defined plugin then start the output plugin
    if args.outfile is not None and plugin is not None:	
        bufferoutputthread = threading.Thread(target=BufferOutput, args=[databuffer, args.outfile, plugin, StopEvent, args.debug, None])
        bufferoutputthread.start()
    #if you're using a dump file and an actual device
    elif args.dumpfile is not None and args.device is not None:
        pass
    elif args.view is not None and args.device is not None:
        pass
    else:
        bufferoutputthread = threading.Thread(target=BlankConsumer, args=[databuffer, StopEvent])
        bufferoutputthread.start()
    
    #main thread while loop - basically just wait until the stop event is signalled
    try:
        while not StopEvent.isSet():
            if livecapture == True:
                #read(1) will cause the main thread to wait for the user to press enter, then the statistics will be displayed
                sys.stdin.read(1)
                print("Running Time: " + str(int(time.time() - databuffer.start_time)) + " seconds, Packets Captured: " + str(databuffer.total_packets_captured) + ", Bytes Captured: " + str(databuffer.total_bytes_captured) + "b")
            else:
                time.sleep(0.5)
    except KeyboardInterrupt:
        print("Shutting Down")
        StopEvent.set()
    
    #wait for the threads to close and quit
    DataReaderThread.join()

    if bufferoutputthread is not None:
        bufferoutputthread.join()
    
    sys.stdout.write("\n")
    print("Running Time: " + str(int(time.time() - databuffer.start_time)) + " seconds, Packets Captured: " + str(databuffer.total_packets_captured) + ", Bytes Captured: " + str(databuffer.total_bytes_captured) + "b")
    sys.stdout.write("\n")
    quit()
