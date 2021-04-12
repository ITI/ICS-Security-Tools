# ICS Protocols

Developed as a community asset

## General / Miscellaneous Releases

- [ICS System Ports List](PORTS.md) - This is a list of common ICS tcp/udp ports.
- [PoC 2013 SCADA Release](https://github.com/atimorin/PoC2013) - Power of Community 2013 conference special release of ICS/SCADA toolkit
- [Industrial Control Systems Network Protocol Parsers (ICSNPP)](https://github.com/cisagov/ICSNPP) - DHS CISA Industrial Control Systems protocol parsers plugins for the Zeek network security monitoring framework

## AMI

- [Termineter](https://github.com/GrayHatLabs/john_commor_c1218) - c1218 powermeter emulator

## BACnet

- [BACpypes](https://github.com/JoelBender/bacpypes) - BACpypes provides a BACnet application layer and network layer written in Python for daemons, scripting, and graphical interfaces.
- [zeek plugin bacnet](https://github.com/amzn/zeek-plugin-bacnet) - BACnet Zeek Plugin from Amazon
- [yet another zeek plugin bacnet](https://github.com/delta-2-4/Zeek-BACnetIP) - BACnet Zeek Plugin by Aaron Heller + [SANS whitepaper](https://www.sans.org/reading-room/whitepapers/ICS/paper/39240)
- [ICSNPP Bacnet for Zeek](https://github.com/cisagov/icsnpp-bacnet) - DHS CISA Bacnet parser for Zeek

## Bristol Standard Asynchronous Protocol (BSAP)

- [ICSNPP BSAP IP for Zeek](https://github.com/cisagov/icsnpp-bsap-ip) - DHS CISA BSAP over IP plugin for Zeek
- [ICSNPP BSAP Serial for Zeek](https://github.com/cisagov/icsnpp-bsap-serial) - DHS CISA BSAP over Serial plugin for Zeek

## DNP3

### Protocol Implementation

- [OpenDNP3](https://github.com/automatak/dnp3) - Opendnp3 is the de facto reference implementation of IEEE-1815 (DNP3) provided under the Apache License.
- [DNP3 Simulator](https://github.com/automatak/dnp3-simulator) - Graphical DNP3 Master/Outstation simulator
- [PIFaceRTU](https://github.com/automatak/pifacertu) - Opendnp3 running on a Raspberry Pi with Piface I/O board
- [LangSec DNP3 Parser](https://github.com/Dartmouth-Trustlab/dnp3) - Parsing DNP3 using parser combinators in C.
- [Proxyd](https://github.com/Dartmouth-Trustlab/proxy) - TCP Proxy for testing hammer based parsers (such as the DNP3 parser above)
- [ICSNPP DNP3 for Zeek](https://github.com/cisagov/icsnpp-dnp3) - DHS CISA DNP3 logging extensions to Zeek

### Traffic Generation

- [DNP3 Traffic Generation](https://github.com/iti/ics-trafficgen/) - OpenDNP3-based traffic generation that can take a profile of traffic, apply scriptable variation, and result in output of DNP3 traffic matching that profile.

### Fuzzing

- [AEGIS Fuzzer](https://www.automatak.com/aegis/) - Aegis™ is a smart fuzzing framework for a growing number of protocols that can identify robustness and security issues in communications software before it is deployed in a production system. **[commercial]** Early Open Source version is mirrored here: [Open-Source](aegis-opensource).

## Ethernet/IP and CIP

- [EtherNet/IP+CIP dissector for Scapy](https://github.com/scy-phy/scapy-cip-enip) - a Python library which can be used to interact with components of a network using ENIP (Ethernet/IP) and CIP (Common Industrial Protocol) protocols. It uses scapy to implement packet dissectors which are able to decode a part of the network traffic. These dissectors can also be used to craft packets, which allows directly communicating with the PLCs (Programmable Logic Controllers) of the network. [Use case](https://labs.mwrinfosecurity.com/blog/offensive-ics-exploitation-a-technical-description/)
- [Scapy implementation of DLR (Device Level Ring) protocol](https://github.com/scy-phy/scapy-dlr)
- [Zeek implementation for ethernet/ip](https://github.com/scy-phy/bro-cip-enip) - This repository contains the necessary files in order to inspect Ethernet/IP and Common Industrial Protocol packets with Zeek.
- [CPPPO - Communications Protocol Python Parser and Originator (EtherNet/IP CIP implementation)](https://github.com/pjkundert/cpppo) - Cpppo is used to implement binary communications protocol parsers. The protocol’s communication elements are described in terms of state machines which change state in response to input events, collecting the data and producing output data artifacts.
- [pycomm](https://github.com/ruscito/pycomm) - **pycomm** is a package that includes a collection of modules used to communicate with PLCs. At the moment the first module in the package is **ab_comm**. **ab_comm** is a module that contains a set of classes used to interface Rockwell PLCs using Ethernet/IP protocol. The "clx" class can be used to communicate with Compactlogix, Controllogix PLCs The "slc" can be used to communicate with Micrologix or SLC PLCs
- [pyCIP](https://github.com/cpchrispye/PyCIP) - CIP protocol implementation in Python3
- [zeek plugin enip](https://github.com/amzn/zeek-plugin-enip) - EthernetIP Zeek Plugin from Amazon
- [ICSNPP ENIP and CIP for Zeek](https://github.com/cisagov/icsnpp-enip) - DHS CISA ENIP and CIP plugin for Zeek

## IEC 104

- [IEC Server](../tools/mirrored/iec-server/) - Software to simulate server side of systems using a telecontrol message Protocol specified in the IEC 60870-5. Original website http://area-x1.lima-city.de is down, so this has been mirrored.
- [OpenMRTS](https://sourceforge.net/projects/mrts/) - MRTS is an attempt to create open source IEC 870-5-101/104 based components for telecontrol and supervisory systems and to become a complete solution in future.
- [QTester104](https://sourceforge.net/projects/qtester104/) - This software implements the IEC60870-5-104 protocol (client side) for substation data acquisition and control via tcp/ip network using the QT UI Framework. It can be compiled on Linux and Windows platforms. It's possible to poll and view data from the substation system (RTU/concentrator) and also send commands.
- [lib60870](https://github.com/mz-automation/lib60870) - Implements IEC 60870-5-104 protocol.

## IEC 61850

### Protocol Implementation

- [libIEC61850](http://libiec61850.com/libiec61850/) - open source library for IEC 61850.
- [rapid61850](https://github.com/stevenblair/rapid61850) - Rapid-prototyping protection and control schemes with IEC 61850

### Tools

- [IEDScout](https://www.omicronenergy.com/en/products/all/secondary-testing-calibration/iedscout/noc/1/) - IEDScout provides access to 61850-based IEDs and can simulate entire Ed. {1,2} IEDs. Specifically, IEDScout lets you look inside the IED and at its communication. All data modeled and exchanged becomes visible and accessible. Additionally, IEDScout serves numerous useful tasks, which could otherwise only be performed with dedicated engineering tools or even a functioning master station. IEDScout shows an overview representing the typical workflow of commissioning, but also provides detailed information upon request. **[commercial]** Free 30 day evaluation license.
- [Goose-Stalker](https://github.com/cutaway-security/goosestalker) - Goose-Stalker is a project to analyze and interact with Ethernet types associated with IEC 61850. Currently, the project is focused on Ethernet type 0x88b8 as published by the goose-IEC61850-scapy. The project has morphed significantly and the direction is to progress this even further.

### Traffic Generation

- [IEC 61850 Toolchain](https://github.com/smartgridadsc/IEC61850ToolChain) - This toolchain aims to enable users (e.g., power grid operators) to easily create customized datasets for the validation of cybersecurity solutions for IEC 61850 communication-based substations. This toolchain processes different inputs (e.g., substation configurations, attack configurations, and simulation settings) and carries out the necessary processing steps needed for generating the customized datasets. This toolchain is basing on an open source project libIEC61850.

## IEEE C37.118

### Protocol Implementation

- [C37.118-2005 Spec](https://ieeexplore.ieee.org/document/1611105/) -- C37.118-2005 (deprecated). Note, this is a paid IEEE spec
- [C37.118-2011 Spec](https://ieeexplore.ieee.org/document/6111219/) -- C37.118-2011 (current). Note, this is a paid IEEE spec
- [pyMU](https://github.com/iti/pymu) - Python C37.118-2011 parser
- [pyPMU](https://github.com/iicsys/pypmu) - WIP Python implementation
- [Wireshark Dissector](https://github.com/boundary/wireshark/blob/master/epan/dissectors/packet-synphasor.c) - Implemented C37.118 wireshark dissector
- [Grid Solutions Framework C37.118](https://github.com/GridProtectionAlliance/gsf/tree/master/Source/Libraries/GSF.PhasorProtocols/IEEEC37_118) - GSF implementation (.net)
- [LangSec C37.118 Parser](https://github.com/Dartmouth-Trustlab/C37.118PMU) - LangSec based C37.118 parser

### Tools

- [pyMU](https://github.com/iti/pymu) - Python C37.118-2011 parser
- [pyPMU](https://github.com/iicsys/pypmu) - WIP Python implementation
- [PMU Connection Tester](https://github.com/GridProtectionAlliance/PMUConnectionTester) - Full fledged PMU connection tester, speaking c37.118 amongst many other synchrophasor protocols

## LoRaWAN

### Tools

- [chirpotle](https://github.com/seemoo-lab/chirpotle) - A LoRaWAN Securiy Evaluation Framework

## Modbus

### Protocol Implementation

- [pyModBus](https://github.com/bashwork/pymodbus) - A full modbus protocol written in python.
- [Modbus for Go](https://github.com/goburrow/modbus) - Fault-tolerant implementation of modbus protocol in Go (golang)
- [ModbusPal](http://modbuspal.sourceforge.net) - ModbusPal is a MODBUS slave simulator. Its purpose is to offer an easy to use interface with the capabilities to reproduce complex and realistic MODBUS environments. Mirror available [here](../tools/mirrored/modbuspal/).
- [SMOD](../tools/mirrored/smod/) - MODBUS Penetration Testing Framework. smod is a modular framework with every kind of diagnostic and offensive feature you could need in order to pentest modbus protocol. It is a full Modbus protocol implementation using Python and Scapy. (mirrored as original source is now gone)
- [mbtget](https://github.com/sourceperl/mbtget) - A simple modbus/TCP client write in pure Perl.
- [ICSNPP Modbus for Zeek](https://github.com/cisagov/icsnpp-modbus) - DHS CISA Modbus extensions to logging for Zeek

### Traffic Generation

- [Modbus Traffic Generation](https://github.com/iti/ics-trafficgen/) - pyModbus-based traffic generation that can take a profile of traffic, apply scriptable variation, and result in output of Modbus traffic matching that profile.

### Fuzzing

- [AEGIS Fuzzer](https://www.automatak.com/aegis/) - Aegis™ is a smart fuzzing framework for a growing number of protocols that can identify robustness and security issues in communications software before it is deployed in a production system. **[commercial]** Early Open Source version is mirrored here: [Open-Source](../tools/mirrored/aegis-opensource).

## OPC UA

### Protocol Implementation

- [OPC UA .Net Standard](https://github.com/OPCFoundation/UA-.NETStandard) - Official OPC UA .Net Standard Stack and Samples from the OPC Foundation
- [OPC UA Client GUI](https://github.com/FreeOpcUa/opcua-client-gui) - A simple OPC-UA GUI client.
- [OPC UA Server Simulator](https://integrationobjects.com/opc-products/opc-unified-architecture/opc-ua-server-simulator/) - Simulate real-time and historical data using OPC UA Server Simulator.
- [OPC UA Server and Client C++ Libraries](https://github.com/FreeOpcUa/freeopcua) - LGPL OPC-UA server and client library written in C++ and with a lot of code auto-generated from xml specification using python.

## OpenADR

### Protocol Implementation / Tools

- [OpenADR 2.0a VEN Python](https://github.com/EnerNOC/oadr2-ven-python) - EnerNOC Open Source Python OpenADR 2.0a VEN client implementation
- [EPRI OpenADR VTN Implementation](https://github.com/epri-dev/OpenADR-Virtual-Top-Node) - OpenADR 2.0 Profile Specification B Profile for virtual top node implementation.
- [OpenADR Java Implementation](https://github.com/avob/OpenADR) - OpenADR minimal VEN / VTN 2.0a / 2.0b skeleton implementations in Java
- [Node-Red Implementation](https://github.com/Argonne-National-Laboratory/node-red-contrib-oadr-ven) - Node-Red OADR2 VEN Implementation w/ HTTP transport

## PROFINET

### Protocol Implementation

- [Profinet - Python](https://github.com/devkid/profinet) - Simple PROFINET implementation in python
- [Profinet - C](https://github.com/kprovost/libs7comm) - PROFINET implementation in C
- [Profinet Explorer](https://sourceforge.net/projects/profinetexplorer/) - Simple PROFINET explorer written in C#
- [zeek plugin Profinet](https://github.com/amzn/zeek-plugin-profinet) - PROFINET Zeek Plugin from Amazon
- [PROFINET GSD Checker](https://www.profibus.com/download/profinet-gsd-checker/) - Free tool to show and edit the content of GSD files for PROFINET in an easy to understand table view. It also contains a function to check the accuracy of GSD files which helps to build a valid description file.

### Fuzzing

- [ProFuzz](https://github.com/HSASec/ProFuzz) - Simple PROFINET fuzzer based on Scapy

## SEL Fast Message

- [Wireshark Dissector - SEL Fast Message](https://github.com/boundary/wireshark/blob/master/epan/dissectors/packet-selfm.c) - Wireshark Dissector for SEL Fast Message
- [Grid Solutions Framework SEL Fast Message](https://github.com/GridProtectionAlliance/gsf/tree/master/Source/Libraries/GSF.PhasorProtocols/SelFastMessage) - GSF implementation (.net)
- [SEL Applications Guides](https://www.selinc.com) - Look up AG95-10 and AG2002-14 product codes.
- [SELProtoPy](https://github.com/engineerjoe440/selprotopy) - Schweitzer Engineering Laboratories (SEL) Protocol Bindings in Python. Implements SEL Fast Meter, Fast Message, and Fast Operate.

## Siemens S7

- [Snap7](http://snap7.sourceforge.net/) - open source Siemens S7 communication library.
- [LibNoDave](http://libnodave.sourceforge.net/) - Another (less complete) open source communication library for the S7 protocol.
- [S7comm](http://sourceforge.net/projects/s7commwireshark/) - open source Wireshark dissector plugin for the Siemens S7 protocol.
- [Python Snap7 Wrapper](https://github.com/gijzelaerr/python-snap7) - A Python wrapper for the snap7 PLC communication library
- [Zeek-IDS S7 Protocol Parser](https://github.com/dw2102/S7Comm-Analyzer) - S7 protocol parser for Zeek IDS
- [zeek plugin s7](https://github.com/amzn/zeek-plugin-s7comm) - S7 Zeek Plugin from Amazon
- [SPPA S7 Data port dissector](https://github.com/klsecservices/SPPA/blob/master/sppa_dissector.lua) - SPPA-T3000 Automation server (PLC) dissector + [whitepaper](https://github.com/klsecservices/SPPA/blob/master/Security%20of%20DCS%20for%20turbines%20-%202020.pdf) by Kaspersky Security Services team

## SSP21

- [SSP21 - Specification](https://github.com/ssp21/ssp21-spec) - SSP21 specification
- [SSP21 - C++](https://github.com/ssp21/ssp21-spec) - SSP21 reference implementation in C++
- [SSP21 - Rust](https://github.com/ssp21/ssp21-rs-core) - SSP21 core library in Rust

## TriStation

- [FireEye TriStation Wireshark Dissector](https://github.com/stvemillertime/TriStation) - reverse engineered wireshark dissector from Mandiant/FireEye team after Triton discovery.
- [Nozomi TriStation Wireshark Dissector](https://github.com/NozomiNetworks/tricotools) - another TriStation dissector, this time from Nozomi, also incldues pcap, and basic honeypot simulator.

## Zigbee

- [Killerbee](https://github.com/riverloopsec/killerbee) - IEEE 802.15.4/ZigBee Security Research Toolkit.

## General Protocol Fuzzing

- [AFL](http://lcamtuf.coredump.cx/afl/) - American fuzzy lop is a security-oriented fuzzer that employs a novel type of compile-time instrumentation and genetic algorithms to automatically discover clean, interesting test cases that trigger new internal states in the targeted binary.

- [WinAFL](https://github.com/ivanfratric/winafl) - AFL that works for Windows binaries.

(creative commons license)
