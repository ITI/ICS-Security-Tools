package iadgov.offlinepcap;

import core.exec.IEEE802154Data;
import core.fingerprint.PMetaData;
import core.fingerprint.PacketData;
import core.importmodule.ImportItem;
import core.protocol.IEEE_802_15_4;
import core.protocol.Zep;
import org.jnetpcap.nio.JBuffer;
import util.Cidr;

import java.lang.InterruptedException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.HashMap;

public class PacketHandler {
    private final ImportItem source;
    private final BlockingQueue<Object> packetQueue;

    public PacketHandler(final ImportItem source, final BlockingQueue<Object> packetQueue) {
        this.source = source;
        this.packetQueue = packetQueue;
    }

    private final HashMap<Integer, byte[]> fragments = new HashMap<>();

    /**
     *
     * @param bufPacket
     * @param msSinceEpoch
     * @return The amount of enqueued progress.
     */
    public int handle(final ByteBuffer bufPacket, final long msSinceEpoch, final int idxFrame) {
        final int cbPacket = bufPacket.limit() - bufPacket.position();
        int startCurrentHeader = bufPacket.position();
        //Link Layer (Only Ethernet supported)
        // Check ethernet header, extract src/dst MAC
        final byte[] macDestination = new byte[6];
        final byte[] macSource = new byte[6];
        for(int idxByte = 0; idxByte < 6; idxByte++) {
            macDestination[idxByte] = bufPacket.get(startCurrentHeader + 0 + idxByte);
            macSource[idxByte] = bufPacket.get(startCurrentHeader + 6 + idxByte);
        }
        final int etherType = 0xFF00 & bufPacket.get(startCurrentHeader + 12) << 8 | 0xFF & bufPacket.get(startCurrentHeader + 13);
        //Only allow IPv4
        if(etherType != 0x0800) {
            return 0;
        }

        startCurrentHeader = 14;

        // Internet Layer (Only support IPv4)
        final byte protocol;
        final Cidr ipSource;
        final Cidr ipDest;
        final int ttl;
        final int cbIp;
        final int idxLastIpByte;

        final byte ipVersionAndHeaderSize = bufPacket.get(startCurrentHeader);
        final ByteBuffer bufPayload;
        if((ipVersionAndHeaderSize & 0xF0) == 0x40) {
            // Process IPv4 header, extract src/dst IP
            final int cbHeader = (ipVersionAndHeaderSize & 0x0F) * 4;

            protocol = bufPacket.get(startCurrentHeader + 9);
            ipSource = new Cidr(((long)bufPacket.getInt(startCurrentHeader + 12)) & 0x00000000FFFFFFFFL);
            ipDest = new Cidr(((long)bufPacket.getInt(startCurrentHeader + 16)) & 0x00000000FFFFFFFFL);
            ttl = (int)bufPacket.get(startCurrentHeader + 8) & 0x000000FF;
            cbIp = (int)bufPacket.getShort(startCurrentHeader + 2) & 0x0000FFFF;
            //if cbIp is 0 there is a good chance that TSO is happening, we'er just going to guess that the packet is
            // the length of the buffer
            if (cbIp > 0) {
                idxLastIpByte = startCurrentHeader + cbIp;
            } else {
                idxLastIpByte = cbPacket;
            }

            final int wFragment = bufPacket.getShort(startCurrentHeader + 6);
            final boolean hasMoreFragments = (wFragment & 0x20) == 0x20;
            final int offsetFragment = (wFragment & 0x1FFF) << 3;

            // If we receive a fragment then there will be no protocol header, except on the first fragment.
            if(hasMoreFragments || offsetFragment != 0) {
                final int idFragment = (int)bufPacket.getShort(startCurrentHeader + 4) & 0x0000FFFF;
                final int idxLastByteInThisFragment = offsetFragment + cbIp - cbHeader;
                final byte[] bufNew;
                if(fragments.containsKey(idFragment)) {
                    // Resize fragment, if needed
                    final byte[] bufOld = fragments.get(idFragment);
                    if(bufOld.length < idxLastByteInThisFragment) {
                        bufNew = new byte[idxLastByteInThisFragment];
                        for(int idx = 0; idx < bufOld.length; idx++) {
                            bufNew[idx] = bufOld[idx];
                        }
                        fragments.put(idFragment, bufNew);
                    } else {
                        //fits without resizing
                        bufNew = bufOld;
                    }
                } else {
                    //Add fragment, setting size appropriately
                    bufNew = new byte[idxLastByteInThisFragment];
                    fragments.put(idFragment, bufNew);
                }
                //Copy the contents of this fragment to the appropriate place in the array.
                for(int idx = offsetFragment; idx < idxLastByteInThisFragment; idx++) {
                    bufNew[idx] = bufPacket.get(startCurrentHeader + cbHeader + idx - offsetFragment);
                }

                if(hasMoreFragments) {
                    //There is no more processing to do on the fragment at this time.
                    //TODO: Correct return result (up through IP header?)
                    return 0;
                }

                //We've pieced together the buffer, now we need to wrap it in a ByteBuffer and process it further.
                startCurrentHeader = 0;
                bufPayload = ByteBuffer.wrap(fragments.get(idFragment));
                fragments.remove(idFragment);
            } else {
                //This is not a fragment and the index is 0, therefore it is not fragmented
                startCurrentHeader += cbHeader;
                bufPayload = bufPacket;
            }
        } else if((ipVersionAndHeaderSize & 0xF0)== 0x60) {
            //TODO: IPv6 support
            return 0;
        } else {
            //No idea what the packet is.
            return 0;
        }

        //Associate the macs with the hosts
        try {
            final java.util.Map<String, String> propertiesSource = new java.util.HashMap<>();
            propertiesSource.put("MAC", new util.Mac(macSource).toString());
            packetQueue.put(new core.importmodule.LogicalProcessor.Host(ipSource, propertiesSource, null));
            final java.util.Map<String, String> propertiesDest = new java.util.HashMap<>();
            propertiesDest.put("MAC", new util.Mac(macDestination).toString());
            packetQueue.put(new core.importmodule.LogicalProcessor.Host(ipDest, propertiesDest, null));
        } catch(InterruptedException ex) {
            //Ignore the error; we probably have redundant data.
        }

        // Transport Layer (Itemize Tcp and Udp with metadata-only handling of other packets)
        final int portSource;
        final int portDest;
        final JBuffer temp;
        final PMetaData meta;

        switch(protocol) {
            case 6:     //TCP
                portSource = bufPayload.getShort(startCurrentHeader) & 0x0000FFFF;
                portDest = bufPayload.getShort(startCurrentHeader + 2) & 0x0000FFFF;
                final int cbTcpHeaders = 0x3C & (bufPayload.get(startCurrentHeader + 12) >>> 2);
                final int cbPayload;

                if(idxLastIpByte - (startCurrentHeader + cbTcpHeaders) == 0) {
                    //Hack to allow 0-byte TCP packets
                    temp = new JBuffer(1);
                    cbPayload = 0;
                } else {
                    final byte[] contents = new byte[idxLastIpByte - (startCurrentHeader + cbTcpHeaders)];
                    bufPayload.position(startCurrentHeader + cbTcpHeaders);
                    bufPayload.get(contents);
                    temp = new JBuffer(contents);
                    cbPayload = contents.length;
                }
                meta = new PMetaData(source, msSinceEpoch, idxFrame, portSource, portDest, protocol,
                        ipSource, macSource, ipDest, macDestination, -1, cbPayload, etherType,
                        -1, -1, ttl, -1, null);
                try {
                    packetQueue.put(new PacketData(cbPacket, meta, temp));
                } catch(InterruptedException ex) {
                    //Probably nothing worth worrying about.
                    ex.printStackTrace();
                }
                return cbPacket;
            case 17:    //UDP
                portSource = bufPayload.getShort(startCurrentHeader) & 0x0000FFFF;
                portDest = bufPayload.getShort(startCurrentHeader + 2) & 0x0000FFFF;
                final int cbUdp = bufPayload.getShort(startCurrentHeader + 4) & 0x0000FFFF;

                final byte[] contents;
                try {
                    if(startCurrentHeader + cbUdp > cbPacket) {
                        contents = new byte[cbPacket - startCurrentHeader - 8];
                    } else {
                        contents = new byte[cbUdp - 8];
                    }
                    bufPayload.position(startCurrentHeader + 8);
                    bufPayload.get(contents);
                } catch(java.nio.BufferUnderflowException | java.lang.NegativeArraySizeException ex) {
                    System.out.println("idxFrame=" + idxFrame + ", cbUdp=" + cbUdp + ", cbPacket=" + cbPacket + ", startCurrentHeader=" + startCurrentHeader + ", bufPacket.position()=" + bufPacket.position() + ", bufPacket.limit()=" + bufPacket.limit());

                    throw ex;
                }
                if (Zep.isZEPProtocol(portSource, portDest)) {
                    final Zep zep = new Zep();
                    final IEEE_802_15_4 ieee802154 = new IEEE_802_15_4();

                    zep.fromArray(Arrays.copyOf(contents, contents.length));
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
                        packetQueue.put(meshData);
                    } catch (InterruptedException e) {

                    }
                }
                if (contents.length > 0) {
                    temp = new JBuffer(contents);
                } else {
                    temp = null;
                }
                meta = new PMetaData(source, msSinceEpoch, idxFrame, portSource, portDest, protocol,
                        ipSource, macSource, ipDest, macDestination, -1, contents.length, etherType,
                        -1, -1, ttl, -1, null);
                try {
                    packetQueue.put(new PacketData(cbPacket, meta, temp));
                } catch(InterruptedException ex) {
                    //Probably nothing worth worrying about.
                    ex.printStackTrace();
                }
                return cbPacket;
            //case 2:     //IGMP (We need better handling, but keep the default for now)
            case 1:     //ICMP
                //Only process ping responses.  Ping requests are not necessarily for hosts that exist, etc.
                final byte icmpType = bufPayload.get(startCurrentHeader);
                if(icmpType == 0) {
                    meta = new PMetaData(source, msSinceEpoch, idxFrame, -1, -1, (short)-1, ipSource,
                            macSource, ipDest, macDestination, -1,
                            cbPacket, etherType, -1, -1, ttl, -1, null);
                    try {
                        packetQueue.put(new PacketData(cbPacket, meta));
                    } catch (InterruptedException ex) {
                        // Don't Care
                    }
                    return cbPacket;
                }
                return 0;
            default:
                meta = new PMetaData(source, msSinceEpoch, idxFrame, -1, -1, (short)-1, ipSource,
                        macSource, ipDest, macDestination, -1,
                        cbPacket, etherType, -1, -1, ttl, -1, null);
                try {
                    packetQueue.put(new PacketData(cbPacket, meta));
                } catch (InterruptedException ex) {
                    // Don't Care
                }
                return cbPacket;
        }
    }
}