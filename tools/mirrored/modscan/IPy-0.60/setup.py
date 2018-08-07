#!/usr/bin/env python
# $Id: setup.py 671 2004-08-22 21:02:29Z md $

import sys
if "--setuptools" in sys.argv:
    sys.argv.remove("--setuptools")
    from setuptools import setup
else:
    from distutils.core import setup

# Open IPy.py to read version
from imp import load_source
IPy = load_source("IPy", "IPy.py")

README = open('README').read().strip() + "\n\n"
ChangeLog = \
    "What's new\n" + \
    "==========\n" + \
    "\n" + \
    open('ChangeLog').read().strip()

LONG_DESCRIPTION = README + ChangeLog
CLASSIFIERS = [
    'Development Status :: 5 - Production/Stable',
    'Intended Audience :: Developers',
    'Intended Audience :: System Administrators',
    'Environment :: Plugins',
    'Topic :: Software Development :: Libraries :: Python Modules',
    'Topic :: Communications',
    'Topic :: Internet',
    'Topic :: System :: Networking',
    'License :: OSI Approved :: BSD License',
    'Operating System :: OS Independent',
    'Natural Language :: English',
    'Programming Language :: Python']
URL = "http://software.inl.fr/trac/trac.cgi/wiki/IPy"

setup(name="IPy",
      version=IPy.__version__,
      description="Class and tools for handling of IPv4 and IPv6 addresses and networks",
      long_description=LONG_DESCRIPTION,
      author="Maximillian Dornseif",
      maintainer="Victor Stinner",
      maintainer_email="victor.stinner AT inl.fr",
      license="BSD License",
      keywords="ipv4 ipv6 netmask",
      url=URL,
      download_url=URL,
      classifiers= CLASSIFIERS,
      py_modules=["IPy"])

