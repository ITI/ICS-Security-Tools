#!/bin/python
import optparse
from elftools import elf
import binascii
from collections import OrderedDict

import archinfo
import cle
import pyvex
import pyd20mx

symbols_whitelist = [
     "standTbl",
     "statTbl",
    #"__EH_FRAME_BEGIN__"
]

def fd_peek(fd, length):
    pos = fd.tell()
    data = fd.read(length)
    fd.seek(pos)
    return data


def fd_memcmp(fd1, fd2, length, chunk_size = 4):
    fd1_old_tell = fd1.tell()
    fd2_old_tell = fd2.tell()
    cbread = 0
    valid = []
    invalid = []
    run_start = (fd1_old_tell, fd2_old_tell)
    run_here = run_start
    valid_run = True
    while cbread < length:
        chunk_size = min(chunk_size, length-cbread)
        data1 = fd1.read(min(chunk_size, length - cbread))
        data2 = fd2.read(min(chunk_size, length - cbread))
        if (data1 == data2) == valid_run and cbread < length-4:
            pass
        else:
            if run_here[0] - run_start[0]:
                #print "Adding run of length %d to %s" % (run_here[0] - run_start[0], valid_run)
                if valid_run:
                    valid.append((run_start, run_here, run_here[0] - run_start[0]))
                else:
                    invalid.append((run_start, run_here, run_here[0] - run_start[0]))
                run_start = run_here
                valid_run = not valid_run
        run_here = (fd1.tell(), fd2.tell())
        cbread += chunk_size

    fd1.seek(fd1_old_tell)
    fd2.seek(fd2_old_tell)
    return valid, invalid

def prev_symbol(ld, addr):
    prev =  max([x for x in  ld.symbols_by_addr.keys() if x <= addr])
    return ld.symbols_by_addr[prev]


def get_diff_meta(ld_cle, fd_memory_image, memory_offset=None):
    diff_meta = {}
    fd_image_on_disk = ld_cle.memory
    if memory_offset is None:
        memory_offset = elf.getBaseAddress()
    diff_meta['sections'] = OrderedDict()
    mem_results_valid = []
    mem_results_invalid = []

    for sec in ld_cle.main_bin.sections:
        if sec.type == "SHT_NULL":
            continue
        if not "SHT_PROGBITS" in sec.type:
            pass
        elif not sec.flags & 0x02 == 0x02:
            pass
        else:
            fd_image_on_disk.seek(sec.min_addr)
            fd_memory_image.seek(sec.min_addr)
            mem_results_valid, mem_results_invalid = fd_memcmp(fd_image_on_disk, fd_memory_image, sec.memsize)
        diff_meta['sections'][sec.name] = {'sec_meta': sec,
                                           'sec_flags': sec.flags,
                                           'sec_type': sec.type,
                                           'mem_results_invalid': mem_results_invalid,
                                           'mem_results_valid': mem_results_valid}
    return diff_meta

def main():
    parser = optparse.OptionParser()
    parser.add_option("--mem_image", dest="mem_image",
                      help="mem image", metavar="FILE", default=None)
    parser.add_option("--mem_offset", dest="mem_offset",
                      help="newer offset", metavar="offset", default=0, type="int")
    parser.add_option("--disk_image", dest="disk_image",
                      help="disk image", metavar="FILE", default=None)
    parser.add_option("--disk_offset", dest="disk_offset",
                      help="older offset", metavar="FILE", default=0, type="int")
    options, args = parser.parse_args()

    if not options.disk_image or not options.mem_image:
        parser.print_help()
        parser.exit(1)

    fd_disk = open(options.disk_image, "rb")
    fd_mem = None
    if options.mem_image == "fd_tcp": 
        fd_mem = pyd20mx.fd_d20mx_tcp()
    elif options.mem_image == "fd_serial":
        fd_mem = pyd20mx.fd_d20mx_serial()
    elif options.mem_image == "fd_cache":
        fd_mem = pyd20mx.fd_d20mx_cache()
    else:
        fd_mem = open(options.mem_image, "rb")
    fd_disk.seek(0)
    
    ld_cle = cle.Loader(options.disk_image)
    diff_meta = get_diff_meta(ld_cle, fd_mem, options.mem_offset)
    print "Section Name".ljust(20),
    print "Address".ljust(20),
    print "Size".ljust(20),
    print "Status"
    for section in diff_meta['sections'].values():
        match_status = ''
        if not section['sec_meta'].type == "SHT_PROGBITS":
            match_status = "NOT_PROGBITS"
        elif not section['sec_meta'].flags & 0x02 == 0x02:
            match_status = "NO_ALLOC"
        elif section['sec_meta'].memsize == 0:
            match_status = "ZERO_SIZE"
        elif len(section['mem_results_invalid']):
            match_status = "!!! MISMATCH !!!"
        else:
            match_status = "MATCH"
        print section['sec_meta'].name.ljust(20),
        print hex(section['sec_meta'].vaddr).ljust(20),
        print hex(section['sec_meta'].memsize).ljust(20),
        print '[%s]' % match_status
        #print section['sec_meta'].sh_flags, section['sec_meta'].sh_type
        #print section['mem_results_invalid']
        for mem_result in section['mem_results_invalid']:
            fd_disk.seek(mem_result[0][0])
            fd_mem.seek(mem_result[0][1])
            sym = prev_symbol(ld_cle.main_bin, mem_result[0][0])
            if sym.name in symbols_whitelist:
                continue
            #print "\t", sym.name
            #print section['sec_meta'].flags
            if section['sec_meta'].flags & elf.constants.SH_FLAGS.SHF_EXECINSTR == elf.constants.SH_FLAGS.SHF_EXECINSTR:
                disk_data = fd_peek(fd_disk, mem_result[2])
                mem_data = fd_peek(fd_mem, mem_result[2])
                print "="*81
                print "%08x" % sym.rebased_addr, sym.name
                cap = ld_cle.main_bin.arch.capstone
                d_old = cap.disasm(disk_data, mem_result[0][0])
                d_new = cap.disasm(mem_data, mem_result[0][0])
                print 'DISK'.center(40, '-')+'|'+'MEMORY'.center(40,'-')
                for x, y in zip(d_old, d_new):
                    print("%08x: %s %s" %(x.address, x.mnemonic.ljust(10), x.op_str.ljust(20))).ljust(40),
                    print("%08x: %s %s" %(y.address, y.mnemonic.ljust(10), y.op_str.ljust(20))),
                    print 
                print "="*81
            else:
                pass
                #print "\t\t[DEAD] ", "%08x" % fd_disk.tell(),
                #print hexdump.dump(fd_peek(fd_disk, mem_result[2])).ljust(40),
                #print "\t\t[LIVE] ", "%08x" % fd_mem.tell(),
                #print hexdump.dump(fd_peek(fd_mem, mem_result[2]))

            #print "\t", "[!] matched for %d/0x%08x out of %d/0x%08x bytes at %x" % \
            #            (mem_result, mem_result, sec.sh_size, sec.sh_size, sec.sh_offset)


if __name__ == "__main__":
    main()
