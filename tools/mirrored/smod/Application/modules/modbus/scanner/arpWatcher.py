import threading
import os

from System.Core.Global import *
from System.Core.Colors import *
from System.Lib.scapy.all import sniff,ARP

class Module:


	info = {
		'Name': 'ARP Watcher',
		'Author': ['@enddo'],
		'Description': ("ARP Watcher"),

        }
	options = {
		'Output'	:[True		,False	,'The stdout save in output directory']
	}	
	output = ''


	def exploit(self):

		moduleName 	= self.info['Name']
		print bcolors.OKBLUE + '[+]' + bcolors.ENDC + ' Module ' + moduleName + ' Start'
		thread 	= threading.Thread(target=self.do,args=())
		thread.start()
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

	def do(self):

		def watch_arp(pkt):
			self.printLine("[+] "+pkt.summary(),bcolors.OKGREEN)
		sniff(filter="arp",prn=watch_arp,store=0)
		
