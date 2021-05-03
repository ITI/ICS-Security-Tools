# ICS PCAPs

Developed as a community asset

## Tools

- [CapSan](https://github.com/jsiwek/capsan) - Packet capture sanitizer/anonymizer for Jon Siwek at University of Illinois.
- [Malcolm](https://github.com/cisagov/Malcolm) - Malcolm is a powerful, easily deployable network traffic analysis tool suite for full packet capture artifacts (PCAP files) and Zeek logs.
- [Reference Table of ICS Protocol - Wireshark Dissectors](/pcaps/wireshark-disectors.tsv)

## Protocol Organized PCAPs

- [Jason Smith's Organized ICS PCAP repo](https://github.com/automayt/ICS-pcap) - A comprehensive collection of ICS/SCADA PCAPs organized by protocol. Make sure to have git lfs support and do a git lfs clone of the linked repo to get the actual files.

## Captures

- [Bro-IDS DNP3 & Modbus Captures](bro) - Test captures from the parser testing tree.
- [Coimbra PCAPs](https://github.com/tjcruz-dei/ICS_PCAPS) - ICS Cybersecurity PCAP repository from the Univ. of Coimbra CyberSec team
- [OpenICS test data](openics) - Test captures from the OpenICS effort.
- [Profinet Captures](profinet) - Random profinet captures from the wild
- [QuickDraw test data](quickdraw) - PCAPs from the quickdraw initiative to test the sensor filters.
- [OpenDNP3 3.0](dnp3/opendnp3-3/conformance) - OpenDNP 3.0 Conformance Captures and [Report](dnp3/opendnp3-3/conformance/report.html). [Original Source](https://github.com/dnp3/dnp3.github.io/tree/master/conformance).
- [Various DNP3 captures](dnp3) - This covers a variety of DNP3 captures broken out by function types. Includes some very obscure functionality and were designed for firewall testing.
- [Various Siemens S7 captures](https://github.com/gymgit/s7-pcaps) - Covers a subset of the S7 protocol, includes a few security critical functions such as authentication and firmware update.
- [More S7 Captures](s7) - Some more S7 captures
- [Various C37.118 Captures](C37.118) - Example C37.118 captures and spec details
- [DLMS-COSEM Security Review](DLMS-COSEM) - third party security review of DLMS-COSEM
- [Various EthernetIP Captures](EthernetIP) - Various EthernetIP captures
- [Various IEC 60870-5-104 Captures](IEC60870-5-104) - Various IEC 60870-5-104 captures
- [Various IEC 61850 Captures](IEC61850) - Various IEC 61850 captures
- [Various ModBus TCP Captures](ModbusTCP) - Various Modbus TCP captures
- [Various OPC Specifications](OPC) - Various OPC specifications
- [Various Zigbee Captures](Zigbee) - Various Zigbee captures
- [Netresec PCAP collection](https://www.netresec.com/?page=PcapFiles) - This is a list of public packet capture repositories, which are freely available on the Internet. Most of the sites listed below share Full Packet Capture (FPC) files, but some do unfortunately only have truncated frames.
- [ControlThings I/O PCAP collection](https://github.com/ControlThings-io/ct-samples/tree/master/Protocols) - ICS PCAP repository from ControlThings I/O

## Datasets

- [4SICS](https://www.netresec.com/?page=PCAP4SICS) - 4SICS 2015 PCAP Files from their Geek Lounge
- [batadal datasets](https://www.batadal.net/data.html) - water distribution datasets, also used [here](https://github.com/scy-phy/ICS-Evasion-Attacks)
- [control logic attack dataset](https://gitlab.com/safelab/control-logic-attack-datasets/-/tree/master/) - these consist of training datasets (normal), datasets of traditional control logic injection attacks, and datasets of (new) stealthy control logic injection attacks (i.e., Data Execution, Fragmentation and Noise Padding), for Schneider Electric's Modicon M221 PLC and Allen-Bradley's MicroLogix 1400 PLC.
- [covert modbus](https://github.com/antoine-lemay/Modbus_dataset) - Modbus Dataset from CSET 2016 demonstrating covert communications with modbus.
- [cybercity dataset](https://assets.contentstack.io/v3/assets/blt36c2e63521272fdc/bltff8e7c1232f3bcbc/5fbd7be072a3526f28dbed75/sansholidayhack2013.pcap) - SANS Holiday Hack 2013 dataset
- [DoS Modbus dataset](https://github.com/tjcruz-dei/ICS_PCAPS/releases/tag/MODBUSTCP%231) - used in CRITIS 2018 paper about machine learning fragility
- [electra modbus dataset](http://perception.inf.um.es/ICS-datasets/) - The Electra dataset models the behaviour of an electric traction substation used in a real high-speed railway area.
- [HAI Dataset](https://github.com/icsdataset/hai) - The HAI dataset was collected from a realistic industrial control system (ICS) testbed augmented with a Hardware-In-the-Loop (HIL) simulator that emulates steam-turbine power generation and pumped-storage hydropower generation.
- [hvac traces](https://github.com/gkabasele/HVAC_Traces) - This repository contains pcap traces of the HVAC system of a university
- [ics attack datasets](https://sites.google.com/a/uah.edu/tommy-morris-uah/ics-data-sets) - five datasets representing power systems, gas, and water storage ICS systems from Tommy Morris, et al.
- [iTrust Secure Water Treatment Testbed (SWaT/SUTD) Dataset](https://itrust.sutd.edu.sg/research/dataset/dataset_characteristics/#swat) - The SWaT Dataset was systematically generated from the Secure Water Treatment Testbed (SUTD) to address this need. The data collected from the testbed consists of 11 days of continuous operation. 7 days’ worth of data was collected under normal operation while 4 days’ worth of data was collected with attack scenarios. During the data collection, all network traffic, sensor and actuator data were collected [available by request]
- [iTrust WADI Dataset](https://itrust.sutd.edu.sg/research/dataset/dataset_characteristics/#wadi) - Similar to the SWaT dataset, the data collected from the Water Distribution testbed consists of 16 days of continuous operation, of which 14 days’ worth of data was collected under normal operation and 2 days with attack scenarios. During the data collection, all network traffic, sensor and actuator data were collected. [available by request]
- [iTrust EPIC Dataset](https://itrust.sutd.edu.sg/research/dataset/dataset_characteristics/#blaq) - Blaq_0 Hackathon was first organized in January 2018 for SUTD undergraduate students. Independent attack teams design and launch attacks on EPIC. Attack teams are scored according to how successful they are in performing attacks based on specific intents. [available by request]
- [Illinois ADSC 61850 Dataset](https://github.com/smartgridadsc/IEC61850SecurityDataset) - This repository contains network traces that describe GOOSE communications in a mock substation that consists of 4-buses and 18 IEDs. The IEDs communicate with each other using the IEC 61850 GOOSE protocol. These are traces that represent normal, disturbance, and attack scenarios.
- [mining s7 dataset](https://cloudstor.aarnet.edu.au/plus/index.php/s/9qFfeVmfX7K5IDH) - Process control cyber-attacks and labelled datasets on S7Comm critical infrastructure.
- [s4x15 dataset](https://www.netresec.com/?page=DigitalBond_S4) - captures from s4x15 conference.
- [WUSTL-IIOT-2018 Dataset](https://www.cse.wustl.edu/~jain/iiot/index.html) - captures for 2018 paper demonstrating machine-learning applied to a representative ICS testbed.
- [QUT 2017 DNP3 dataset](https://github.com/qut-infosec/2017QUT_DNP3) - DNP3 Cyber-attack dataset
- [QUT 2017 S7 dataset](https://cloudstor.aarnet.edu.au/plus/index.php/s/9qFfeVmfX7K5IDH) - S7 Cyber-attack dataset

(creative commons license)
