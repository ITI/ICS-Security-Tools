package iadgov.offlinepcap;

import core.exec.IEEE802154Data;
import core.fingerprint.PMetaData;
import core.fingerprint.PacketData;
import core.importmodule.ImportItem;
import core.protocol.IEEE_802_15_4;
import core.protocol.Zep;
import org.jnetpcap.nio.JBuffer;
import util.Cidr;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.InterruptedException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PcapFileParser {
    private final ImportItem source;
    protected final Path inPath;
    private boolean done;
    private BlockingQueue<Object> packetQueue;

    private final PacketHandler handler;

    protected PcapFileParser(ImportItem source, Path inPath) {
        this.source = source;
        this.inPath = inPath;
        this.packetQueue = new ArrayBlockingQueue<>(100);
        handler = new PacketHandler(source, packetQueue);
    }

    public static Iterator<Object> getPcapFileIterator(ImportItem source, Path inPath) throws IllegalStateException{
        PcapFileParser parser = new PcapFileParser(source, inPath);

        parser.parseSource();

        return parser.new LogicalIterator();
    }

    protected void parseSource() throws IllegalStateException{
        done = false;

        Runnable loop = () -> {
            int idxFrame = 1;
            try(ByteChannel reader = Files.newByteChannel(inPath)) {
                // Read header
                byte[] header = new byte[24];
                ByteBuffer buffer = ByteBuffer.allocate(header.length);
                reader.read(buffer);
                buffer.rewind();
                buffer.get(header);
                // Process header
                final boolean isSwapped;
                //TODO: These need to be adjsuted for sign, what with signed byte values being a thing.
                if(header[0] == -95 && header[1] == -78 && header[2] == -61 && header[3] == -44) {
                    isSwapped = true;
                } else if(header[0] == -44 && header[1] == -61 && header[2] == -78 && header[3] == -95) {
                    isSwapped = false;
                } else {
                    //Invalid header
                    return;
                }

                //final int majorVersion = intFromBytes(header, 5, 2, isSwapped);
                //final int minorVersion = intFromBytes(header, 7, 2, isSwapped);
                final long secGmtOffset = intFromBytes(header, 9, 4, isSwapped);
                //TODO: SigFigs
                //TODO: SnapLen
                //TODO: Network

                final ByteBuffer reusableBuffer = ByteBuffer.allocateDirect(1500);

                buffer = ByteBuffer.allocateDirect(16);
                buffer.mark();
                if (isSwapped) {
                    buffer.order(ByteOrder.BIG_ENDIAN);
                } else {
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                }
                while(16 == reader.read(buffer)) {
                    final long sTimestamp = buffer.getInt(0);//intFromBytes(headerPacket, 0, 4, true);
                    final long usTimestamp = buffer.getInt(4);//intFromBytes(headerPacket, 4, 4, true);
                    final int lengthPacket = buffer.getInt(8);//intFromBytes(headerPacket, 8, 4, true);       //This is the captured length

                    final ByteBuffer contentsPacket;
                    if (lengthPacket <= 1500) {
                        contentsPacket = reusableBuffer;
                        contentsPacket.limit(lengthPacket);
                        contentsPacket.position(0);
                    } else {
                        contentsPacket = ByteBuffer.allocateDirect(lengthPacket);
                    }

                    int read = reader.read(contentsPacket);
                    if(lengthPacket != read) {
                        //Insufficient bytes in file to read packet
                        return;
                    }
                    contentsPacket.rewind();

                    final int cbProcessed = handler.handle(contentsPacket, (sTimestamp + secGmtOffset) * 1000L + usTimestamp / 1000, idxFrame++);
                    source.recordTaskProgress(lengthPacket + 16 - cbProcessed);

                    buffer.reset();
                }
            } catch(IOException ex) {
                ex.printStackTrace();
            } finally {
                done = true;
            }
        };
        Thread loopThread = new Thread(loop, "pcap loop");
        loopThread.setDaemon(true);
        loopThread.start();
    }

    protected static int intFromBytes(byte[] buffer, int offset, int size, boolean isSwapped) {
        int result = 0;
        for(int idx = 0; idx < size; idx++) {
            result <<= 8;
            if(isSwapped) {
                result |= (buffer[offset + size - idx - 1] & 0xFF);
            } else {
                result |= (buffer[offset + idx] & 0xFF);
            }
        }
        return result;
    }

    protected static long longFromBytes(byte[] buffer, int offset, int size, boolean isSwapped) {
        long result = 0;
        for(int idx = 0; idx < size; idx++) {
            result <<= 8;
            if(isSwapped) {
                result |= (buffer[offset + size - idx - 1] & 0xFF);
            } else {
                result |= (buffer[offset + idx] & 0xFF);
            }
        }
        return result;
    }

    protected class LogicalIterator implements Iterator<Object> {

        @Override
        public boolean hasNext() {
            return !(done && packetQueue.isEmpty());
        }

        @Override
        public Object next() {
            return packetQueue.poll();
        }
    }
}
