s7-metasploit-modules
=====================

Siemens Simatic S7 Metasploit Modules

The Siemens Simatic S7 modules are fairly straightforward. Set the the IP address(s) in RHOSTS
and the cycle to the number of times you would like to start and stop the CPU. 

For example, if you only want to stop and start the CPU once use this command in metasploit.

	set CYCLES 1

msf  auxiliary(simatic_s7_300_command) > show options

Module options (auxiliary/admin/scada/simatic_s7_300_command):

   Name     Current Setting  Required  Description
   ----     ---------------  --------  -----------
   CYCLES   10               yes       Set the amount of CPU STOP/RUN cycles.
   MODE     false            no        Set true to put the CPU back into RUN mode.
   RHOSTS                    yes       The target address range or CIDR identifier
   RPORT    102              yes       The target port
   THREADS  1                yes       The number of concurrent threads

msf  auxiliary(simatic_s7_300_command) > set CYCLES 1
CYCLES => 1
msf  auxiliary(simatic_s7_300_command) > show options

Module options (auxiliary/admin/scada/simatic_s7_300_command):

   Name     Current Setting  Required  Description
   ----     ---------------  --------  -----------
   CYCLES   1                yes       Set the amount of CPU STOP/RUN cycles.
   MODE     false            no        Set true to put the CPU back into RUN mode.
   RHOSTS                    yes       The target address range or CIDR identifier
   RPORT    102              yes       The target port
   THREADS  1                yes       The number of concurrent threads

I'm currently in the process of updating the repo and adding other modules. Please test
on different S7-300 models and let me know which ones are supported so I can add a list. 

-Dillon

Please report any issues to dillon.beresford@gmail.com

