# ICS Tools - Auditing and Signatures
## Developed as a community asset

## Auditing
* [Bandolier Security Audit Files](http://www.digitalbond.com/tools/bandolier/) - These audit files are used with the Nessus scanner’s compliance plugins to audit the security settings of control system components. A typical control system will have over 1,000 security settings including the OS settings, database and webserver settings, and the SCADA or DCS application settings. Digital Bond worked with the vendors, such as ABB, AREVA, Emerson, OSIsoft, Telvent, …, to identify the optimal security settings for their systems. Bandolier Security Audit Files are very useful at FAT to insure the system is installed in an optimal security configuration and periodically to verify the configuration has not degraded.
* [Portaledge](http://www.digitalbond.com/tools/portaledge/) - The OSIsoft PI Server is an extremely popular historian that aggregates and correlates process data. In Portaledge, Digital Bond has created modules to aggregate security events and correlate these events to detect cyber attacks. There are a variety of modules including modules that meet the NERC CIP monitoring requirements.
* [DHS CSET](https://ics-cert.us-cert.gov/Downloading-and-Installing-CSET) - The Cyber Security Evaluation Tool (CSET®) is a Department of Homeland Security (DHS) product that assists organizations in protecting their key national cyber assets. It was developed under the direction of the DHS National Cyber Security Division (NCSD) by cybersecurity experts and with assistance from the National Institute of Standards and Technology. This tool provides users with a systematic and repeatable approach for assessing the security posture of their cyber systems and networks. It includes both high-level and detailed questions related to all industrial control and IT systems.
* [NSA GRASSMARLIN](https://github.com/iadgov/GRASSMARLIN) - GRASSMARLIN provides IP network situational awareness of industrial control systems (ICS) and Supervisory Control and Data Acquisition (SCADA) networks to support network security. Passively map, and visually display, an ICS/SCADA network topology while safely conducting device discovery, accounting, and reporting on these critical cyber-physical systems.
* [Misc SCADA Tools](https://github.com/atimorin/scada-tools) - A collection of miscellaneous SCADA tools written in python.
* [PLCscan](../mirrored/plcsan) - Tool for scan PLC devices over s7comm or modbus protocols.
* [modscan](../mirrored/modscan) - Tool to scan modbus devices and gather information.

## IDS Signatures
* [Quickdraw ICS IDS](http://www.digitalbond.com/tools/quickdraw/) - Digital Bond’s original research project was to develop a set of IDS rules for SCADA protocols. The initial rules for Modbus TCP and DNP3 have now been enhanced for EtherNet/IP, Vulnerability rules and Device Specific rules. Quickdraw also includes Snort preprocessors and plugins that allow rules for more complex control system protocols.

## IDS Extensions
* [Profinet for Suricata](https://github.com/rain8841/Suricata_Profinet_MOD) - Profinet extensions for Suricata

## IoC Tools
* [FireEye IoC Editor](https://www.fireeye.com/services/freeware/ioc-editor.html) - IOCs are XML documents that help incident responders capture diverse information about threats, including attributes of malicious files, characteristics of registry changes and artifacts in memory. The IOC Editor provides an interface for managing data, including: 1) Manipulation of the logical structures that define the IOC, 2) Application of meta-information to IOCs, including detailed descriptions or arbitrary labels, 3) Conversion of IOCs into XPath filters, and 4) Management of lists of “terms” used within IOCs.

(creative commons license)
