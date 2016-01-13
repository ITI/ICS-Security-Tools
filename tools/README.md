# ICS Tools
## Developed as a community asset at S4x16

## General
* [OpenSCADA](http://oscada.org/main/) - OpenSCADA system is an open implementation of SCADA (Supervisory Control And Data Acquisition) and HMI (Human-Machine Interface) systems. The main properties of the system are: openness(GPL), multiplatform, modularity and scalability.

* [xutools](https://github.com/gabriel-weaver/xutools) - eXtended UNIX text-processing tools

## Auditing
* [Bandolier Security Audit Files](http://www.digitalbond.com/tools/bandolier/) - These audit files are used with the Nessus scanner’s compliance plugins to audit the security settings of control system components. A typical control system will have over 1,000 security settings including the OS settings, database and webserver settings, and the SCADA or DCS application settings. Digital Bond worked with the vendors, such as ABB, AREVA, Emerson, OSIsoft, Telvent, …, to identify the optimal security settings for their systems. Bandolier Security Audit Files are very useful at FAT to insure the system is installed in an optimal security configuration and periodically to verify the configuration has not degraded.
* [Portaledge](http://www.digitalbond.com/tools/portaledge/) - The OSIsoft PI Server is an extremely popular historian that aggregates and correlates process data. In Portaledge, Digital Bond has created modules to aggregate security events and correlate these events to detect cyber attacks. There are a variety of modules including modules that meet the NERC CIP monitoring requirements.
* [Quickdraw ICS IDS](http://www.digitalbond.com/tools/quickdraw/) - Digital Bond’s original research project was to develop a set of IDS rules for SCADA protocols. The initial rules for Modbus TCP and DNP3 have now been enhanced for EtherNet/IP, Vulnerability rules and Device Specific rules. Quickdraw also includes Snort preprocessors and plugins that allow rules for more complex control system protocols.

## Analysis
### Logs
* [Plaso - Log2timeline](https://github.com/log2timeline/plaso/wiki) - log2timeline is a tool designed to extract timestamps from various files found on a typical computer system(s) and aggregate them.

### Malware
* [YARA](https://plusvic.github.io/yara/) - YARA is a tool aimed at (but not limited to) helping malware researchers to identify and classify malware samples. With YARA you can create descriptions of malware families (or whatever you want to describe) based on textual or binary patterns. Each description, a.k.a rule, consists of a set of strings and a boolean expression which determine its logic.



## Honeypots
* [SCADA Honeynet](http://www.digitalbond.com/tools/scada-honeynet/) - The SCADA Honeynet appears to be a PLC. It is highly realistic with support for the management interfaces, a points list taken from an actual installation, and default parameters unchanged.
* [Conpot](https://github.com/mushorg/conpot) - Conpot is an ICS honeypot with the goal to collect intelligence about the motives and methods of adversaries targeting industrial control systems.


## Simulation Tools
* [OpenDSS](http://smartgrid.epri.com/SimulationTool.aspx) - The OpenDSS is a comprehensive electrical power system simulation tool primarly for electric utility power distribution systems. It supports nearly all frequency domain (sinusoidal steady‐state) analyses commonly performed on electric utility power distribution systems.
* [GridLab-D](http://www.gridlabd.org) - GridLAB-D™ is a new power distribution system simulation and analysis tool that provides valuable information to users who design and operate distribution systems, and to utilities that wish to take advantage of the latest energy technologies. It incorporates the most advanced modeling techniques, with high-performance algorithms to deliver the best in end-use modeling. GridLAB-D™ is coupled with distribution automation models and software integration tools for users of many power system analysis tools.
* [PowerWorld Simulator](http://www.powerworld.com) - PowerWorld Simulator is an interactive power system simulation package designed to simulate high voltage power system operation on a time frame ranging from several minutes to several days. The software contains a highly effective power flow analysis package capable of efficiently solving systems of up to 250,000 buses. **[Commercial, Free for educational use.]**
* [Siemens PSSE](http://w3.siemens.com/smartgrid/global/en/products-systems-solutions/software-solutions/planning-data-management-software/planning-simulation/pages/pss-e.aspx) - PSS®E is a trusted leader in the power industry for cutting-edge electric transmission system analysis and planning. Used in over 115 countries worldwide, PSS®E is powerful, customizable, and fully-featured. With the addition of integrated node-breaker support in version 34, PSS®E is leading the market in advances in electric transmission modeling and simulation. **[Commercial]**
* [GE PSLF](http://www.geenergyconsulting.com/practice-area/software-products/pslf) - Effective power system analysis often requires large-scale simulations and manipulation of large volumes of data. When performing these analyses, efficient algorithms are just as important as the engineering models in which the data is used. GE Energy recognizes these imperatives, and has developed Concorda PSLF. The algorithms in the PSLF suite have been developed to handle large utility-scale systems of up to 80,000 buses. A complete set of tools allows the user to switch smoothly between data visualization, system simulation, and results analysis. **[Commercial]**

## Other
Note: The following tools haven't necessarily been utilized in an ICS context, but could be helpful.

* [Laika Boss](https://github.com/lmco/laikaboss) - Laika is an object scanner and intrusion detection system that strives to achieve the goal of a scalable, flexible, and verbose system.

(creative commons license)

