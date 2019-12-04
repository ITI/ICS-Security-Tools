import os
import imp

class plugins:
	pluginTree	= list()
	modules		= dict()
	def __init__(self,path):
		self.path 	= path
	def crawler(self):
		for top,dirs,files in os.walk(self.path):
			for sub in files:
				if(sub.endswith('.py')):
					self.pluginTree.append(os.path.join(top,sub).replace(self.path,'').replace('.py','').split('/'))
		
	def load(self):
		for plugin in self.pluginTree:
			name = plugin[-1]
			item 	= '/'.join(plugin)
			self.modules.update({item:imp.load_source(name, self.path + item + '.py').Module()})
		
