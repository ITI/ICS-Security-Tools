# serial2pcap
Serial2pcap converts serial IP data, typically collected from Industrial Control System devices, to the more commonly used Packet Capture (PCAP) format. 
It is designed to support multiple serial protocols and plug-ins can be developed by independent users.

### Dependencies
Python 2.7

Pyserial

### Usage
Please see the [usage file](./USAGE.md)

## Supported protocols
modbusASCII: Plugin to detect serial modbusASCII

byte: Single Byte - Generates A Packet For Every Byte

dnp3: Plugin to detect serial dnp3

selfm: SEL Fast Message

df1: Allen Bradly DF1 Serial Protocol

modbusRTU: Modbus RTU Frame Format Serial Protocol - Currently Only Works Well With 9600 and 19200 Baud Rate On Non Merged Taps

## Versioning
Currently on Verison 2.0

## License
See [LICENSE](./LICENSE.md).

## Disclaimer
See [DISCLAIMER](./DISCLAIMER.md).