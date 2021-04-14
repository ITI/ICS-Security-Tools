# ICS Tools - Auditing and Signatures

Developed as a community asset

## Auditing

- [Bandolier Security Audit Files](http://www.digitalbond.com/tools/bandolier/) - These audit files are used with the Nessus scanner’s compliance plugins to audit the security settings of control system components. A typical control system will have over 1,000 security settings including the OS settings, database and webserver settings, and the SCADA or DCS application settings. Digital Bond worked with the vendors, such as ABB, AREVA, Emerson, OSIsoft, Telvent, …, to identify the optimal security settings for their systems. Bandolier Security Audit Files are very useful at FAT to insure the system is installed in an optimal security configuration and periodically to verify the configuration has not degraded.
- [PI-Security-Audit-Tools](https://github.com/osisoft/PI-Security-Audit-Tools) - The PI Security Audit Tools project is a [PowerShell] framework to baseline the security configuration of your PI System. The module (PISysAudit) can be executed locally or remotely to validate the security configuration of various PI System components: PI Data Archive, PI AF Server, PI Vision, SQL Server and the hosting OS [based on Digital Bond Bandolier Security Audit Files].
- [Configuration Hardening Assessment PowerShell Script (CHAPS)](https://github.com/cutaway-security/chaps) - CHAPS is a PowerShell script for checking system security settings where additional software and assessment tools, such as Microsoft Policy Analyzer, cannot be installed.
- [Portaledge](http://www.digitalbond.com/tools/portaledge/) - The OSIsoft PI Server is an extremely popular historian that aggregates and correlates process data. In Portaledge, Digital Bond has created modules to aggregate security events and correlate these events to detect cyber attacks. There are a variety of modules including modules that meet the NERC CIP monitoring requirements.
- [DHS CSET](https://github.com/cisagov/cset) - The Cyber Security Evaluation Tool (CSET®) is a Department of Homeland Security (DHS) product that assists organizations in protecting their key national cyber assets. It was developed under the direction of the DHS National Cyber Security Division (NCSD) by cybersecurity experts and with assistance from the National Institute of Standards and Technology. This tool provides users with a systematic and repeatable approach for assessing the security posture of their cyber systems and networks. It includes both high-level and detailed questions related to all industrial control and IT systems.
- [NSA GRASSMARLIN](https://github.com/iadgov/GRASSMARLIN) - GRASSMARLIN provides IP network situational awareness of industrial control systems (ICS) and Supervisory Control and Data Acquisition (SCADA) networks to support network security. Passively map, and visually display, an ICS/SCADA network topology while safely conducting device discovery, accounting, and reporting on these critical cyber-physical systems.
- [Misc SCADA Tools](https://github.com/atimorin/scada-tools) - A collection of miscellaneous SCADA tools written in python.
- [PLCscan](../mirrored/plcsan) - Tool for scan PLC devices over s7comm or modbus protocols.
- [s7scan](https://github.com/klsecservices/s7scan) - Replacement for PLCscan.
- [modscan](../mirrored/modscan) - Tool to scan modbus devices and gather information.
- [modbus-scanner](https://github.com/arnaudsoullie/modbus-scanner) - Live scanner that looks for register changes via modbus.
- [Metasploit Modules for OPC UA](https://github.com/COMSYS/msf-opcua) - New Metasploit modules for assessing the security of OPC UA deployments, [paper](https://arxiv.org/abs/2003.12341)
- [Open PHA](https://www.kenexis.com/software/openpha/download/) - Open PHA™ is a HAZOP and LOPA software tool. Open PHA™ provides an easy to use, light-weight platform for performing HAZOP and LOPA analysis. Includes the ability to perform a Security PHA Review directly in the PHA study (description: https://www.kenexis.com/security-pha-review-spr-open-pha/)
- [Industrial Security Auditing Framework](https://gitlab.com/d0ubl3g/industrial-security-auditing-framework/) - ISAF aims to be a framework that provides the necessary tools for the correct security audit of industrial environments.
- [Shodan](https://www.shodan.io) - Shodan is the world's first search engine for Internet-connected devices
- [Censys](https://censys.io/) - Another search engine for Internet-connected devices
- [ZoomEye](https://www.zoomeye.org/topic?id=ics_project) - Chinese search engine for Internet-connected devices
- [FOFA pro](https://fofa.so/subject) - Chinese search engine for Internet-connected devices
- [Zhifeng](https://zhifeng.io/monitor) - Chinese search engine for internet-connected IoT/ICS assets
- [Ditecting](http://www.ditecting.com/) - Chinese search engine for Industrial Control System Devices
- [kamerka](https://github.com/woj-ciech/kamerka) - Build interactive map of ICS devices from Shodan
- [splonebox](https://splone.com/splonebox/) - splonebox is an open source network assessment tool with focus on Industry Control Systems. It offers an ongoing analysis of your network and its devices. A modular design allows writing of additional plugins.
- [CHAPS](https://github.com/cutaway-security/chaps) - Configuration Hardening Assessment PowerShell Script, a script for checking Windows system security settings where additional software and assessment tools cannot be installed (e.g. Industrial Control System (ICS) environments)
- [WES-NG](https://github.com/bitsadmin/wesng) - Windows Exploit Suggester - Next Generation, a tool based on the output of Windows' systeminfo utility which provides the list of vulnerabilities the OS is vulnerable to, including any exploits for these vulnerabilities. Every Windows OS between Windows XP and Windows 10, including their Windows Server counterparts, is supported.
- [Siemens Simatic PCS 7 Hardening Tool](https://github.com/otoriocyber/PCS7-Hardening-Tool) - Powershell script for assessing the security configurations of Siemens - SIMATIC PCS 7 OS client, OS Server or Engineering station
- [General Electric CIMPLICITY Hardening Tool](https://github.com/otoriocyber/CIMPLICITY-Hardening-Tool) - Powershell script for assessing the security configurations of windows machines in the CIMPLICITY environment

## Robotics

- [Aztarna](https://github.com/aliasrobotics/aztarna) - A footprinting tool for robots

## IDS Signatures

- [Quickdraw ICS IDS](https://github.com/digitalbond/quickdraw) - Digital Bond’s original research project was to develop a set of IDS rules for SCADA protocols. The initial rules for Modbus TCP and DNP3 have now been enhanced for EtherNet/IP, Vulnerability rules and Device Specific rules. Quickdraw also includes Snort preprocessors and plugins that allow rules for more complex control system protocols.
- [Quickdraw Suricata Signatures for EtherNet/IP](https://github.com/digitalbond/Quickdraw-Suricata) - A set of EtherNet/IP IDS rules for use with Suricata.
- [Triton SNORT rules](https://www.bsi.bund.de/DE/Themen/Industrie_KRITIS/ICS/Tools/RAPSN_SETS/RAPSN_SETS_node.html) by BSI

## IDS Extensions

- [Profinet for Suricata](https://github.com/rain8841/Suricata_Profinet_MOD) - Profinet extensions for Suricata

## IoC Tools

- [FireEye IoC Editor](https://www.fireeye.com/services/freeware/ioc-editor.html) - IOCs are XML documents that help incident responders capture diverse information about threats, including attributes of malicious files, characteristics of registry changes and artifacts in memory. The IOC Editor provides an interface for managing data, including: 1) Manipulation of the logical structures that define the IOC, 2) Application of meta-information to IOCs, including detailed descriptions or arbitrary labels, 3) Conversion of IOCs into XPath filters, and 4) Management of lists of “terms” used within IOCs.

(creative commons license)
