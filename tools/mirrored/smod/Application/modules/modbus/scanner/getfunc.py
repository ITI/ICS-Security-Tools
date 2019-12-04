import os
import threading

from System.Core.Global import *
from System.Core.Colors import *
from System.Core.Modbus import *
from System.Lib import ipcalc

class Module:


	info = {
		'Name': 'Get Function',
		'Author': ['@enddo'],
		'Description': ("Enumeration Function on Modbus"),

        }
	options = {
		'RHOSTS'	:[''		,True	,'The target address range or CIDR identifier'],
		'RPORT'		:[502		,False	,'The port number for modbus protocol'],
		'UID'		:[None		,True	,'Modbus Slave UID.'],
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
			print str.replace('[-]',color + '[+]' + bcolors.ENDC)
		else:
			print str

	def do(self,ip):
		c = connectToTarget(ip,self.options['RPORT'][0])
		if(c == None):
			self.printLine('[-] Modbus is not running on : ' + ip,bcolors.WARNING)
			return None
		self.printLine('[+] Looking for supported function codes on ' + ip,bcolors.OKGREEN)
		for i in range(0,256): # Total of 127 (legal) function codes
			ans = c.sr1(ModbusADU(transId=getTransId(),unitId=int(self.options['UID'][0]))/ModbusPDU_Read_Generic(funcCode=i),timeout=timeout, verbose=0)
	
			# We are using the raw data format, because not all function
			# codes are supported out by this library.
			if ans:
				data = str(ans)
				data2 = data.encode('hex')
				returnCode = int(data2[14:16],16)
				exceptionCode = int(data2[17:18],16)
	
				if returnCode > 127 and exceptionCode == 0x01:
					# If return function code is > 128 --> error code
					#print "Function Code "+str(i)+" not supported."
					a=1
				else:
					if(function_code_name.get(i) != None):
						self.printLine("[+] Function Code "+str(i)+"("+function_code_name.get(i)+") is supported.",bcolors.OKGREEN)
						
					else:
						self.printLine("[+] Function Code "+str(i)+" is supported.",bcolors.OKGREEN)
						
			else:
				self.printLine("[+] Function Code "+str(i)+" probably supported.",bcolors.OKGREEN)
				

		
		
