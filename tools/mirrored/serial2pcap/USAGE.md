## Usage

serial2pcap.py [-h] [-p {modbusASCII,byte,dnp3,selfm,df1,modbusRTU}]
                      [--list] [-o OUTFILE] [-b BAUDRATE] [-P {N,E,O,M,S}]
                      [-s {5,6,7,8}] [-x {1,1.5,2}] [-d DEVICE] [--view]
                      [--dumpfile DUMPFILE] [-t] [-r RAWINPUT] [-w SPLIT]
                      [--debug]

optional arguments:
  
  -h, --help            show this help message and exit
  
  -p {modbusASCII,byte,dnp3,selfm,df1,modbusRTU}, --protocol {modbusASCII,byte,dnp3,selfm,df1,modbusRTU}
                        to see a more detailed list of protocols use --list
  
  --list                print a detailed list of all available protocols
  
  -o OUTFILE, --outfile OUTFILE
                        Location of the output PCAP file to use
  
  -b BAUDRATE, --baudrate BAUDRATE
                        Baud rate such as 9600 or 115200 etc.
  
  -P {N,E,O,M,S}, --parity {N,E,O,M,S}
                        Enable parity checking. Possible values: [N]one,
                        [E]ven, [O]dd, [M]ark, [S]pace. Default is None
  
  -s {5,6,7,8}, --bytesize {5,6,7,8}
                        Number of data bits. Possible values: 5, 6, 7, 8.
                        Default is 8
  
  -x {1,1.5,2}, --stopbits {1,1.5,2}
                        Number of stop bits. Possible values: 1, 1.5, 2,
                        Default is 1
  
  -d DEVICE, --device DEVICE
                        Device name or port number number
  
  --view                Print the raw capture data to the screen
  
  --dumpfile DUMPFILE   Output raw capture data to this file
  
  -t, --timestamp       Add timing information the the raw dumpfile
  
  -r RAWINPUT, --rawinput RAWINPUT
                        raw dump input file
  
  -w SPLIT, --split SPLIT
                        Split output files after capturing specified number of
                        bytes. New files will be appended with .1, .2, etc.
  
  --debug               Display debugging messages


At any time while the program is running, press the enter key to display live statistics about the running capture (running time, packets captured, and bytes captured)

Press ctrl-c to stop the program



## Supported Protocols
modbusASCII: Plugin to detect serial modbusASCII

byte: Single Byte - Generates A Packet For Every Byte

dnp3: Plugin to detect serial dnp3

selfm: SEL Fast Message

df1: Allen Bradly DF1 Serial Protocol

modbusRTU: Modbus RTU Frame Format Serial Protocol - Currently Only Works Well With 9600 and 19200 Baud Rate On Non Merged Taps


## Example Usage
Dump Raw Data To A File: 
serial2pcap.py -d [device] --dumpfile [filename]

Dump Raw Data To A File With Embedded Time Information:	
serial2pcap.py -d [device] --dumpfile [filename] -t

View Raw Data: 
serial2pcap.py -d [device] --view

Dump Raw Data To PCAP When Raw Data Protocol Is DNP3:
serial2pcap.py -d [device] -p dnp3 -o [pcap filename]

Create A PCAP File and Split The PCAP File Every 1MB Captured:
serial2pcap.py -d [device] -p [protocol] -o [pcap filename] -w 1000000

Create A Dump File and Split The File Every 1MB Captured:
serial2pcap.py -d [device] --dumpfile [filename] -w 1000000

Convert A Dump File With Timestamps (-t) To A Raw Dump File:
serial2pcap.py -r [infile] --dumpfile [outfile]


