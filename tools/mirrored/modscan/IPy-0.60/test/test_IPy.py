"""Unit test for IPy.py

Further Information might be available at http://c0re.jp/c0de/IPy/

Hacked 2001 by drt@un.bewaff.net
"""

# TODO: unify assert / FilIf usage

import sys
import threading
sys.path.append('.')
sys.path.append('..')

import IPy
import unittest
import random

testloops = 250

class parseAddress(unittest.TestCase):
    okValues = [('FEDC:BA98:7654:3210:FEDC:BA98:7654:3210', 338770000845734292534325025077361652240L),
                ('FEDCBA9876543210FEDCBA9876543210', 338770000845734292534325025077361652240L),
                ('0xFEDCBA9876543210FEDCBA9876543210', 338770000845734292534325025077361652240L),
                ('1080:0000:0000:0000:0008:0800:200C:417A', 21932261930451111902915077091070067066L),
                ('1080:0:0:0:8:800:200C:417A', 21932261930451111902915077091070067066L),
                ('1080:0::8:800:200C:417A', 21932261930451111902915077091070067066L),
                ('1080::8:800:200C:417A', 21932261930451111902915077091070067066L),
                ('FF01:0:0:0:0:0:0:43', 338958331222012082418099330867817087043L),
                ('FF01:0:0::0:0:43', 338958331222012082418099330867817087043L),
                ('FF01::43', 338958331222012082418099330867817087043L),
                ('0:0:0:0:0:0:0:1', 1L),
                ('0:0:0::0:0:1', 1L),
                ('::1', 1L),
                ('0:0:0:0:0:0:0:0', 0L),
                ('0:0:0::0:0:0', 0L),
                ('::', 0L),
                ('0:0:0:0:0:0:13.1.68.3', 218186755L),
                ('::13.1.68.3', 218186755L),
                ('0:0:0:0:0:FFFF:129.144.52.38', 281472855454758L),
                ('::FFFF:129.144.52.38', 281472855454758L),
                ('1080:0:0:0:8:800:200C:417A', 21932261930451111902915077091070067066L),
                ('1080::8:800:200C:417A', 21932261930451111902915077091070067066L),
                ('0.0.0.0', 0L),
                ('0', 0L),
                ('127.0.0.1', 2130706433L),
                ('255.255.255.255', 4294967295L),
                ('0.0.0.1', 1L),
                ('1', 16777216L),
                ('213.221.113.87', 3588059479L),
                ('0000', 0L),
                ('127001', 127001L),
                ('1234576', 1234576L),
                ('1', 16777216L),
                ('232111387', 232111387L),
                ('255', 4278190080L),
                ('256', 256L),
                ('0xffffffff', 4294967295L),
                ('0x100000000', 4294967296L),
                ('0xffffffffffffffffffffffffffffffff', 0xffffffffffffffffffffffffffffffffL),
                ('0xdeadbeef', 0xdeadbeefL),
                ('0xdeadbabe', 0xdeadbabeL),
                ('0xdeadc0de', 0xdeadc0deL),
                ('0xc0decafe', 0xc0decafeL),
                ('0xc0debabe', 0xc0debabeL),
                ('0xbabec0de', 0xbabec0deL),
                ('0xcafebabe', 0xcafebabeL),
                ('0x1', 1L),
                ('0xabcdef', 11259375L)]

    # TODO: check for more invalid input

    def testKnownValues(self):
        """parsing of known values should give known results"""
        for x in self.okValues:
            (question, answer) = x
            (result, version) = IPy.parseAddress(question)
            self.assertEqual(answer, result, "%r, %r, %r" % (question, answer, result))

    def testVersionDistinction(self):
        """problems destinguishing IPv4 and IPv6"""
        (result, version) = IPy.parseAddress('0xffffffff')
        self.assertEqual(version, 4)
        (result, version) = IPy.parseAddress('0x100000000')
        self.assertEqual(version, 6)

    def testEmpty(self):
        """'' should raise an exception"""
        self.assertRaises(ValueError, IPy.parseAddress, '')

    def testTooBig(self):
        """'' should raise an exception"""
        self.assertRaises(ValueError, IPy.parseAddress, '0x100000000000000000000000000000000')

    def testLongIPv4(self):
        """'1.2.3.4.5' should raise an exception"""
        self.assertRaises(ValueError, IPy.parseAddress, '1.2.3.4.5')

    def testNonByteIPv4(self):
        """'1.2.3.256' should raise an exception"""
        self.assertRaises(ValueError, IPy.parseAddress, '1.2.3.256')

    def testNegativeByteIPv4(self):
        """'-1.2.3.4' and '1.2.3.-4' should raise an exception"""
        self.assertRaises(ValueError, IPy.parseAddress, '-1.2.3.4')
        self.assertRaises(ValueError, IPy.parseAddress, '1.2.3.-4')

    def testTripleColonIPv6(self):
        """'2001:::1' should raise an exception"""
        self.assertRaises(ValueError, IPy.parseAddress, '2001:::1')

    def testRepeatDoubleColonIPv6(self):
        """'2001::ABCD::1' should raise an exception"""
        self.assertRaises(ValueError, IPy.parseAddress, '2001::ABCD::1')

    def testDoubleColonWithEightHextetsIPv6(self):
        """'1111::2222:3333:4444:5555:6666:7777:8888' should raise an exception"""
        self.assertRaises(ValueError, IPy.parseAddress, '1111::2222:3333:4444:5555:6666:7777:8888')

    def testBeginningColonWithEightHextetsIPv6(self):
        """':1111:2222:3333:4444:5555:6666:7777:8888' should raise an exception"""
        self.assertRaises(ValueError, IPy.parseAddress, ':1111:2222:3333:4444:5555:6666:7777:8888')

    def testEndingColonWithEightHextetsIPv6(self):
        """'1111:2222:3333:4444:5555:6666:7777:8888:' should raise an exception"""
        self.assertRaises(ValueError, IPy.parseAddress, '1111:2222:3333:4444:5555:6666:7777:8888:')

    def testNegativeHexletIPv6(self):
        """'2001:-ABCD::1' should raise an exception"""
        self.assertRaises(ValueError, IPy.parseAddress, '2001:-ABCD::1')

    def testTooBigHexletIPv6(self):
        """'2001:10000::1' should raise an exception"""
        self.assertRaises(ValueError, IPy.parseAddress, '2001:10000::1')

    def testShortAddressIPv6(self):
        """'1111:2222:3333:4444:5555:6666:7777' should raise an exception"""
        self.assertRaises(ValueError, IPy.parseAddress, '1111:2222:3333:4444:5555:6666:7777')

    def testLongAddressIPv6(self):
        """'1111:2222:3333:4444:5555:6666:7777:8888:9999' should raise an exception"""
        self.assertRaises(ValueError, IPy.parseAddress, '1111:2222:3333:4444:5555:6666:7777:8888:9999')

class _intToIP(unittest.TestCase):
    v4values = [(0x7f000001, '127.0.0.1'),
                (0x0, '0.0.0.0'),
                (0x1, '0.0.0.1'),
                (0xf, '0.0.0.15'),
                (0xff, '0.0.0.255'),
                (0xFFFFFFFFL, '255.255.255.255')]
    v6values = [(0x7f000001, '0000:0000:0000:0000:0000:0000:7f00:0001'),
                (0x0, '0000:0000:0000:0000:0000:0000:0000:0000'),
                (0x1, '0000:0000:0000:0000:0000:0000:0000:0001'),
                (0xf, '0000:0000:0000:0000:0000:0000:0000:000f'),
                (0xff, '0000:0000:0000:0000:0000:0000:0000:00ff'),
                (0xFFFFFFFFL, '0000:0000:0000:0000:0000:0000:ffff:ffff'),
                (0x100000000L, '0000:0000:0000:0000:0000:0001:0000:0000'),
                (0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFL, 'ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff')]

    def testKnownValuesv4(self):
        """printing of known IPv4 values should give known results"""
        for x in self.v4values:
            (question, answer) = x
            result  = IPy.intToIp(question, 4).lower()
            self.assertEqual(answer, result, "%r, %r, %r" % (question, answer, result))

    def testKnownValuesv6(self):
        """printing of known IPv6 values should give known results"""
        for x in self.v6values:
            (question, answer) = x
            result  = IPy.intToIp(question, 6).lower()
            self.assertEqual(answer, result, "%r, %r, %r" % (question, answer, result))

    def testNegativeIPv4(self):
        """negative IPv4 Values should raise an exception"""
        self.assertRaises(ValueError, IPy.intToIp, -1, 4)

    def testNegativeIPv6(self):
        """negative IPv6 Values should raise an exception"""
        self.assertRaises(ValueError, IPy.intToIp, -1, 6)

    def testLargeIPv4(self):
        """IPv4: Values > 0xffffffff should raise an exception"""
        self.assertRaises(ValueError, IPy.intToIp, 0x100000000L, 4)

    def testLargeIPv6(self):
        """IPv6: Values > 0xffffffffffffffffffffffffffffffff should raise an exception"""
        self.assertRaises(ValueError, IPy.intToIp, 0x100000000000000000000000000000000L, 6)

    def testIllegalVersion(self):
        """IPVersion other than 4 and 6 should raise an exception"""
        self.assertRaises(ValueError, IPy.intToIp, 1, 0)
        self.assertRaises(ValueError, IPy.intToIp, 1, 1)
        self.assertRaises(ValueError, IPy.intToIp, 1, 2)
        self.assertRaises(ValueError, IPy.intToIp, 1, 3)
        self.assertRaises(ValueError, IPy.intToIp, 1, 5)
        self.assertRaises(ValueError, IPy.intToIp, 1, 7)
        self.assertRaises(ValueError, IPy.intToIp, 1, 8)

class ParseAndBack(unittest.TestCase):
    def testRandomValuesv4(self):
        for i in range(testloops):
            question = long(random.randrange(0x7fffffff)) + long(random.randrange(0x7fffffff))
            self.assertEqual(IPy.parseAddress(IPy.intToIp(question, 4)), (question, 4), hex(question))

    def testRandomValuesv6(self):
        for i in range(testloops):
            question = ((long(random.randrange(0x7fffffff)) + long(random.randrange(0x7fffffff))) +
                        ((long(random.randrange(0x7fffffff)) + long(random.randrange(0x7fffffff))) << 32) +
                        ((long(random.randrange(0x7fffffff)) + long(random.randrange(0x7fffffff))) << 64) +
                        ((long(random.randrange(0x7fffffff)) + long(random.randrange(0x7fffffff))) << 96))
            self.assertEqual(IPy.parseAddress(IPy.intToIp(question, 6)), (question, 6), hex(question))


class _countXBits(unittest.TestCase):
    def testCount1Bits(self):
        self.assertEqual(IPy._count1Bits(0), 0)
        self.assertEqual(IPy._count1Bits(0xf), 4)
        self.assertEqual(IPy._count1Bits(0x10), 5)
        self.assertEqual(IPy._count1Bits(0xff), 8)
        self.assertEqual(IPy._count1Bits(0xffff), 16)
        self.assertEqual(IPy._count1Bits(0xffffffffL), 32)
        self.assertEqual(IPy._count1Bits(0xffffffffffffffffffffffffffffffffL), 128)

    def testCount1Bits(self):
        self.assertEqual(IPy._count0Bits(0), 0)
        self.assertEqual(IPy._count0Bits(0xf0L), 4)
        self.assertEqual(IPy._count0Bits(0xf00L), 8)
        self.assertEqual(IPy._count0Bits(0xf000L), 12)
        self.assertEqual(IPy._count0Bits(0xf0000L), 16)
        self.assertEqual(IPy._count0Bits(0xf00000L), 20)
        self.assertEqual(IPy._count0Bits(0xf000000L), 24)
        self.assertEqual(IPy._count0Bits(0xf0000000L), 28)
        self.assertEqual(IPy._count0Bits(0xff000000L), 24)
        self.assertEqual(IPy._count0Bits(0xfff00000L), 20)
        self.assertEqual(IPy._count0Bits(0x80000000L), 31)
        self.assertEqual(IPy._count0Bits(0xf0000000000000000000000000000000L), 124)
        self.assertEqual(IPy._count0Bits(0x80000000000000000000000000000000L), 127)


class _intToBin(unittest.TestCase):
    knownValues = [(0, '0'), (1, '1'), (2, '10'), (3, '11'), (4, '100'), (5, '101'),
                   (6, '110'), (7, '111'), (8, '1000'), (9, '1001'),
                   (0xf, '1111'), (0xff, '11111111'),
                   (0xFFFFFFFFL, '11111111111111111111111111111111'),
                   (0x100000000L, '100000000000000000000000000000000'),
                   (0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFL, '11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111'),
                   (0x100000000000000000000000000000000L, '100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000')]

    def testKnownValues(self):
        """conversion of known values values should give known results"""
        for x in self.knownValues:
            (question, answer) = x
            result  = IPy._intToBin(question)
            self.assertEqual(answer, result, str(question))

    def testNegativeIPv4(self):
        """negative Values should raise an exception"""
        self.assertRaises(ValueError, IPy._intToBin, -1)

class netmaskPrefixlenConv(unittest.TestCase):
    known4Values = [(0xFFFFFFFFL, 32), (0xFFFFFFFEL, 31), (0xFFFFFFFCL, 30), (0xFFFFFFF8L, 29),
                    (0xFFFFFFF0L, 28), (0xFFFFFFE0L, 27), (0xFFFFFFC0L, 26), (0xFFFFFF80L, 25),
                    (0xFFFFFF00L, 24), (0xFFFFFE00L, 23), (0xFFFFFC00L, 22), (0xFFFFF800L, 21),
                    (0xFFFFF000L, 20), (0xFFFFE000L, 19), (0xFFFFC000L, 18), (0xFFFF8000L, 17),
                    (0xFFFF0000L, 16), (0xFFFE0000L, 15), (0xFFFC0000L, 14), (0xFFF80000L, 13),
                    (0xFFF00000L, 12), (0xFFE00000L, 11), (0xFFC00000L, 10), (0xFF800000L, 9),
                    (0xFF000000L, 8), (0xFE000000L, 7), (0xFC000000L, 6), (0xF8000000L, 5),
                    (0xF0000000L, 4), (0xE0000000L, 3), (0xC0000000L, 2), (0x80000000L, 1)]
    known6Values = [(0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFL, 128),
                    (0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEL, 127),
                    (0xFFFFFFFFFFFFFFFFFFFFFFFF80000000L, 97),
                    (0xFFFFFFFFFFFFFFFFFFFFFFFF00000000L, 96),
                    (0xFFFFFFFFFFFFFFFFFFFFFFFE00000000L, 95),
                    (0xFFFFFFFFFFFFFFFF8000000000000000L, 65),
                    (0xFFFFFFFFFFFFFFFF0000000000000000L, 64),
                    (0xFFFFFFFFFFFFFFFE0000000000000000L, 63),
                    (0xFFFFFFFF800000000000000000000000L, 33),
                    (0xFFFFFFFF000000000000000000000000L, 32),
                    (0xFFFFFFFE000000000000000000000000L, 31),
                    (0xC0000000000000000000000000000000L, 2),
                    (0x80000000000000000000000000000000L, 1)]

    def testKnownValuesv4n2p(self):
        """conversion of known values values should give known results"""
        for x in self.known4Values:
            (question, answer) = x
            result  = IPy._netmaskToPrefixlen(question)
            self.assertEqual(answer, result, hex(question))

    def testKnownValuesv6n2p(self):
        """conversion of known values values should give known results"""
        for x in self.known6Values:
            (question, answer) = x
            result  = IPy._netmaskToPrefixlen(question)
            self.assertEqual(answer, result, hex(question))

    def testKnownValuesv4p2n(self):
        """conversion of known values values should give known results"""
        for x in self.known4Values:
            (answer, question) = x
            result  = IPy._prefixlenToNetmask(question, 4)
            self.assertEqual(answer, result, hex(question))

    def testKnownValuesv6p2n(self):
        """conversion of known values values should give known results"""
        for x in self.known6Values:
            (answer, question) = x
            result  = IPy._prefixlenToNetmask(question, 6)
            self.assertEqual(answer, result, "%d: %s != %s" % (question, hex(answer), result))

    def testInvalidv4n2p(self):
        """Netmasks should be all ones in the first part and all zeros in the second part"""
        self.failUnlessRaises(ValueError, IPy._netmaskToPrefixlen, 0xff00ff00L)

    def testInvalidv6n2p(self):
        """Netmasks should be all ones in the first part and all zeros in the second part"""
        self.failUnlessRaises(ValueError, IPy._netmaskToPrefixlen, 0xff00ff00ff00ff00ff00ff00ff00ff00L)


class checkChecks(unittest.TestCase):

    def testCheckNetmaskOk(self):
        """Legal Netmasks should be allowed."""
        self.failIf(IPy._checkNetmask(0xffffffffL, 32))
        self.failIf(IPy._checkNetmask(0xffffff00L, 32))
        self.failIf(IPy._checkNetmask(0xffff0000L, 32))
        self.failIf(IPy._checkNetmask(0xff000000L, 32))
        self.failIf(IPy._checkNetmask(0, 32))

    def testCheckNetmaskFail(self):
        """Illegal Netmasks should be rejected."""
        self.failUnlessRaises(ValueError, IPy._checkNetmask, 0xf0ffffffL, 32)
        self.failUnlessRaises(ValueError, IPy._checkNetmask, 0xf0f0f0f0L, 32)
        self.failUnlessRaises(ValueError, IPy._checkNetmask, 0xff00ff00L, 32)
        self.failUnlessRaises(ValueError, IPy._checkNetmask, 0x70000001L, 32)
        self.failUnlessRaises(ValueError, IPy._checkNetmask, 0xfffffffL, 32)

    def testCheckPrefixOk(self):
        """Legal IP/prefix combinations should check ok."""
        self.failUnless(IPy._checkPrefix(0x0, 32, 4))
        self.failUnless(IPy._checkPrefix(0xffffffffL, 32, 4))
        self.failUnless(IPy._checkPrefix(0x7f000001L, 32, 4))
        self.failUnless(IPy._checkPrefix(0x80000000L, 1, 4))
        self.failUnless(IPy._checkPrefix(0x40000000L, 2, 4))
        self.failUnless(IPy._checkPrefix(0x80000000L, 3, 4))
        self.failUnless(IPy._checkPrefix(0x80000000L, 4, 4))
        self.failUnless(IPy._checkPrefix(0xffffff00L, 24, 4))
        self.failUnless(IPy._checkPrefix(0xffffff00L, 24, 4))
        self.failUnless(IPy._checkPrefix(0xfffffff0L, 28, 4))
        self.failUnless(IPy._checkPrefix(0x0, 32, 4))
        self.failUnless(IPy._checkPrefix(0x0, 1, 4))
        self.failUnless(IPy._checkPrefix(0x0, 0, 4))
        self.failUnless(IPy._checkPrefix(0xffffffffffffffff0000000000000000L, 64, 6))
        self.failUnless(IPy._checkPrefix(0x0L, 64, 6))
        self.failUnless(IPy._checkPrefix(0x0L, 0, 6))
        self.failUnless(IPy._checkPrefix(0x0L, 128, 6))
        self.failUnless(IPy._checkPrefix(0xffffffffffffffffffffffffffffffffL, 128, 6))


    def testCheckPrefixFail(self):
        """Illegal Prefixes should be catched."""
        self.failIf(IPy._checkPrefix(0x7f000001L, -1, 4))
        self.failIf(IPy._checkPrefix(0x7f000001L, 33, 4))
        self.failIf(IPy._checkPrefix(0x7f000001L, 24, 4))
        self.failIf(IPy._checkPrefix(0x7f000001L, 31, 4))
        self.failIf(IPy._checkPrefix(0x7f000080L, 24, 4))
        self.failIf(IPy._checkPrefix(0x7f000100L, 23, 4))
        self.failIf(IPy._checkPrefix(0x7f000000L, 1, 4))
        self.failIf(IPy._checkPrefix(0x7f000000L, 0, 4))
        self.failIf(IPy._checkPrefix(0x1L, -1, 6))
        self.failIf(IPy._checkPrefix(0x1L, 129, 6))
        self.failIf(IPy._checkPrefix(0xffffffffffffffff0000000000000001L, 64, 6))
        self.failIf(IPy._checkPrefix(0xffffffffffffffff1000000000000000L, 64, 6))


    # TODO: _checkNetaddrWorksWithPrefixlen(net, prefixlen, version):

class PythonObjectBehaviour(unittest.TestCase):
    def testIfUsuableAsDictionaryKey(self):
        """IP Object should be usable as dictionary key"""
        d = {}
        d[IPy.IP('127.0.0.1')] = 1
        d[IPy.IP('2001::1')] = 1
        d[IPy.IP('127.0.0.0/24')] = 1
        d[IPy.IP('2001::/64')] = 1

    def testIfCanBeInteratedOver(self):
        """It should be possible to iterate over an IP Object."""
        i = 0
        for x in IPy.IP('127.0.0.0/24'):
            i += 1
        self.assertEqual(i, 256, "iteration over a /24 should yiels 256 values")
        i = 0
        for x in IPy.IP('2001::/124'):
            i += 1
        self.assertEqual(i, 16, "iteration over a /124 should yiels 16 values")

    def testIfComparesEqual(self):
        """nets of the same base and size should be considered equal, others not"""
        a = IPy.IP('127.0.0.0/24')
        a2 = a
        b = IPy.IP('127.0.0.0/24')
        c = IPy.IP('127.0.0.0/23')
        d = IPy.IP('127.0.0.0/22')
        e = IPy.IP('64.0.0.0/24')
        self.assertEqual(a2, a)
        self.assertEqual(a2, b)
        self.assertEqual(a, a)
        self.assertEqual(a, b)
        self.assertNotEqual(a, c)
        self.assertNotEqual(a, d)
        self.assertNotEqual(a, e)
        self.assertNotEqual(b, c)
        self.assertNotEqual(b, d)
        self.assertNotEqual(b, e)
        self.assertNotEqual(c, d)
        self.assertNotEqual(c, e)
        self.assertNotEqual(d, e)

    def testIfContainsInt(self):
        """__contains__() should work somewhat with ints"""
        ip = IPy.IP('127.0.0.0/28')
        for x in ip:
            self.failUnless(x.int() in ip)
        ip = IPy.IP('2001::/124')
        for x in ip:
            self.failUnless(x.int() in ip)

    def testIfContainsStr(self):
        """__contains__() should work somewhat with strings"""
        ip = IPy.IP('127.0.0.0/28')
        for x in ip:
            self.failUnless(x.strNormal() in ip, "%r not in %r" % (x.strNormal(), ip))
        ip = IPy.IP('2001::/124')
        for x in ip:
            self.failUnless(x.strNormal() in ip, "%r not in %r" % (x.strNormal(), ip))

    def testIfContainsIPobj(self):
        """__contains__() should work somewhat with IP instances"""
        ip = IPy.IP('127.0.0.0/28')
        for x in ip:
            self.failUnless(x in ip)
        ip = IPy.IP('2001::/124')
        for x in ip:
            self.failUnless(x in ip)

    def testActingAsArray(self):
        """An IP-object should handle indices."""
        ip = IPy.IP('127.0.0.0/24')
        self.assertEqual(ip[0], ip.net())
        self.assertEqual(ip[-1], ip.broadcast())
        self.failUnless(ip[255])
        self.failUnlessRaises(IndexError, ip.__getitem__, 256)

    def testStr(self):
        """string() should work somewhat with IP instances"""
        ip = IPy.IP('127.0.0.0/28')
        for x in ip:
            self.failUnless(str(x))
        ip = IPy.IP('2001::/124')
        for x in ip:
            self.failUnless(str(x))

    def testRepr(self):
        """repr() should work somewhat with IP instances"""
        ip = IPy.IP('127.0.0.0/28')
        for x in ip:
            self.failUnless(repr(x))
        ip = IPy.IP('2001::/124')
        for x in ip:
            self.failUnless(repr(x))

    def testLen(self):
        """object should have an working __len__() interface."""
        self.failUnlessEqual(len(IPy.IP('127.0.0.0/28')), 16)
        self.failUnlessEqual(len(IPy.IP('127.0.0.0/30')), 4)
        self.failUnlessEqual(len(IPy.IP('127.0.0.0/26')), 64)
        self.failUnlessEqual(len(IPy.IP('127.0.0.0/16')), 2**16)

    # cmp
    # IP[0xffffffff]
    # IP + IP
    # reverse
    # netmsk
    # ip

class IPobject(unittest.TestCase):
    def testStrCompressed(self):
        """Compressed string Output."""
        testValues = ['127.0.0.1',
                  'dead::beef',
                  'dead:beef::',
                  'dead:beef::/48',
                  'ff00:1::',
                  'ff00:0:f000::',
                  '0:0:1000::',
                  '::e000:0/112',
                  '::e001:0/112',
                  'dead:beef::/48',
                  'ff00:1::/64',
                  'ff00:0:f000::/64',
                  '0:0:1000::/64',
                  '::e000:0/112',
                  '::e001:0/112',
                  '::1:0:0:0:2',
                  '0:1:2:3:4:5:6:7',
                  '1:2:3:4:0:5:6:7',
                  '1:2:3:4:5:6:7:0',
                  '1:0:0:2::',
                  '1:0:0:2::3',
                  '1::2:0:0:3']
        for question in testValues:
            result = IPy.IP(question).strCompressed()
            self.failUnlessEqual(question, result, (question, result))

    def testStrBin(self):
        """Binary string Output."""

        testValues = [('0.0.0.0', '00000000000000000000000000000000'),
                      ('0.0.0.1', '00000000000000000000000000000001'),
                      ('255.255.255.255', '11111111111111111111111111111111'),
                      ('128.0.0.0', '10000000000000000000000000000000'),
                      ('::0', '00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000'),
                      ('::1', '00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001'),
                      ('ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff', '11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111'),
                      ('5555:5555:5555:5555:5555:5555:5555:5555', '01010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101'),
                      ('aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa', '10101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010'),
                      ('85.85.85.85', '01010101010101010101010101010101'),
                      ('170.170.170.170', '10101010101010101010101010101010'),
                      ('127.0.0.1', '01111111000000000000000000000001'),
                      ('1::2:0:0:3', '00000000000000010000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000011')]
        for (question, answer) in testValues:
            result = IPy.IP(question).strBin()
            self.failUnlessEqual(answer, result, (question, answer, result))

    def testStrNormal(self):
        """Normal string Output."""
        testValues = [(338770000845734292534325025077361652240L, 'fedc:ba98:7654:3210:fedc:ba98:7654:3210'),
                      (21932261930451111902915077091070067066L, '1080:0:0:0:8:800:200c:417a'),
                      (338958331222012082418099330867817087043L, 'ff01:0:0:0:0:0:0:43'),
                      (0L, '0.0.0.0'),
                      (2130706433L, '127.0.0.1'),
                      (4294967295L, '255.255.255.255'),
                      (1L, '0.0.0.1'),
                      (3588059479L, '213.221.113.87')]
        for (question, answer) in testValues:
            result = IPy.IP(question).strNormal(question)
            self.failUnlessEqual(answer, result, (question, result, answer))

    def testStrFullsize(self):
        """Normal / 0-padded string Output."""
        testValues = [(338770000845734292534325025077361652240L, 'fedc:ba98:7654:3210:fedc:ba98:7654:3210'),
                      (21932261930451111902915077091070067066L, '1080:0000:0000:0000:0008:0800:200c:417a'),
                      (338958331222012082418099330867817087043L, 'ff01:0000:0000:0000:0000:0000:0000:0043'),
                      (0L, '0.0.0.0'),
                      (2130706433L, '127.0.0.1'),
                      (4294967295L, '255.255.255.255'),
                      (1L, '0.0.0.1'),
                      (3588059479L, '213.221.113.87')]
        for (question, answer) in testValues:
            result = IPy.IP(question).strFullsize(question)
            self.failUnlessEqual(answer, result, (question, result, answer))

    def testStrHex(self):
        """Hex string Output."""
        testValues = [(338770000845734292534325025077361652240L, '0xfedcba9876543210fedcba9876543210'),
                      (21932261930451111902915077091070067066L, '0x108000000000000000080800200c417a'),
                      (338958331222012082418099330867817087043L, '0xff010000000000000000000000000043'),
                      (0L, '0x0'),
                      (1L, '0x1'),
                      (4294967295l, '0xffffffff'),
                      (3588059479L, '0xd5dd7157'),
                      (0x12345678, '0x12345678')]
        for (question, answer) in testValues:
            result = IPy.IP(question).strHex(question).lower()
            self.failUnlessEqual(answer, result, (question, result, answer))

    def testStrDec(self):
        """Decimal string Output."""
        testValues = [(338770000845734292534325025077361652240L, '338770000845734292534325025077361652240'),
                      (21932261930451111902915077091070067066L, '21932261930451111902915077091070067066'),
                      (338958331222012082418099330867817087043L, '338958331222012082418099330867817087043'),
                      (0L, '0'),
                      (1L, '1'),
                      (0xFFFFFFFFL, '4294967295'),
                      (0xD5DD7157L, '3588059479')]
        for (question, answer) in testValues:
            result = IPy.IP(question).strDec(question)
            self.failUnlessEqual(answer, result, (question, result, answer))

    def testNet(self):
        """Returning of the Network Address"""
        self.failUnlessEqual(str(IPy.IP("127.0.0.1").net()), "127.0.0.1")
        self.failUnlessEqual(str(IPy.IP("0.0.0.0/0").net()), "0.0.0.0")
        self.failUnlessEqual(str(IPy.IP("2001:1234:5678:1234::/64").net()), "2001:1234:5678:1234::")


    def testBroadcast(self):
        """Returning of broadcast address."""
        self.failUnlessEqual(str(IPy.IP("127.0.0.1").broadcast()), "127.0.0.1")
        self.failUnlessEqual(str(IPy.IP("0.0.0.0/0").broadcast()), "255.255.255.255")
        self.failUnlessEqual(str(IPy.IP("2001:1234:5678:1234::/64").broadcast()), "2001:1234:5678:1234:ffff:ffff:ffff:ffff")


    def testStrNetmask(self):
        """StrNetmask should return netmasks"""
        self.failUnlessEqual(IPy.IP("0.0.0.0/0").strNetmask(), "0.0.0.0")
        self.failUnlessEqual(IPy.IP("0.0.0.0/32").strNetmask(), "255.255.255.255")
        self.failUnlessEqual(IPy.IP("127.0.0.0/24").strNetmask(), "255.255.255.0")
        self.failUnlessEqual(IPy.IP("2001:1234:5678:1234::/64").strNetmask(), "/64")


    def testNetmask(self):
        """Netmask should return netmasks"""
        self.failUnlessEqual(str(IPy.IP("0.0.0.0/0").netmask()), "0.0.0.0")
        self.failUnlessEqual(str(IPy.IP("0.0.0.0/32").netmask()), "255.255.255.255")
        self.failUnlessEqual(str(IPy.IP("127.0.0.0/24").netmask()), "255.255.255.0")
        self.failUnlessEqual(str(IPy.IP("2001:1234:5678:1234::/64").netmask()), "ffff:ffff:ffff:ffff:0000:0000:0000:0000")

    def testInt(self):
        """Prefixlen"""
        self.failUnlessEqual(IPy.IP("127.0.0.1").int(), 2130706433)
        self.failUnlessEqual(IPy.IP("0.0.0.0").int(), 0)
        self.failUnlessEqual(IPy.IP("255.255.255.255").int(), 0xffffffffL)
        self.failUnlessEqual(IPy.IP("0000:0000:0000:0000:0000:0000:0000:0000").int(), 0)
        self.failUnlessEqual(IPy.IP("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff").int(), 0xffffffffffffffffffffffffffffffffL)
        self.failUnlessEqual(IPy.IP("2001:1234:5678:9abc:de00:0000:0000:0000").int(), 42540857391974671903776007410583339008L)


    def testPrefixlen(self):
        """Prefixlen"""
        self.failUnlessEqual(IPy.IP("127.0.0.1").prefixlen(), 32)
        self.failUnlessEqual(IPy.IP("::1").prefixlen(), 128)
        self.failUnlessEqual(IPy.IP("10.0.0.0/24").prefixlen(), 24)
        self.failUnlessEqual(IPy.IP("10.0.0.0-10.0.0.255").prefixlen(), 24)
        self.failUnlessEqual(IPy.IP("10.0.0.0/255.255.255.0").prefixlen(), 24)
        self.failUnlessEqual(IPy.IP("2001::/64").prefixlen(), 64)


    def testVersion(self):
        """IP-version detection should work"""
        self.failUnlessEqual(IPy.IP("0.0.0.0/0").version(), 4)
        self.failUnlessEqual(IPy.IP("::1").version(), 6)

    # TODO:
    #def reverseNames(self):
    #def reverseName(self):
    #def __cmp__(self, other):
    #def __add__(self, other):
    #def _printPrefix(self, want):

    def testOverlaps(self):
        """Overlapping Address Ranges."""
        testValues = [('192.168.0.0/23', '192.168.1.0/24', 1),
                      ('192.168.0.0/23', '192.168.0.0/20', 1),
                      ('192.168.0.0/23', '192.168.2.0', 0),
                      ('192.168.0.0/23', '192.167.255.255', 0),
                      ('192.168.0.0/23', '192.168.0.0', 1),
                      ('192.168.0.0/23', '192.168.1.255', 1),
                      ('192.168.1.0/24', '192.168.0.0/23', -1),
                      ('127.0.0.1', '127.0.0.1', 1),
                      ('127.0.0.1', '127.0.0.2', 0)]
        for (a, b, answer) in testValues:
            result = IPy.IP(a).overlaps(b)
            self.failUnlessEqual(answer, result, (a, b, result, answer))

    def testNetmask(self):
        """Normal string Output."""
        testValues = [(338770000845734292534325025077361652240L, '0xfedcba9876543210fedcba9876543210'),
                      (21932261930451111902915077091070067066L, '0x108000000000000000080800200c417a'),
                      (338958331222012082418099330867817087043L, '0xff010000000000000000000000000043'),
                      (0L, '0x0'),
                      (1L, '0x1'),
                      (4294967295l, '0xffffffff'),
                      (3588059479L, '0xd5dd7157')]
        for (question, answer) in testValues:
            result = IPy.IP(question).strHex(question).lower()
            self.failUnlessEqual(answer, result, (question, result, answer))

# TODO
#eval(repr(IPy))
# differences between IP and IPint


# I ported this checks to be sure that I don't have errors in my own checks.
class NetIPChecks(unittest.TestCase):
    """Checks taken from perls Net::IP"""
    def testMisc(self):
        ip = IPy.IP('195.114.80/24')
        self.assertEqual(ip.int(), 3279048704L)
        self.assertEqual(ip.reverseName(),'80.114.195.in-addr.arpa.')
        self.assertEqual(ip.strBin(),'11000011011100100101000000000000')
        self.assertEqual(str(ip.net()),'195.114.80.0')
        self.assertEqual(str(ip),'195.114.80.0/24')
        self.assertEqual(ip.prefixlen(),24)
        self.assertEqual(ip.version(),4)
        self.assertEqual(ip.len(),256)
        self.assertEqual(IPy._intToBin(ip.netmask().int()),'11111111111111111111111100000000')
        self.assertEqual(ip.strNetmask(),'255.255.255.0')
        self.assertEqual(ip.iptype(), 'PUBLIC')
        self.assertEqual(ip.broadcast().strBin(),'11000011011100100101000011111111')
        self.assertEqual(str(ip.broadcast()),'195.114.80.255')

        ip = IPy.IP('202.31.4/24')
        self.assertEqual(str(ip.net()),'202.31.4.0')

        self.failUnlessRaises(ValueError, IPy.IP, '234.245.252.253/2')

        # because we ar using integer representation we don't need a special "binadd"
        ip = IPy.IP('62.33.41.9')
        ip2 = IPy.IP('0.1.0.5')
        self.assertEqual(str(IPy.IP(ip.int() + ip2.int())),'62.34.41.14')
        #$T->ok_eq ($ip->binadd($ip2)->ip(),'62.34.41.14',$ip->error());

        ip = IPy.IP('133.45.0/24')
        ip2 = IPy.IP('133.45.1/24')
        self.assertEqual((ip + ip2).prefixlen(),23)

        ip2 = IPy.IP('133.44.255.255');
        #$T->ok_eqnum ($ip->bincomp('gt',$ip2),1,$ip->error());

        # this is something we can't do with IPy
        #ip = IPy.IP('133.44.255.255-133.45.0.42');
        #$T->ok_eq (($ip->find_prefixes())[3],'133.45.0.40/31',$ip->error());

        ip = IPy.IP('201.33.128.0/22');
        ip2 = IPy.IP('201.33.129.0/24');
        #$T->ok_eqnum ($ip->overlaps($ip2),$IP_B_IN_A_OVERLAP,$ip->error());

        ip = IPy.IP('dead:beef:0::/48')
        self.assertEqual(str(ip.net()),'dead:beef::')
        self.assertEqual(ip.int(), 295990755014133383690938178081940045824L)
        self.assertEqual(ip.strBin(),'11011110101011011011111011101111000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000')
        self.assertEqual(ip.strCompressed(),'dead:beef::/48')
        self.assertEqual(ip.prefixlen(), 48)
        self.assertEqual(ip.version(), 6)
        self.assertEqual(ip.strNetmask(),'/48')
        self.assertEqual(str(ip.netmask()),'ffff:ffff:ffff::')
        self.assertEqual(ip.iptype(),'UNASSIGNED')
        self.assertEqual(ip.reverseName(),'0.0.0.0.f.e.e.b.d.a.e.d.ip6.int.')
        self.assertEqual(str(ip.broadcast()),'dead:beef:0:ffff:ffff:ffff:ffff:ffff')

        ip = IPy.IP('202.31.4/24')
        self.assertEqual(str(ip.net()),'202.31.4.0')

        # TODO: fix this in IPy ... after rereading the RfC
        # ip = IPy.IP(':1/128');
        #$T->ok_eq ($ip->error(),'Invalid address :1 (starts with :)',$ip->error());
        #$T->ok_eqnum ($ip->errno(),109,$ip->error());

        ip = IPy.IP('ff00:0:f000::')
        ip2 = IPy.IP('0:0:1000::')
        self.assertEqual(IPy.IP(ip.int() + ip2.int()).strCompressed(), 'ff00:1::')

        ip = IPy.IP('::e000:0/112')
        ip2 = IPy.IP('::e001:0/112')
        self.assertEqual(ip.__add__(ip2).prefixlen(),111)

        ip2 = IPy.IP('::dfff:ffff')
        #$T->ok_eqnum ($ip->bincomp('gt',$ip2),1,$ip->error());

        #ip = IPy.IP('::e000:0 - ::e002:42')
        #$T->ok_eq (($ip->find_prefixes())[2],'0000:0000:0000:0000:0000:0000:e002:0040/127',$ip->error());

        ip = IPy.IP('ffff::/16')
        ip2 = IPy.IP('8000::/16')
        #$T->ok_eqnum ($ip->overlaps($ip2),$IP_NO_OVERLAP,$ip->error());

def timeout(func, args=(), kwargs={}, timeout_duration=1, default=None):
    """
    ASPN receipe written by dustin lee to call a function with
    a timeout using threads:
    http://aspn.activestate.com/ASPN/Cookbook/Python/Recipe/473878

    Small patch: add setDaemon(True) to allow Python to leave whereas the
    thread is not done.
    """
    class InterruptableThread(threading.Thread):
        def __init__(self):
            threading.Thread.__init__(self)
            self.result = None

        def run(self):
            try:
                self.result = func(*args, **kwargs)
            except:
                self.result = default

    it = InterruptableThread()
    it.setDaemon(True)
    it.start()
    it.join(timeout_duration)
    if it.isAlive():
        return default
    else:
        return it.result

class RegressionTest(unittest.TestCase):
    def testNulNetmask(self):
        ip = timeout(IPy.IP, ["0.0.0.0/0.0.0.0"], timeout_duration=0.250, default=None)
        if ip:
            text = str(ip)
        else:
            text = "*TIMEOUT*"
        self.assertEqual(text, "0.0.0.0/0")

    def testNonZeroType(self):
        self.assertEqual(bool(IPy.IP("0.0.0.0/0")), True)

    def testPrivate169(self):
        """
        RFC 3330 indicates that 169.254.0.0/16 addresses are private.
        They are automatically configured for links in the absence of other
        information and should not be used on the internet
        """
        self.assertEqual(IPy.IP("169.254.191.164").iptype(), "PRIVATE")

    def testCheckAddrPrefixlenOn(self):
        self.assertEqual(len(IPy.IP('192.168.0.0/24')), 256)
        self.assertRaises(ValueError, IPy.IP, '192.168.1.0/42')
        self.assertRaises(ValueError, IPy.IP, '172.30.1.0/22')

    def testCheckAddrPrefixlenOff(self):
        old = IPy.check_addr_prefixlen
        IPy.check_addr_prefixlen = False
        try:
            self.assertEqual(len(IPy.IP('192.168.0.0/24')), 256)
            self.assertRaises(ValueError, IPy.IP, '192.168.1.0/42')
            self.assertEqual(len(IPy.IP('172.30.1.0/22')), 1024)
        finally:
            IPy.check_addr_prefixlen = old

if __name__ == "__main__":
    unittest.main()

