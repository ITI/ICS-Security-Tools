# ICS Tools - Other

Developed as a community asset

## General

- [Cutaway Security Tools and Scripts](https://github.com/cutaway-security/cutsec_tools) - Scripts and other tools to help parse data or gather information.
- [ICS Security Resources](https://github.com/selmux/ICS-Security) - Various wordlists, models, tools, and scripts from academic work.

## Evasion

- [Modshaft](https://github.com/reidmefirst/modshaft/) - Modshaft is an IP-over-Modbus/TCP tunnel. It is useful for evading application-layer firewalls.
- [ICS Evasion Attacks](https://github.com/scy-phy/ICS-Evasion-Attacks) - Implementation of white box and black box classifier evasion from SUTD. Paper in repo.

## Spoofing

- [Modbus-VCR](https://github.com/reidmefirst/modbus-vcr/) - The Modbus VCR records and replays Modbus traffic
- [Ettercap plugin for IEC 60870-5-104](https://github.com/PMaynard/ettercap-104-mitm) - Ettercap Plugin for Man-In-The-Middle Attacks on IEC 60870-5-104

## PLC Injection

- [PlcInjector](https://github.com/BorjaMerino/PlcInjector) - Modbus stager in assembly and some scripts to upload/download data to the holding register of a PLC. More info [here](http://www.shelliscoming.com/2016/12/modbus-stager-using-plcs-as.html).
- [plcinject](https://github.com/SCADACS/PLCinject) - S7 PLC injection using Snap7

## Exploit frameworks

- [Sixnet Tools](https://github.com/mssabr01/sixnet-tools) - Tool for exploiting sixnet RTUs

## Demonstrations

- [Defcon26 Tools](https://github.com/thiagoralves/defcon26) - Tools demonstrated at DEF CON 26 talk "Hacking PLCs and Causing Havoc on Critical Infrastructures"

## Common Toolsets

- [Metasploit](http://www.metasploit.com) - Exploitation framework.
- [Bettercap](https://github.com/evilsocket/bettercap) - A complete, modular, portable and easily extensible MITM framework.
- [ISF (Industrial Exploitation Framework)](https://github.com/dark-lbp/isf) - an exploitation framework based on open source project routersploit
- [ISF(Industrial Security Exploitation Framework)](https://github.com/w3h/isf) - ISF(Industrial Security Exploitation Framework) is an exploitation framework based on Python, claiming to be based on the NSA Equation Group Fuzzbunch toolkit, developed by the ICSMASTER team.
- [EtherSploit/IP](https://github.com/thiagoralves/EtherSploit-IP) - An interactive shell with a bunch of helpful commands to exploit EtherNet/IP vulnerabilities (more specifically Allen-Bradley MicroLogix implementation of ENIP)

## Metasploit Modules

- [Gleg SCADA+ Pack](http://gleg.net/agora_scada.shtml) - **Commercial**
- [S7 Metasplot pack](../mirrored/s7-metasploit-modules) - Initial s7 metasploit modules.
- [Schneider Electric PLC / Modbus modules from DEFCON 25](https://github.com/arnaudsoullie/funwithmodbus0x5a) - Downloading a program from the PLC, gathering information about the PLC and forcing the values of the digital outputs, START/STOP
- [IEC 104 Module](https://github.com/michaelj0hn/iec104) - IEC104 Client for Metasploit [merged into mainline](https://github.com/rapid7/metasploit-framework/pull/10386)
- [random modbus tools](https://github.com/arnaudsoullie/funwithmodbus0x5a) - ICS Village talk at DEFCON 25

## Other

- [Siemens S7 PLC Bootloader Code Execution Utility](https://github.com/RUB-SysSec/SiemensS7-Bootloader) - Non-invasive arbitrary code execution on the Siemens S7 PLC by using an undocumented bootloader protocol over UART. Siemens assigned SSA-686531 (CVE-2019-13945) for this vulnerability. Affected devices are Siemens S7-1200 (all variants including SIPLUS) and S7-200 Smart.

Note: The following tools haven't necessarily been utilized in an ICS context, but could be helpful.

- [Laika Boss](https://github.com/lmco/laikaboss) - Laika is an object scanner and intrusion detection system that strives to achieve the goal of a scalable, flexible, and verbose system.

(creative commons license)
