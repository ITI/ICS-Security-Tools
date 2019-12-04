# -*- coding: utf-8 -*-
# pep8-ignore: E501, E241

'''
====================================
 :mod:`ipcalc` IP subnet calculator
====================================
.. moduleauthor:: Wijnand Modderman-Lenstra <maze@pyth0n.org>
.. note:: BSD License

About
=====

This module allows you to perform network calculations.

References
==========

References:
 * http://www.estoile.com/links/ipv6.pdf
 * http://www.iana.org/assignments/ipv4-address-space
 * http://www.iana.org/assignments/multicast-addresses
 * http://www.iana.org/assignments/ipv6-address-space
 * http://www.iana.org/assignments/ipv6-tla-assignments
 * http://www.iana.org/assignments/ipv6-multicast-addresses
 * http://www.iana.org/assignments/ipv6-anycast-addresses

Thanks
======

Thanks to all who have contributed:

https://github.com/tehmaze/ipcalc/graphs/contributors
'''

__version__ = '1.1.3'


import re
import warnings


try:
    bin
except NameError:
    def bin(x):
        '''
        Stringifies an int or long in base 2.
        '''
        if x < 0:
            return '-%s' % bin(-x)
        out = []
        if x == 0:
            out.append('0')
        while x > 0:
            out.append('01'[x & 1])
            x >>= 1
        out.reverse()
        return '0b%s' % ''.join(out)


class IP(object):
    '''
    Represents a single IP address.

    :param ip: the ip address
    :type ip: :class:`IP` or str or long or int

    >>> localhost = IP("127.0.0.1")
    >>> print localhost
    127.0.0.1
    >>> localhost6 = IP("::1")
    >>> print localhost6
    0000:0000:0000:0000:0000:0000:0000:0001
    '''

    # Hex-to-Bin conversion masks
    _bitmask = {
        '0': '0000', '1': '0001', '2': '0010', '3': '0011',
        '4': '0100', '5': '0101', '6': '0110', '7': '0111',
        '8': '1000', '9': '1001', 'a': '1010', 'b': '1011',
        'c': '1100', 'd': '1101', 'e': '1110', 'f': '1111'
    }

    # IP range specific information, see IANA allocations.
    _range = {
        # http://www.iana.org/assignments/iana-ipv4-special-registry/iana-ipv4-special-registry.xhtml
        4: {
            '00000000':                 'THIS HOST',            # 0/8
            '00001010':                 'PRIVATE',              # 10/8
            '0110010001':               'SHARED ADDRESS SPACE', # 100.64/10
            '01111111':                 'LOOPBACK',             # 127/8
            '101011000001':             'PRIVATE',              # 172.16/12
            '110000000000000000000000': 'IETF PROTOCOL',        # 192/24
            '110000000000000000000010': 'TEST-NET-1',           # 192.0.2/24
            '110000000101100001100011': '6TO4-RELAY ANYCAST',   # 192.88.99/24
            '1100000010101000':         'PRIVATE',              # 192.168/16
            '110001100001001':          'BENCHMARKING',         # 198.18/15
            '110001100011001':          'TEST-NET-2',           # 198.51.100/24
            '110010110000000':          'TEST-NET-3',           # 203.0.113/24
            '1111':                     'RESERVED',             # 240/4

        },
        # http://www.iana.org/assignments/iana-ipv6-special-registry/iana-ipv6-special-registry.xhtml
        6: {
            '0' * 128:                          'UNSPECIFIED',  # ::/128
            '0' * 127 + '1':                    'LOOPBACK',     # ::1/128
            '0' * 96:                           'IPV4COMP',     # ::/96
            '0' * 80 + '1' * 16:                'IPV4MAP',      # ::ffff:0:0/96
                                                                # 64:ff9b::/96
            '00000000011001001111111110011011' + 64 * '0': 'IPV4-IPV6',
            '00000001' + 56 * '0':              'DISCARD-ONLY', # 100::/64
            '0010000000000001' +  7 * '0':      'IETF PROTOCOL',# 2001::/23
            '0010000000000001' + 16 * '0':      'TEREDO',       # 2001::/32
                                                                # 2001:2::/48
            '00100000000000010000000000000010000000000000000': 'BENCHMARKING',
            '00100000000000010000110110111000': 'DOCUMENTATION',# 2001:db8::/32
            '0010000000000001000000000001':     'DEPRECATED',   # 2001:10::/28
            '0010000000000001000000000010':     'ORCHIDv2',     # 2001:20::/28
            '0010000000000010':                 '6TO4',         # 2002::/16
            '11111100000000000':                'UNIQUE-LOCAL', # fc00::/7
            '1111111010':                       'LINK-LOCAL',   # fe80::/10
        }
    }

    def __init__(self, ip, mask=None, version=0):
        self.mask = mask
        self.v = 0
        # Parse input
        if ip is None:
            raise ValueError('Can not pass None')
        elif isinstance(ip, IP):
            self.ip = ip.ip
            self.dq = ip.dq
            self.v = ip.v
            self.mask = ip.mask
        elif isinstance(ip, (int, long)):
            self.ip = long(ip)
            if self.ip <= 0xffffffff:
                self.v = version or 4
                self.dq = self._itodq(ip)
            else:
                self.v = version or 6
                self.dq = self._itodq(ip)
        else:
            # If string is in CIDR or netmask notation
            if '/' in ip:
                ip, mask = ip.split('/', 1)
                self.mask = mask
            self.v = version or 0
            self.dq = ip
            self.ip = self._dqtoi(ip)
            assert self.v != 0, 'Could not parse input'
        # Netmask defaults to one ip
        if self.mask is None:
            self.mask = self.v == 4 and 32 or 128
        # Netmask is numeric CIDR subnet
        elif isinstance(self.mask, (int, long)) or self.mask.isdigit():
            self.mask = int(self.mask)
        # Netmask is in subnet notation
        elif isinstance(self.mask, basestring):
            limit = [32, 128][':' in self.mask]
            inverted = ~self._dqtoi(self.mask)
            count = 0
            while inverted & pow(2, count):
                count += 1
            self.mask = (limit - count)
        else:
            raise ValueError('Invalid netmask')
        # Validate subnet size
        if self.v == 6:
            self.dq = self._itodq(self.ip)
            if not 0 <= self.mask <= 128:
                raise ValueError('IPv6 subnet size must be between 0 and 128')
        elif self.v == 4:
            if not 0 <= self.mask <= 32:
                raise ValueError('IPv4 subnet size must be between 0 and 32')

    def bin(self):
        '''
        Full-length binary representation of the IP address.

        >>> ip = IP("127.0.0.1")
        >>> print ip.bin()
        01111111000000000000000000000001
        '''
        return bin(self.ip).split('b')[1].rjust(self.mask, '0')

    def hex(self):
        '''
        Full-length hexadecimal representation of the IP address.

        >>> ip = IP("127.0.0.1")
        >>> print ip.hex()
        7f000001
        '''
        if self.v == 4:
            return '%08x' % self.ip
        else:
            return '%032x' % self.ip

    def subnet(self):
        return self.mask

    def version(self):
        '''
        IP version.

        >>> ip = IP("127.0.0.1")
        >>> print ip.version()
        4
        '''
        return self.v

    def info(self):
        '''
        Show IANA allocation information for the current IP address.

        >>> ip = IP("127.0.0.1")
        >>> print ip.info()
        CLASS A
        '''
        b = self.bin()
        self.v == 4 and 32 or 128
        for i in range(len(b), 0, -1):
            if b[:i] in self._range[self.v]:
                return self._range[self.v][b[:i]]
        return 'UNKNOWN'

    def _dqtoi(self, dq):
        '''
        Convert dotquad or hextet to long.
        '''
        # hex notation
        if dq.startswith('0x'):
            ip = long(dq[2:], 16)
            if ip > 0xffffffffffffffffffffffffffffffffL:
                raise ValueError('%s: IP address is bigger than 2^128' % dq)
            if ip <= 0xffffffff:
                self.v = 4
            else:
                self.v = 6
            return ip

        # IPv6
        if ':' in dq:
            # Split hextets
            hx = dq.split(':')
            if ':::' in dq:
                raise ValueError("%s: IPv6 address can't contain :::" % dq)
            # Mixed address (or 4-in-6), ::ffff:192.0.2.42
            if '.' in dq:
                return self._dqtoi(hx[-1])
            if len(hx) > 8:
                raise ValueError('%s: IPv6 address with more than 8 hexlets' % dq)
            elif len(hx) < 8:
                # No :: in address
                if not '' in hx:
                    raise ValueError('%s: IPv6 address invalid: '
                                     'compressed format malformed' % dq)
                elif not (dq.startswith('::') or dq.endswith('::')) and len([x for x in hx if x == '']) > 1:
                    raise ValueError('%s: IPv6 address invalid: '
                                     'compressed format malformed' % dq)
                ix = hx.index('')
                px = len(hx[ix + 1:])
                for x in xrange(ix + px + 1, 8):
                    hx.insert(ix, '0')
            elif dq.endswith('::'):
                pass
            elif '' in hx:
                raise ValueError('%s: IPv6 address invalid: '
                                 'compressed format detected in full notation' % dq())
            ip = ''
            hx = [x == '' and '0' or x for x in hx]
            for h in hx:
                if len(h) < 4:
                    h = '%04x' % int(h, 16)
                if not 0 <= int(h, 16) <= 0xffff:
                    raise ValueError('%r: IPv6 address invalid: '
                                     'hexlets should be between 0x0000 and 0xffff' % dq)
                ip += h
            self.v = 6
            return long(ip, 16)
        elif len(dq) == 32:
            # Assume full heximal notation
            self.v = 6
            return long(h, 16)

        # IPv4
        if '.' in dq:
            q = dq.split('.')
            q.reverse()
            if len(q) > 4:
                raise ValueError('%s: IPv4 address invalid: '
                                 'more than 4 bytes' % dq)
            for x in q:
                if not 0 <= int(x) <= 255:
                    raise ValueError('%s: IPv4 address invalid: '
                                     'bytes should be between 0 and 255' % dq)
            while len(q) < 4:
                q.insert(1, '0')
            self.v = 4
            return sum(long(byte) << 8 * index for index, byte in enumerate(q))

        raise ValueError('Invalid address input')

    def _itodq(self, n):
        '''
        Convert long to dotquad or hextet.
        '''
        if self.v == 4:
            return '.'.join(map(str, [
                (n >> 24) & 0xff,
                (n >> 16) & 0xff,
                (n >> 8) & 0xff,
                n & 0xff,
            ]))
        else:
            n = '%032x' % n
            return ':'.join(n[4 * x:4 * x + 4] for x in xrange(0, 8))

    def __str__(self):
        '''
        Return dotquad representation of the IP.

        >>> ip = IP("::1")
        >>> print str(ip)
        0000:0000:0000:0000:0000:0000:0000:0001
        '''
        return self.dq

    def __repr__(self):
        '''
        Return canonical representation of the IP.

        >>> repr(IP("::1"))
        "IP('::1')"
        >>> repr(IP("fe80:0000:0000:0000:abde:3eff:ffab:0012/64"))
        "IP('fe80::abde:3eff:ffab:12/64')"
        >>> repr(IP("1.2.3.4/29"))
        "IP('1.2.3.4/29')"
        >>> repr(IP("127.0.0.1/8"))
        "IP('127.0.0.1/8')"
        '''
        dq = self.dq if self.v == 4 else self.to_compressed()
        args = (self.__class__.__name__, dq, self.mask)
        if (self.version(), self.mask) in [(4, 32), (6, 128)]:
            fmt = "{0}('{1}')"
        else:
            fmt = "{0}('{1}/{2}')"
        return fmt.format(*args)

    def __hash__(self):
        return hash(self.to_tuple())

    hash = __hash__

    def __int__(self):
        return int(self.ip)

    def __long__(self):
        return self.ip

    def __lt__(self, other):
        return long(self) < long(IP(other))

    def __le__(self, other):
        return long(self) <= long(IP(other))

    def __ge__(self, other):
        return long(self) >= long(IP(other))

    def __gt__(self, other):
        return long(self) > long(IP(other))

    def __eq__(self, other):
        return long(self) == long(IP(other))

    def size(self):
        return 1

    def clone(self):
        '''
        Return a new <IP> object with a copy of this one.

        >>> ip = IP('127.0.0.1')
        >>> ip2 = ip.clone()
        >>> ip2
        IP('127.0.0.1')
        >>> ip is ip2
        False
        >>> ip == ip2
        True
        >>> ip.mask = 24
        >>> ip2.mask
        32
        '''
        return IP(self)

    def to_compressed(self):
        '''
        Compress an IP address to its shortest possible compressed form.

        >>> print IP('127.0.0.1').to_compressed()
        127.1
        >>> print IP('127.1.0.1').to_compressed()
        127.1.1
        >>> print IP('127.0.1.1').to_compressed()
        127.0.1.1
        >>> print IP('2001:1234:0000:0000:0000:0000:0000:5678').to_compressed()
        2001:1234::5678
        >>> print IP('1234:0000:0000:beef:0000:0000:0000:5678').to_compressed()
        1234:0:0:beef::5678
        >>> print IP('0000:0000:0000:0000:0000:0000:0000:0001').to_compressed()
        ::1
        >>> print IP('fe80:0000:0000:0000:0000:0000:0000:0000').to_compressed()
        fe80::
        '''
        if self.v == 4:
            quads = self.dq.split('.')
            try:
                zero = quads.index('0')
                if zero == 1 and quads.index('0', zero + 1):
                    quads.pop(zero)
                    quads.pop(zero)
                    return '.'.join(quads)
                elif zero == 2:
                    quads.pop(zero)
                    return '.'.join(quads)
            except ValueError:  # No zeroes
                pass

            return self.dq
        else:
            quads = map(lambda q: '%x' % (int(q, 16)), self.dq.split(':'))
            quadc = ':%s:' % (':'.join(quads),)
            zeros = [0, -1]

            # Find the largest group of zeros
            for match in re.finditer(r'(:[:0]+)', quadc):
                count = len(match.group(1)) - 1
                if count > zeros[0]:
                    zeros = [count, match.start(1)]

            count, where = zeros
            if count:
                quadc = quadc[:where] + ':' + quadc[where + count:]

            quadc = re.sub(r'((^:)|(:$))', '', quadc)
            quadc = re.sub(r'((^:)|(:$))', '::', quadc)

            return quadc

    def to_ipv4(self):
        '''
        Convert (an IPv6) IP address to an IPv4 address, if possible. Only works
        for IPv4-compat (::/96), IPv4-mapped (::ffff/96), and 6-to-4 (2002::/16) addresses.

        >>> ip = IP('2002:c000:022a::')
        >>> print ip.to_ipv4()
        192.0.2.42
        '''
        if self.v == 4:
            return self
        else:
            if self.bin().startswith('0' * 96):
                return IP(long(self), version=4)
            elif self.bin().startswith('0' * 80 + '1' * 16):
                return IP(long(self) & 0xffffffff, version=4)
            elif long(self) & 0x20020000000000000000000000000000L:
                return IP((long(self) - 0x20020000000000000000000000000000L) >> 80, version=4)
            else:
                return ValueError('%s: IPv6 address is not IPv4 compatible or mapped, '
                                  'nor an 6-to-4 IP' % self.dq)

    @classmethod
    def from_bin(cls, value):
        value = value.lstrip('b')
        if len(value) == 32:
            return cls(int(value, 2))
        elif len(value) == 128:
            return cls(long(value, 2))
        else:
            return ValueError('%r: invalid binary notation' % (value,))

    @classmethod
    def from_hex(cls, value):
        if len(value) == 8:
            return cls(int(value, 16))
        elif len(value) == 32:
            return cls(long(value, 16))
        else:
            raise ValueError('%r: invalid hexadecimal notation' % (value,))

    def to_ipv6(self, type='6-to-4'):
        '''
        Convert (an IPv4) IP address to an IPv6 address.

        >>> ip = IP('192.0.2.42')
        >>> print ip.to_ipv6()
        2002:c000:022a:0000:0000:0000:0000:0000

        >>> print ip.to_ipv6('compat')
        0000:0000:0000:0000:0000:0000:c000:022a

        >>> print ip.to_ipv6('mapped')
        0000:0000:0000:0000:0000:ffff:c000:022a
        '''
        assert type in ['6-to-4', 'compat', 'mapped'], 'Conversion type not supported'
        if self.v == 4:
            if type == '6-to-4':
                return IP(0x20020000000000000000000000000000L | long(self) << 80, version=6)
            elif type == 'compat':
                return IP(long(self), version=6)
            elif type == 'mapped':
                return IP(0xffff << 32 | long(self), version=6)
        else:
            return self

    def to_reverse(self):
        '''
        Convert the IP address to a PTR record in .in-addr.arpa for IPv4 and
        .ip6.arpa for IPv6 addresses.

        >>> ip = IP('192.0.2.42')
        >>> print ip.to_reverse()
        42.2.0.192.in-addr.arpa
        >>> print ip.to_ipv6().to_reverse()
        0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.a.2.2.0.0.0.0.c.2.0.0.2.ip6.arpa
        '''
        if self.v == 4:
            return '.'.join(list(self.dq.split('.')[::-1]) + ['in-addr', 'arpa'])
        else:
            return '.'.join(list(self.hex())[::-1] + ['ip6', 'arpa'])

    def to_tuple(self):
        '''
        Used for comparisons.
        '''
        return (self.dq, self.mask)


class Network(IP):
    '''
    Network slice calculations.

    :param ip: network address
    :type ip: :class:`IP` or str or long or int
    :param mask: netmask
    :type mask: int or str


    >>> localnet = Network('127.0.0.1/8')
    >>> print localnet
    127.0.0.1/8
    '''

    def netmask(self):
        '''
        Network netmask derived from subnet size, as IP object.

        >>> localnet = Network('127.0.0.1/8')
        >>> print localnet.netmask()
        255.0.0.0
        '''
        return IP(self.netmask_long(), version=self.version())

    def netmask_long(self):
        '''
        Network netmask derived from subnet size, as long.

        >>> localnet = Network('127.0.0.1/8')
        >>> print localnet.netmask_long()
        4278190080
        '''
        if self.version() == 4:
            return (0xffffffffL >> (32 - self.mask)) << (32 - self.mask)
        else:
            return (0xffffffffffffffffffffffffffffffffL >> (128 - self.mask)) << (128 - self.mask)

    def network(self):
        '''
        Network address, as IP object.

        >>> localnet = Network('127.128.99.3/8')
        >>> print localnet.network()
        127.0.0.0
        '''
        return IP(self.network_long(), version=self.version())

    def network_long(self):
        '''
        Network address, as long.

        >>> localnet = Network('127.128.99.3/8')
        >>> print localnet.network_long()
        2130706432
        '''
        return self.ip & self.netmask_long()

    def broadcast(self):
        '''
        Broadcast address, as IP object.

        >>> localnet = Network('127.0.0.1/8')
        >>> print localnet.broadcast()
        127.255.255.255
        '''
        # XXX: IPv6 doesn't have a broadcast address, but it's used for other
        #      calculations such as <Network.host_last>
        return IP(self.broadcast_long(), version=self.version())

    def broadcast_long(self):
        '''
        Broadcast address, as long.

        >>> localnet = Network('127.0.0.1/8')
        >>> print localnet.broadcast_long()
        2147483647
        '''
        if self.version() == 4:
            return self.network_long() | (0xffffffffL - self.netmask_long())
        else:
            return self.network_long() \
                | (0xffffffffffffffffffffffffffffffffL - self.netmask_long())

    def host_first(self):
        '''
        First available host in this subnet.
        '''
        if (self.version() == 4 and self.mask > 30) or \
                (self.version() == 6 and self.mask > 126):
            return self
        else:
            return IP(self.network_long() + 1, version=self.version())

    def host_last(self):
        '''
        Last available host in this subnet.
        '''
        if (self.version() == 4 and self.mask == 32) or \
                (self.version() == 6 and self.mask == 128):
            return self
        elif (self.version() == 4 and self.mask == 31) or \
                (self.version() == 6 and self.mask == 127):
            return IP(long(self) + 1, version=self.version())
        else:
            return IP(self.broadcast_long() - 1, version=self.version())

    def in_network(self, other):
        '''
        Check if the given IP address is within this network.
        This function is deprecated, use check_collision instead.
        '''
        warnings.warn('%s.in_network is deprecated, use check_collision in '
                      'stead' % (self.__class__.__name__,),
                      DeprecationWarning,
                      stacklevel=2)
        other = Network(other)
        return self.network_long() <= other.network_long() <= self.broadcast_long()

    def check_collision(self, other):
        '''
        Check another network against the given network and checks for IP collisions.
        '''
        other = Network(other)
        return self.network_long() <= other.network_long() <= self.broadcast_long() or \
            other.network_long() <= self.network_long() <= other.broadcast_long()

    def __str__(self):
        '''
        Return CIDR representation of the network.

        >>> net = Network("::1/64")
        >>> print str(net)
        0000:0000:0000:0000:0000:0000:0000:0001/64
        '''
        return "%s/%d" % (self.dq, self.mask)

    def __contains__(self, ip):
        '''
        Check if the given ip is part of the network.

        >>> '192.0.2.42' in Network('192.0.2.0/24')
        True
        >>> '192.168.2.42' in Network('192.0.2.0/24')
        False
        '''
        return self.check_collision(ip)

    def __lt__(self, other):
        return self.size() < IP(other).size()

    def __le__(self, other):
        return self.size() <= IP(other).size()

    def __gt__(self, other):
        return self.size() > IP(other).size()

    def __ge__(self, other):
        return self.size() >= IP(other).size()

    def __eq__(self, other):
        return self.size() == IP(other).size()

    def __getitem__(self, key):
        if isinstance(key, slice):
            # Work-around IPv6 subnets being huge. Slice indices don't like
            # long int.
            x = key.start or 0
            slice_stop = (key.stop or self.size()) - 1
            slice_step = key.step or 1
            arr = list()
            while x < slice_stop:
                arr.append(IP(long(self) + x))
                x += slice_step
            return tuple(arr)
        else:
            return IP(long(self) + key)

    def __iter__(self):
        '''
        Generate a range of usable host IP addresses within the network, as IP
        objects.

        >>> for ip in Network('192.168.114.0/30'):
        ...     print str(ip)
        ...
        192.168.114.1
        192.168.114.2
        '''

        curr = long(self.host_first())
        stop = long(self.host_last())
        while curr <= stop:
            yield IP(curr)
            curr += 1

    def has_key(self, ip):
        '''
        Check if the given ip is part of the network.

        :param ip: the ip address
        :type ip: :class:`IP` or str or long or int

        >>> net = Network('192.0.2.0/24')
        >>> net.has_key('192.168.2.0')
        False
        >>> net.has_key('192.0.2.42')
        True
        '''
        return self.__contains__(ip)

    def size(self):
        '''
        Number of ip's within the network.

        >>> net = Network('192.0.2.0/24')
        >>> print net.size()
        256
        '''
        return 2 ** ((self.version() == 4 and 32 or 128) - self.mask)


if __name__ == '__main__':
    tests = [
        ('192.168.114.42', 23, ['192.168.0.1', '192.168.114.128', '10.0.0.1']),
        ('123::', 128, ['123:456::', '::1', '123::456']),
        ('::42', 64, ['::1', '1::']),
        ('2001:dead:beef:1:c01d:c01a::', 48, ['2001:dead:beef:babe::']),
        ('10.10.0.0', '255.255.255.0', ['10.10.0.20', '10.10.10.20']),
        ('2001:dead:beef:1:c01d:c01a::', 'ffff:ffff:ffff::', ['2001:dead:beef:babe::']),
        ('10.10.0.0/255.255.240.0', None, ['10.10.0.20', '10.10.250.0']),
    ]

    for ip, mask, test_ip in tests:
        net = Network(ip, mask)
        print '==========='
        print 'ip address:', net
        print 'to ipv6...:', net.to_ipv6()
        print 'ip version:', net.version()
        print 'ip info...:', net.info()
        print 'subnet....:', net.subnet()
        print 'num ip\'s..:', net.size()
        print 'integer...:', long(net)
        print 'hex.......:', net.hex()
        print 'netmask...:', net.netmask()
        # Not implemented in IPv6
        if net.version() == 4:
            print 'network...:', net.network()
            print 'broadcast.:', net.broadcast()
        print 'first host:', net.host_first()
        print 'reverse...:', net.host_first().to_reverse()
        print 'last host.:', net.host_last()
        print 'reverse...:', net.host_last().to_reverse()
        for ip in test_ip:
            print '%s in network: ' % ip, ip in net
