import os
import threading

from System.Core.Global import *
from System.Core.Colors import *
from System.Core import Modbus
from System.Lib import ipcalc

class Module:


	info = {
		'Name': 'Modbus Discover',
		'Author': ['@enddo'],
		'Description': ("Check Modbus Protocols"),

        }
	options = {
		'RHOSTS'	:[''		,True	,'The target address range or CIDR identifier'],
		'RPORT'		:[502		,False	,'The port number for modbus protocol'],
		'Threads'	:[1		,False	,'The number of concurrent threads'],
		'Output'	:[True		,False	,'The stdout save in output directory']
	}	
	output = ''

	def exploit(self):

		moduleName 	= self.info['Name']
		print bcolors.OKBLUE + '[+]' + bcolors.ENDC + ' Module ' + moduleName + ' Start'
		ips = list()
		for ip in ipcalc.Network(self.options['RHOSTS'][0]):
			ips.append(str(ip))
		while ips:
			for i in range(int(self.options['Threads'][0])):
				if(len(ips) > 0):
					thread 	= threading.Thread(target=self.do,args=(ips.pop(0),))
					thread.start()
					THREADS.append(thread)
				else:
					break
			for thread in THREADS:
				thread.join()
		if(self.options['Output'][0]):
			open(mainPath + '/Output/' + moduleName + '_' + self.options['RHOSTS'][0].replace('/','_') + '.txt','a').write('='*30 + '\n' + self.output + '\n\n')
		self.output 	= ''

	def printLine(self,str,color):
		self.output += str + '\n'
		if(str.find('[+]') != -1):
			print str.replace('[+]',color + '[+]' + bcolors.ENDC)
		elif(str.find('[-]') != -1):
			print str.replace('[-]',color + '[-]' + bcolors.ENDC)
		else:
			print str

	def do(self,ip):
		result = Modbus.connectToTarget(ip,self.options['RPORT'][0])
		if (result != None):
			self.printLine('[+] Modbus is running on : ' + ip,bcolors.OKGREEN)
			
		else:
			self.printLine('[-] Modbus is not running on : ' + ip,bcolors.WARNING)
		
