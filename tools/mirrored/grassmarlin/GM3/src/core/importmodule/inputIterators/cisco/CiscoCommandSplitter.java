package core.importmodule.inputIterators.cisco;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class CiscoCommandSplitter {
    /**
     * Enum containing command types and the matching regular expressions for finding them within the output file
     */
    public enum Command {
        VERSION(Pattern.compile("Cisco IOS Software, [a-zA-Z\\d]+? Software \\([a-zA-Z\\d\\-]+\\), Version [\\d\\.()a-zA-Z]+, .*"), "version"),
        RUNNING_CONFIG(Pattern.compile("version [0-9]+(?:\\.[0-9]+)*"), "running config"),
        INTERFACES(Pattern.compile("(?:(?:V[Ll][Aa][Nn][0-9])|(?:Fast|Gigabit|TenGigabit)Ethernet[0-9](?:/[0-9]+)*?)[\\s\\w]*,?[\\s\\w()]*"), "interfaces"),
        MAC(Pattern.compile("(?:\\s*Mac Address Table\\s*)|(?:Non-static Address Table:\\s*)"), "mac"),
        ARP(Pattern.compile("\\s*Protocol\\s*Address\\s*Age[\\s\\w()]*?\\s*Hardware Addr\\s*Type\\s*Interface\\s*"), "arp");

        private final Pattern pattern;
        private final String displayName;

        Command(Pattern pattern, String displayName) {
            this.pattern = pattern;
            this.displayName = displayName;
        }

        private boolean matches(String line) {
            return this.pattern.matcher(line).matches();
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public static Command findNextCommand(String line, Command currentCommand) {
            for (Command  c: Command.values()) {
                if (c != currentCommand && c.matches(line)) {
                    return c;
                }
            }

            return null;
        }
    }
    /**
     * number of lines required for a valid command, less than this number is
     * thrown out
     */
    static final int LINES_REQUIRED = 3;

    Map<Command, List<String>> splitFiles;
    String deviceName;

    /**
     * The invalid marker, if seen directly after a command-line indicates
     * it is valid, and should be discarded.
     */
    final static String INVALID_MARKER = "^";

    public CiscoCommandSplitter() {
        splitFiles = new HashMap<>();
    }

    public String getDeviceName() {
        return deviceName;
    }
    public Map<Command, List<String>> getSplitMap() {
        return splitFiles;
    }

    public void split(BufferedReader reader) throws IOException {
        List<String> linesInCommand = new LinkedList<>();
        String line;
        Command cmdCurrent = null;

        while ((line = reader.readLine()) != null) {
            Command cmdNew = Command.findNextCommand(line, cmdCurrent);

            if (cmdNew != null) {
                if(cmdCurrent != null && linesInCommand.size() >= LINES_REQUIRED) {
                    splitFiles.put(cmdCurrent, new ArrayList<>(linesInCommand));
                }
                cmdCurrent = cmdNew;
                linesInCommand.clear();
                linesInCommand.add(line);
            } else if(line.startsWith("hostname")) {
                deviceName = line.split(" ")[1];
            } else {
                if( line.trim().equals(INVALID_MARKER) ) {
                    linesInCommand.clear(); // reset for invalid input
                } else {
                    linesInCommand.add(line);
                }
            }

        }

        if(cmdCurrent != null && linesInCommand.size() >= LINES_REQUIRED) {
            splitFiles.put(cmdCurrent, new ArrayList<>(linesInCommand));
        }
    }
}

