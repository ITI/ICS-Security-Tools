package core.document.serialization;

import core.document.ImportList;
import core.document.PhysicalDevice;
import core.document.PhysicalTopology;
import core.document.Session;
import core.document.graph.*;
import core.document.graph.LogicalGraph;
import core.importmodule.ImportItem;
import core.importmodule.ImportProcessors;
import core.logging.Logger;
import core.logging.Severity;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import org.apache.commons.lang3.ArrayUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;
import ui.TabController;
import ui.dialog.importmanager.ImportDialog;
import ui.graphing.*;
import ui.graphing.graphs.*;
import ui.graphing.graphs.MeshGraph;
import ui.graphing.graphs.PhysicalGraph;
import ui.graphing.logical.FactoryCurvedEdgesLogical;
import ui.graphing.logical.FactoryLogicalGroups;
import ui.graphing.physical.*;
import util.Cidr;
import util.Launcher;
import util.Mac;

import javax.xml.parsers.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Grassmarlin_3_2 {

    public static Grassmarlin_3_2 getInstance() {
        return new Grassmarlin_3_2();
    }



    protected Grassmarlin_3_2() {

    }


    public boolean loadDocumentSax(ZipFile inFile, Session session, TabController tabs) {

        ZipEntry manifestEntry = inFile.getEntry("manifest.xml");
        ZipEntry sessionEntry = inFile.getEntry("session.xml");
        ZipEntry logicalEntry = inFile.getEntry("logical.xml");
        ZipEntry physicalEntry = inFile.getEntry("physical.xml");
        ZipEntry meshEntry = inFile.getEntry("mesh.xml");

        if (manifestEntry == null || sessionEntry == null) {
            return false;
        }

        try {
            session.getPhysicalTopologyMapper().startLoading();

            Reader sessionReader = new InputStreamReader(inFile.getInputStream(sessionEntry), StandardCharsets.UTF_8);
            InputSource sessionSource = new InputSource(sessionReader);
            sessionSource.setEncoding("UTF-8");
            loadSession(sessionSource, session);

            sessionReader.close();
            sessionReader = null;
            sessionSource = null;

            Reader logicalReader = new InputStreamReader(inFile.getInputStream(logicalEntry), StandardCharsets.UTF_8);
            InputSource logicalSource = new InputSource(logicalReader);
            logicalSource.setEncoding("UTF-8");
            loadLogical(logicalSource, session);

            logicalReader.close();
            logicalReader = null;
            logicalSource = null;

            Reader physicalReader = new InputStreamReader(inFile.getInputStream(physicalEntry), StandardCharsets.UTF_8);
            InputSource physicalSource = new InputSource(physicalReader);
            physicalSource.setEncoding("UTF-8");
            loadPhysical(physicalSource, session);

            physicalReader.close();
            physicalReader = null;
            physicalSource = null;

            Reader meshReader = new InputStreamReader(inFile.getInputStream(meshEntry), StandardCharsets.UTF_8);
            InputSource meshSource = new InputSource(meshReader);
            meshSource.setEncoding("UTF-8");
            loadMesh(meshSource, session);

            meshReader.close();
            meshReader = null;
            meshSource = null;

            session.getPhysicalTopologyMapper().endLoading();

            // At this point the graphs have been restored and we now need to restore UI properties.
            //Modifying UI elements must happen in the UI thread.

            Platform.runLater(() -> {
                for (Graph graph : tabs.getGraphs()) {
                    graph.suspendLayout();
                }
                session.getLogicalGraph().refresh();
                session.getMeshGraph().refresh();
                session.getPhysicalGraph().refresh();
                Enumeration<? extends ZipEntry> entries = inFile.entries();
                while (entries.hasMoreElements()) {
                    try {
                        ZipEntry entry = entries.nextElement();
                        if (entry.getName().contains("_graph")) {
                            Reader graphReader = new InputStreamReader(inFile.getInputStream(entry), StandardCharsets.UTF_8);
                            InputSource graphSource = new InputSource(graphReader);
                            graphSource.setEncoding("UTF-8");

                            loadTab(graphSource, session, tabs);
                        }
                    } catch(IOException ex) {
                        //TODO: Error.
                    }
                }

                for (Graph graph : tabs.getGraphs()) {
                    graph.resumeLayout(false);
                    Platform.runLater(() -> graph.getVisualizationView().zoomToFit());
                }
            });

        } catch (IOException e) {
            Logger.log(this, Severity.Error, "Error loading file " + inFile.getName());
            return false;
        }
        Logger.log(this, Severity.Information, "Loaded " + inFile.getName());
        return true;
    }

    //<editor-fold defaultstate="collapsed" desc="Load Session">
    private void loadSession(InputSource source, Session session) throws IOException {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

            parser.parse(source, new SessionLoadHandler(session));
        } catch (SAXException | ParserConfigurationException e) {
            Logger.log(this, Severity.Error, "Error parsing session information");
        }
    }

    private class SessionLoadHandler extends DefaultHandler {
        private final Session session;

        private List<PhysicalDevice> devices = new ArrayList<>();

        private boolean inSession = false;
        private boolean inImports = false;
        private boolean inImport = false;
        private boolean inPendingImports = false;
        private boolean inPhysical = false;
        private boolean inDevice;
        private boolean inPort;
        private boolean inVlan;
        private boolean inEndpoint;

        private Attributes pendingImportAttributes = null;
        private Attributes importAttributes = null;
        private String importChars = null;

        private Attributes deviceAttributes;
        private PhysicalDevice currentDevice;

        private Attributes portAttributes;
        private PhysicalDevice.Port currentPort;

        private Attributes vlanAttributes;

        private Attributes endpointAttributes;

        public SessionLoadHandler(Session session) {
            this.session = session;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch (qName.toLowerCase()) {
                case "session":
                    inSession = true;
                    break;
                case "imports":
                    inImports = true;
                    break;
                case "import":
                    inImport = true;
                    if (inSession && inImports) {
                        importAttributes = new AttributesImpl(attributes);
                    }
                    break;
                case "pending_imports":
                    inPendingImports = true;
                    break;
                case "item":
                    if(inSession && inPendingImports) {
                        pendingImportAttributes = new AttributesImpl(attributes);
                    }
                    break;
                case "physical_topology":
                    inPhysical = true;
                    break;
                case "device":
                    inDevice = true;
                    if (inSession && inPhysical) {
                        deviceAttributes = new AttributesImpl(attributes);
                        currentDevice = buildDevice(deviceAttributes);
                    }
                    break;
                case "port":
                    inPort = true;
                    if (inSession && inPhysical && inDevice && currentDevice != null) {
                        portAttributes = new AttributesImpl(attributes);
                        currentPort = buildPort(portAttributes);
                    }
                    break;
                case "vlan":
                    inVlan = true;
                    vlanAttributes = new AttributesImpl(attributes);
                    break;
                case "endpoint":
                    inEndpoint = true;
                    endpointAttributes = new AttributesImpl(attributes);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {

        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch (qName.toLowerCase()) {
                case "session":
                    inSession = false;
                    break;
                case "imports":
                    inImports = false;
                    break;
                case "import":
                    if (inSession && inImports && inImport && importAttributes != null) {
                        addImport(importAttributes, session.getImports());

                        importAttributes = null;
                    }
                    inImport = false;
                    break;
                case "pending_imports":
                    inPendingImports = false;
                    break;
                case "item":
                    if(inSession && inPendingImports && pendingImportAttributes != null) {
                        addPendingImport(pendingImportAttributes, session.getPendingImports());
                        pendingImportAttributes = null;
                    }
                    break;
                case "physical_topology":
                    if (inPhysical && devices.size() > 0) {
                        session.getPhysicalTopologyMapper().getDevices().addAll(devices);
                    }
                    devices.clear();
                    inPhysical = false;
                    break;
                case "device":
                    if (inPhysical && inDevice && currentDevice != null) {
                        devices.add(currentDevice);
                    }
                    inDevice = false;
                    break;
                case "port":
                    if (inPhysical && inDevice && currentDevice != null && currentPort != null) {
                        currentDevice.getPorts().add(currentPort);
                    }
                    currentPort = null;
                    inPort = false;
                    break;
                case "vlan":
                    if (inPhysical && inDevice && inPort && inVlan && vlanAttributes != null && currentPort != null) {
                        addVlan(vlanAttributes, currentPort);
                    }
                    vlanAttributes = null;
                    inVlan = false;
                    break;
                case "endpoint":
                    if (inPhysical && inDevice && inPort && inEndpoint && endpointAttributes != null && currentPort != null) {
                        addEndpoint(endpointAttributes, currentPort);
                    }
                    endpointAttributes = null;
                    inEndpoint = false;
                    break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void addImport(Attributes attributes, ImportList listImports) {
        //TODO: If/when serialization of fingerprint lists is supported, deserialization of fingerprint list goes here
        try {
            ImportItem item = ImportProcessors.newItem((Class<? extends ImportItem>)Launcher.loaderFor(attributes.getValue("plugin")).loadClass(attributes.getValue("type")), Paths.get(attributes.getValue("src")), null);
            item.recordTaskCompletion();
            listImports.add(item);
        } catch(ClassNotFoundException ex) {
            Logger.log(this, Severity.Error, "Unable to process import; plugin may be missing: " + ex.getMessage());
        }
    }

    protected void addPendingImport(Attributes attributes, List<ImportDialog.PreliminaryImportItem> listPendingItems) {
        final ImportDialog.PreliminaryImportItem item = new ImportDialog.PreliminaryImportItem(new File(attributes.getValue("path")));
        final ImportProcessors.ProcessorWrapper wrapper = ImportProcessors.processorForName(attributes.getValue("type"));
        if(wrapper == null) {
            Logger.log(this, Severity.Error, "Unable to process import; plugin may be missing (" + attributes.getValue("type") + ")");
        } else {
            item.setType(wrapper);
            listPendingItems.add(item);
        }
    }

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Phsyical Devices">
    /**
     * The device XML resembles:
     * <device name=string version=string>
     *     <version ...? />
     *     <port name=string mac=string isTrunk=boolean isConnected=boolean isEnabled=boolean>
     *         <vlan id=integer />...
     *         <endpoint vlan=integer mac=string />...
     *     </port>...
     * </device>
     * @param deviceAttributes
     * @return
     */
    protected PhysicalDevice buildDevice(Attributes deviceAttributes) {
        PhysicalDevice result = new PhysicalDevice(deviceAttributes.getValue("name"));
        result.versionNameProperty().set(deviceAttributes.getValue("version"));

        return result;
    }

    protected PhysicalDevice.Port buildPort(Attributes portAttributes) {
        PhysicalDevice.Port port = new PhysicalDevice.Port(portAttributes.getValue("name"));
        port.macProperty().setValue(new Mac(portAttributes.getValue("mac")));
        port.trunkProperty().setValue(Boolean.parseBoolean(portAttributes.getValue("isTrunk")));
        port.connectedProperty().setValue(Boolean.parseBoolean(portAttributes.getValue("isConnected")));
        port.enabledProperty().setValue(Boolean.parseBoolean(portAttributes.getValue("isEnabled")));

        return port;
    }

    protected void addVlan(Attributes vlanAttributes, PhysicalDevice.Port port) {
        int vlan = Integer.parseInt(vlanAttributes.getValue("id"));
        port.getVlans().add(vlan);
    }

    protected void addEndpoint(Attributes endpointAttributes, PhysicalDevice.Port port) {
        int vlan = Integer.parseInt(endpointAttributes.getValue("vlan"));
        Mac mac = new Mac(endpointAttributes.getValue("mac"));
        PhysicalDevice.Endpoint endpoint = new PhysicalDevice.Endpoint(vlan, mac);

        port.getEndpoints().add(endpoint);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Load Logical">
    private void loadLogical(InputSource source, Session session) throws IOException {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

            parser.parse(source, new LogicalLoadHandler(session));
        } catch (SAXException | ParserConfigurationException e) {
            Logger.log(this, Severity.Error, "Error parsing session information");
        }
    }

    private class LogicalLoadHandler extends DefaultHandler {
        private final Session session;

        boolean inGraph;
        boolean inNodes;
        boolean inNode;
        boolean inGroup;
        boolean inValue;
        boolean inEdges;
        boolean inEdge;
        boolean inDetails;
        boolean inSource;
        boolean inFrame;
        boolean inCidrs;
        boolean inCidr;

        private Attributes nodeAttributes;
        private String nodeChars;
        private LogicalNode currentNode;

        private Attributes groupAttributes;
        private String groupChars;

        private Attributes valueAttributes;
        private String valueChars;

        private Attributes edgeAttributes;
        private String edgeChars;
        private LogicalEdge currentEdge;

        private Attributes detailsAttributes;
        private String detailsChars;

        private Attributes sourceAttributes;
        private String sourceChars;

        private Attributes frameAttributes;
        private String frameChars;

        private Attributes cidrAttributes;
        private String cidrChars;


        public LogicalLoadHandler(Session session) {
            this.session = session;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch (qName.toLowerCase()) {
                case "graph":
                    inGraph = true;
                    break;
                case "nodes":
                    inNodes = true;
                    break;
                case "node":
                    inNode = true;
                    nodeAttributes = new AttributesImpl(attributes);
                    currentNode = buildLogicalNode(nodeAttributes, session.getLogicalGraph().getCidrList());
                    break;
                case "group":
                    inGroup = true;
                    groupAttributes = new AttributesImpl(attributes);
                    break;
                case "v":
                    inValue = true;
                    valueAttributes = new AttributesImpl(attributes);
                case "edges":
                    inEdges = true;
                    break;
                case "edge":
                    inEdge = true;
                    edgeAttributes = new AttributesImpl(attributes);
                    currentEdge = buildLogicalEdge(edgeAttributes, session.getLogicalGraph());
                    break;
                case "details":
                    inDetails = true;
                    detailsAttributes = new AttributesImpl(attributes);
                    break;
                case "source":
                    inSource = true;
                    sourceAttributes = new AttributesImpl(attributes);
                    break;
                case "frame":
                    inFrame = true;
                    frameAttributes = new AttributesImpl(attributes);
                    break;
                case "cidrs":
                    session.getLogicalGraph().getCidrList().clear();
                    inCidrs = true;
                    break;
                case "cidr":
                    inCidr = true;
                    cidrAttributes = new AttributesImpl(attributes);
                    break;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (inValue) {
                valueChars = new String(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch (qName.toLowerCase()) {
                case "graph":
                    inGraph = false;
                    break;
                case "nodes":
                    inNodes = false;
                    break;
                case "node":
                    if (inGraph && inNodes && inNode) {
                        session.getLogicalGraph().addNode(currentNode);
                        currentNode = null;
                    }
                    inNode = false;
                    nodeAttributes = null;
                    break;
                case "group":
                    if (inGraph && inNodes && inNode && inGroup) {
                        groupAttributes = null;
                        groupChars = null;
                    }
                    inGroup = false;
                    break;
                case "v":
                    if (inGraph && inNodes && inNode && inGroup && inValue && currentNode != null && groupAttributes != null && valueAttributes != null && valueChars != null) {
                        addAnnotation(currentNode, groupAttributes, valueAttributes, valueChars);
                        valueAttributes = null;
                        valueChars = null;
                    }
                    inValue = false;
                    break;
                case "edges":
                    inEdges = false;
                    break;
                case "edge":
                    if (inEdges && inEdge && currentEdge != null) {
                        session.getLogicalGraph().addEdge(currentEdge);
                        currentEdge = null;
                    }
                    inEdge = false;
                    break;
                case "details":
                    inDetails = false;
                    break;
                case "source":
                    inSource = false;
                    break;
                case "frame":
                    if (inEdges && inEdge && inDetails && inSource && inFrame && edgeAttributes != null && detailsAttributes != null && sourceAttributes != null && frameAttributes != null) {
                        addFrame(currentEdge, detailsAttributes, sourceAttributes, frameAttributes, session.getImports());
                    }
                    inFrame = false;
                    break;
                case "cidrs":
                    inCidrs = false;
                    break;
                case "cidr":
                    addCidr(cidrAttributes, session.getLogicalGraph().getCidrList());
                    inCidr = false;
                    break;
            }
        }
    }
    protected LogicalNode buildLogicalNode(Attributes attributes, ObservableList<Cidr> cidrs) {
        final Cidr ip = new Cidr(attributes.getValue("title"));
        final byte[] mac;
        if(attributes.getValue("mac") != null) {
            mac = ArrayUtils.toPrimitive(LogicalNode.parseMac(attributes.getValue("mac")));
        } else {
            mac = null;
        }

        final LogicalNode result = new LogicalNode(ip, mac, cidrs);
        //Country and Network are optional, but if they are not present (evaluate to null) then they still need to explicitly be set to null (because they are handled by lazy evaluation, they have to be set or they will be recalculated on-demand).
        result.countryProperty().set(attributes.getValue("country"));
        String network = attributes.getValue("network");
        if(network == null) {
            result.networkProperty().set(null);
        } else {
            result.networkProperty().set(new Cidr(network));
        }

        return result;
    }

    protected void addAnnotation(LogicalNode node, Attributes groupAttributes, Attributes valueAttributes, String valueText) {
        String fingerprint = groupAttributes.getValue("fingerprint");
        String key = groupAttributes.getValue("key");
        int confidence = Integer.parseInt(valueAttributes.getValue("confidence"));

        node.addAnnotation(fingerprint, key, new ComputedProperty(valueText, confidence));
    }

    protected LogicalEdge buildLogicalEdge(Attributes edgeAttributes, LogicalGraph graph) {
        List<LogicalNode> logicalNodes = graph.getRawNodeList();

        LogicalNode source = logicalNodes.get(Integer.parseInt(edgeAttributes.getValue("from")));
        LogicalNode dest = logicalNodes.get(Integer.parseInt(edgeAttributes.getValue("to")));

        return new LogicalEdge(source, dest);
    }

    protected void addFrame(LogicalEdge edge, Attributes details, Attributes source, Attributes frame, ImportList importItems) {
        ImportItem item = importItems.get(Integer.parseInt(source.getValue("ref")));

        int srcPort = Integer.parseInt(frame.getValue("srcPort"));
        int dstPort = Integer.parseInt(frame.getValue("dstPort"));
        int proto = Integer.parseInt(frame.getValue("protocol"));
        int fNum = Integer.parseInt(frame.getValue("frame"));
        int bytes = Integer.parseInt(frame.getValue("bytes"));
        long time = Long.parseLong(frame.getValue("time"));

        switch (details.getValue("direction")) {
            case "destination":
                edge.getDetailsToDestination().AddPacket(item, srcPort, dstPort, bytes, fNum, proto, time);
                break;
            case "source":
                edge.getDetailsToSource().AddPacket(item, srcPort, dstPort, bytes, fNum, proto, time);
                break;
        }
    }


    protected void addCidr(Attributes cidrAttributes, List<Cidr> cidrs) {
        cidrs.add(new Cidr(cidrAttributes.getValue("t")));
    }

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Load Physical">

    private void loadPhysical(InputSource source, Session session) throws IOException{
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

            parser.parse(source, new PhysicalLoadHandler(session));
        } catch (SAXException | ParserConfigurationException e) {
            Logger.log(this, Severity.Error, "Error parsing session information");
        }
    }

    private class PhysicalLoadHandler extends DefaultHandler {
        private Session session;

        private HashMap<String, PhysicalDevice> devices;

        private boolean inGraph;

        private boolean inNodes;

        private boolean inNode;
        private Attributes nodeAttributes;
        private PhysicalNode currentNode;
        private HashMap<String, String> groups;

        private boolean inGroup;
        private Attributes groupAttributes;
        private String groupChars;

        private boolean inEdges;

        private boolean inEdge;
        private Attributes edgeAttributes;


        public PhysicalLoadHandler(Session session) {
            this.session = session;

            this.devices = new HashMap<>();

            session.getPhysicalTopologyMapper().getDevices().forEach(device -> devices.put(device.nameProperty().get(), device));
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch (qName.toLowerCase()) {
                case "graph":
                    inGraph = true;
                    break;
                case "nodes":
                    inNodes = true;
                    break;
                case "node":
                    inNode = true;
                    if (inGraph && inNodes && inNode) {
                        nodeAttributes = new AttributesImpl(attributes);
                    }
                    break;
                case "group":
                    inGroup = true;
                    groupAttributes = new AttributesImpl(attributes);
                    if (groupAttributes != null && groups == null) {
                        groups = new HashMap<>();
                    }
                    break;
                case "edges":
                    inEdges = true;
                    break;
                case "edge":
                    inEdge = true;
                    edgeAttributes = new AttributesImpl(attributes);
                    break;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (inGraph && inNodes && inNode && inGroup) {
                groupChars = new String(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch (qName.toLowerCase()) {
                case "graph":
                    inGraph = false;
                    break;
                case "nodes":
                    inNodes = false;
                    break;
                case "node":
                    if (inGraph && inNodes && inNode) {
                        if (nodeAttributes != null) {
                            currentNode = buildPhysicalNode(nodeAttributes, devices, groups);
                        }
                        session.getPhysicalGraph().addNode(currentNode);
                    }
                    currentNode = null;
                    nodeAttributes = null;
                    groups = null;
                    inNode = false;
                    break;
                case "group":
                    if (inGraph && inNodes && inNode && inGroup && groupAttributes != null) {
                        groups.put(groupAttributes.getValue("name"), groupChars);
                    }
                    groupAttributes = null;
                    groupChars = null;
                    inGroup = false;
                    break;
                case "edges":
                    inEdges = false;
                    break;
                case "edge":
                    if (inGraph && inEdges && inEdge && edgeAttributes != null) {
                        int from = Integer.parseInt(edgeAttributes.getValue("from"));
                        int to = Integer.parseInt(edgeAttributes.getValue("to"));

                        PhysicalNode fromNode = session.getPhysicalGraph().getRawNodeList().get(from);
                        PhysicalNode toNode = session.getPhysicalGraph().getRawNodeList().get(to);

                        PhysicalEdge edge = new PhysicalEdge(fromNode, toNode);

                        session.getPhysicalGraph().addEdge(edge);
                    }
            }
        }
    }

    private PhysicalNode buildPhysicalNode(Attributes nodeAttributes, HashMap<String, PhysicalDevice> devices, Map<String, String> groups) {
        PhysicalNode node = null;

        switch(nodeAttributes.getValue("type")) {
            case "nic":
                String[] deviceTokens = nodeAttributes.getValue("title").split(" ");
                if (deviceTokens.length == 2) {
                    String macString = deviceTokens[1];
                    node = new PhysicalNic(new Mac(macString));
                    ((PhysicalNic) node).vendorProperty().setValue(nodeAttributes.getValue("vendor"));
                    ((PhysicalNic) node).deviceProperty().setValue(nodeAttributes.getValue("device"));
                    String vlans = groups.get(PhysicalNode.FIELD_VLAN);
                    node.getVLans().addAll(Arrays.stream(vlans.split(", ")).map(vlan -> Integer.parseInt(vlan)).collect(Collectors.toList()));
                }
                break;
            case "port":
                PhysicalDevice device = devices.get(nodeAttributes.getValue("owner"));
                if (device != null) {
                    String name = nodeAttributes.getValue("title");
                    Mac mac = new Mac(nodeAttributes.getValue("mac"));
                    PhysicalDevice.Port port = device.getPort(mac, name);
                    port.macProperty().setValue(new Mac(nodeAttributes.getValue("mac")));
                    port.connectedProperty().setValue(Boolean.parseBoolean(nodeAttributes.getValue("connected")));
                    port.enabledProperty().setValue(Boolean.parseBoolean(nodeAttributes.getValue("enabled")));
                    port.trunkProperty().setValue(Boolean.parseBoolean(nodeAttributes.getValue("trunk")));

                    node = new PhysicalPort(device, port);
                    ((PhysicalPort) node).indexProperty().setValue(Integer.parseInt(nodeAttributes.getValue("index")));
                    ((PhysicalPort) node).unknownConnectionProperty().setValue(Boolean.parseBoolean(nodeAttributes.getValue("unknownConnections")));
                    ((PhysicalPort) node).groupProperty().setValue(nodeAttributes.getValue("group"));
                }
                break;
            case "cloud":
                int index = Integer.parseInt(nodeAttributes.getValue("index"));
                String subtype = nodeAttributes.getValue("subtype");

                node = new PhysicalCloud(index, subtype);
                break;
        }

        return node;
    }

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Load Mesh">
    private void loadMesh(InputSource source, Session session) throws IOException {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

            parser.parse(source, new MeshLoadHandler(session));
        } catch (SAXException | ParserConfigurationException e) {
            Logger.log(this, Severity.Error, "Error parsing session information");
        }
    }

    private class MeshLoadHandler extends DefaultHandler {
        private final Session session;

        boolean inGraph;
        boolean inNodes;
        boolean inNode;
        boolean inEdges;
        boolean inEdge;

        Attributes nodeAttributes;
        MeshNode currentNode;

        Attributes edgeAttributes;
        MeshEdge currentEdge;

        public MeshLoadHandler(Session session) {
            this.session = session;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch (qName.toLowerCase()) {
                case "graph":
                    inGraph = true;
                    break;
                case "nodes":
                    inNodes = true;
                    break;
                case "node":
                    inNode = true;
                    nodeAttributes = new AttributesImpl(attributes);
                    if (inGraph && inNodes && nodeAttributes != null) {
                        currentNode = buildMeshNode(nodeAttributes);
                    }
                    break;
                case "edges":
                    inEdges = true;
                    break;
                case "edge":
                    edgeAttributes = new AttributesImpl(attributes);
                    if (inGraph && inEdges && edgeAttributes != null) {
                        currentEdge = buildMeshEdge(edgeAttributes, session.getMeshGraph().getRawNodeList());
                    }
                    inEdge = true;

            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch (qName.toLowerCase()) {
                case "graph":
                    inGraph = false;
                    break;
                case "nodes":
                    inNodes = false;
                    break;
                case "node":
                    if (inGraph && inNodes && inNode && currentNode != null) {
                        session.getMeshGraph().addNode(currentNode);
                    }
                    currentNode = null;
                    nodeAttributes = null;
                    inNode = false;
                    break;
                case "edges":
                    inEdges = false;
                    break;
                case "edge":
                    if (inGraph && inEdges && inEdge && currentEdge != null) {
                        session.getMeshGraph().addEdge(currentEdge);
                    }
                    currentEdge = null;
                    edgeAttributes = null;
                    inEdge = false;
                    break;
            }
        }
    }

    private MeshNode buildMeshNode(Attributes nodeAttributes) {
        MeshNode node = new MeshNode(nodeAttributes.getValue("title"), Integer.parseInt(nodeAttributes.getValue("pan")));

        return node;
    }

    private MeshEdge buildMeshEdge(Attributes edgeAttributes, List<MeshNode> nodes) {
        MeshNode source = nodes.get(Integer.parseInt(edgeAttributes.getValue("from")));
        MeshNode dest = nodes.get(Integer.parseInt(edgeAttributes.getValue("to")));
        MeshEdge edge = new MeshEdge(source, dest);

        return edge;
    }

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Tabs">
    protected void loadTab(InputSource source, Session session, TabController tabs) throws  IOException {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

            parser.parse(source, new TabLoadHandler(session, tabs));
        } catch (ParserConfigurationException | SAXException e) {
            Logger.log(this, Severity.Error, "Error parsing while loading tab info");
        }
    }

    protected class TabLoadHandler extends DefaultHandler {

        boolean inLogicalGraph;
        boolean inMeshGraph;
        boolean inPhysicalGraph;
        boolean inLogicalWatchGraph;
        boolean inLogicalFilterGraph;
        boolean inCell;
        boolean inEdge;
        boolean inFactory;
        boolean inGroupFactory;
        boolean inGroupBy;
        boolean inGroup;
        boolean inPortGroup;
        boolean inHiddenNodes;
        boolean inNode;

        private Graph currentVisualization;
        private CellGroup<PhysicalNode, PhysicalEdge> currentGroup;

        private Session session;
        private TabController tabs;

        private Attributes logicalGraphAttributes;
        private Attributes meshGraphAttributes;
        private Attributes physicalGraphAttributes;
        private Attributes logicalWatchGraphAttributes;
        private Attributes logicalFilterGraphAttributes;

        private Attributes cellAttributes;

        private Attributes edgeAttributes;

        private Attributes factoryAttributes;

        private Attributes groupByAttributes;

        private Attributes groupAttributes;

        private Attributes portGroupAttributes;

        private Attributes nodeAttributes;

        public TabLoadHandler(Session session, TabController tabs) {
            this.session = session;
            this.tabs = tabs;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            switch (qName.toLowerCase()) {
                case "logical_graph":
                    logicalGraphAttributes = new AttributesImpl(attributes);
                    currentVisualization = tabs.getGraphs().stream().filter(graph -> graph.titleProperty().get().equals("Logical Graph")).findFirst().get();
                    initVisualization(logicalGraphAttributes, currentVisualization);
                    inLogicalGraph = true;
                    break;
                case "mesh_graph":
                    meshGraphAttributes = new AttributesImpl(attributes);
                    currentVisualization = tabs.getGraphs().stream().filter(graph -> graph.titleProperty().get().equals("Sniffles")).findFirst().get();
                    initVisualization(meshGraphAttributes, currentVisualization);
                    inMeshGraph = true;
                    break;
                case "physical_graph":
                    physicalGraphAttributes = new AttributesImpl(attributes);
                    currentVisualization = tabs.getGraphs().stream().filter(graph -> graph.titleProperty().get().equals("Physical Graph")).findFirst().get();
                    initVisualization(physicalGraphAttributes, currentVisualization);
                    inPhysicalGraph = true;
                    break;
                case "logical_watch":
                    logicalWatchGraphAttributes = new AttributesImpl(attributes);
                    LogicalGraph baseGraph = session.getLogicalGraph();
                    String[] nodeDegreeArray = logicalWatchGraphAttributes.getValue("title").split(":");
                    LogicalNode root = baseGraph.getRawNodeList().get(Integer.parseInt(nodeDegreeArray[0]));
                    Integer degree = Integer.parseInt(nodeDegreeArray[1]);
                    currentVisualization = new LogicalWatchGraph(baseGraph, root, degree);

                    inLogicalWatchGraph = true;
                    break;
                case "logical_filter_graph":
                    logicalFilterGraphAttributes = new AttributesImpl(attributes);
                    Graph rootGraph = tabs.getGraphs().stream().filter(graph -> graph.titleProperty().get().equals("Logical Graph")).findFirst().get();
                    currentVisualization = new LogicalFilterGraph((ui.graphing.graphs.LogicalGraph)rootGraph);
                    inLogicalFilterGraph = true;
                    break;
                case "cell":
                    cellAttributes = new AttributesImpl(attributes);
                    inCell = true;
                    break;
                case "edge":
                    edgeAttributes = new AttributesImpl(attributes);
                    inEdge = true;
                    break;
                case "factory":
                    factoryAttributes = new AttributesImpl(attributes);
                    inFactory = true;
                    if (factoryAttributes.getValue("type").equals("group")) {
                        inGroupFactory = true;
                    }
                    break;
                case "groupby":
                    groupByAttributes = new AttributesImpl(attributes);
                    inGroupBy = true;
                    break;
                case "group":
                    groupAttributes = new AttributesImpl(attributes);
                    if (inPhysicalGraph && inGroupFactory && inGroupBy && groupByAttributes != null && groupAttributes != null) {
                        currentGroup = restorePhysicalGroupFactory(groupByAttributes, groupAttributes, ((PhysicalGraph) currentVisualization));
                    }
                    inGroup = true;
                    break;
                case "portGroup":
                    portGroupAttributes = new AttributesImpl(attributes);
                    inPortGroup = true;
                    break;
                case "hidden_nodes":
                    inHiddenNodes = true;
                    break;
                case "node":
                    nodeAttributes = new AttributesImpl(attributes);
                    inNode = true;
                    break;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {

        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch (qName.toLowerCase()) {
                case "logical_graph":
                    currentVisualization = null;
                    logicalGraphAttributes = null;
                    inLogicalGraph = false;
                    break;
                case "mesh_graph":
                    currentVisualization = null;
                    meshGraphAttributes = null;
                    inMeshGraph = false;
                    break;
                case "physical_graph":
                    currentVisualization = null;
                    physicalGraphAttributes = null;
                    inPhysicalGraph = false;
                    break;
                case "logical_watch":
                    if (inLogicalWatchGraph && currentVisualization != null && logicalWatchGraphAttributes != null) {
                        tabs.AddContent(currentVisualization);
                    }
                    currentVisualization = null;
                    logicalWatchGraphAttributes = null;
                    inLogicalWatchGraph = false;
                    break;
                case "logical_filter_graph":
                    if (inLogicalFilterGraph && currentVisualization != null && logicalFilterGraphAttributes != null) {
                        tabs.AddContent(currentVisualization);
                    }
                    currentVisualization = null;
                    logicalFilterGraphAttributes = null;
                    inLogicalFilterGraph = false;
                    break;
                case "cell":
                    if (inLogicalGraph && inCell && logicalGraphAttributes != null && cellAttributes != null) {
                        layoutGraphNode(cellAttributes, session.getLogicalGraph().getRawNodeList(), currentVisualization);
                    } else if (inMeshGraph && inCell && meshGraphAttributes != null && cellAttributes != null) {
                        layoutGraphNode(cellAttributes, session.getMeshGraph().getRawNodeList(), currentVisualization);
                    } else if (inPhysicalGraph && inCell && physicalGraphAttributes != null && cellAttributes != null) {
                        layoutGraphNode(cellAttributes, session.getPhysicalGraph().getRawNodeList(), currentVisualization);
                    } else if (inLogicalWatchGraph && inCell && logicalWatchGraphAttributes != null && cellAttributes != null) {
                        layoutGraphNode(cellAttributes, session.getLogicalGraph().getRawNodeList(), currentVisualization);
                    } else if (inLogicalFilterGraph && inCell && logicalFilterGraphAttributes != null && cellAttributes != null) {
                        NetworkGraph<?, ?> graph = currentVisualization.getGraph();
                        layoutGraphNode(cellAttributes, graph.getRawNodeList(), currentVisualization);
                    }
                    cellAttributes = null;
                    inCell = false;
                    break;
                case "edge":
                    if (inPhysicalGraph && inEdge && physicalGraphAttributes != null && edgeAttributes != null) {
                        restorePhysicalEdgeState(edgeAttributes, session.getPhysicalGraph().getRawEdgeList(), ((PhysicalGraph) currentVisualization));
                    }
                case "factory":
                    if (inFactory && factoryAttributes != null) {
                        if (inLogicalGraph) {
                            switch (factoryAttributes.getValue("type").toLowerCase()) {
                                case "edge":
                                    restoreLogicalEdgeFactory(factoryAttributes, ((ui.graphing.graphs.LogicalGraph) currentVisualization));
                                    break;
                                case "group":
                                    inGroupFactory = false;
                                    break;
                            }
                        } else if (inMeshGraph) {
                            switch (factoryAttributes.getValue("type")) {
                                case "edge":
                                    restoreMeshEdgeFactory(factoryAttributes, (MeshGraph) currentVisualization);
                                    break;
                                case "group":
                                    inGroupFactory = false;
                                    break;
                            }
                        } else if (inPhysicalGraph) {
                            switch (factoryAttributes.getValue("type")) {
                                case "edge":
                                    restorePhysicalEdgeFactory(factoryAttributes, (PhysicalGraph) currentVisualization);
                                    break;
                                case "group":
                                    inGroupFactory = false;
                            }
                        } else if (inLogicalWatchGraph) {
                            switch (factoryAttributes.getValue("type")) {
                                case "edge":
                                    restoreLogicalEdgeFactory(factoryAttributes, ((LogicalWatchGraph) currentVisualization));
                                    break;
                                case "group":
                                    inGroupFactory = false;
                                    break;
                            }
                        } else if (inLogicalFilterGraph) {
                            switch (factoryAttributes.getValue("type")) {
                                case "edge":
                                    restoreLogicalEdgeFactory(factoryAttributes, (LogicalFilterGraph) currentVisualization);
                                    break;
                                case "group":
                                    inGroupFactory =false;
                                    break;
                            }
                        }
                    }
                    factoryAttributes = null;
                    inFactory = false;
                    break;
                case "groupby":
                    groupByAttributes = null;
                    inGroupBy = false;
                    break;
                case "group":
                    if (inGroupFactory && inGroupBy && inGroup && groupByAttributes != null && groupAttributes != null) {
                        if (inLogicalGraph) {
                            restoreLogicalGroupFactory(groupByAttributes, groupAttributes, ((ui.graphing.graphs.LogicalGraph) currentVisualization));
                        } else if (inMeshGraph) {
                            restoreMeshGroupFactory(groupByAttributes, groupAttributes, (MeshGraph)currentVisualization);
                        } else if (inLogicalWatchGraph) {
                            restoreLogicalGroupFactory(groupByAttributes, groupAttributes, ((LogicalWatchGraph) currentVisualization));
                        } else if (inLogicalFilterGraph) {
                            restoreLogicalGroupFactory(groupByAttributes, groupAttributes, (LogicalFilterGraph) currentVisualization);
                        }
                    }
                    currentGroup = null;
                    groupAttributes = null;
                    inGroup = false;
                    break;
                case "portGroup":
                    if (inGroupFactory && inGroupBy && inGroup && inPortGroup && portGroupAttributes != null && currentGroup != null) {
                        if (currentGroup instanceof GroupSwitch) {
                            addPortGroup(portGroupAttributes, ((GroupSwitch) currentGroup));
                        }
                    }
                    break;
                case "hidden_nodes":
                    inHiddenNodes = false;
                    break;
                case "node":
                    if (inHiddenNodes && inNode && nodeAttributes != null) {
                        LogicalNode hiddenNode = ((LogicalNode) currentVisualization.getGraph().getRawNodeList().get(Integer.parseInt(nodeAttributes.getValue("ref"))));
                        ((LogicalFilterGraph) currentVisualization).setNodeVisibility(hiddenNode, false);
                    }
                    nodeAttributes = null;
                    inNode = false;
                    break;
            }
        }

    }

    protected void initVisualization(Attributes graphAttributes, Graph visualization) {
        visualization.activeGroupProperty().set(graphAttributes.getValue("group"));
        visualization.setZoomAfterLayout(Boolean.parseBoolean(graphAttributes.getValue("zoomAfterLayout")));
    }

    protected void layoutGraphNode(Attributes cellAttributes, List<? extends INode> nodeList, Graph visualization) {

        int ref = Integer.parseInt(cellAttributes.getValue("ref"));
        boolean layout = Boolean.parseBoolean(cellAttributes.getValue("autolayout"));
        double x = Double.parseDouble(cellAttributes.getValue("x"));
        double y = Double.parseDouble(cellAttributes.getValue("y"));

        Cell<?> cell = visualization.cellFor(nodeList.get(ref));
        if (!(cell instanceof CellNic || cell instanceof CellPort)) {
            if (!(cell.layoutXProperty().isBound() || cell.layoutYProperty().isBound())) {
                cell.setLayoutX(x);
                cell.setLayoutY(y);
            }
        }
        cell.autoLayoutProperty().setValue(layout);
    }

    protected void restorePhysicalEdgeState(Attributes edgeAttributes, List<PhysicalEdge> edgeList, PhysicalGraph visualization) {
        int ref = Integer.parseInt(edgeAttributes.getValue("ref"));

        Edge edge = visualization.edgeFor(edgeList.get(ref));

        if (edge instanceof EdgePhysicalCurved) {
            ((EdgePhysicalCurved) edge).useDefaultColor(Boolean.parseBoolean(edgeAttributes.getValue("usedefaultcolor")));
            ((EdgePhysicalCurved) edge).colorProperty().setValue(Color.web(edgeAttributes.getValue("color")));
        }
    }

    protected void restoreLogicalEdgeFactory(Attributes attributes, ui.graphing.graphs.LogicalGraph visualization) {
        FactoryCurvedEdgesLogical factory = (FactoryCurvedEdgesLogical) visualization.getEdgeFactory();

        boolean curved = Boolean.parseBoolean(attributes.getValue("isCurved"));
        boolean details = Boolean.parseBoolean(attributes.getValue("showDetails"));
        boolean weighted = Boolean.parseBoolean(attributes.getValue("isWeighted"));

        factory.useCurvedLinesProperty().setValue(curved);
        factory.useWeightedLinesProperty().setValue(weighted);
        factory.showDetailsProperty().setValue(details);
    }

    protected void restoreMeshEdgeFactory(Attributes attributes, MeshGraph visualization) {
        FactoryCurvedEdges<MeshNode, MeshEdge> factory = (FactoryCurvedEdges) visualization.getEdgeFactory();

        boolean curved = Boolean.parseBoolean(attributes.getValue("isCurved"));
        boolean details = Boolean.parseBoolean(attributes.getValue("showDetails"));

        factory.useCurvedLinesProperty().setValue(curved);
        factory.showDetailsProperty().setValue(details);
    }

    protected void restorePhysicalEdgeFactory(Attributes attributes, PhysicalGraph visualization) {
        FactoryCustomizablePhysicalCurvedEdges factory = ((FactoryCustomizablePhysicalCurvedEdges) visualization.getEdgeFactory());

        boolean curved = Boolean.parseBoolean(attributes.getValue("isCurved"));
        boolean details = Boolean.parseBoolean(attributes.getValue("showDetails"));
        Color trunkColor = Color.web(attributes.getValue("trunkcolor"));
        Color wireColor = Color.web(attributes.getValue("wirecolor"));

        factory.useCurvedLinesProperty().setValue(curved);
        factory.showDetailsProperty().setValue(details);
        factory.defaultTrunkColorProperty().setValue(trunkColor);
        factory.defaultColorProperty().setValue(wireColor);
    }

    protected void restoreLogicalGroupFactory(Attributes groupByAttributes, Attributes groupAttributes, ui.graphing.graphs.LogicalGraph visualization) {
        FactoryLogicalGroups factory = (FactoryLogicalGroups) visualization.getGroupFactory();

        String groupBy = groupByAttributes.getValue("name");
        String groupName = groupAttributes.getValue("name");

        CellGroup<LogicalNode, LogicalEdge> group = factory.getGroup(groupBy, groupName);

        group.fillColorProperty().setValue(Color.web(groupAttributes.getValue("color")));
        if (group instanceof CellGroupCollapsible) {
            ((CellGroupCollapsible) group).collapsedProperty().setValue(Boolean.parseBoolean(groupAttributes.getValue("collapse")));
            ((CellGroupCollapsible) group).showLabelProperty().setValue(Boolean.parseBoolean(groupAttributes.getValue("showlabel")));
        }
    }

    protected void restoreMeshGroupFactory(Attributes groupByAttributes, Attributes groupAttributes, MeshGraph visualization) {
        FactoryCollapsibleGroups<MeshNode, MeshEdge> factory = ((FactoryCollapsibleGroups) visualization.getGroupFactory());

        String groupBy = groupByAttributes.getValue("name");
        String groupName = groupAttributes.getValue("name");

        CellGroup<MeshNode, MeshEdge> group = factory.getGroup(groupBy, groupName);

        group.fillColorProperty().setValue(Color.web(groupAttributes.getValue("color")));
        if (group instanceof CellGroupCollapsible) {
            ((CellGroupCollapsible) group).collapsedProperty().setValue(Boolean.parseBoolean(groupAttributes.getValue("collapse")));
            ((CellGroupCollapsible) group).showLabelProperty().setValue(Boolean.parseBoolean(groupAttributes.getValue("showlabel")));
        }
    }

    protected CellGroup<PhysicalNode, PhysicalEdge> restorePhysicalGroupFactory(Attributes groupByAttributes, Attributes groupAttributes, PhysicalGraph visualization) {
        FactoryDeviceGroups factory = ((FactoryDeviceGroups) visualization.getGroupFactory());

        String groupBy = groupByAttributes.getValue("name");
        String groupName = groupAttributes.getValue("name");
        String groupType = groupAttributes.getValue("type");

        //put identifying prefix on computer group names
        if (groupType.equals("computer")) {
            groupName = PhysicalTopology.TOKEN_WORKSTATION + groupName;
        }

        CellGroup<PhysicalNode, PhysicalEdge> group = factory.getGroup(groupBy, groupName);
        group.fillColorProperty().setValue(Color.web(groupAttributes.getValue("color")));
        if (group instanceof CellGroupCollapsible) {
            ((CellGroupCollapsible) group).collapsedProperty().setValue(Boolean.parseBoolean(groupAttributes.getValue("collapse")));
            ((CellGroupCollapsible) group).collapsedProperty().setValue(Boolean.parseBoolean(groupAttributes.getValue("showlabel")));
        }
        if (group instanceof LayoutManagedGroup) {
            ((LayoutManagedGroup) group).offsetXProperty().setValue(Double.parseDouble(groupAttributes.getValue("x")));
            ((LayoutManagedGroup) group).offsetYProperty().setValue(Double.parseDouble(groupAttributes.getValue("y")));
            ((LayoutManagedGroup) group).rectangularHullProperty().setValue(Boolean.parseBoolean(groupAttributes.getValue("rect")));
        }

        return group;
    }

    protected void addPortGroup(Attributes portGroupAttributes, GroupSwitch group) {
        String portGroupName = portGroupAttributes.getValue("name");
        GroupSwitch.EGroupLayout layout = GroupSwitch.EGroupLayout.valueOf(portGroupAttributes.getValue("layout"));
        group.setPortGroupLayout(portGroupName, layout);
    }

    //</editor-fold>

}
