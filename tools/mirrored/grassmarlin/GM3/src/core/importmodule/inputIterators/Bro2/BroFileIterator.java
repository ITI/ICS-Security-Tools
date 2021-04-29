package core.importmodule.inputIterators.Bro2;

import core.fingerprint.PMetaData;
import core.fingerprint.PacketData;
import core.importmodule.ImportItem;
import core.importmodule.inputIterators.pcap.PcapFileParser;
import core.logging.Logger;
import core.logging.Severity;
import util.Cidr;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class for parsing and iterating over Bro2 files
 */
public class BroFileIterator implements Iterator<PacketData> {

    public enum Field {
        TIME("ts"),
        SRC_IP("id.orig_h"),
        SRC_PRT("id.orig_p"),
        DST_IP("id.resp_h"),
        DST_PRT("id.resp_p"),
        PROTO("proto"),
        SRC_BTS("orig_bytes"),
        DST_BTS("resp_bytes");

        String fieldLabel;
        int fieldIndex;

        Field(String fieldLabel) {
            this.fieldLabel = fieldLabel;
        }

        public String getLabel() {
            return this.fieldLabel;
        }

        public void setIndex(int index) {
            this.fieldIndex = index;
        }

        public int getIndex() {
            return this.fieldIndex;
        }
    }

    private static String separator = "\\s";
    private static String empty;
    private static String unset;

    /**
     * The reader required to import any Bro2.
     */


    private final ImportItem source;

    private final Path inPath;

    private boolean done;

    private BlockingQueue<PacketData> packetQueue;

    private Thread loopThread;

    private long size;



    private BroFileIterator(ImportItem source, Path inPath) {
        this.source = source;
        this.inPath = inPath;
        done = false;
        this.packetQueue = new ArrayBlockingQueue<>(100);
    }

    public long getSize() {
        return this.size;
    }

    public static Iterator<PacketData> getBro2LogIterator(ImportItem source, Path inPath) throws IllegalStateException{
        BroFileIterator iterator = new BroFileIterator(source, inPath);

        final AtomicLong size = new AtomicLong(0);
        try (final BufferedReader reader = Files.newBufferedReader(inPath)) {
            reader.lines().filter(line -> !line.startsWith("#")).forEach(line -> size.addAndGet(2));
        } catch (IOException ioe) {
            throw new IllegalStateException("Error reading File: " + inPath);
        }

        iterator.size = size.get();

        if (size.get() > 0) {
            iterator.parseFile();
        }

        return iterator;
    }

    @Override
    public boolean hasNext() {
        return !(done && packetQueue.isEmpty());
    }

    @Override
    public PacketData next() {
        return packetQueue.poll();
    }

    private void parseFile() {
        loopThread = new Thread(() -> {
            try (final BufferedReader reader = Files.newBufferedReader(inPath)) {
                reader.lines().forEach(this::parseLine);
                done = true;
            } catch (IOException ioe) {
                done = true;
                Logger.log(this, Severity.Error, "Error reading File: " + inPath);
            }
        });
        loopThread.start();
    }

    private void parseLine(String line) {
        if (line != null & !line.isEmpty()) {
            String[] tokens = line.split(separator);
            if (tokens[0].startsWith("#")) {
                String directive = tokens[0].substring(1);
                switch (directive) {
                    case "separator":
                        String character = tokens[1];
                        if (character.startsWith("\\x")) {
                            int codepoint = Integer.parseInt(character.substring(2), 16);
                            separator = String.valueOf((char) codepoint);
                        } else {
                            separator = tokens[1];
                        }
                        break;
                    case "set_separator":
                        break;
                    case "empty_field":
                        empty = tokens[1];
                        break;
                    case "unset_field":
                        unset = tokens[1];
                        break;
                    case "fields":
                        List<String> fieldList = Arrays.asList(Arrays.copyOfRange(tokens, 1, tokens.length));
                        for (Field field : Field.values()) {
                            field.setIndex(fieldList.indexOf(field.getLabel()));
                        }
                        break;
                }
            } else {
                final String[] comps = tokens[Field.TIME.getIndex()].split("\\.");
                final String milliString = comps[0] + (comps[1] + "000").substring(0, 3);
                final long millis = Long.parseLong(milliString);
                final String srcPortString = tokens[Field.SRC_PRT.getIndex()];
                final int srcPort = !srcPortString.equals(empty) && !srcPortString.equals(unset) ? Integer.parseInt(srcPortString) : -1;
                final String dstPortString = tokens[Field.DST_PRT.getIndex()];
                final int dstPort = !dstPortString.equals(empty) && !dstPortString.equals(unset) ? Integer.parseInt(dstPortString) : -1;
                final String protoString = tokens[Field.PROTO.getIndex()];
                final short protoNum = protoString.equals("tcp") ? PcapFileParser.TCP_ID : protoString.equals("udp") ? PcapFileParser.UDP_ID : PcapFileParser.UNKNOWN_ID;
                final String sentString = tokens[Field.SRC_BTS.getIndex()];
                final long sentBytes = !sentString.equals(empty) && !sentString.equals(unset) ? Long.parseLong(sentString) : 0;
                final String rcvString = tokens[Field.DST_BTS.getIndex()];
                final long recvBytes = !rcvString.equals(empty) && !rcvString.equals(unset) ? Long.parseLong(rcvString) : 0;
                final String srcIP = tokens[Field.SRC_IP.getIndex()];
                final String dstIP = tokens[Field.DST_IP.getIndex()];
                if (sentBytes > 0) {
                    PMetaData sentMeta = new PMetaData(source, millis, -1, srcPort, dstPort, protoNum, new Cidr(srcIP),
                            null, new Cidr(dstIP), null, -1, sentBytes, 2048, -1, -1, -1, -1, null);
                    PacketData data = new PacketData(recvBytes > 0 ? 1 : 2, sentMeta);

                    try {
                        packetQueue.put(data);
                    } catch (InterruptedException e) {
                        // program must be closing or something
                    }
                }
                if (recvBytes > 0) {
                    PMetaData rcvMeta = new PMetaData(source, millis, -1, dstPort, srcPort, protoNum, new Cidr(dstIP),
                            null, new Cidr(srcIP), null, -1, recvBytes, 2048, -1, -1, -1, -1, null);
                    PacketData data = new PacketData(sentBytes > 0 ? 1 : 2, rcvMeta);

                    try {
                        packetQueue.put(data);
                    } catch (InterruptedException e) {
                        // program must be closing or something
                    }
                }
            }
        }
    }
}
