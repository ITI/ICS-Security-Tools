package core.importmodule.inputIterators.pcap;

import core.Configuration;
import core.importmodule.ImportItem;
import core.logging.Logger;
import core.logging.Severity;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;

import java.nio.file.Path;
import java.util.Iterator;

/**
 * Class to iterate through live pcap
 */
public class PcapLiveParser extends PcapFileParser {

    private final PcapIf device;
    private Pcap handle;

    private PcapLiveParser(ImportItem source, Path dumpPath, PcapIf device, Runnable fnOnNewPacket) {
        super(source, dumpPath);
        this.device = device;

        this.fnOnNewPacket = fnOnNewPacket;
    }

    public static PcapLiveParser getInstance(ImportItem source, Path dumpPath, PcapIf device, Runnable fnOnNewPacket) {
        return new PcapLiveParser(source, dumpPath, device, fnOnNewPacket);
    }

    public Iterator<Object> start() {
        this.parseSource();

        return this.new LogicalIterator();
    }

    public void stop() {
        if (handle != null) {
            handle.breakloop();
            handle.close();
            handle = null;
        }
    }


    @Override
    protected Pcap getHandle() {
        StringBuilder errorBuffer = new StringBuilder();
        handle = null;

        int snaplen = (int) Configuration.getPreferenceLong(Configuration.Fields.PCAP_FLAG_SNAPLEN);
        int mode = (int) Configuration.getPreferenceLong(Configuration.Fields.PCAP_FLAG_MODE);
        int timeout = (int) Configuration.getPreferenceLong(Configuration.Fields.PCAP_FLAG_TIMEOUT);

        try {
            handle = Pcap.openLive(device.getName(), snaplen, mode, timeout, errorBuffer);
        } catch (UnsatisfiedLinkError err) {
            Logger.log(this, Severity.Error, "Importing PCAP is disabled: " + err);
        }

        if (handle != null) {
            String nameDumpFile = inPath.toString();
            dumper = handle.dumpOpen(nameDumpFile);
            Logger.log(this, Severity.Information, "Live PCAP is being logged to " + nameDumpFile);
        }
        return handle;
    }
}
