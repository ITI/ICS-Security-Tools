#!/usr/bin/python

from libserial2pcap import Serial2PCAP

import unittest
import tempfile
import time

class TestSerial2PCAP(unittest.TestCase):

    def test_sanity(self):
        #check to see if any exceptions are raised
        s = Serial2PCAP()
    
    def test_arguments_file(self):
        #get a temp file
        t = tempfile.NamedTemporaryFile()
    
        s = Serial2PCAP()
    
        #make sure that normal operation is normal
        s.setup_file(t.name)
    
        #close the file to make it disappear from the system
        t.close()
    
        #since the file is gone this should raise and IOError
        with self.assertRaises(IOError):
            s.setup_file(t.name)
    
    def test_arguments_device(self):
        s = Serial2PCAP()
        
        #good inputs
        baudrate = [9600,19200,115200]
        parity = ['N','E','O','M','S']
        bytesize = ['5','6','7','8']
        stopbits = ['1','1.5','2']
        #device = ["/dev/ttyS0"]
        device = ["com1"]
    
        #check every combination of good inputs
        for x in baudrate:
            for p in parity:
                for b in bytesize:
                    for z in stopbits:
                        for d in device:
                            #should not throw exception
                            s.setup_device(x,p,b,z,d)
    
        #Check Bad inputs in every field
        with self.assertRaises(RuntimeError):
            s.setup_device("asdf","N","8","1","/dev/ttyS0")
        with self.assertRaises(RuntimeError):
            s.setup_device(9600,"X","8","1","/dev/ttyS0")
        with self.assertRaises(RuntimeError):
            s.setup_device(9600,"N","99","1","/dev/ttyS0")
        with self.assertRaises(RuntimeError):
            s.setup_device(9600,"N","8","10101","/dev/ttyS0")
        #Cant do this check on windows
        #with self.assertRaises(RuntimeError):
        #    s.setup_device(9600,"N","8","1","--blank--")
    
    def test_getPlugins(self):
        s = Serial2PCAP()
    
        #make sure that getPlugins returns something
        self.assertIsNotNone(s.get_plugins())
        #make sure that get plugins does not just return an empty []
        self.assertNotEqual(s.get_plugins(), [])
    
        #get the list of plugins for the next test
        plugins = s.get_plugins()
    
        #check the help output for every plugin to make sure that it does not return none or ""
        for plugin in plugins:
            self.assertIsNotNone(s.get_plugin_description(plugin))
            self.assertNotEqual(s.get_plugin_description(plugin), "")
    
    #this test may error in windows because it cant set wr+b on a named temp file
    def test_pipeline_file(self):
        test_data = "test_data"
    
        #create a temp file and write the test data to it
        t = tempfile.NamedTemporaryFile(mode="wr+b")
        t.write(test_data)
        t.flush()
    
        s = Serial2PCAP()
    
        #set the file options
        s.setup_file(t.name)
    
        #make sure that s.start checks for valid protocols
        with self.assertRaises(RuntimeError):		
            s.start("asdfasdfasdf")
    
        #make sure the pipeline registers as shut down
        self.assertEqual(s.get_status(), "Stopped")
    
        #start the pipeline
        self.assertEqual(s.start("byte"), True)
    
        #wait for the pipeline to process the file
        time.sleep(3)
    
        #make sure the pipeline registers as shut down
        self.assertEqual(s.get_status(), "Stopped")
    
        #check to see the available data length
        self.assertEqual(s.available_packets(), len(test_data))	
    
        for i in range(0, s.available_packets()):
            data = s.get_next_packet()["packet"]
            #make sure the pipeline returns the correct data
            self.assertEqual(data[len(data)-1], test_data[i])
    
            #make sure available packets is correct
            self.assertEqual(s.available_packets(), len(test_data)-i-1)
    
        #check the statistics output
        sat = s.getStatistics()
        self.assertEqual(sat.total_packets_captured, len(test_data))
        self.assertEqual(sat.total_bytes_captured, len(test_data))
        self.assertNotEqual(sat.running_time, 0)
    
        #make sure that the pipeline can be stopped
        self.assertEqual(s.stop(), True)
        #make sure the pipeline can be stopped twice
        self.assertEqual(s.stop(), True)
        #make sure the pipeline registers as shut down
        self.assertEqual(s.get_status(), "Stopped")
    
        #try to restart the pipeline
        self.assertEqual(s.start("byte"), True)
        #wait for the pipeline to process the file
        time.sleep(3)
        #check to see the available data length
        self.assertEqual(s.available_packets(), len(test_data))	
    
        s.stop()
    
    def test_pipeline_device(self):
        s = Serial2PCAP()
    
        s.setup_device(9600, "N", "8", "1", "com1")
    
        #make sure the pipeline registers as shut down
        self.assertEqual(s.get_status(), "Stopped")
    
        #make sure the pipeline starts
        self.assertEqual(s.start("byte"), True)
    
        #give the pipeline time to start
        time.sleep(3)
    
        #make sure the pipeline registers as Running
        self.assertEqual(s.get_status(), "Running")
    
        #make sure the pipeline complains about being started twice
        self.assertEqual(s.start("byte"), False)	
    
        #make sure the pipeline registers as Running
        self.assertEqual(s.get_status(), "Running")
    
        #make sure that the pipeline can be stopped
        self.assertEqual(s.stop(), True)
        #make sure the pipeline can be stopped twice
        self.assertEqual(s.stop(), True)
    
        #make sure the pipeline registers as shut down
        self.assertEqual(s.get_status(), "Stopped")
    
        if s.available_packets() != 0:
            raise RuntimeError("Something Went Wrong With The Test - Received Data On Serial Line When No Data Was Supposed To Be Received")
    
        #make sure that getNextPacket returns none when there is no data in the pipeline and the pipeline is stopped
        x1 = time.time()
        self.assertIsNone(s.get_next_packet(timeout=5))
        x2 = time.time()
        #make sure that it didn't just time out
        self.assertLess(x2 - x1, 5)

if __name__ == "__main__":
    unittest.main()
