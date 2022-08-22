import optparse
import pyd20mx

def main():
    parser = optparse.OptionParser()

    parser.add_option("--data", dest="dat_image",
                      help="disk image", metavar="FILE")
    parser.add_option("--address", dest="address",
                      help="older offset", metavar="FILE", default=0, type="int")
    options, args = parser.parse_args()




    fd_mem = pyd20mx.fd_d20mx_cache()
    fd_dat = open(options.dat_image, "rb")

    fd_mem.seek(options.address)
    fd_mem.write(fd_dat.read())

if __name__ == "__main__":
    main()

    
