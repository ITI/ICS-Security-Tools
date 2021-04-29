package iadgov.csvimport;

import core.document.graph.LogicalNode;
import core.fingerprint.PMetaData;
import core.fingerprint.PacketData;
import core.importmodule.ImportItem;
import core.importmodule.LogicalProcessor;
import core.logging.Logger;
import core.logging.Severity;
import org.jnetpcap.protocol.tcpip.Tcp;
import util.Cidr;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CsvFileIterator implements Iterator<Object> {
    /**
     * A BlockingQueue is used to restrict the number of items which can be enqueued for processing.  Without
     * throttling, it is common for excessive memory use to cripple the performance of the application.
     */
    private BlockingQueue<Object> packetQueue;
    /**
     * A flag to indicate whether or not hte entire file has been processed.  Once the file has been read and the queue
     * is empty, the iterator is done.
     */
    private boolean done;
    /**
     * The path of the CSV file to import.
     */
    private final Path path;

    public CsvFileIterator(Path path) {
        packetQueue = new ArrayBlockingQueue<>(100);
        done = false;
        this.path = path;
    }

    /**
     * Creates a thread that parses the file, placing the results of parsing into packetQueue.
     * This is performed in a separate thread since parseFile is called from the UI thread.
     */
    public void parseFile() {
        new Thread(() -> {
            System.out.println("Beginning parsing of " + path);
            try(final BufferedReader reader = Files.newBufferedReader(path)) {
                //All headers.
                final List<String> headers = new ArrayList<>();
                Integer idxIp = -1;
                for(String line : reader.lines().collect(Collectors.toList())) {
                    System.out.println("> " + line);
                    final List<String> tokens = parseCsvLine(line);
                    System.out.println("+ " + tokens.stream().collect(Collectors.joining("','", "['", "']")));
                    //This works for the first line, it also allows multiple tables to be concatenated in a single file.
                    if(tokens.size() != headers.size()) {
                        System.out.println("Processing as headers.");
                        headers.clear();
                        idxIp = -1;

                        for(int idx = 0; idx < tokens.size(); idx++) {
                            System.out.println("Index: " + idx);
                            final String header = tokens.get(idx);
                            System.out.println("Token: " + header);
                            headers.add(header);
                            if(header.equalsIgnoreCase("ip")) {
                                System.out.println("The IP Address is in column#" + idx);
                                idxIp = idx;
                            }
                        }
                    } else {
                        if(idxIp == -1) {
                            // If we don't have an IP column, we can't do much.
                            System.out.println("We don't have an IP column.");
                        } else {
                            final Cidr cidr = new Cidr(tokens.get(idxIp));
                            final HashMap<String, String> properties = new HashMap<>();

                            for(int idxField = 0; idxField < headers.size(); idxField++) {
                                if(idxField == idxIp) {
                                    continue;
                                }
                                properties.put(headers.get(idxField), tokens.get(idxField));
                            }

                            try {
                                //There are three types of Object that are processed for the LogicalGraph:
                                // core.fingerprint.PacketData contains information about an edge and both endpoints.  This will also be fed through the Fingerprinting engine.
                                // core.exec.IEEE802154Data contains information related to the Mesh graph.  This is likely to change in the near future.
                                // core.importmodule.LogicalProcessor.Host identifies a host with properties to add.  This skips fingerprinting.
                                packetQueue.put(new LogicalProcessor.Host(cidr, properties, path.getFileName()));
                            } catch(InterruptedException ex) {
                                //Ignore the problem and maybe it will go away.
                                //This is, after all, just a proof-of-concept for implementing a custom import Plugin.
                            }

                        }
                    }
                }
            } catch(IOException ex) {
                Logger.log(CsvFileIterator.this, Severity.Error, "An error occurred while processing a CSV File: " + ex.getMessage());
            } finally {
                done = true;
            }
        }).start();
    }

    protected final static Pattern reCsvLine = Pattern.compile("(?<=^|,)(\"([^\"]*)\"|([^,]*))(?=,|$)");
    public static List<String> parseCsvLine(String line) {
        if(line.trim().equals("")) {
            return new LinkedList<>();
        }
        Matcher matcher = reCsvLine.matcher(line);
        List<String> tokens = new LinkedList<>();
        while(matcher.find()) {
            tokens.add(matcher.group(0));
        }

        return tokens;
    }

    // == Iterator<Object> Interface ====================

    /**
     * The iterator will be queried for data as long as hasNext returns true.  If we have processed the entire file and
     * the queue is empty, then we are done, otherwise there is more data to process.
     * @return Returns false when it is known there is no more data to process, true otherwise.
     */
    @Override
    public boolean hasNext() {
        return !(done && packetQueue.isEmpty());
    }

    /**
     * The call to poll() will return null if no data is ready.  Any nulls will be discarded, and it is expected that
     * this will happen regularly.
     * @return Null if no data is ready, otherwise the next element to process.  If there is no data remaining, it should continue to return null.
     */
    @Override
    public Object next() {
        return packetQueue.poll();
    }
}
