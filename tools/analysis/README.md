# ICS Tools - Analysis

Developed as a community asset

## Analysis

### Firmware

- [OWASP General Firmware Pointers](https://www.owasp.org/index.php/OWASP_Internet_of_Things_Project#tab=Firmware_Analysis) - General pointers that will get you started in understanding embedded systems firmware images.
- [Firmwalker](https://github.com/craigz28/firmwalker) - Script for extracting some useful things from embedded firmware images
- [Firmware Mod Kit](https://github.com/mirror/firmware-mod-kit) - Basic framework for doing firmware modifications
- [Firmadyne](https://github.com/firmadyne/firmadyne) - System for emulation and dynamic analysis of Linux-based firmware
- [ICSREF](https://github.com/momalab/ICSREF) - ICSREF is a modular framework that automates the reverse engineering process of CODESYS binaries compiled with the CODESYS v2 compiler.
- [Thomas Roth - Embedded Serial Gateways](https://github.com/nezza/scada-stuff) - Various scripts for analysis of some serial embedded gateways. See also: [video](https://media.ccc.de/v/34c3-8956-scada_-_gateway_to_s_hell)
- [ICS Mem Collect](https://github.com/fireeye/ics_mem_collect) - Memory Analysis Collection Toolkit for GE D20MX platform from FireEye.
- [Recon 2017 Work on Protection Relays](https://github.com/rigmar/Recon2017) - More information [here](http://www.scada.sl/2017/10/hopeless-relay-protection-for.html)

### Logs

- [Plaso - Log2timeline](https://github.com/log2timeline/plaso/wiki) - log2timeline is a tool designed to extract timestamps from various files found on a typical computer system(s) and aggregate them.

### Malware

- [YARA](https://plusvic.github.io/yara/) - YARA is a tool aimed at (but not limited to) helping malware researchers to identify and classify malware samples. With YARA you can create descriptions of malware families (or whatever you want to describe) based on textual or binary patterns. Each description, a.k.a rule, consists of a set of strings and a boolean expression which determine its logic.
- [Volatility](https://github.com/volatilityfoundation/volatility) - The Volatility Framework is a completely open collection of tools,implemented in Python under the GNU General Public License, for the extraction of digital artifacts from volatile memory (RAM) samples.
- [OPC Data Access IDAPython script](https://github.com/eset/malware-research/tree/master/industroyer) - An IDAPython script for IDA Pro that helps reverse engineer binaries that are using the OPC Data Access protocol. It can be used to analyse such malware families as Havex RAT and Win32/Industroyer.
- [FLARE VM](https://github.com/fireeye/flare-vm) - FLARE VM is a fully customizable, Windows-based security distribution for malware analysis, incident response, penetration testing, etc.
- [Control Things Tools Bin](https://github.com/ControlThingsTools/ctbin) - The goal of ctbin is to become the security professional’s Swiss army knife for analyzing binary files.

### Network

- [ICS Protocol Tools](/protocols/) - See ICS Protocols for more information.
- [NSA GRASSMARLIN](https://github.com/nsacyber/GRASSMARLIN) - GRASSMARLIN provides IP network situational awareness of industrial control systems (ICS) and Supervisory Control and Data Acquisition (SCADA) networks to support network security. Passively map, and visually display, an ICS/SCADA network topology while safely conducting device discovery, accounting, and reporting on these critical cyber-physical systems. Now Deprecated, so mirrored [here](/tools/mirrored/grassmarlin).
- [ARMORE](https://github.com/ITI/ARMORE) - ARMORE was developed to be an open-source software solution that will aid asset owners by increasing visibility, securing communications, and inspecting ICS communications for behavior that is not intended. Built around Bro and Linux.
- [EDMAND](https://github.com/ITI/EDMAND) - EDMAND Anomaly detection framework. Built around Bro.
- [AIUS](https://github.com/ITI/aius) - AIUS Repository (EDMAND/CAPTAR combination). Built around Bro.
- [ML NIDS For ICS](https://github.com/Rocionightwater/ML-NIDS-for-SCADA) - Machine learning techniques for Intrusion Detection in SCADA Systems.
- [Malcolm](https://github.com/cisagov/Malcolm) - Malcolm is a powerful, easily deployable network traffic analysis tool suite for full packet capture artifacts (PCAP files) and Zeek logs.
- [Flare](https://github.com/austin-taylor/flare) - An analytical framework for network traffic and behavioral analytics
- [Skydive](https://github.com/skydive-project/skydive) - An open source real-time network topology and protocols analyzer
- [Zeek Goose Protocol Parser](https://github.com/smartgridadsc/Goose-protocol-parser-for-Zeek-IDS) - A Zeek GOOSE parser has been developed to enable detailed analysis of the transmitted data and allow rule-based identification of anomalies related to cybersecurity attacks. It is compatible with an older instance of Zeek Network Security Monitor (v2.6).
- [ntopng (community version)](https://github.com/ntop/ntopng) - a web-based network traffic monitoring application released under GPLv3. Supports Industrial IOT and Scada with modbus, DNP3 and IEC 60870-5-104 since ntopng 4.2 (October 2020)
- [Sequence 2 Sequence Anomaly Detection on SWaT](https://github.com/jukworks/swat-seq2seq) - Anomaly Detection for SWaT Dataset using Sequence-to-Sequence Neural Networks
- [Poisoning ICS Attack Detectors](https://github.com/mkravchik/poisoning-ics-ad) - Poisoning Attacks on Cyber Attack Detectors for Industrial Control Systems -- operates on the SWaT dataset

### Protocols

- [TruffleHog](https://github.com/TruffleHog/TruffleHog) - A network analysis tool that works together with snort to visually represent a PROFINET network graph.
- [PowerMeter Reader](https://github.com/lucab85/PowerMeter-Reader) - Python code to read energetic usage data from a modbus connected PowerMeter device. Tested against Schneider Electric iEM3255 (Acti 9 iEM 3000 series - code A9MEM3255).

### Radio

- [GR Smart Meters](https://github.com/BitBangingBytes/gr-smart_meters) - GNU Radio module containing decoders for smart meter manufacturers.
- [GR Elster](https://github.com/argilo/gr-elster) - GNU Radio block and sample flow graph are intended to receive packets transmitted by Elster smart meters on the 902-928 MHz band (tested against Elster R2S).
- [RTL-SDR Itron ERT](https://github.com/bemasher/rtlamr) - An rtl-sdr receiver for Itron ERT compatible smart meters operating in the 900MHz ISM band.

### Reverse Engineering

- [Binwalk](https://github.com/ReFirmLabs/binwalk) - Binwalk is a fast, easy to use tool for analyzing, reverse engineering, and extracting firmware images.
- [ANGR](https://github.com/angr/angr) - A powerful and user-friendly binary analysis platform.
- [Floss](https://github.com/fireeye/flare-floss) - FireEye Labs Obfuscated String Solver (FLOSS) uses advanced static analysis techniques to automatically deobfuscate strings from malware binaries.

### Symbolic Execution

- [SymCC](https://github.com/eurecom-s3/symcc) - SymCC: efficient compiler-based symbolic execution
- [IEC-Checker](https://github.com/jubnzv/iec-checker) - This project aims to implement an open source tool for static code analysis of IEC 61131-3 programs.

### Samples

- [Trisis/Triton/Hatman](https://github.com/MDudek-ICS/TRISIS-TRITON-HATMAN) - Repository containing original and decompiled files of TRISIS/TRITON/HATMAN malware

### System Analysis

- [PASAD](https://github.com/mikeliturbe/pasad) - Process-Aware Stealthy Attack Detection using SWaT and DVCP-TE
- [Archive Walker](https://github.com/pnnl/archive_walker) - PMU data analysis tool

(creative commons license)
