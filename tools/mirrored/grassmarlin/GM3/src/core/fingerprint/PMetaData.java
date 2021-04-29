package core.fingerprint;

import core.importmodule.ImportItem;
import org.jnetpcap.protocol.tcpip.Tcp;
import util.Cidr;

import java.util.Collections;
import java.util.Set;

/**
 * Class to contain a packet's meta data
 */
public class PMetaData {

    public final ImportItem source;
    public final long time;
    public final long frame;
    public final int sourcePort;
    public final int destPort;
    public final short transportProtocol;
    public final Cidr sourceIp;
    public final byte[] sourceMac;
    public final Cidr destIp;
    public final byte[] destMac;
    public final long ack;
    public final long dSize;
    public final int ethertype;
    public final int mss;
    public final long seqNum;
    public final int ttl;
    public final int windowNum;
    public final Set<Tcp.Flag> flags;

    public PMetaData(ImportItem source, long time, long frame, int sourcePort, int destPort, short transportProtocol, Cidr sourceIp, byte[] sourceMac, Cidr destIp, byte[] destMac, long ack,
                     long dSize, int ethertype, int mss, long seqNum, int ttl, int windowNum, Set<Tcp.Flag> flags) {

        this.source = source;
        this.time = time;
        this.frame = frame;
        this.sourcePort = sourcePort;
        this.destPort = destPort;
        this.transportProtocol = transportProtocol;
        this.sourceIp = sourceIp;
        this.sourceMac = sourceMac;
        this.destIp = destIp;
        this.destMac = destMac;
        this.ack = ack;
        this.dSize = dSize;
        this.ethertype = ethertype;
        this.mss = mss;
        this.seqNum = seqNum;
        this.ttl = ttl;
        this.windowNum = windowNum;
        if (flags != null) {
            this.flags = Collections.unmodifiableSet(flags);
        } else {
            this.flags = null;
        }
    }

    public PMetaData() {
        this(null, -1, -1, -1, -1, (short)-1, null, new byte[0], null, new byte[0], -1, -1, -1, -1, -1, -1, -1, null);
    }

}
