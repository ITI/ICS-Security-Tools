#smod
smod is a modular framework with every kind of diagnostic and offensive feature you could need in order to pentest modbus protocol. It is a full Modbus protocol implementation using Python and Scapy. This software could be run on Linux/OSX under python 2.7.x. 

Feel free to make pull requests, if there's anything you feel we could do better.

##Summery
SCADA (Process Control Networks) based systems have moved from proprietary closed networks to open source solutions and TCP/IP enabled networks steadily over recent years. This has made them vulnerable to the same security vulnerabilities that face our traditional computer networks.

The Modbus/TCP protocol was used as the reference protocol to display the effectiveness of the test bed in carrying out cyber attacks on a power system protocol. Modbus/TCP was chosen specifically for these reasons:
+ modbus is still widely used in power systems.
+ modbus/TCP is simple and easy to implement.
+ modbus protocol libraries are freely available for utilities to implement smart grid applications.

You can use this tool to vulnerability assessment a modbus protocol.

##Demo
Just a little demo showing off the basics
```
root@kali:~/smod# python smod.py 
 _______ 
< SMOD >
 ------- 
        \   ^__^
         \  (xx)\_______
            (__)\       )\/\
             U  ||----w |
                ||     ||
          --=[MODBUS Penetration Test FrameWork
       --+--=[Version : 1.0.4
       --+--=[Modules : 23
       --+--=[Coder   : Farzin Enddo
          --=[github  : www.github.com/enddo

SMOD >help
 Command  Description                                      
 -------  -----------                                      
 back     Move back from the current context               
 exit     Exit the console                                 
 exploit  Run module                                       
 help     Help menu                                        
 show     Displays modules of a given type, or all modules 
 set      Sets a variable to a value                       
 use      Selects a module by name                         
SMOD >show modules
 Modules                                       Description                                       
 -------                                       -----------                                       
 modbus/dos/arp                                DOS with Arp Poisoning                            
 modbus/dos/galilRIO                           DOS Galil RIO-47100
 modbus/dos/writeAllCoils                      DOS With Write All Coils                          
 modbus/dos/writeAllRegister                   DOS With Write All Register Function 
 modbus/dos/writeSingleCoils                   DOS With Write Single Coil Function               
 modbus/dos/writeSingleRegister                DOS Write Single Register Function                
 modbus/function/fuzzing                       Fuzzing Modbus Functions                          
 modbus/function/readCoils                     Fuzzing Read Coils Function                       
 modbus/function/readCoilsException            Fuzzing Read Coils Exception Function             
 modbus/function/readDiscreteInput             Fuzzing Read Discrete Inputs Function             
 modbus/function/readDiscreteInputException    Fuzzing Read Discrete Inputs Exception Function   
 modbus/function/readExceptionStatus           Fuzzing Read Exception Status Function            
 modbus/function/readHoldingRegister           Fuzzing Read Holding Registers Function           
 modbus/function/readHoldingRegisterException  Fuzzing Read Holding Registers Exception Function 
 modbus/function/readInputRegister             Fuzzing Read Input Registers Function             
 modbus/function/readInputRegisterException    Fuzzing Read Input Registers Exception Function   
 modbus/function/writeSingleCoils              Fuzzing Write Single Coil Function                
 modbus/function/writeSingleRegister           Fuzzing Write Single Register Function            
 modbus/scanner/arpWatcher                     ARP Watcher                                       
 modbus/scanner/discover                       Check Modbus Protocols                            
 modbus/scanner/getfunc                        Enumeration Function on Modbus                    
 modbus/scanner/uid                            Brute Force UID                                   
 modbus/sniff/arp                              Arp Poisoning   
SMOD >
```
Brute Force Modbus UID
```
SMOD >use modbus/scanner/uid
SMOD modbus(uid) >show options
 Name      Current Setting  Required  Description                                 
 ----      ---------------  --------  -----------                                 
 Function  1                False     Function code, Defualt:Read Coils.          
 Output    True             False     The stdout save in output directory         
 RHOSTS                     True      The target address range or CIDR identifier 
 RPORT     502              False     The port number for modbus protocol         
 Threads   1                False     The number of concurrent threads            
SMOD modbus(uid) >set RHOSTS 192.168.1.6
SMOD modbus(uid) >exploit 
[+] Module Brute Force UID Start
[+] Start Brute Force UID on : 192.168.1.6
[+] UID on 192.168.1.6 is : 10
SMOD modbus(uid) >
```
Enumeration Function on Modbus
```
SMOD >use modbus/scanner/getfunc
SMOD modbus(getfunc) >show options
 Name     Current Setting  Required  Description                                 
 ----     ---------------  --------  -----------                                 
 Output   True             False     The stdout save in output directory         
 RHOSTS                    True      The target address range or CIDR identifier 
 RPORT    502              False     The port number for modbus protocol         
 Threads  1                False     The number of concurrent threads            
 UID      None             True      Modbus Slave UID.                           
SMOD modbus(getfunc) >set RHOSTS 192.168.1.6
SMOD modbus(getfunc) >set UID 10
SMOD modbus(getfunc) >exploit 
[+] Module Get Function Start
[+] Looking for supported function codes on 192.168.1.6
[+] Function Code 1(Read Coils) is supported.
[+] Function Code 2(Read Discrete Inputs) is supported.
[+] Function Code 3(Read Multiple Holding Registers) is supported.
[+] Function Code 4(Read Input Registers) is supported.
[+] Function Code 5(Write Single Coil) is supported.
[+] Function Code 6(Write Single Holding Register) is supported.
[+] Function Code 7(Read Exception Status) is supported.
[+] Function Code 8(Diagnostic) is supported.
[+] Function Code 15(Write Multiple Coils) is supported.
[+] Function Code 16(Write Multiple Holding Registers) is supported.
[+] Function Code 17(Report Slave ID) is supported.
[+] Function Code 20(Read File Record) is supported.
[+] Function Code 21(Write File Record) is supported.
[+] Function Code 22(Mask Write Register) is supported.
[+] Function Code 23(Read/Write Multiple Registers) is supported.
SMOD modbus(getfunc) >
```

Fuzzing Read Coils Function 
```
SMOD >use modbus/function/readCoils
SMOD modbus(readCoils) >show options
 Name       Current Setting  Required  Description                                 
 ----       ---------------  --------  -----------                                 
 Output     True             False     The stdout save in output directory         
 Quantity   0x0001           True      Registers Values.                           
 RHOSTS                      True      The target address range or CIDR identifier 
 RPORT      502              False     The port number for modbus protocol         
 StartAddr  0x0000           True      Start Address.                              
 Threads    1                False     The number of concurrent threads            
 UID        None             True      Modbus Slave UID.                           
SMOD modbus(readCoils) >set RHOSTS 192.168.1.6
SMOD modbus(readCoils) >set UID 10
SMOD modbus(readCoils) >exploit 
[+] Module Read Coils Function Start
[+] Connecting to 192.168.1.6
[+] Response is :
###[ ModbusADU ]###
  transId   = 0x2
  protoId   = 0x0
  len       = 0x4
  unitId    = 0xa
###[ Read Coils Answer ]###
     funcCode  = 0x1
     byteCount = 1L
     coilStatus= [0]
SMOD modbus(readCoils) >
```
