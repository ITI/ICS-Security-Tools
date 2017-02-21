#!/usr/bin/env python
import doctest
import sys
if hasattr(doctest, "testfile"):
    print "=== Test file: README ==="
    failure,  tests = doctest.testfile('README', optionflags=doctest.ELLIPSIS)
    if failure:
        sys.exit(1)

    print "=== Test file: test.rst ==="
    failure, tests = doctest.testfile('test/test.rst', optionflags=doctest.ELLIPSIS)
    if failure:
        sys.exit(1)

    print "=== Test IPy module ==="
    import IPy
    failure, tests = doctest.testmod(IPy)
    if failure:
        sys.exit(1)
else:
    sys.stderr.write("WARNING: doctest has no function testfile (before Python 2.4), unable to check README\n")

