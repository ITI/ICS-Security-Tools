package iadgov.offlinepcap;

import core.exec.IEEE802154Data;
import core.fingerprint.PMetaData;
import core.fingerprint.PacketData;
import core.importmodule.ImportItem;
import core.logging.Logger;
import core.logging.Severity;
import core.protocol.IEEE_802_15_4;
import core.protocol.Zep;
import org.jnetpcap.nio.JBuffer;
import util.Cidr;

import java.io.IOException;
import java.lang.InterruptedException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PcapNgFileParser {
    // Parser components
    private final ImportItem source;
    protected final Path inPath;
    private boolean done;
    private int idxFrame;
    private BlockingQueue<Object> packetQueue;
    private final PacketHandler handler;

    // Properties of the data stream
    protected boolean isLittleEndian = false;
    protected Map<Integer, Long> timestampResolutions = new HashMap<>();
    protected int idNextBlock = 0;

    //Buffers
    private final ByteBuffer bufEnhancedHeader = ByteBuffer.allocateDirect(20);

    protected PcapNgFileParser(ImportItem source, Path inPath) {
        this.source = source;
        this.inPath = inPath;
        done = false;
        idxFrame = 1;
        this.packetQueue = new ArrayBlockingQueue<>(100);
        handler = new PacketHandler(source, packetQueue);
    }

    public static Iterator<Object> getPcapFileIterator(ImportItem source, Path inPath) throws IllegalStateException{
        PcapNgFileParser parser = new PcapNgFileParser(source, inPath);

        parser.parseSource();

        return parser.new LogicalIterator();
    }

    protected static long divisorFromTsresol(byte resolution) throws IOException {
        if((resolution & 0x80) == 0x80) {
            // MSB is 1 -> power of 2
            if((resolution & 0x7F) > 63) {
                throw new IOException("Unsupported timestamp resolution (" + resolution + ")");
            }
            return 1 << (resolution & 0x7F);
        } else {
            // MSB is 0 -> power of 10
            long result = 1;
            while(resolution-- > 0) {
                result *= 10;
            }
            return result;
        }
    }

    protected Map<Integer, ByteBuffer> parseOptions(ByteBuffer bufOptions) {
        HashMap<Integer, ByteBuffer> mapOptions = new HashMap<>();
        if(!bufOptions.hasRemaining()) {
            //If no options are present return now.
            return mapOptions;
        }

        int codeOption;
        int lengthOption;
        final byte[] arrData = bufOptions.array();
        do {
            codeOption = bufOptions.getShort();
            lengthOption = bufOptions.getShort();
            ByteBuffer bufOption = ByteBuffer.wrap(arrData, bufOptions.position(), lengthOption);
            bufOption.order(bufOptions.order());

            mapOptions.put(codeOption, bufOption);
            while(lengthOption > 0) {
                lengthOption -= 4;
                bufOptions.getInt();
            }
        } while(codeOption != 0 && bufOptions.hasRemaining());

        return mapOptions;
    }

    protected void processIdb(SeekableByteChannel channel, int size) throws IOException {
        final int idBlock = idNextBlock++;
        final ByteBuffer buf = ByteBuffer.allocate(size);
        if(isLittleEndian) {
            buf.order(ByteOrder.LITTLE_ENDIAN);
        } else {
            buf.order(ByteOrder.BIG_ENDIAN);
        }

        channel.read(buf);
        buf.position(8);
        Map<Integer, ByteBuffer> options = parseOptions(buf);

        //if_tsresol
        if(options.containsKey(9)) {
            timestampResolutions.put(idBlock, divisorFromTsresol(options.get(9).get()));
        } else {
            timestampResolutions.put(idBlock, 1000000L);
        }

        //TODO: Support for if_tzone (10) (can't be done now since the format isn't in the standard yet)
    }

    /**
     * @param channel
     * @return The number of bytes to advance the channel
     */
    protected int processBlockHeader(SeekableByteChannel channel) throws IOException {
        //Read first 8 bytes of header
        final byte[] header = new byte[8];
        ByteBuffer buffer = ByteBuffer.allocate(header.length);
        final int cbRead = channel.read(buffer);
        //End of stream
        if(cbRead == -1) {
            return -1;
        } else if(cbRead != 8) {
            throw new IOException("Unable to read start of PcapNg Block Header.");
        }

        buffer.rewind();
        buffer.get(header);

        final int typeBlock = intFromBytes(header, 0, 4, isLittleEndian);
        final int sizeBlock = intFromBytes(header, 4, 4, isLittleEndian);

        //If we're reading a SHB, then we don't know the endianness and the size can be wrong, so skip the integrity checks.
        if(typeBlock != 0x0A0D0D0A) {
            if ((sizeBlock & 3) != 0) {
                throw new IOException("PcapNg contains invalid block size (" + sizeBlock + ")");
            }

            if (typeBlock < 0) {
                return sizeBlock - 8;
            }
        }

        final ByteBuffer bufPacket;
        final int cbProcessed;

        switch(typeBlock) {
            case 1: //Interface Description Block
                //Don't process the trailing size; it will throw off bounds checking when processing options.
                processIdb(channel, sizeBlock - 12);
                source.recordTaskProgress(sizeBlock);
                return 4;   // 4 bytes of trailing size
            // Skip these blocks (Don't need the data)
            case 5: //Interface Statistics Block
            case 7: //IRIG Timestamp Block
            case 8: //ARINC 429 in AFDX Encapsulation Block
                source.recordTaskProgress(sizeBlock);
                return sizeBlock - 8;
            case 3: //Simple Packet Block
                source.recordTaskProgress(16);
                //The Block contains the original packet length (4 bytes) then the packet contents (padded to a 32-bit boundary) then the total length is repeated.
                channel.position(channel.position() + 4);
                bufPacket = ByteBuffer.allocateDirect(sizeBlock - 16);
                channel.read(bufPacket);
                bufPacket.rewind();
                cbProcessed = handler.handle(bufPacket, Instant.now().toEpochMilli(), idxFrame++);
                source.recordTaskProgress(sizeBlock - cbProcessed);
                //Skip the last 4 bytes which is the repeated size
                return 4;
            case 2: //Packet Block (Obsolete, but still has to be readable)
                //The only difference between these two is the interfaceID is 2 bytes (instead of 4) in the Packet Block, with the following 2 bytes for the Drops Count (not present in Enhanced Packet Block).  Since we use neither, parsing is the same.
            case 6: //Enhanced Packet Block
                bufEnhancedHeader.rewind();
                channel.read(bufEnhancedHeader);

                final int idInterface;
                if(typeBlock == 2) {
                    idInterface = bufEnhancedHeader.getShort(0);
                } else {
                    idInterface = bufEnhancedHeader.getInt(0);
                }
                final long ts = ((long)bufEnhancedHeader.getInt(4) << 32) | ((long)bufEnhancedHeader.getInt(8) & 0x00000000FFFFFFFFL);

                final int cbCapture = (bufEnhancedHeader.getInt(12) + 3) & ~0x3;  // Round up to the nearest multiple of 32-bits.
                bufPacket = ByteBuffer.wrap(new byte[cbCapture]);
                channel.read(bufPacket);
                bufPacket.rewind();
                cbProcessed = handler.handle(bufPacket, ts * 1000L / timestampResolutions.get(idInterface), idxFrame++);
                //Variable length options will be included.
                source.recordTaskProgress(sizeBlock - cbProcessed);
                return sizeBlock - (28 + cbCapture);
            case 4: //Name Resolution Block
                //TODO: Parse the name resolution block and use the data to augment nodes
                source.recordTaskProgress(sizeBlock);
                return sizeBlock - 8;
            case 0x0A0D0D0A:    //Section Header Block
                final ByteBuffer blockSectionHeader = ByteBuffer.wrap(new byte[4]);
                channel.read(blockSectionHeader);

                final int magicNumber = intFromBytes(blockSectionHeader.array(), 0, 4, false);
                if(magicNumber == 0x1A2B3C4D) {
                    isLittleEndian = false;
                    bufEnhancedHeader.order(ByteOrder.BIG_ENDIAN);
                } else if(magicNumber == 0x4D3C2B1A) {
                    isLittleEndian = true;
                    bufEnhancedHeader.order(ByteOrder.LITTLE_ENDIAN);
                } else {
                    throw new IOException("BOM Field in Section Header Block is wrong (0x" + Integer.toHexString(magicNumber) + ")");
                }

                final int sizeShb = intFromBytes(header, 4, 4, isLittleEndian);

                source.recordTaskProgress(sizeShb);
                return sizeShb - 12;
            //Error conditions of varying severity:
            case 0x00000BAD:    //Custom block that rewriters can copy into new files.
            case 0x40000BAD:    //Custom block that rewriters should not copy into new files.
                Logger.log(this, Severity.Warning, "PcapNg Files contains unparsable data (" + sizeBlock + " bytes)");
                source.recordTaskProgress(sizeBlock);
                return sizeBlock - 8;
            case 0:
            default:
                throw new IOException("Unknown block type: 0x" + ("0000000" + Integer.toHexString(typeBlock)).replaceAll("^.*(?=.{8}$)", ""));
        }
    }

    protected void parseSource() throws IllegalStateException{
        done = false;

        Runnable loop = () -> {
            try(SeekableByteChannel reader = Files.newByteChannel(inPath)) {
                while (true) {
                    // Hand off to the parse routine...
                    long cbRemaining = (long) processBlockHeader(reader);
                    //Check for end-of-stream
                    if (cbRemaining == -1) {
                        break;
                    }
                    //And then advance the pointer by however many bytes are remaining
                    if (cbRemaining != 0) {
                        reader.position(reader.position() + cbRemaining);
                    }
                }
            } catch(IOException ex) {
                ex.printStackTrace();
            } finally {
                done = true;
            }
        };
        Thread loopThread = new Thread(loop, "pcapng loop");
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
