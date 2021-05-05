# ICS Tools - Auditing and Signatures

Developed as a community asset

## Organizational Assessment Tools

- [DHS CSET](https://github.com/cisagov/cset) - The Cyber Security Evaluation Tool (CSET®) is a Department of Homeland Security (DHS) product that assists organizations in protecting their key national cyber assets. It was developed under the direction of the DHS National Cyber Security Division (NCSD) by cybersecurity experts and with assistance from the National Institute of Standards and Technology. This tool provides users with a systematic and repeatable approach for assessing the security posture of their cyber systems and networks. It includes both high-level and detailed questions related to all industrial control and IT systems.
- [ENISA SARP](https://www.enisa.europa.eu/topics/threat-risk-management/risk-management/files/tools/sarm-2009-05-10.xls/view) - Beta version of a tool to assess risk management requirements.
- [ENISA CSIRT Maturity](https://www.enisa.europa.eu/topics/csirts-in-europe/csirt-capabilities/csirt-maturity/csirt-maturity-self-assessment-survey) - This tool helps CSIRTs to self-assess their team’s maturitylogo maturity in terms of 44 parameters of the SIM3 model.
- [JPCERT J-CLICS](https://www.jpcert.or.jp/english/cs/jclics.html) - J-CLICS (Check List for Industrial Control Systems of Japan; a self-assessment tool for security) consists of "Check List" which helps understanding the status of security measure implementation in ICS, and "Guidance" which provides detailed measures for each question in the Checklist.
- [BSI LARS](https://www.bsi.bund.de/EN/Topics/Industry_CI/ICS/Tools/LarsICS/LarsICS_node.html) - Light and Right Security ICS (LARS ICS) is a free tool that makes it easier for small and midsize enterprises involved in industrial control systems to take their first steps towards achieving cyber security. It provides organisations with questions they can use to assess the current state of their own cyber security and recommends the safeguards they should implement next (and in which areas). All safeguards are assigned to corresponding parts of the standards and procedures of IT-Grundschutz, ISO 27001, IEC62443, and the BSI ICS Security Compendium, which facilitates the transition to using a holistic management system for information security.

## Auditing / Scanning

- [Bandolier Security Audit Files](http://www.digitalbond.com/tools/bandolier/) - These audit files are used with the Nessus scanner’s compliance plugins to audit the security settings of control system components. A typical control system will have over 1,000 security settings including the OS settings, database and webserver settings, and the SCADA or DCS application settings. Digital Bond worked with the vendors, such as ABB, AREVA, Emerson, OSIsoft, Telvent, …, to identify the optimal security settings for their systems. Bandolier Security Audit Files are very useful at FAT to insure the system is installed in an optimal security configuration and periodically to verify the configuration has not degraded.
- [PI-Security-Audit-Tools](https://github.com/osisoft/PI-Security-Audit-Tools) - The PI Security Audit Tools project is a [PowerShell] framework to baseline the security configuration of your PI System. The module (PISysAudit) can be executed locally or remotely to validate the security configuration of various PI System components: PI Data Archive, PI AF Server, PI Vision, SQL Server and the hosting OS [based on Digital Bond Bandolier Security Audit Files].
- [Configuration Hardening Assessment PowerShell Script (CHAPS)](https://github.com/cutaway-security/chaps) - CHAPS is a PowerShell script for checking system security settings where additional software and assessment tools, such as Microsoft Policy Analyzer, cannot be installed.
- [Portaledge](http://www.digitalbond.com/tools/portaledge/) - The OSIsoft PI Server is an extremely popular historian that aggregates and correlates process data. In Portaledge, Digital Bond has created modules to aggregate security events and correlate these events to detect cyber attacks. There are a variety of modules including modules that meet the NERC CIP monitoring requirements.
- [NSA GRASSMARLIN](https://github.com/nsacyber/GRASSMARLIN) - GRASSMARLIN provides IP network situational awareness of industrial control systems (ICS) and Supervisory Control and Data Acquisition (SCADA) networks to support network security. Passively map, and visually display, an ICS/SCADA network topology while safely conducting device discovery, accounting, and reporting on these critical cyber-physical systems. Now Deprecated, so mirrored [here](/tools/mirrored/grassmarlin).
- [Misc SCADA Tools](https://github.com/atimorin/scada-tools) - A collection of miscellaneous SCADA tools written in python.
- [PLCscan](/tools/mirrored/plcsan) - Tool for scan PLC devices over s7comm or modbus protocols.
- [s7scan](https://github.com/klsecservices/s7scan) - Replacement for PLCscan.
- [modscan](/tools/mirrored/modscan) - Tool to scan modbus devices and gather information.
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
- [Hello Proto - Banner Grabbing](https://github.com/industrialarmy/hello_proto) - banner grabbing tools for ICS protocols
- [Modbus Recon Functions](https://github.com/industrialarmy/recon_modbus_functions) - Modbus tool to poke a device for valid functions
- [SSASS-E](https://github.com/pnnl/ssass-e) - Safe Secure Autonomous Scanning Solution for Energy Delivery Systems (SSASSE). Note: UIUC was involved in this project and there are a bunch of great nuggets in the code.

## Robotics

- [Aztarna](https://github.com/aliasrobotics/aztarna) - A footprinting tool for robots

## IDS Signatures / Scripts

- [Quickdraw Snort](https://github.com/digitalbond/Quickdraw-Snort) - mirror: [v4.3.1](/configurations/rules/quickdraw_4_3_1.zip) - The Quickdraw IDS signature download includes the Modbus TCP, DNP3, EtherNet/IP, and ICS Vulnerability signatures. Each category is in its own rules file, and Digital Bond recommends only adding the signatures appropriate for your control system. See the [pcap quickdraw](/pcaps/quickdraw/) section for test pcaps.
- [Quickdraw Suricata Signatures for EtherNet/IP](https://github.com/digitalbond/Quickdraw-Suricata) - A set of EtherNet/IP IDS rules for use with Suricata.
- [RAPSN SETS](https://www.bsi.bund.de/EN/Topics/Industry_CI/ICS/Tools/RAPSN_SETS/RAPSN_SETS_node.html;jsessionid=2277684A363EEE2B6F130F09E6964DA5.internet471) - RAPSN SETS (Recognizing Anomalies in Protocols of Safety Networks: Schneider Electric‘s TriStation) is a set of rules for the Intrusion Detection System (IDS) Snort. They have been developed for Schneider Electric‘s proprietary TriStation protocol and are published under Mozilla Public License Version 2.0.
- [Cisco Talos Snort IDS Rules](/configurations/rules/talos-snort.rules) - These are a handful of community rules that correspond to the SCADA Strangelove default credentials. More community rules are available [here](https://www.snort.org/downloads/community/community-rules.tar.gz)
- [ARMORE](https://github.com/ITI/ARMORE) - ARMORE was developed to be an open-source software solution that will aid asset owners by increasing visibility, securing communications, and inspecting ICS communications for behavior that is not intended. Built around Bro and Linux.
- [EDMAND](https://github.com/ITI/EDMAND) - EDMAND Anomaly detection framework. Built around Bro.
- [AIUS](https://github.com/ITI/aius) - AIUS Repository (EDMAND/CAPTAR combination). Built around Bro.
- [ML NIDS For ICS](https://github.com/Rocionightwater/ML-NIDS-for-SCADA) - Machine learning techniques for Intrusion Detection in SCADA Systems.

## IDS Extensions

- [Profinet for Suricata](https://github.com/rain8841/Suricata_Profinet_MOD) - Profinet extensions for Suricata

## IoC Tools

- [FireEye IoC Editor](https://www.fireeye.com/services/freeware/ioc-editor.html) - IOCs are XML documents that help incident responders capture diverse information about threats, including attributes of malicious files, characteristics of registry changes and artifacts in memory. The IOC Editor provides an interface for managing data, including: 1) Manipulation of the logical structures that define the IOC, 2) Application of meta-information to IOCs, including detailed descriptions or arbitrary labels, 3) Conversion of IOCs into XPath filters, and 4) Management of lists of “terms” used within IOCs.

(creative commons license)
