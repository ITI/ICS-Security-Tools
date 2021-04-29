package core.importmodule.inputIterators.cisco;

import core.document.PhysicalDevice;
import core.importmodule.ImportItem;
import core.logging.Logger;
import core.logging.Severity;
import util.Cidr;
import util.Mac;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.*;

public class CiscoFileIterator implements Iterator<PhysicalDevice> {
    @FunctionalInterface
    protected interface SubParser {
        boolean parse(List<String> content, PhysicalDevice device);
    }

    public static Iterator<PhysicalDevice> getCiscoFileIterator(ImportItem source, Path inPath) throws IllegalStateException {
        //Cisco files are simple enough that parsing will happen in next with direct IO calls; there is no need to buffer anything.
        return new CiscoFileIterator(source, inPath);
    }

    /**
     * A Map of complete commands to their associated methods. A method will be
     * selected by the longest matching string of a key to the command parsed
     * from the input file. Example: 'SwitchName1#show interf'(raw) =
     * 'interf'(parsed) = 'interfaces'(matched)
     */
    private static final Map<CiscoCommandSplitter.Command, SubParser> SUPPORT_MAP;

    static {
        HashMap<CiscoCommandSplitter.Command, SubParser> map = new HashMap<>();
        map.put(CiscoCommandSplitter.Command.RUNNING_CONFIG, CiscoFileIterator::Parse_RunningConfig);
        map.put(CiscoCommandSplitter.Command.ARP, CiscoFileIterator::Parse_Arp);
        map.put(CiscoCommandSplitter.Command.INTERFACES, CiscoFileIterator::Parse_Interface);
        map.put(CiscoCommandSplitter.Command.MAC, CiscoFileIterator::Parse_MacAddressTable);
        map.put(CiscoCommandSplitter.Command.VERSION, CiscoFileIterator::Parse_Version);
        SUPPORT_MAP = Collections.unmodifiableMap(map);
    }

    private final ImportItem source;
    private final Path inPath;
    private boolean isDone = false;

    private CiscoFileIterator(ImportItem source, Path inPath) {
        this.source = source;
        this.inPath = inPath;
    }

    // == Iterator<PhysicalDevice> Implementation =============================
    public PhysicalDevice next() {
        if(isDone) {
            return null;
        } else {
            //Regardless of the outcome, we will be done after this completes.
            isDone = true;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(inPath.toFile()))) {
            final CiscoCommandSplitter splitter = new CiscoCommandSplitter();
            splitter.split(reader);

            final String deviceName = splitter.getDeviceName();
            final Map<CiscoCommandSplitter.Command, List<String>> commands = splitter.getSplitMap();

            if(commands == null || commands.isEmpty()) {
                Logger.log(this, Severity.Warning, "Unable to identify any commands in Cisco file: " + inPath);
                return null;
            }
            if(deviceName == null || deviceName.isEmpty()) {
                Logger.log(this, Severity.Error, "Unable to parse file; device name is not set: " + inPath);
                return null;
            }

            PhysicalDevice dev = new PhysicalDevice(deviceName);
            commands.forEach((command, contents) -> {
                SubParser parseMethod = SUPPORT_MAP.get(command);

                if (parseMethod != null) {
                    try {
                        if (!parseMethod.parse(contents, dev)) {
                            Logger.log(this, Severity.Warning, String.format("Command \"%s\" in \"%s\" has no useable data.", command.getDisplayName(), inPath));
                        }
                    } catch (Exception ex) {
                        Logger.log(this, Severity.Error, "Command '" + command.getDisplayName() + "' in '" + inPath + "' failed to parse.");
                        ex.printStackTrace();
                    }
                } else {
                    Logger.log(this, Severity.Error, String.format("Unsupported command, \"%s\".", command.getDisplayName()));
                }
            });

            //This will trigger the rebuild of the graph.
            return dev;
        } catch (Exception ex) {
            Logger.log(this, Severity.Error, "An error occurred while processing '" + inPath + "'. Import Failed: " + ex.getMessage());
            return null;
        }
    }

    public boolean hasNext() {
        if(isDone && source.getStatus() != ImportItem.Status.Complete) {
            source.recordTaskCompletion();
        }
        return !isDone;
    }

    // == Parsing Methods =====================================================
    protected static boolean Parse_RunningConfig(List<String> content, PhysicalDevice device) {
        Iterator<String> iterator = content.iterator();
        while(iterator.hasNext()) {
            String line = iterator.next();

            if(line.startsWith("interface")) {
                String interfacePort = line.replaceAll("^interface\\s*", "");
                if(interfacePort.startsWith("Null")) {
                    continue;
                }

                //Separate handling for virtual/physical.
                if(interfacePort.startsWith("Vlan") || interfacePort.startsWith("VLAN")) {
                    //Add the vlan to the device
                    int idVlan = Integer.parseInt(interfacePort.replaceAll("\\D+", ""));
                    device.addVlan(idVlan);
                    while(iterator.hasNext()) {
                        line = iterator.next();
                        if(line.startsWith("!")) {
                            break;
                        } else {
                            //TODO: The 3.1 code ignores all the VLan fields, but it appears there had been code to parse the IP/Subnet Mask
                        }
                    }
                } else if(interfacePort.matches("[A-Z][a-zA-Z]*[0-9/]+")) {
                    // Physical
                    PhysicalDevice.Port port;

                    //Check for Sub-Interfaces
                    if(interfacePort.contains(".")) {
                        String[] tokens = interfacePort.split(".", 2);
                        interfacePort = tokens[0];
                        String subInterface = firstWordOf(tokens[1]);
                        port = device.getPort(interfacePort);
                        port.getSubinterfaces().add(subInterface);
                    } else {
                        port = device.getPort(interfacePort);
                    }

                    while(iterator.hasNext()) {
                        line = iterator.next();
                        if(line.startsWith("!")) {
                            break;
                        } else {
                            //We expect there to be leading whitespace.
                            line = line.trim();

                            if(line.startsWith("ip address")) {
                                String strIp = firstWordOf(line.replaceAll("^ip address\\s+", ""));
                                port.cidrProperty().set(new Cidr(strIp));
                            } else if(line.startsWith("description")) {
                                port.descriptionProperty().set(line.replaceAll("^description\\s+", ""));
                            } else if(line.equals("shutdown")) {
                                port.enabledProperty().set(false);
                            } else if(line.startsWith("switchport")) {
                                if(line.contains("vlan")) {
                                    //The line will end with a list of integers/integer-ranges
                                    // This could either be for "trunk allowed" or "access", but either way we need the list of vlans.
                                    port.getVlans().addAll(integersFrom(lastWordOf(line)));
                                } else if(line.contains("switchport mode trunk")) {
                                    //Although not necessarily true, any interface set to mode trunk should connect to another switch.
                                    port.trunkProperty().set(true);
                                }
                            }
                        }
                    }
                }
            } else if(line.startsWith("hostname")) {
                device.nameProperty().set(line.replaceAll("^hostname\\s*", ""));
            } else if(line.startsWith("username")) {
                //TODO: Parse user info--the 3.1 code parses it but doesn't use it.  The samples lack this data.
            } else if(line.startsWith("version")) {
                device.versionNameProperty().set(line.replaceAll("^version\\s*", ""));
            } else {
                //Skip line
            }
        }
        return true;
    }

    /**
     * The Arp table contains a list of the endpoints to which each port connects.
     */
    protected static boolean Parse_Arp(List<String> content, PhysicalDevice device) {
        for(String line : content) {
            String[] cols = line.split("\\s+");

            if(cols.length != 6) {
                continue;
            }

            final String strIp = cols[1];
            final String strAge = cols[2];
            final String strMac = cols[3];
            final String strInterface = cols[5];

            final Cidr ip = new Cidr(strIp);
            final Mac mac = new Mac(bytesFromHexString(strMac));

            final PhysicalDevice.Port port = device.getPort(strInterface);
            if(strAge.equals("-")) {
                //This is the line for the port
                port.cidrProperty().set(ip);
                port.macProperty().set(mac);
            } else {
                //This is the line for a remote endpoint
                port.getEndpoints().add(new PhysicalDevice.Endpoint(null, mac));
            }
        }
        return true;
    }

    /**
     * The data will consist of a series of entries that are similar to the following:
     *
     * Virtual Interface:
     * <pre>
     Vlan30 is up, line protocol is up
     Hardware is EtherSVI, address is 501c.bfcc.bfc1 (bia 501c.bfcc.bfc1)
     Internet address is 3.3.3.1/24
     MTU 1500 bytes, BW 1000000 Kbit/sec, DLY 10 usec,
     reliability 255/255, txload 1/255, rxload 1/255
     Encapsulation ARPA, loopback not set
     Keepalive not supported
     ARP type: ARPA, ARP Timeout 04:00:00
     Last input 00:00:01, output 06:11:59, output hang never
     Last clearing of "show interface" counters never
     Input queue: 0/75/0/0 (size/max/drops/flushes); Total output drops: 0
     Queueing strategy: fifo
     Output queue: 0/40 (size/max)
     5 minute input rate 0 bits/sec, 0 packets/sec
     5 minute output rate 0 bits/sec, 0 packets/sec
     41626 packets input, 3817799 bytes, 0 no buffer
     Received 0 broadcasts (0 IP multicasts)
     0 runts, 0 giants, 0 throttles
     0 input errors, 0 CRC, 0 frame, 0 overrun, 0 ignored
     161 packets output, 12268 bytes, 0 underruns
     0 output errors, 1 interface resets
     0 unknown protocol drops
     0 output buffer failures, 0 output buffers swapped out
     * </pre>
     *
     * Physical Interface:
     * <pre>
     GigabitEthernet1/0/1 is up, line protocol is up (connected)
     Hardware is Gigabit Ethernet, address is 501c.bfcc.bf81 (bia 501c.bfcc.bf81)
     MTU 1500 bytes, BW 1000000 Kbit/sec, DLY 10 usec,
     reliability 255/255, txload 1/255, rxload 1/255
     Encapsulation ARPA, loopback not set
     Keepalive set (10 sec)
     Full-duplex, 1000Mb/s, media type is 10/100/1000BaseTX
     input flow-control is off, output flow-control is unsupported
     ARP type: ARPA, ARP Timeout 04:00:00
     Last input never, output 00:00:00, output hang never
     Last clearing of "show interface" counters never
     Input queue: 0/75/0/0 (size/max/drops/flushes); Total output drops: 0
     Queueing strategy: fifo
     Output queue: 0/40 (size/max)
     5 minute input rate 0 bits/sec, 0 packets/sec
     5 minute output rate 1000 bits/sec, 1 packets/sec
     41832 packets input, 4262955 bytes, 0 no buffer
     Received 40611 broadcasts (26 multicasts)
     0 runts, 0 giants, 0 throttles
     0 input errors, 0 CRC, 0 frame, 0 overrun, 0 ignored
     0 watchdog, 26 multicast, 0 pause input
     0 input packets with dribble condition detected
     650942 packets output, 49193085 bytes, 0 underruns
     0 output errors, 0 collisions, 1 interface resets
     0 unknown protocol drops
     0 babbles, 0 late collision, 0 deferred
     0 lost carrier, 0 no carrier, 0 pause output
     0 output buffer failures, 0 output buffers swapped out
     * </pre>
     */
    protected static boolean Parse_Interface(List<String> content, PhysicalDevice device) {
        Iterator<String> iter = content.iterator();
        while(iter.hasNext()) {
            String line = iter.next();
            //Skip blank lines.
            if(line.isEmpty()) {
                continue;
            }

            if(Character.isWhitespace(line.codePointAt(0))) {
                //Skip lines that are part of a previously-processed interface.
            } else {
                String strInterface = firstWordOf(line);
                if(line.startsWith("Vlan") || line.startsWith("VLAN")) {
                    // Virtual
                    //If the line ends with "down" then we won't have a CIDR
                    line = line.trim();
                    Mac mac;
                    Cidr cidr;
                    if(line.endsWith("down")) {
                        mac = new Mac(firstWordOf(iter.next().replaceAll("^.*address.is.", "")));
                        cidr = null;
                    } else {
                        mac = new Mac(firstWordOf(iter.next().replaceAll("^.*address.is.", "")));
                        String cidrText;
                        do {
                            cidrText = firstWordOf(iter.next().replaceAll("^.*address.is.", ""));
                        }
                        while(cidrText.equals(""));
                        cidr = new Cidr(cidrText);
                    }
                    int idVlan = Integer.parseInt(strInterface.replaceAll("\\D+", ""));
                    PhysicalDevice.VLan vlan = device.getVlan(idVlan);
                    vlan.cidrProperty().set(cidr);
                    vlan.macProperty().set(mac);
                } else if (line.matches("^\\S+\\sis\\s(up|down|administratively down).*$")) { // {Interface} is (up|down|administratively down)
                    // Physical
                    boolean isDown = line.contains("administratively down");
                    boolean isConnected = line.trim().endsWith("(connected)");
                    String addressLine = iter.next();
                    while (addressLine != null && !addressLine.contains("address is")) {
                        addressLine = iter.next();
                    }
                    if (addressLine != null) {
                        Mac mac = new Mac(firstWordOf(addressLine.replaceAll("^.*address.is.", "")));
                        //getPort will create the port if necessary and set the MAC of the port if it has not already been set.
                        PhysicalDevice.Port port = device.getPort(mac, strInterface);
                        port.enabledProperty().set(!isDown);
                        port.connectedProperty().set(isConnected);
                    }
                }
            }
        }
        return true;
    }

    protected static boolean Parse_MacAddressTable(List<String> content, PhysicalDevice device) {
        /**
         * Mac Address Table (MAT) parser
         *
         * Parses a MAT with four columns as shown below,
         * <pre>
         * 12.2 and later
         * Vlan Mac Address    Type     Ports
         * ---- -----------    -------- -----
         * All  xxxx.xxxx.xxxx STATIC   CPU
         * </pre>
         *
         * OR
         *
         * <pre>
         * pre 12.2
         * Destination Address   Address Type    VLAN    Destination Port
         * -------------------   ------------    ----    ----------------
         * xxxx.xxxx.xxxx        Dynamic            1    FastEthernet0/1
         * </pre>
         *
         */
        boolean foundHeader = false;
        int idxVlan = -1;
        int idxMac = -1;
        int idxInterface = -1;
        int cntColumns = -1;

        for(String line : content) {
            if(line.isEmpty() || line.startsWith("-")) {
                continue;
            }
            if(!foundHeader) {
                if(line.startsWith("Vlan")) {
                    // 12.2 and newer
                    idxVlan = 0;
                    idxMac = 1;
                    idxInterface = 3;

                    cntColumns = 4;
                    foundHeader = true;
                } else if(line.startsWith("Destination Address")) {
                    // Pre 12.2
                    idxVlan = 2;
                    idxMac = 0;
                    idxInterface = 3;

                    cntColumns = 4;
                    foundHeader = true;
                }
            } else {
                String[] row = line.trim().split("\\s+");

                if(row.length != cntColumns) {
                    continue;
                }

                //CPU rows should map to broadcast and internal MACs; we don't need to track these.
                if(row[idxInterface].equals("CPU")) {
                    continue;
                }

                PhysicalDevice.Port port = device.getPort(row[idxInterface]);
                Integer vlan = null;
                if(!row[idxVlan].equals("All")) {
                    vlan = Integer.parseInt(row[idxVlan]);
                    port.getVlans().add(vlan);
                }
                port.getEndpoints().add(new PhysicalDevice.Endpoint(vlan, new Mac(row[idxMac])));
            }
        }
        return foundHeader;
    }

    protected static boolean Parse_Version(List<String> content, PhysicalDevice device) {
        PhysicalDevice.VersionData result = new PhysicalDevice.VersionData();

        for(String line : content) {
            if(line.startsWith("IOS")) {                //V12
                result.setVersion(line.split(",")[1]);
            } else if(line.startsWith("Cisco IOS")) {   //V12.2
                result.setVersion(line.split(",")[2]);
            } else if(line.startsWith("System image file is")) {
                result.setSoftware(line.replace("\"", ""));
            } else if(line.startsWith("Model number")) {
                result.setModel(lastWordOf(line));
            } else if(line.startsWith("System serial number") || line.startsWith("Motherboard serial number")) {
                result.setSerial(lastWordOf(line));
            }
        }

        device.setVersion(result);
        return true;
    }

    // == Parsing utility functions ===========================================
    private static Byte[] bytesFromHexString(String hex) {
        //Strip all non-hex digits
        hex = hex.replaceAll("[^0-9A-Fa-f]", "");

        //Prepend 0 if odd length, since we will be parsing 2 characters at a time.
        if(hex.length() % 2 == 1) {
            hex = "0" + hex;
        }
        Byte[] result = new Byte[hex.length() / 2];

        for(int idx = 0; idx < result.length; idx++) {
            result[idx] = (byte)Integer.parseInt(hex.substring(idx * 2, idx * 2 + 2), 16);
        }
        return result;
    }
    private static String lastWordOf(String line) {
        for(int idx = line.length() - 1; idx >= 0; idx--) {
            if(Character.isWhitespace(line.codePointAt(idx))) {
                return line.substring(idx + 1);
            }
        }
        //Didn't find any whitespace; return the line
        return line;
    }
    private static String firstWordOf(String line) {
        for(int idx = 0; idx < line.length(); idx++) {
            if(Character.isWhitespace(line.codePointAt(idx))) {
                return line.substring(0, idx);
            }
        }
        return line;
    }
    private static List<Integer> integersFrom(String text) {
        LinkedList<Integer> result = new LinkedList<>();
        for(String token : text.split(",")) {
            if(token.contains("-")) {
                String[] tokens = token.split("-");
                int low = Integer.parseInt(tokens[0]);
                int high = Integer.parseInt(tokens[1]);

                for(int val = low; val <= high; val++) {
                    result.add(val);
                }
            } else {
                result.add(Integer.parseInt(token));
            }
        }

        return result;
    }
}
