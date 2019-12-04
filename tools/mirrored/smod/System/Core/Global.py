import os
import sys

mainPath 	= os.path.abspath(os.path.join(os.path.dirname(__file__), os.path.pardir, os.path.pardir))
modulesPath 	= os.path.abspath(os.path.join(os.path.dirname(__file__), os.path.pardir, os.path.pardir)) + '/Application/modules/'
VERSION 	= '1.0.4'
pluginNumber 	= 0
modules 	= None
POINTER		= None
THREADS 	= list()
timeout  	= 1
