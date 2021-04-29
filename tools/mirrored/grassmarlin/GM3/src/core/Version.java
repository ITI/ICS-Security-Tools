package core;

import com.sun.javafx.collections.ObservableMapWrapper;
import javafx.beans.property.ReadOnlyMapWrapper;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Version {
    public static final String APPLICATION_VERSION = "3.2.1";
    public static final String APPLICATION_TITLE = "GrassMarlin " + APPLICATION_VERSION;
    public static final int APPLICATION_REVISION = 26;
    public static final String FILENAME_USER_GUIDE = "GRASSMARLIN_User_Guide3.2.pdf";

    public static final Map<String, List<String>> PATCH_NOTES = new ReadOnlyMapWrapper<>(new ObservableMapWrapper<>(new LinkedHashMap<String, List<String>>() {
        {
            this.put("3.2.1", Arrays.asList(
                    "While porting the Fingerprinting engine to version 3.3, a few low-risk-high-impact optimizations were found which were backported to 3.2.1; fingerprinting throughput is roughly doubled as a result.",
                    "Following the release of GrassMarlin 3.2.0 we have, with help from a wide base of users, identified and fixed several bugs.",
                    "Importing PcapNg files had some issues that have been addressed, specifically with ARP and files created with a certain Endianness.",
                    "Many bugs were fixed in Fingerprinting.",
                    "Additional Fingerprints have been added and existing Fingerprints have been updated.",
                    "If Wireshark is not auto-detected (or manually configured) properly, the application will no longer crash.",
                    "Improved support for builds that disable Live Pcap.",
                    "The packet list in the View Frames Dialog can now be exported to CSV.",
                    "The chart on the View Frames Dialog can be exported to SVG from the context menu; the chart component does not scale, it is effectively taken as a screenshot of the current display.  It is slightly better than screenshot-and-crop, though."
            ));
            this.put("3.2.0", Arrays.asList(
                    "KNOWN ISSUES:",
                    "Some context menu items are missing from the Physical Graph tree view; the missing commands are available from the Visualization context menu.",
                    "If a session is associated with a file (it was loaded or saved) and you start saving it again (to the same path), then cancel, the original file will be deleted.",
                    "Some PcapNg files do not contain timestamp information for packets.  These packets will be imported with the current timestamp; this is intentional but can be unexpected.",
                    "Occasionally, attempting to import a pcapng file as a pcap file (or vice versa) does not report an appropriate error message.",
                    "UPDATES:",
                    "The core UI has been rebuilt using the JavaFX UI library.  While the layout and function is similar to the 3.1 interface, the vast majority of the interface had to be rebuilt using JavaFX.  Many minor changes are unavoidable, but this brought increased stability.  These notes should detail the majority of the changes, but do not detail everything.",
                    "Many of the icons used throughout the application have been changed.",
                    "Instead of exporting the data from GrassMarlin as was done in 3.1 and previous builds, the session state can be saved.  The session state is a ZIP containing multiple XML files which contains all the data and the UI state information (It includes all the data generated from reading the imported files, but not hte actual file contents;  a test case of 5.4GB of PCAP yields an approximately 300MB file).  This state can be restored by loading the session.  Starting a new Session will restore many values to defaults, whereas clear Topology will simply un-import files.",
                    "The Fingerprint Editor has been integrated into the GrassMarlin application and has been merged with the functionality of the fingerprint Manager.",
                    "Dropping files onto the main application window will import them, if the file type is recognized.",
                    "A preliminary plugin model is in place; presently the only supported feature of a plugin is to create additional import formats.  An example plugin that imports host data from a CSV is available.",
                    "PcapNg files are no longer converted to Pcap files to be imported.  Most of the functionality of the PcapNg format is not yet supported, but it is feature-complete with respect to 3.1;  future builds should expand on the amount of data parsed from PcapNg files.",
                    "Fingerprints are loaded while the application is loading--there is no longer a separate task that loads them.",
                    "Graphs can now be exported to SVG.  SVG is a widely-used vector graphics format that allows the visualization of a graph to be exported, in full.",
                    "The Print command has been removed in favor of exporting a SVG and printing from an image editing/viewing application.  Supporting a robust, useful, platform-agnostic print interface has proven to be an inefficient use of development resources given the readily available external solutions.",
                    "Reports can be viewed from the relevant menu items in the View menu.",
                    "The network groupings used by the Logical Graph can be changed from the Packet Capture -> Manage Networks menu item.",
                    "By default the logical graph groups elements by Network, however right-clicking on the graph allows the nodes to be grouped by any field which has been matched by a Fingerprint or otherwise added to the node.",
                    "The logical graph now uses a force-directed model to position the groups, and then positions the elements within each group in a circle.  Auto-collapse of groups is no longer an option.",
                    "Watching a node on the Logical Graph will create a new Watch Tab.  The number of degrees of connection to track can be set through the context (right-click) menu.",
                    "The Physical Graph does a better job of identifying and positioning the ports on a switch, allowing the ports to be rendered in various styles.",
                    "The toolbar at the bottom of the visualization has been removed; instead the functionality has been integrated into a context menu on the visualization.",
                    "The context menus on the Tree View and Graph Visualizations reflect operations that can be performed on nodes, groups, edges connected to a node, or to the graph itself.  Consult the user guide for a complete list of commands.",
                    "The Zoom to Fit context menu item will zoom and center the view on the graph, whereas the Fit to Window will reposition the elements in the graph to fill the window.",
                    "The CPU no longer overheats when you hold down spacebar.",
                    "The Scroll Wheel can be used to zoom in or out on the graph.  Holding the Ctrl key while scrolling with the mouse over a group (so the group is highlighted) will expand or contract the elements in the group.",
                    "Nodes cannot be hidden on the primary Logical View; instead, create a new Filtered View from the View menu; nodes can be hidden and shown there.",
                    "GrassMarlin will no longer use the user directory to store documents, images, built-in fingerprints, and related files.",
                    "During startup GrassMarlin will run an integrity check on the GeoIp database, reporting CIDRs that resolve to a country Id that lacks a name as well as country names for which flag files cannot be located.",
                    "During startup GrassMarlin will also output all configurable values.",
                    "Version notes will now display the first time GrassMarlin is run after an update; they may also be viewed through the Help menu."
            ));
            this.put("3.1.0", Arrays.asList(
                    "Added Bro2ConnJson parsing.",
                    "Updates to Import dialog.",
                    "Fixed persistence of settings in Preferences dialog."
            ));
        }
    }));

    // final abstract isn't permitted, so final with a private constructor that won't be called is the next best solution.
    private Version() {}
}
