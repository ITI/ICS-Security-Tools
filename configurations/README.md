# ICS Configurations

Developed as a community asset

## Common Files or Extensions

Note, if you find ICS-relevant file extensions that are not listed in the above list, please submit a pull to contribute those to the TSV's in this project.

- [Common File Extensions for Configurations/Projects](SCADA-Common-File-Extensions.tsv)
- [Other Files of Interest](SCADA-Other-Files-of-Interest.tsv)
- [File-extension seeker](https://file-extension.net/seeker/) - aggregate search engine to find info about file extensions.
- [Data Types](https://datatypes.net) - another file extension search engine
- [ICS File Extension Search](https://github.com/ISSVS/ICSExtearch) - Powershell script to search on a system for common ICS file extensions. Sources this list from here.

## Default Password Lists

- [SCADA Strangelove's SCADAPASS](/configurations/passwords/scadapass.csv)
- [Arnaud Soullie's ICS Default Passwords](/configurations/passwords/ics-default-passwords.csv)
- [TrackerNode Research](https://github.com/Trackernodes/trackernodesresearch)
- [CRITIFENCE Default Password Database](http://www.critifence.com/default-password-database/)

## IDS Signatures / Scripts

- [Quickdraw Snort](https://github.com/digitalbond/Quickdraw-Snort) - mirror: [v4.3.1](/rules/quickdraw_4_3_1.zip) - The Quickdraw IDS signature download includes the Modbus TCP, DNP3, EtherNet/IP, and ICS Vulnerability signatures. Each category is in its own rules file, and Digital Bond recommends only adding the signatures appropriate for your control system. See the [pcap quickdraw](/pcaps/quickdraw/) section for test pcaps.
- [Quickdraw Suricata Signatures for EtherNet/IP](https://github.com/digitalbond/Quickdraw-Suricata) - A set of EtherNet/IP IDS rules for use with Suricata.
- [RAPSN SETS](https://www.bsi.bund.de/EN/Topics/Industry_CI/ICS/Tools/RAPSN_SETS/RAPSN_SETS_node.html;jsessionid=2277684A363EEE2B6F130F09E6964DA5.internet471) - RAPSN SETS (Recognizing Anomalies in Protocols of Safety Networks: Schneider Electric‘s TriStation) is a set of rules for the Intrusion Detection System (IDS) Snort. They have been developed for Schneider Electric‘s proprietary TriStation protocol and are published under Mozilla Public License Version 2.0.
- [Cisco Talos Snort IDS Rules](/configurations/rules/talos-snort.rules) - These are a handful of community rules that correspond to the SCADA Strangelove default credentials. More community rules are available [here](https://www.snort.org/downloads/community/community-rules.tar.gz)
- [ARMORE](https://github.com/ITI/ARMORE) - ARMORE was developed to be an open-source software solution that will aid asset owners by increasing visibility, securing communications, and inspecting ICS communications for behavior that is not intended. Built around Bro and Linux.
- [EDMAND](https://github.com/ITI/EDMAND) - EDMAND Anomaly detection framework. Built around Bro.
- [AIUS](https://github.com/ITI/aius) - AIUS Repository (EDMAND/CAPTAR combination). Built around Bro.
- [ML NIDS For ICS](https://github.com/Rocionightwater/ML-NIDS-for-SCADA) - Machine learning techniques for Intrusion Detection in SCADA Systems.

## Recommended Best Practices

- [Security Technical Implementation Guides (STIG)](http://iase.disa.mil/stigs/Pages/index.aspx) - The Security Technical Implementation Guides (STIGs) and the NSA Guides are the configuration standards for DOD IA and IA-enabled devices/systems. Since 1998, DISA has played a critical role enhancing the security posture of DoD's security systems by providing the Security Technical Implementation Guides (STIGs). The STIGs contain technical guidance to "lock down" information systems/software that might otherwise be vulnerable to a malicious computer attack.

(creative commons license)
