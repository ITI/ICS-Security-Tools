# ICS Protocols
## Developed as a community asset

## General / Miscellaneous Releases
* [PoC 2013 SCADA Release](https://github.com/atimorin/PoC2013) - Power of Community 2013 conference special release of ICS/SCADA toolkit

## AMI
* [Termineter](https://github.com/GrayHatLabs/john_commor_c1218) - c1218 powermeter emulator

## BACnet
* [BACpypes](https://github.com/JoelBender/bacpypes) - BACpypes provides a BACnet application layer and network layer written in Python for daemons, scripting, and graphical interfaces.

## DNP3
### Protocol Implementation
* [OpenDNP3](https://github.com/automatak/dnp3) - Opendnp3 is the de facto reference implementation of IEEE-1815 (DNP3) provided under the Apache License.
* [DNP3 Simulator](https://github.com/automatak/dnp3-simulator) - Graphical DNP3 Master/Outstation simulator
* [PIFaceRTU](https://github.com/automatak/pifacertu) - Opendnp3 running on a Raspberry Pi with Piface I/O board
* [LangSec DNP3 Parser](https://github.com/Dartmouth-Trustlab/dnp3) - Parsing DNP3 using parser combinators in C.
* [Proxyd](https://github.com/Dartmouth-Trustlab/proxy) - TCP Proxy for testing hammer based parsers (such as the DNP3 parser above)

### Fuzzing
* [AEGIS Fuzzer](https://www.automatak.com/aegis/) - Aegis™ is a smart fuzzing framework for a growing number of protocols that can identify robustness and security issues in communications software before it is deployed in a production system. **[commercial]** Early Open Source version is mirrored here: [Open-Source](aegis-opensource).

## Ethernet/IP and CIP
* [EtherNet/IP+CIP dissector for Scapy](https://github.com/scy-phy/scapy-cip-enip) - a Python library which can be used to interact with components of a network using ENIP (Ethernet/IP) and CIP (Common Industrial Protocol) protocols. It uses scapy to implement packet dissectors which are able to decode a part of the network traffic. These dissectors can also be used to craft packets, which allows directly communicating with the PLCs (Programmable Logic Controllers) of the network. [Use case](https://labs.mwrinfosecurity.com/blog/offensive-ics-exploitation-a-technical-description/)
* [Scapy implementation of DLR (Device Level Ring) protocol](https://github.com/scy-phy/scapy-dlr)
* [CPPPO - Communications Protocol Python Parser and Originator (EtherNet/IP CIP implementation)](https://github.com/pjkundert/cpppo) - Cpppo is used to implement binary communications protocol parsers. The protocol’s communication elements are described in terms of state machines which change state in response to input events, collecting the data and producing output data artifacts.
* [pycomm](https://github.com/ruscito/pycomm) - **pycomm** is a package that includes a collection of modules used to communicate with PLCs. At the moment the first module in the package is **ab_comm**. **ab_comm** is a module that contains a set of classes used to interface Rockwell PLCs using Ethernet/IP protocol. The "clx" class can be used to communicate with Compactlogix, Controllogix PLCs The "slc" can be used to communicate with Micrologix or SLC PLCs
* [pyCIP](https://github.com/cpchrispye/PyCIP) - CIP protocol implementation in Python3

## IEC 104
* [IEC Server](../tools/mirrored/iec-server/) - Software to simulate server side of systems using a telecontrol message Protocol specified in the IEC 60870-5. Original website http://area-x1.lima-city.de is down, so this has been mirrored.
* [OpenMRTS](https://sourceforge.net/projects/mrts/) - MRTS is an attempt to create open source IEC 870-5-101/104 based components for telecontrol and supervisory systems and to become a complete solution in future.
* [QTester104](https://sourceforge.net/projects/qtester104/) - This software implements the IEC60870-5-104 protocol (client side) for substation data acquisition and control via tcp/ip network using the QT UI Framework. It can be compiled on Linux and Windows platforms. It's possible to poll and view data from the substation system (RTU/concentrator) and also send commands.
* [lib60870](https://github.com/mz-automation/lib60870) - Implements IEC 60870-5-104 protocol.

## IEC 61850
### Protocol Implementation
* [libIEC61850](http://libiec61850.com/libiec61850/) - open source library for IEC 61850.
* [rapid61850](https://github.com/stevenblair/rapid61850) - Rapid-prototyping protection and control schemes with IEC 61850

### Tools
* [IEDScout](https://www.omicronenergy.com/en/products/all/secondary-testing-calibration/iedscout/noc/1/) - IEDScout provides access to 61850-based IEDs and can simulate entire Ed. {1,2} IEDs. Specifically, IEDScout lets you look inside the IED and at its communication. All data modeled and exchanged becomes visible and accessible. Additionally, IEDScout serves numerous useful tasks, which could otherwise only be performed with dedicated engineering tools or even a functioning master station. IEDScout shows an overview representing the typical workflow of commissioning, but also provides detailed information upon request. **[commercial]** Free 30 day evaluation license.

## IEEE C37.118
### Protocol Implementation
* [C37.118-2005 Spec](https://ieeexplore.ieee.org/document/1611105/) -- C37.118-2005 (deprecated). Note, this is a paid IEEE spec
* [C37.118-2011 Spec](https://ieeexplore.ieee.org/document/6111219/) -- C37.118-2011 (current). Note, this is a paid IEEE spec
* [pyMU](https://github.com/iti/pymu) - Python C37.118-2011 parser
* [pyPMU](https://github.com/iicsys/pypmu) - WIP Python implementation
* [Wireshark Dissector](https://github.com/boundary/wireshark/blob/master/epan/dissectors/packet-synphasor.c) - Implemented C37.118 wireshark dissector
* [Grid Solutions Framework C37.118](https://github.com/GridProtectionAlliance/gsf/tree/master/Source/Libraries/GSF.PhasorProtocols/IEEEC37_118) - GSF implementation (.net)
* [LangSec C37.118 Parser](https://github.com/Dartmouth-Trustlab/C37.118PMU) - LangSec based C37.118 parser

### Tools
* [pyMU](https://github.com/iti/pymu) - Python C37.118-2011 parser
* [pyPMU](https://github.com/iicsys/pypmu) - WIP Python implementation
* [PMU Connection Tester](https://github.com/GridProtectionAlliance/PMUConnectionTester) - Full fledged PMU connection tester, speaking c37.118 amongst many other synchrophasor protocols

## Modbus
### Protocol Implementation
* [pyModBus](https://github.com/bashwork/pymodbus) - A full modbus protocol written in python.
* [ Modbus for Go](https://github.com/goburrow/modbus) - Fault-tolerant implementation of modbus protocol in Go (golang)
* [ModbusPal](http://modbuspal.sourceforge.net) - ModbusPal is a MODBUS slave simulator. Its purpose is to offer an easy to use interface with the capabilities to reproduce complex and realistic MODBUS environments. Mirror available [here](../tools/mirrored/modbuspal/).
* [SMOD](https://github.com/enddo/smod) - MODBUS Penetration Testing Framework. smod is a modular framework with every kind of diagnostic and offensive feature you could need in order to pentest modbus protocol. It is a full Modbus protocol implementation using Python and Scapy. This software could be run on Linux/OSX under python 2.7.x.

### Fuzzing
* [AEGIS Fuzzer](https://www.automatak.com/aegis/) - Aegis™ is a smart fuzzing framework for a growing number of protocols that can identify robustness and security issues in communications software before it is deployed in a production system. **[commercial]** Early Open Source version is mirrored here: [Open-Source](aegis-opensource).

## PROFINET
### Protocol Implementation
* [Profinet - Python](https://github.com/devkid/profinet) - Simple PROFINET implementation in python
* [Profinet - C](https://github.com/kprovost/libs7comm) - PROFINET implementation in C
* [Profinet Explorer](https://sourceforge.net/projects/profinetexplorer/) - Simple PROFINET explorer written in C#

### Fuzzing
* [ProFuzz](https://github.com/HSASec/ProFuzz) - Simple PROFINET fuzzer based on Scapy

## SEL Fast Message
* [Wireshark Dissector - SEL Fast Message](https://github.com/boundary/wireshark/blob/master/epan/dissectors/packet-selfm.c) - Wireshark Dissector for SEL Fast Message
* [Grid Solutions Framework SEL Fast Message](https://github.com/GridProtectionAlliance/gsf/tree/master/Source/Libraries/GSF.PhasorProtocols/SelFastMessage) - GSF implementation (.net)
* [SEL Applications Guides](https://www.selinc.com) - Look up AG95-10 and AG2002-14 product codes.

## Siemens S7
* [Snap7](http://snap7.sourceforge.net/) - open source Siemens S7 communication library.
* [LibNoDave](http://libnodave.sourceforge.net/) - Another (less complete) open source communication library for the S7 protocol.
* [S7comm](http://sourceforge.net/projects/s7commwireshark/) - open source Wireshark dissector plugin for the Siemens S7 protocol.
* [Python Snap7 Wrapper](https://github.com/gijzelaerr/python-snap7) - A Python wrapper for the snap7 PLC communication library
* [Bro-IDS S7 Protocol Parser](https://github.com/dw2102/S7Comm-Analyzer) - S7 protocol parser for Bro IDS

## TriStation
* [FireEye TriStation Wireshark Dissector](https://github.com/stvemillertime/TriStation) - reverse engineered wireshark dissector from Mandiant/FireEye team after Triton discovery.
* [Nozomi TriStation Wireshark Dissector](https://github.com/NozomiNetworks/tricotools) - another TriStation dissector, this time from Nozomi, also incldues pcap, and basic honeypot simulator.

## Zigbee
* [Killerbee](https://github.com/riverloopsec/killerbee) - IEEE 802.15.4/ZigBee Security Research Toolkit.

## General Protocol Fuzzing
* [AFL](http://lcamtuf.coredump.cx/afl/) - American fuzzy lop is a security-oriented fuzzer that employs a novel type of compile-time instrumentation and genetic algorithms to automatically discover clean, interesting test cases that trigger new internal states in the targeted binary.

(creative commons license)
