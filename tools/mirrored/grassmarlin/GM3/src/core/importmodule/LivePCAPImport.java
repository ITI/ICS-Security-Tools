package core.importmodule;

import core.fingerprint3.Fingerprint;
import core.importmodule.inputIterators.pcap.PcapLiveParser;
import core.logging.Logger;
import core.logging.Severity;
import org.jnetpcap.PcapIf;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

/**
 * This is a special import item for capturing PCAP from a device instead of a file.
 * 
*/
public class LivePCAPImport extends ImportItem {
    private final PcapLiveParser parser;
    private final ImportItem source;

    public LivePCAPImport(Path dumpPath, PcapIf device, List<Fingerprint> fingerprints, Runnable fnOnNewPacket) {
        super(dumpPath, fingerprints);
        //HACK: This can fail if the .pcap processor is undefined.  Since we moved to a plugin for offline pcap, this is a risk.
        source = ImportProcessors.newItem(ImportProcessors.processorForPath(dumpPath).getProcessor(), dumpPath, fingerprints);

        source.progressProperty().bind(progressProperty());

        progress.setForceUpdate(-1.0);

        parser = PcapLiveParser.getInstance(source, dumpPath, device, fnOnNewPacket);
    }

    public ImportItem getSource() {
        return source;
    }

    @Override
    protected long getTotalUnits() {
        return 1;
    }

    @Override
    protected Iterator<?> getLogicalIterator() {
        return parser.start();
    }

    @Override
    protected Iterator<?> getPhysicalIterator() {
        return null;
    }

    @Override
    public String getDisplaySize() {
        return null;
    }

    @Override
    public void recordTaskProgress(int unitsProcessed) {
        //Do nothing.  The progress should be continuous until stopped.
        return;
    }

    public void stop() {
        recordTaskCompletion();
        parser.stop();
        Logger.log(this, Severity.Information, "Live PCAP terminated.");
    }
}