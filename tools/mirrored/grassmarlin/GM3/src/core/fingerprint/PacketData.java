package core.fingerprint;

import core.document.graph.LogicalNode;
import core.importmodule.ImportItem;
import org.apache.commons.lang3.ArrayUtils;
import org.jnetpcap.nio.JBuffer;
import org.jnetpcap.protocol.tcpip.Tcp;
import util.Cidr;

import java.math.BigInteger;
import java.util.Set;

/**
 * Wrapper to contain the meta data and payload for a packet, along with the logical Nodes for the source and dest
 */
public class PacketData {

    private final int completionUnits;

    private  LogicalNode sourceNode;
    private  LogicalNode destNode;

    private final PMetaData meta;

    private final JBuffer payload;

    public PacketData(int completionUnits, PMetaData meta, JBuffer payload) {
        this.completionUnits = completionUnits;
        this.meta = meta;
        this.payload = payload;
    }

    public PacketData(int completionUnits, PMetaData meta) {
        this(completionUnits, meta, null);
    }

    public int getCompletionUnits() {
        return this.completionUnits;
    }

    public void setSourceNode(LogicalNode sourceNode) {
        this.sourceNode = sourceNode;
    }

    public void setDestNode(LogicalNode destNode) {
        this.destNode = destNode;
    }

    public ImportItem getSource() { return this.meta.source; }

    public LogicalNode getSourceNode() {
        return this.sourceNode;
    }

    public LogicalNode getDestNode() {
        return this.destNode;
    }


    public byte getByte(int i) {
        if (payload != null) {
            return payload.getByte(i);
        } else {
            return 0;
        }
    }

    public byte[] getByteArray(int index, byte[] bytes, int offset, int length, boolean bigEndian) {
        byte[] ret = new byte[0];
        if (payload != null) {
            ret = payload.getByteArray(index, bytes, offset, length);

            if (!bigEndian) {
                ArrayUtils.reverse(ret);
            }
        }


        return ret;
    }

    public byte[] getByteArray(int index, byte[] bytes, int offset, int length) {
        return this.getByteArray(index, bytes, offset, length, true);
    }

    public byte[] getByteArray(int offset, int length) {
        byte[] ret = new byte[0];
        if (payload != null && payload.size() > offset + length) {
            ret = payload.getByteArray(offset, length);
        }

        return ret;
    }

    public int getInt(int offset, boolean bigEndian) {
        int ret = 0;
        if (payload != null && payload.size() > offset + Integer.BYTES) {
            byte[] bytes = this.getByteArray(offset, Integer.BYTES);
            if (!bigEndian) {
                ArrayUtils.reverse(bytes);
            }
            ret = new BigInteger(1, bytes).intValue();
        }
        return ret;
    }

    public int getInt(int offset, int length, boolean bigEndian) {
        int ret = 0;
        if (payload != null && payload.size() > offset + length) {
            byte[] bytes = this.getByteArray(offset, length);
            if (!bigEndian) {
                ArrayUtils.reverse(bytes);
            }
            ret = new BigInteger(1, bytes).intValue();
        }

        return ret;
    }

    /**
     * Locates the bytes within given length from the offset in the buffer.
     * @param search Bytes to search for.
     * @param offset Offset to start search at.
     * @param length Length to search within.
     * @return Location of the start of the matched sequence on success, else -1 on failure.
     */
    public int match(byte[] search, int offset, int length) {
        int ret = -1;
        // you can not look at data at negative indexes
        if (offset < 0) {
            offset = 0;
        }
        if (payload != null && search.length <= length) {
            int searchLength = search.length;
            if (searchLength > 0) {
                int limit = Math.min(offset + length, payload.size()) - searchLength - offset;
                byte byte0 = search[0];
                for (int start = offset; start <= limit; ++start) {
                    if (payload.getByte(start) == byte0) {
                        int i = 0;
                        for (; i < searchLength; ++i) {
                            if (search[i] != payload.getByte(start + i)) {
                                break;
                            }
                        }
                        if (i == searchLength) {
                            ret = start;
                            break;
                        }
                    }
                }
            }
        }
        return ret;
    }

    public byte[] extract(int from, int to, int length) {
        byte[] ret = new byte[0];

        if (from >= 0 && from < payload.size() && to >= 0 && to < payload.size()) {
            int start = Math.min(from, to);
            int end = Math.min(start + length, Math.max(to, from));

            ret = payload.getByteArray(start, end - start);
        }

        return ret;
    }

    public byte[] extractLittle(final int from, final int to, final int length) {
        byte[] bytes = extract(from, to, length);
        ArrayUtils.reverse(bytes);
        return bytes;
    }

    public int size() {
        if (this.payload != null) {
            return payload.size();
        } else {
            return 0;
        }
    }

    /**
     *
     * @return whether there is payload data for this connection info
     */
    public boolean hasPayload() {
        return this.payload != null;
    }

    public int getSourcePort() {
        return this.meta.sourcePort;
    }

    public int getDestPort() {
        return this.meta.destPort;
    }

    public short getTransportProtocol() {
        return this.meta.transportProtocol;
    }

    public Cidr getSourceIp() {
        return this.meta.sourceIp;
    }

    public Cidr getDestIp() {
        return this.meta.destIp;
    }

    public byte[] getSourceMac() {
        return this.meta.sourceMac;
    }

    public byte[] getDestMac() {
        return this.meta.destMac;
    }

    public long getAck() {
        return this.meta.ack;
    }

    public long getdSize() {
        return this.meta.dSize;
    }

    public long getFrame() {
        return this.meta.frame;
    }

    public long getTime() {
        return this.meta.time;
    }

    public int getEthertype() {
        return this.meta.ethertype;
    }

    public int getMss() {
        return this.meta.mss;
    }

    public long getSeqNum() {
        return this.meta.seqNum;
    }

    public int getTtl() {
        return this.meta.ttl;
    }

    public int getWindowNum() {
        return this.meta.windowNum;
    }

    public Set<Tcp.Flag> getFlags() { return this.meta.flags; }

}
