package core.importmodule.inputIterators.pcap;

import core.Configuration;
import core.exec.IEEE802154Data;
import core.fingerprint.PMetaData;
import core.fingerprint.PacketData;
import core.importmodule.ImportItem;
import core.logging.Logger;
import core.logging.Severity;
import core.protocol.IEEE_802_15_4;
import core.protocol.Zep;
import javafx.application.Platform;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.PcapDumper;
import org.jnetpcap.nio.JBuffer;
import org.jnetpcap.packet.JPacket;
import org.jnetpcap.packet.JPacketHandler;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Tcp;
import org.jnetpcap.protocol.tcpip.Udp;
import util.Cidr;
import util.RateLimitedTask;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Class to import a pcap file to return an {@link Iterator} of {@link core.fingerprint.PacketData}
 */
public class PcapFileParser {

    private static final int PACKET_INTERVAL_MILLIS = 232;
    private static final int PACKET_INTERVAL_PACKETS = 4000;

    private long lastCheckTime;
    private int numPackets;
    protected PcapDumper dumper = null;

    protected Runnable fnOnNewPacket = null;

    /**
     * IANA id for TCP protocol.
     */
    public static final short TCP_ID = 6;
    /**
     * IANA id for UDP protocol.
     */
    public static final short UDP_ID = 17;
    /**
     * UNKNOWN protocol.
     */
    public static final short UNKNOWN_ID = -1;

    private final ImportItem source;

    protected final Path inPath;

    private boolean done;

    private BlockingQueue<Object> packetQueue;



    protected PcapFileParser(ImportItem source, Path inPath) {
        this.source = source;
        this.inPath = inPath;
        this.packetQueue = new ArrayBlockingQueue<>(100);

        this.lastCheckTime = System.currentTimeMillis();
        numPackets = 0;
    }

    public static Iterator<Object> getPcapFileIterator(ImportItem source, Path inPath) throws IllegalStateException{
        PcapFileParser parser = new PcapFileParser(source, inPath);

        parser.parseSource();

        return parser.new LogicalIterator();
    }

    protected void parseSource() throws IllegalStateException{
        done = false;
        Pcap pcap = getHandle();

        String txtFilter = Configuration.getPreferenceString(Configuration.Fields.PCAP_FILTER_STRING);
        if(txtFilter != null && !txtFilter.trim().equals("")) {
            final PcapBpfProgram filter = new PcapBpfProgram();
            final int mask = ~((1 << (32 - Configuration.getPreferenceLong(Configuration.Fields.LOGICAL_DYNAMIC_SUBNET_BITS))) - 1);

            if(pcap.compile(filter, txtFilter, 1, mask) != Pcap.OK) {
                Logger.log(this, Severity.Warning, "Unable to initialize PCAP filter for '" + txtFilter + "' (" + pcap.getErr() + ").  Filtering will not be performed.");
            } else {
                Logger.log(this, Severity.Information, "Using PCAP filter: '" + txtFilter + "'");
                pcap.setFilter(filter);
            }
        }

        if (pcap == null) {
            throw new IllegalStateException("Unable load pcap from " + this.inPath);
        }

        Runnable loop = () -> {
            pcap.loop(Pcap.LOOP_INFINITE, new PcapPacketHandler(), this.packetQueue);
            done = true;
        };
        Thread loopThread = new Thread(loop, "pcap loop");
        loopThread.setDaemon(true);
        loopThread.start();
    }

    /**
     * Retrieves a new PCAP handle. May return null if JNetPCAP is not
     * available.  This is non-static so that Live PCAP capture can override it.
     *
     * @return Pcap handle to the pcap file that belongs to this ImportItem.
     */
    protected Pcap getHandle() {
        //We will receive error messages, but don't preserve them.
        StringBuilder errorBuffer = new StringBuilder();
        Pcap handle = null;

        try {
            handle = Pcap.openOffline(this.inPath.toString(), errorBuffer);
            if( handle == null || errorBuffer.length() > 0 ) {
                String msg = errorBuffer.toString();
                throw new java.lang.IllegalArgumentException(msg);
            }
        } catch (UnsatisfiedLinkError err) {
            Logger.log(this, Severity.Error, "Importing PCAP is disabled. " + err.getMessage());
        } catch( IllegalArgumentException ex ) {
            Logger.log(this, Severity.Error, "Failed to import. Reason: " + ex.getMessage());
        }

        return handle;
    }

    private class PcapPacketHandler implements JPacketHandler<BlockingQueue<Object>> {
        protected final Ethernet eth = new Ethernet();
        protected final Ip4 ip4 = new Ip4();
        protected final Tcp tcp = new Tcp();
        protected final Udp udp = new Udp();
        protected final Zep zep = new Zep();
        IEEE_802_15_4 ieee802154 = new IEEE_802_15_4();

        protected final Tcp.MSS mssHeader = new Tcp.MSS();

        @Override
        public void nextPacket(JPacket packet, BlockingQueue<Object> queue) {
            if(dumper != null) {
                dumper.dump(packet);
            }
            try {
                packet = new PcapPacket(packet);
                if (numPackets++ == PACKET_INTERVAL_PACKETS) {
                    long sleepTime = lastCheckTime + PACKET_INTERVAL_MILLIS - System.currentTimeMillis();
                    if (sleepTime >= 0) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                        }
                    }
                    lastCheckTime = System.currentTimeMillis();
                    numPackets = 0;
                }
                if (!packet.hasHeader(ip4) || !packet.hasHeader(eth)) {
                    source.recordTaskProgress(packet.getCaptureHeader().caplen() + 16);
                    return;
                }

                long srcIp = new BigInteger(1, ip4.source()).longValue();
                long destIp = new BigInteger(1, ip4.destination()).longValue();


                //Associate the macs with the hosts
                try {
                    final java.util.Map<String, String> propertiesSource = new java.util.HashMap<>();
                    propertiesSource.put("MAC", new util.Mac(eth.source()).toString());
                    packetQueue.put(new core.importmodule.LogicalProcessor.Host(new Cidr(srcIp), propertiesSource, null));
                    final java.util.Map<String, String> propertiesDest = new java.util.HashMap<>();
                    propertiesDest.put("MAC", new util.Mac(eth.destination()).toString());
                    packetQueue.put(new core.importmodule.LogicalProcessor.Host(new Cidr(destIp), propertiesDest, null));
                } catch(InterruptedException ex) {
                    //Ignore the error; we probably have redundant data.
                }

                PacketData data = null;
                if (packet.hasHeader(tcp)) {
                    JBuffer temp = new JBuffer(tcp.getPayloadLength() + 1);
                    packet.transferTo(temp, tcp.getPayloadOffset(), tcp.getPayloadLength(), 0);

                    int mss = -1;
                    if (tcp.hasSubHeader(mssHeader)) {
                        mss = mssHeader.mss();
                    }

                    PMetaData meta = new PMetaData(source, packet.getCaptureHeader().timestampInMillis(), packet.getFrameNumber(), tcp.source(), tcp.destination(), TCP_ID,
                            new Cidr(srcIp), Arrays.copyOf(eth.source(), eth.source().length), new Cidr(destIp), Arrays.copyOf(eth.destination(), eth.destination().length), tcp.ack(), packet.getPacketWirelen(), 2048,
                            mss, tcp.seq(), ip4.ttl(), tcp.windowScaled(), tcp.flagsEnum());
                    data = new PacketData(packet.getCaptureHeader().caplen() + 16, meta, temp);
                } else if (packet.hasHeader(udp)) {

                    if (zep.hasProtocol(udp)) {
                        ieee802154.setBuffer(zep.getNextBuffer());
                        IEEE802154Data meshData = new IEEE802154Data();
                        meshData.setChannel(zep.getChannelID());
                        meshData.settDevice(zep.getDestinationDeviceID());
                        meshData.setsDevice(zep.getSourceDeviceID());
                        meshData.setSource(ieee802154.getSourceDeviceId());
                        meshData.setTarget(ieee802154.getDestinationDeviceId());
                        meshData.setTargetPan(ieee802154.getDestinationPanId());
                        meshData.setIntraPan(ieee802154.isIntraPan());
                        try {
                            queue.put(meshData);
                        } catch (InterruptedException e) {

                        }
                    }

                    JBuffer temp = new JBuffer(udp.getPayloadLength() + 1);
                    packet.transferTo(temp, udp.getPayloadOffset(), udp.getPayloadLength(), 0);

                    PMetaData meta = new PMetaData(source, packet.getCaptureHeader().timestampInMillis(), packet.getFrameNumber(), udp.source(), udp.destination(), UDP_ID,
                            new Cidr(srcIp), Arrays.copyOf(eth.source(), eth.source().length), new Cidr(destIp), Arrays.copyOf(eth.destination(), eth.destination().length), -1, packet.getCaptureHeader().caplen() + 16, 2048,
                            -1, -1, ip4.ttl(), -1, null);
                    data = new PacketData(packet.getCaptureHeader().caplen() + 16, meta, temp);
                } else {
                    PMetaData meta = new PMetaData(source, packet.getCaptureHeader().timestampInMillis(), packet.getFrameNumber(), -1, -1, UNKNOWN_ID, new Cidr(srcIp),
                            Arrays.copyOf(eth.source(), eth.source().length), new Cidr(destIp), Arrays.copyOf(eth.destination(), eth.destination().length), -1,
                            packet.getPacketWirelen(), 2048, -1, -1, ip4.ttl(), -1, null);
                    data = new PacketData(packet.getCaptureHeader().caplen() + 16, meta);
                }

                if (data != null) {
                    try {
                        queue.put(data);
                    } catch (InterruptedException e) {
                        // program must be closing or something
                    }
                }
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    protected class LogicalIterator implements Iterator<Object> {

        @Override
        public boolean hasNext() {
            return !(done && packetQueue.isEmpty());
        }

        @Override
        public Object next() {
            Object result = packetQueue.poll();
            if(result != null && fnOnNewPacket != null) {
                fnOnNewPacket.run();
            }
            return result;
        }
    }
}
