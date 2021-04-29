package iadgov.offlinepcap;

import javafx.scene.control.MenuItem;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This plugin acts as a replacement for the built-in offline pcap support.
 * This is alpha-level functionality; it is still undergoing active development and testing.
 *
 * The built-in pcap parsing relies on the JNetPcap library for offline pcap.  This parses pcap files directly.
 * At this time, this plugin does not support pcapng files, although such support is anticipated.  Rather than
 * performing the conversion using editcap, it would be able to parse the pcapng format natively.
 *
 * This functionality was developed to fix the issue with frame numbering.
 * It removes the ability to apply a filter to offline pcap files.
 */
public class Plugin implements util.Plugin, util.Plugin.ImportProcessorsV1 {
    /**
     * Constructor required as part of the util.Plugin interface to GrassMarlin.
     */
    public Plugin() {
        // No content
    }

    /**
     * Part of the util.Plugin interface.
     * @return The pretty-print version of the Plugin's name.
     */
    @Override
    public String getName() {
        return "PCap Import";
    }

    /**
     * Part of the util.Plugin interface.
     * @return A MenuItem which will be added to the Tools->Plugin menu.  If null is returned, a disabled item bearing the name of the plugin will be provided automatically.
     */
    @Override
    public MenuItem getMenuItem() {
        return null;
    }

    /**
     * Part of the util.Plugin.ImportProcessors interface.
     * @return A collection of ImportProcessors to be added to GrassMarlin.  An ImportProcessor is a tuple containing a title, a class, and a list of file extensions.
     */
    @Override
    public Collection<ImportProcessorV1> getImportProcessors() {
        ArrayList<ImportProcessorV1> result = new ArrayList<>();

        result.add(new ImportProcessorV1("Pcap", PCAPImport.class, ".pcap"));
        result.add(new ImportProcessorV1("PcapNg", PcapNgImport.class, ".pcapng"));

        return result;
    }
}
