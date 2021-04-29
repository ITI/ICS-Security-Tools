package util;

import core.Configuration;
import core.logging.Logger;
import core.logging.Severity;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for various activities that interact with Wireshark
 */
public abstract class Wireshark {
    public static void OpenPcapFile(String pcap, long idxFrame) {
        String pathWireshark = Configuration.getPreferenceString(Configuration.Fields.WIRESHARK_EXEC);
        if(pathWireshark == null) {
            Logger.log(Wireshark.class, Severity.Error, "Unable to open Wireshark: Path not set.");
            return;
        }
        File fileWireshark = new File(pathWireshark);
        if(!fileWireshark.exists()) {
            Logger.log(Wireshark.class, Severity.Error, "Wireshark could not be located at the specified path.");
            return;
        }

        try {
            Runtime.getRuntime().exec(new String[]{pathWireshark, "-g", Long.toString(idxFrame), "-r", pcap});
        } catch(IOException ex) {
            Logger.log(Wireshark.class, Severity.Error, "Unable to open pcap in Wireshark: " + ex.getMessage());
            return;
        }

        Logger.log(Wireshark.class, Severity.Success, "Opened [" + pcap + "] in Wireshark.");
    }

    public static String getProtocolName(int value) {
        switch(value) {
            case 1:
                return "ICMP";
            case 6:
                return "TCP";
            case 17:
                return "UDP";
            case 41:
                return "IPv6";
            default:
                return Integer.toString(value);
        }
    }
}
