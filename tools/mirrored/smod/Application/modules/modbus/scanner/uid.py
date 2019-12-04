import os
import threading

from System.Core.Global import *
from System.Core.Colors import *
from System.Core.Modbus import *
from System.Lib import ipcalc

class Module:


	info = {
		'Name': 'Brute Force UID',
		'Author': ['@enddo'],
		'Description': ("Brute Force UID"),

        }
	options = {
		'RHOSTS'	:[''		,True	,'The target address range or CIDR identifier'],
		'RPORT'		:[502		,False	,'The port number for modbus protocol'],
		'Function'	:[1		,False	,'Function code, Defualt:Read Coils.'],
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
		self.printLine('[+] Start Brute Force UID on : ' + ip,bcolors.OKGREEN)
		for i in range(1,255): # Total of 255 (legal) uid
			c = connectToTarget(ip,self.options['RPORT'][0])
			if(c == None):
				break
			try:
				
				c.sr1(ModbusADU(transId=getTransId(),unitId=i)/ModbusPDU_Read_Generic(funcCode=1),timeout=timeout, verbose=0)
				self.printLine('[+] UID on ' + ip + ' is : ' + str(i),bcolors.OKGREEN)
				closeConnectionToTarget(c)
			except Exception,e:
				closeConnectionToTarget(c)
				pass

		
