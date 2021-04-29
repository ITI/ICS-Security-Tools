package core.document.serialization;

import core.document.Session;
import core.logging.Logger;
import core.logging.Severity;
import org.xml.sax.SAXException;
import ui.TabController;
import ui.graphing.Graph;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public abstract class Grassmarlin {
    protected static final DocumentBuilderFactory factory;
    protected static final DocumentBuilder builder;
    protected static XPathFactory factoryXpath;
    protected static XPath xpath;

    static {
        factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder_temp = null;
        try {
            builder_temp = factory.newDocumentBuilder();
        } catch(Exception ex) {
            //This is bad.  we may not be able to log the error properly at this time.
            //TODO: Better error handling in serialization
        }
        builder = builder_temp;

        factoryXpath= XPathFactory.newInstance();
        xpath = factoryXpath.newXPath();
    }


    // == SAVE ================================================================
    public static void SaveState(ZipOutputStream zos, Session session, TabController tabs) throws IOException {
        ZipEntry manifestEntry = new ZipEntry("manifest.xml");
        zos.putNextEntry(manifestEntry);
        zos.write("<manifest ver='3.2'>".getBytes(StandardCharsets.UTF_8));
        zos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
        zos.write("</manifest>".getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();

        // Write Session
        // - This will include the list of imports and the logical/physical/sniffles graphs.
        // - this also includes all of the packet data that has been processed and stored.
        session.toXml(zos);

        // Write Graphs
        // - The graphs are effectively the tabs
        // - The position of each node, attachment to layout, group settings, etc all need to be saved to restore the view later.
        // - Additional tabs must indicate how to recreate them--watch tab, etc.
        ZipEntry graphEntry = null;
        for(Graph<?, ?> graph : tabs.getGraphs()) {
            graphEntry = new ZipEntry(graph.getEntryName());
            zos.putNextEntry(graphEntry);
            graph.toXml(zos);
            zos.closeEntry();
        }


    }

    // == LOAD ================================================================
    public static boolean LoadState(Path source, Session doc, TabController tabs) throws IOException {
        try {
            doc.setSavePath(source);
            ZipFile inFile = new ZipFile(source.toFile());
            ZipEntry manifestEntry = inFile.getEntry("manifest.xml");
            org.w3c.dom.Document docManifest =  builder.parse(inFile.getInputStream(manifestEntry));

            XPathExpression xVersion = xpath.compile("/manifest/@ver");

            // Validate version, then hand off to version-specific loader.
            String version = (String)xVersion.evaluate(docManifest, XPathConstants.STRING);
            try {
                tabs.getGraphs().forEach(graph -> graph.suspendLayout());

                if (version.equals("3.2")) {
                    return Grassmarlin_3_2.getInstance().loadDocumentSax(inFile, doc, tabs);
                } else {
                    Logger.log(Grassmarlin.class, Severity.Error, "Unable to load version " + version + " session file.");
                    return false;
                }
            } finally {
                tabs.getGraphs().forEach(graph -> graph.resumeLayout(false));
            }
        } catch(XPathException ex) {
            Logger.log(Grassmarlin.class, Severity.Error, "Unable to parse file: " + ex.getMessage());
        } catch(SAXException ex) {
            Logger.log(Grassmarlin.class, Severity.Error, "Unable to parse file: " + ex.getMessage());
        }
        return false;
    }
}
