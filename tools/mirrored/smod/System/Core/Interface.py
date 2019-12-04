import sys
import os
from Global import *
sys.path.append(mainPath + '/System/')
import readline
import re
import glob
import threading
import Loader
from Colors import bcolors
from Banner import Banner
from Lib import prettytable


class Command:
	COMMANDS 	= ['back','exit','exploit','help','show','set','use']
	helpCommand 	= [
		['back','Move back from the current context'],
		['exit','Exit the console'],
		['exploit','Run module'],
		['help','Help menu'],
		['show','Displays modules of a given type, or all modules'],
		['set','Sets a variable to a value'],
		['use','Selects a module by name']
		]
	def help(self,args,pointer = None):
		table 	 = prettytable.PrettyTable([bcolors.BOLD + 'Command' + bcolors.ENDC,bcolors.BOLD + 'Description' + bcolors.ENDC])
		table.border = False
		table.align  = 'l'
		table.add_row(['-'*7,'-'*11])
		for i in self.helpCommand:
			table.add_row([bcolors.OKBLUE +  i[0] + bcolors.ENDC,i[1]])
			
		print table
	def exit(self,args,pointer = None):
		sys.exit(0)
	def back(self,args,pointer = None):
		global POINTER
		POINTER = None
	def show(self,args,pointer = None):
		if(len(args) < 2):
			return None
		if(args[1] == 'modules'):
			table 	 = prettytable.PrettyTable([bcolors.BOLD + 'Modules' + bcolors.ENDC,bcolors.BOLD + 'Description' + bcolors.ENDC])
			table.border = False
			table.align  = 'l'
			table.add_row(['-'*7,'-'*11])
			for i in sorted(modules):
				table.add_row([bcolors.OKBLUE + i + bcolors.ENDC,modules[i].info['Description']])
				
			print table		
		if(args[1] == 'options'):
			if(pointer):
				table 	 = prettytable.PrettyTable([bcolors.BOLD + 'Name' + bcolors.ENDC,bcolors.BOLD + 'Current Setting' + bcolors.ENDC,bcolors.BOLD + 'Required' + bcolors.ENDC,bcolors.BOLD + 'Description' + bcolors.ENDC])
				table.border = False
				table.align  = 'l'
				table.add_row(['-'*4,'-'*15,'-'*8,'-'*11])
				for i in sorted(modules[pointer].options):
					table.add_row([bcolors.OKBLUE +  i + bcolors.ENDC,modules[pointer].options[i][0],modules[pointer].options[i][1],modules[pointer].options[i][2]])
				
				print table			
	def use(self,args,pointer = None):
		global POINTER
		if(len(args) < 2):
			return None
		POINTER = args[1]
		moduleName = args[1].split('/')
		comp 	= Completer()
		readline.set_completer_delims(' \t\n;')
		readline.parse_and_bind("tab: complete")
		readline.set_completer(comp.complete)
		while True:
			input	= raw_input('SMOD ' + moduleName[0] + '(' + bcolors.OKBLUE + moduleName[-1] + bcolors.ENDC + ') > ').strip().split()
			try:			
				result 	= getattr(globals()['Command'](),input[0])(input,args[1])
			except:
				return None
			if (POINTER == None):
				break
	
	def set(self,args,pointer = None):
		if(len(args) < 2):
			return None
		if(pointer):
			modules[pointer].options[args[1]][0] = args[2]

	def exploit(self,args,pointer = None):
		if(pointer):
			flag = True
			for i in modules[pointer].options:
				if(modules[pointer].options[i][1] and modules[pointer].options[i][0] == ''):
					print bcolors.FAIL + '[-]' + bcolors.ENDC + ' set ' + i
					flag = False
			if(flag):
				modules[pointer].exploit()

class Completer(object):
	RE_SPACE 	= re.compile('.*\s+$', re.M)
	def _listdir(self, root):
		res = []
	        for name in os.listdir(root):
			path = os.path.join(root, name)
	            	if os.path.isdir(path):
	                	name += os.sep
	            		res.append(name[:-1])
			else:
				if(name.endswith('.py')):
					res.append(name[:-3])
        	return res

	def _complete_path(self, path):
        	dirname, rest = os.path.split(path)
        	tmp = dirname if dirname else '.'
        	res = [os.path.join(dirname, p)
                	for p in self._listdir(tmp) if p.startswith(rest)]
        	if len(res) > 1 or not os.path.exists(path):
            		return res

        	if os.path.isdir(path):
            		return [os.path.join(path, p) for p in self._listdir(path)]

        	return [path + ' ']

    	def complete_use(self, args):
        	if not args:
            		return self._complete_path(modulesPath)
        	
        	result = self._complete_path(modulesPath + args[-1])
		for i in range(len(result)):
			result[i] = result[i].replace(modulesPath,'')
		return result

	def complete_show(self,args):
		if (args[0] == ''):
			return ['modules','options']
		if('modules'.find(args[0]) == 0):
			return ['modules']
		elif('options'.find(args[0]) == 0):
			return ['options']

	def complete_set(self,args):
		if(POINTER):
			result 	= list()
			for i in modules[POINTER].options:
				if(i.find(args[0]) == 0):
					result.append(i)
			return result
    	def complete(self, text, state):
        
        	buffer = readline.get_line_buffer()
        	line = readline.get_line_buffer().split()
        
        	if self.RE_SPACE.match(buffer):
            		line.append('')
        
        	cmd = line[0].strip()
        	if cmd in Command.COMMANDS:
            		impl = getattr(self, 'complete_%s' % cmd)
            		args = line[1:]
            		if args:
                		return (impl(args) + [None])[state]
            		return [cmd + ' '][state]
        	results = [c + ' ' for c in Command.COMMANDS if c.startswith(cmd)] + [None]
        	return results[state]

def init():
	global pluginNumber
	global modules
	plugins 	= Loader.plugins(modulesPath)
	plugins.crawler()
	plugins.load()
	pluginNumber 	= len(plugins.pluginTree)
	modules 	= plugins.modules
	Banner(VERSION,pluginNumber)
def mainLoop():
	comp 	= Completer()
	readline.set_completer_delims(' \t\n;')
	readline.parse_and_bind("tab: complete")
	readline.set_completer(comp.complete)
	while True:
		input	= raw_input('SMOD > ').strip().split()
		if(input):
			if(input[0] in Command.COMMANDS):
				result 	= getattr(globals()['Command'](),input[0])(input)
	
	
