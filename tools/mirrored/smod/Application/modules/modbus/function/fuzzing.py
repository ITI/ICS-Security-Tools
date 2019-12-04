import os
import threading

from System.Core.Global import *
from System.Core.Colors import *
from System.Core.Modbus import *
from System.Lib import ipcalc

class Module:


	info = {
		'Name': 'Fuzzing Function',
		'Author': ['@enddo'],
		'Description': ("Fuzzing Modbus Functions"),

        }
	options = {
		'RHOSTS'	:[''		,True	,'The target address range or CIDR identifier'],
		'RPORT'		:[502		,False	,'The port number for modbus protocol'],
		'UID'		:[''		,True	,'Modbus Slave UID.'],
		'Quantity'	:['0x0001'	,True	,'Registers Values.'],
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
		self.printLine('[+] Connecting to ' + ip,bcolors.OKGREEN)
		self.printLine('[+] Fuzzing Read Coils on ' + ip,bcolors.OKGREEN)
		for i in range(0x0000,0x10000):
			c = connectToTarget(ip,self.options['RPORT'][0])
			self.printLine('[+] Address is ' + str(hex(i)),bcolors.OKGREEN)
			ans = c.sr1(ModbusADU(transId=getTransId(),unitId=int(self.options['UID'][0]))/ModbusPDU01_Read_Coils(startAddr=int(hex(i),16),quantity=int(self.options['Quantity'][0],16)),timeout=timeout, verbose=0)
			ans = ModbusADU_Answer(str(ans))
			self.printLine('[+] Response is :',bcolors.OKGREEN)
			ans.show()

		self.printLine('[+] Fuzzing Read Discrete Inputs on ' + ip,bcolors.OKGREEN)
		for i in range(0x0000,0x10000):
			c = connectToTarget(ip,self.options['RPORT'][0])
			self.printLine('[+] Address is ' + str(hex(i)),bcolors.OKGREEN)
			ans = c.sr1(ModbusADU(transId=getTransId(),unitId=int(self.options['UID'][0]))/ModbusPDU02_Read_Discrete_Inputs(startAddr=int(hex(i),16),quantity=int(self.options['Quantity'][0],16)),timeout=timeout, verbose=0)
			ans = ModbusADU_Answer(str(ans))
			self.printLine('[+] Response is :',bcolors.OKGREEN)
			ans.show()		

		self.printLine('[+] Fuzzing Read Holding Registers on ' + ip,bcolors.OKGREEN)
		for i in range(0x0000,0x10000):
			c = connectToTarget(ip,self.options['RPORT'][0])
			self.printLine('[+] Address is ' + str(hex(i)),bcolors.OKGREEN)
			ans = c.sr1(ModbusADU(transId=getTransId(),unitId=int(self.options['UID'][0]))/ModbusPDU03_Read_Holding_Registers(startAddr=int(hex(i),16),quantity=int(self.options['Quantity'][0],16)),timeout=timeout, verbose=0)
			ans = ModbusADU_Answer(str(ans))
			self.printLine('[+] Response is :',bcolors.OKGREEN)
			ans.show()

		
