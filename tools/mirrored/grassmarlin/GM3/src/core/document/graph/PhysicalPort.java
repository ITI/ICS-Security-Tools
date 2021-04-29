package core.document.graph;

import core.document.PhysicalDevice;
import core.document.serialization.xml.XmlElement;
import javafx.beans.property.*;
import util.Mac;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A port on a managed switch or router.
 */
public class PhysicalPort extends PhysicalNode {
    private final SimpleBooleanProperty connected;
    private final SimpleBooleanProperty enabled;
    private final SimpleBooleanProperty trunk;
    private final SimpleStringProperty group;
    private final SimpleIntegerProperty index;
    private final SimpleObjectProperty<PhysicalDevice> owner;
    private final SimpleBooleanProperty unknownConnection;
    private final SimpleStringProperty description;

    private final Set<Mac> connectedMacs;
    private final Set<Integer> connectedVlans;

    public static final String FIELD_CONNECTED_TO = "Connected to";
    public static final String FIELD_DESCRIPTION = "Description";
    public static final String FIELD_CONNECTED = "Connected";
    public static final String FIELD_ENABLED = "Enabled";
    public static final String FIELD_GROUP = "Port Group";
    public static final String FIELD_INDEX = "Index";
    public static final String FIELD_TRUNK = "Trunk";
    public static final String FIELD_STATE = "Connections";
    public static final String FIELD_VLANS_CONNECTED = "VLan(s), Connected";
    public static final String FIELD_VLAN_CONSISTENCY = "VLan Consistency";

    public PhysicalPort(PhysicalDevice device, PhysicalDevice.Port port) {
        super(port.macProperty().get());

        this.connectedMacs = new HashSet<>();
        this.connectedVlans = new HashSet<>();

        String namePort = port.nameProperty().get();
        int idxSplit = namePort.lastIndexOf("/");

        final String groupPort;
        final int idxPort;
        if(idxSplit == -1) {
            groupPort = namePort;
            idxPort = 1;
        } else {
            groupPort = namePort.substring(0, idxSplit);
            idxPort = Integer.parseInt(namePort.substring(idxSplit + 1));
        }

        this.getVLans().addAll(port.getVlans());

        this.owner = new SimpleObjectProperty<>(device);
        this.group = new SimpleStringProperty(groupPort);
        this.index = new SimpleIntegerProperty(idxPort);
        this.trunk = new SimpleBooleanProperty(port.trunkProperty().get());
        this.description = new SimpleStringProperty(port.descriptionProperty().get());

        this.connected = new SimpleBooleanProperty();
        this.connected.bind(port.connectedProperty());

        this.enabled = new SimpleBooleanProperty();
        this.enabled.bind(port.enabledProperty());

        this.unknownConnection = new SimpleBooleanProperty(false);

        this.titleProperty().bind(port.nameProperty());
        this.subtitleProperty().bind(this.owner.get().nameProperty().concat("|").concat(port.nameProperty()));
    }

    public Set<Mac> connectedTo() {
        return connectedMacs;
    }

    @Override
    public Map<String, String> getGroups() {
        Map<String, String> mapProperties = super.getGroups();
        mapProperties.put(FIELD_CONNECTED_TO, connectedMacs.stream().map(mac -> mac.toString()).collect(Collectors.joining("\n")));
        mapProperties.put(GROUP_OWNER, owner.get().nameProperty().get());
        mapProperties.put(FIELD_TRUNK, Boolean.toString(trunk.get()));
        mapProperties.put(FIELD_CONNECTED, Boolean.toString(connected.get()));
        mapProperties.put(FIELD_ENABLED, Boolean.toString(enabled.get()));
        mapProperties.put(FIELD_GROUP, group.get());
        mapProperties.put(FIELD_INDEX, Integer.toString(index.get()));
        if(!connectedVlans.isEmpty()) {
            mapProperties.put(FIELD_VLANS_CONNECTED, connectedVlans.stream().sorted().map(numeric -> Integer.toString(numeric)).collect(Collectors.joining(", ")));
        } else {
            mapProperties.put(FIELD_VLANS_CONNECTED, "1");
        }
        //If no VLAN is set, it is treated as 1.
        if(!mapProperties.containsKey(FIELD_VLAN)) {
            mapProperties.put(FIELD_VLAN, "1");
        }
        if(description.get() != null) {
            mapProperties.put(FIELD_DESCRIPTION, description.get());
        }

        if(connected.get() && !mapProperties.get(FIELD_VLAN).equals(mapProperties.get(FIELD_VLANS_CONNECTED))) {
            //This either sends or receives VLAN traffic for which it cannot reciprocate.
            mapProperties.put(FIELD_VLAN_CONSISTENCY, "Mismatched");
        } else {
            mapProperties.put(FIELD_VLAN_CONSISTENCY, "Matched");
        }
        mapProperties.put(FIELD_STATE, unknownConnection.get() ? "Unknown" : "Computed");

        return mapProperties;
    }

    // == Accessors

    public StringProperty groupProperty() {
        return group;
    }
    public IntegerProperty indexProperty() {
        return index;
    }
    public BooleanProperty connectedProperty() {
        return connected;
    }
    public BooleanProperty enabledProperty() {
        return enabled;
    }
    public BooleanProperty trunkProperty() {
        return trunk;
    }
    public BooleanProperty unknownConnectionProperty() {
        return unknownConnection;
    }
    public Set<Integer> getConnectedVlans() {
        return connectedVlans;
    }
    public Set<Integer> getAllVlans() {
        Set<Integer> result = new HashSet<>();
        result.addAll(connectedVlans);
        result.addAll(getVLans());
        return result;
    }

    // == Comparable Interface ================================================
    @Override
    public int compareTo(PhysicalNode other) {
        if(other == null) {
            return 1;
        }
        if(other instanceof PhysicalPort) {
            if (owner.get() != ((PhysicalPort)other).owner.get()) {
                //Comparing ports that don't belong to the same device makes no sense.
                return -1;
            }
        }

        return super.compareTo(other);
    }

    @Override
    public boolean equals(Object other) {
        if(other == null || !(other instanceof PhysicalPort)) {
            return false;
        }
        //Owner has to match
        PhysicalPort portOther = (PhysicalPort)other;
        if(owner.get() != portOther.owner.get()) {
            return false;
        }

        return super.equals(other);
    }

    // == Serialization =======================================================

    @Override
    public XmlElement toXml() {
        XmlElement xmlPort = super.toXml();

        xmlPort.addAttribute("type").setValue("port");
        xmlPort.addAttribute("mac").setValue(mac.toString());
        xmlPort.addAttribute("owner").setValue(owner.get().nameProperty().get());
        xmlPort.addAttribute("connected").setValue(Boolean.toString(connected.get()));
        xmlPort.addAttribute("enabled").setValue(Boolean.toString(enabled.get()));
        xmlPort.addAttribute("trunk").setValue(Boolean.toString(trunk.get()));
        xmlPort.addAttribute("group").setValue(group.get());
        xmlPort.addAttribute("index").setValue(Integer.toString(index.get()));
        if(description.get() != null) {
            xmlPort.addAttribute("description").setValue(description.get());
        }
        xmlPort.addAttribute("unknownConnections").setValue(Boolean.toString(unknownConnection.get()));

        for(Mac mac : connectedMacs) {
            XmlElement xmlMac = new XmlElement("mac").appendedTo(xmlPort);
            xmlMac.addAttribute("m").setValue(mac.toString());
        }
        for(Integer vlan : connectedVlans) {
            XmlElement xmlVlan = new XmlElement("vlan").appendedTo(xmlPort);
            xmlVlan.addAttribute("id").setValue(vlan.toString());
        }

        return xmlPort;
    }
}
