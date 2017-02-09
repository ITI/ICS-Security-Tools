# ICS Protocols
## Developed as a community asset

## AMI
* [Termineter](https://github.com/GrayHatLabs/john_commor_c1218) - c1218 powermeter emulator

## BACnet
* [BACpypes](https://github.com/JoelBender/bacpypes) - BACpypes provides a BACnet application layer and network layer written in Python for daemons, scripting, and graphical interfaces.


## DNP3
### Protocol Implementation
* [OpenDNP3](https://github.com/automatak/dnp3) - Opendnp3 is the de facto reference implementation of IEEE-1815 (DNP3) provided under the Apache License.
* [DNP3 Simulator](https://github.com/automatak/dnp3-simulator) - Graphical DNP3 Master/Outstation simulator
* [PIFaceRTU](https://github.com/automatak/pifacertu) - Opendnp3 running on a Raspberry Pi with Piface I/O board
* [Langsec DNP3 Parser](https://github.com/pesco/dnp3) - Parsing DNP3 using parser combinators in C.

### Fuzzing
* [AEGIS Fuzzer](https://www.automatak.com/aegis/) - Aegis™ is a smart fuzzing framework for a growing number of protocols that can identify robustness and security issues in communications software before it is deployed in a production system. **[commercial]** Early Open Source version is mirrored here: [Open-Source](aegis-opensource).

## IEC 104
* [IEC Server](http://area-x1.lima-city.de) - Software to simulate server side of systems using a telecontrol message Protocol specified in the IEC 60870-5.
* [OpenMRTS](https://sourceforge.net/projects/mrts/) - MRTS is an attempt to create open source IEC 870-5-101/104 based components for telecontrol and supervisory systems and to become a complete solution in future.

## IEC 61850
### Protocol Implementation
* [libIEC61850](http://libiec61850.com/libiec61850/) - open source library for IEC 61850.
* [rapid61850](https://github.com/stevenblair/rapid61850) - Rapid-prototyping protection and control schemes with IEC 61850

### Tools
* [IEDScout](https://www.omicronenergy.com/en/products/all/secondary-testing-calibration/iedscout/noc/1/) - IEDScout provides access to 61850-based IEDs and can simulate entire Ed. {1,2} IEDs. Specifically, IEDScout lets you look inside the IED and at its communication. All data modeled and exchanged becomes visible and accessible. Additionally, IEDScout serves numerous useful tasks, which could otherwise only be performed with dedicated engineering tools or even a functioning master station. IEDScout shows an overview representing the typical workflow of commissioning, but also provides detailed information upon request. **[commercial]** Free 30 day evaluation license.

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

## Siemens S7
* [Snap7](http://snap7.sourceforge.net/) - open source Siemens S7 communication library.
* [LibNoDave](http://libnodave.sourceforge.net/) - Another (less complete) open source communication library for the S7 protocol.
* [S7comm](http://sourceforge.net/projects/s7commwireshark/) - open source Wireshark dissector plugin for the Siemens S7 protocol.


## Zigbee
* [Killerbee](https://github.com/riverloopsec/killerbee) - IEEE 802.15.4/ZigBee Security Research Toolkit.

## General Protocol Fuzzing
* [AFL](http://lcamtuf.coredump.cx/afl/) - American fuzzy lop is a security-oriented fuzzer that employs a novel type of compile-time instrumentation and genetic algorithms to automatically discover clean, interesting test cases that trigger new internal states in the targeted binary.

(creative commons license)
