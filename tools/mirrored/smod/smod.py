import sys
import os

sys.path.append(os.path.abspath(os.path.dirname(__file__) + '/System'))
from System.Core import Interface

Interface.init()
Interface.mainLoop()
