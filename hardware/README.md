# ICS Hardware

Developed as a community asset

## Research

- [Secure Network Scanner Architecture](https://github.com/hsainnos/ICSscannerDiode) - Architecture for a secure ICS network scanner with an intelligent data diode
- [HYDRA Water Distribution Testbed](https://github.com/hydra-testbed/Part-list) - detailed part list for HYDRA water testbed

## Firmware

- [ICS Legacy Firmware](https://github.com/cutaway-security/ics_firmware_legacy) - some legacy firmware provided for educational purposes.

## Starting Hardware

- [Siemens S7-1200](http://w3.siemens.com/mcms/programmable-logic-controller/en/basic-controller/s7-1200/pages/default.aspx) PLCs - Cheap and commonly used. You can buy a "starter kit" that includes TIA Portal (software to configure the PLC). Trial version of the software is also available. Check eBay or refurb vendors for deals.
- [MicroLogix 1100](http://ab.rockwellautomation.com/Programmable-Controllers/MicroLogix-1100) - Another alternative to the S7-1200 and also available in a starter kit. Again, check eBay or refurb vendors for deals.

## Basic Lab Setup

- [Modicon Quantum](http://www.schneider-electric.com/en/product-range/538-modicon-quantum/)
- [Koyo Direct](<http://www.automationdirect.com/adc/Overview/Catalog/Programmable_Controllers/DirectLogic_Series_PLCs_(Micro_to_Small,_Brick_-a-_Modular)>)
- Any no-name PLC with ModBus

## Supporting Equipment

### Basic

- Network Tap or Hub for capturing network traffic
- [Throwing Star LAN Tap](https://greatscottgadgets.com/throwingstar/) - a small, simple passive Ethernet tap for monitoring Ethernet communications, requiring no power for operation.
- [BusPirate](http://dangerousprototypes.com/docs/Bus_Pirate), or equivalent
- A few USB-Ethernet dongles
- VMWare Workstation, or other virtualization software
- Several Serial Cables, and USB adapters as well if you don't have a 'real' serial port, along with wireable serial connectors from Radio Shack
- About 50 feet of networking cable with all the tools (you won't be doing just Ethernet stuff, but its good wiring for all kinds of things)
- [NooElec SDR](http://www.nooelec.com/store/sdr.html) Dongle
- Basic Switch, 10/100 is fine for automation
- A Basic Serial to Ethernet converter (Digi, MOXA, and several others have decent products)
- Basic Multimeter (e.g., [INNOVA 3320](http://www.amazon.com/INNOVA-3320-Auto-Ranging-Digital-Multimeter/dp/B000EVYGZA))
- A few 120VAC appliance replacement cords. (plug on one end and wire on the other. Lots of this equipment doesn't have standard computer plugs, so having this makes it easy)
- [Raspberry Pi](https://www.raspberrypi.org/products/) or [BeagleBone Black](https://www.sparkfun.com/products/12857) and Breadboard
- [A low-cost, DIY data diode for ICS](https://github.com/wavestone-cdt/dyode)

### Advanced

- Oscilloscope ([RIGOL](http://www.rigolna.com) are decent and cheap.)
- Universal JTAG Adapter (e.g., [Wiggler](http://www.diygadget.com/universal-jtag-adapter-v2-wiggler-and-xilinx-platform-cable-compatible-debrick-routers-modems-and-more.html), [Flyswatter 2](http://www.tincantools.com/JTAG/Flyswatter2.html), [JTAGulator](http://www.grandideastudio.com/portfolio/jtagulator/)) and supporting software (e.g., [OpenOCD](http://openocd.org)).
- Logic Analyzer ([Saleae](https://www.saleae.com) are good for normal use, stick with an 8 channel to start)
- Adjustable Power Supply
- [YardStickOne](https://greatscottgadgets.com/yardstickone/)/[RFCat](http://int3.cc/products/rfcat)/[HackRF](https://greatscottgadgets.com/hackrf/)
- Another laptop for bare Linux
- [IDAPro](https://www.hex-rays.com/products/ida/)

(creative commons license)
