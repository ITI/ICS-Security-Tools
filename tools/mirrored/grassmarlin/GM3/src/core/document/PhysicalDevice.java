package core.document;

import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.javafx.collections.ObservableSetWrapper;
import core.document.serialization.xml.Escaping;
import core.logging.Logger;
import core.logging.Severity;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import util.Cidr;
import util.Mac;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipOutputStream;

/**
 * A PhysicalDevice represents either a Router or Switch.
 */
public class PhysicalDevice {
    // == Support Classes =====================================================
    public static class VersionData {
        private String version;
        private String software;
        private String model;
        private String serial;

        public VersionData() { }

        @Override
        public String toString() {
            return version + "\n" + software + "\n" + model + "\n" + serial;
        }

        public String getVersion() {
            return version;
        }
        public String getSoftware() {
            return software;
        }
        public String getModel() {
            return model;
        }
        public String getSerial() {
            return serial;
        }
        public void setVersion(String version) {
            this.version = version;
        }
        public void setSoftware(String software) {
            this.software = software;
        }
        public void setModel(String model) {
            this.model = model;
        }
        public void setSerial(String serial) {
            this.serial = serial;
        }

        public void toXml(ZipOutputStream zos) throws IOException {
            zos.write(("<version ver='" + Escaping.XmlString(version) + "' soft='" + Escaping.XmlString(software) + "' model='" + Escaping.XmlString(model) + "' serial='" + Escaping.XmlString(serial) + "' />").getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Endpoints are objects connected to a Port.  Since each port represents a single physical connection,
     * having multiple endpoints implies there is an unknown (cloud) entity between the endpoints and the Port.
     * A single Endpoint is a direct connection.
     */
    public static class Endpoint {
        private final IntegerProperty vlan;
        private final SimpleObjectProperty<Mac> mac;

        public Endpoint(Integer vlan, Mac mac) {
            if (vlan == null) {
                vlan = 1;
            }
            this.vlan = new SimpleIntegerProperty(vlan);
            this.mac = new SimpleObjectProperty<>(mac);
        }

        @Override
        public String toString() {
            return "[" + vlan.get() + ": " + mac.get() + "]";
        }

        public IntegerProperty vlanProperty() {
            return vlan;
        }
        public ObjectProperty<Mac> macProperty() {
            return mac;
        }

        public void toXml(ZipOutputStream zos) throws IOException {
            zos.write(("<endpoint vlan='" + vlan.get() + "' mac='" + mac.get() + "' />").getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * A Port is a physical Port on a router or switch.
     */
    public static class Port {
        private final SimpleObjectProperty<Mac> mac;
        private final SimpleStringProperty name;
        private final SimpleStringProperty description;
        private final SimpleObjectProperty<Cidr> cidr;
        private final SimpleBooleanProperty trunk;
        private final SimpleBooleanProperty connected;
        private final SimpleBooleanProperty enabled;

        private final ObservableSetWrapper<Integer> vlans;
        private final ObservableListWrapper<Endpoint> endpoints;
        private final ObservableListWrapper<String> subinterfaces;

        public Port(String name) {
            this.mac = new SimpleObjectProperty<>(null);
            this.name = new SimpleStringProperty(name);
            this.cidr = new SimpleObjectProperty<>(null);
            this.trunk = new SimpleBooleanProperty(false);
            this.connected = new SimpleBooleanProperty(false);
            this.enabled = new SimpleBooleanProperty(true);
            this.endpoints = new ObservableListWrapper<>(new LinkedList<>());
            this.description = new SimpleStringProperty();
            this.subinterfaces = new ObservableListWrapper<>(new LinkedList<>());
            this.vlans = new ObservableSetWrapper<>(new LinkedHashSet<>());
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder()
                    .append("[")
                    .append(name.get())
                    .append("<")
                    .append(mac.get())
                    .append(">");
            if(trunk.get()) {
                result.append(" TRUNK");
            }
            if(!vlans.isEmpty()) {
                result.append(" vlans:[");
                for(Integer vlan : vlans) {
                    result.append(vlan).append(",");
                }
                result.deleteCharAt(result.length() - 1).append("]");
            }
            if(cidr.get() != null) {
                result.append(" ip:").append(cidr.get());
            }
            if(!endpoints.isEmpty()) {
                result.append(" [");
                for (Endpoint endpoint : endpoints) {
                    result.append(endpoint).append(",");
                }
                result.deleteCharAt(result.length() - 1).append("]");
            } else {
                result.append(" []");
            }
            //TODO: Display sub-interfaces

            result.append("]");
            return result.toString();
        }

        public ObjectProperty<Mac> macProperty() {
            return mac;
        }
        public StringProperty nameProperty() {
            return name;
        }
        public ObjectProperty<Cidr> cidrProperty() {
            return cidr;
        }
        public StringProperty descriptionProperty() {
            return description;
        }
        public BooleanProperty trunkProperty() {
            return trunk;
        }
        public BooleanProperty connectedProperty() {
            return connected;
        }
        public BooleanProperty enabledProperty() {
            return enabled;
        }
        public ObservableSet<Integer> getVlans() {
            return vlans;
        }
        public ObservableList<Endpoint> getEndpoints() {
            return endpoints;
        }
        public ObservableList<String> getSubinterfaces() {
            return subinterfaces;
        }

        public void toXml(ZipOutputStream zos) throws IOException {
            zos.write(("<port name='" + Escaping.XmlString(name.get()) +
                    "' mac='" + mac.get() +
                    (description.get() != null ? "' description='" + Escaping.XmlString(description.get()) : "") +
                    (cidr.get() != null ? "' cidr='" + cidr.get().toString() : "") +
                    "' isTrunk='" + trunkProperty().getValue().toString() +
                    "' isConnected='" + connectedProperty().getValue().toString() +
                    "' isEnabled='" + enabled.getValue().toString() +
                    "'>").getBytes(StandardCharsets.UTF_8));
            for(int vlan : vlans) {
                zos.write(("<vlan id='" + vlan + "'/>").getBytes(StandardCharsets.UTF_8));
            }
            for(Endpoint endpoint : getEndpoints()) {
                endpoint.toXml(zos);
            }
            //TODO: Subinterfaces

            zos.write("</port>".getBytes(StandardCharsets.UTF_8));
        }
    }

    public static class VLan {
        private final SimpleObjectProperty<Cidr> cidr;
        private final SimpleObjectProperty<Mac> mac;

        public VLan() {
            cidr = new SimpleObjectProperty<>();
            mac = new SimpleObjectProperty<>();
        }

        public ObjectProperty<Cidr> cidrProperty() {
            return cidr;
        }
        public ObjectProperty<Mac> macProperty() {
            return mac;
        }

        @Override
        public String toString() {
            return "[" + mac.get() + " | " + cidr.get() + "]";
        }
    }

    // == PhysicalDevice ======================================================
    private final SimpleObjectProperty<VersionData> version;
    private final ObservableListWrapper<Port> ports;
    private final SimpleStringProperty name;
    private final SimpleStringProperty versionName;
    private final HashMap<Integer, VLan> vlans;

    public PhysicalDevice(String name) {
        this.version = new SimpleObjectProperty<>(null);
        this.versionName = new SimpleStringProperty(null);
        this.ports = new ObservableListWrapper<>(new ArrayList<>(32));
        this.name = new SimpleStringProperty(name);
        this.vlans = new HashMap<>();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder()
                .append("[Device: ")
                .append(name.get())
                .append(" (")
                .append(versionName.get())
                .append(")]\n")
                .append(version.get())
                .append("\n");
        for(Port port : ports) {
            result.append(port).append("\n");
        }
        for(Map.Entry<Integer, VLan> entry : vlans.entrySet()) {
            result.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        result.append("\n");

        return result.toString();
    }

    public Port getPort(String interfacePort) {
        return getPort(null, interfacePort);
    }
    public Port getPort(Mac macPort, String interfacePort) {
        final String nameInterface = interfacePort.replaceAll("[^A-Za-z]+", "");
        final String idxInterface = interfacePort.replaceAll("^[A-Za-z]+", "");

        Optional<Port> result = ports.stream().filter(port -> {
            final String namePort = port.nameProperty().get().replaceAll("[^A-Za-z]+", "");
            final String idxPort = port.nameProperty().get().replaceAll("^[A-Za-z]+", "");

            return idxInterface.equals(idxPort) && (namePort.startsWith(nameInterface) || nameInterface.startsWith(namePort));
        }).findFirst();
        if(result.isPresent()) {
            if(interfacePort.length() > result.get().nameProperty().get().length()) {
                result.get().nameProperty().set(interfacePort);
                //Rename might affect the sort order.
                ports.sort((o1, o2) -> o1.nameProperty().get().compareTo(o2.nameProperty().get()));
            }
            if(macPort != null && !macPort.equals(Mac.NULL_MAC)) {
                if (result.get().macProperty().get() == null || result.get().macProperty().get().equals(Mac.NULL_MAC)) {
                    result.get().macProperty().set(macPort);
                } else {
                    if (!result.get().macProperty().get().equals(macPort)) {
                        Logger.log(this, Severity.Warning, "MAC Address Mismatch for " + name.get() + ":" + interfacePort + " " + result.get().macProperty().get() + " != " + macPort);
                    }
                }
            }
            return result.get();
        } else {
            Port port = new Port(interfacePort);
            port.macProperty().set(macPort);
            ports.add(port);
            //Addition may affect the sort order.
            ports.sort((o1, o2) -> o1.nameProperty().get().compareTo(o2.nameProperty().get()));
            return port;
        }
    }

    public void addVlan(int id) {
        if(!vlans.containsKey(id)) {
            vlans.put(id, new VLan());
        }
    }
    public VLan getVlan(int id) {
        addVlan(id);
        return vlans.get(id);
    }

    // == Accessors ===========================================================
    public ObjectProperty<VersionData> versionProperty() {
        return version;
    }
    public void setVersion(VersionData data) {
        version.set(data);
    }
    public VersionData getVersion() {
        return version.get();
    }

    public ObservableList<Port> getPorts() {
        return ports;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty versionNameProperty() {
        return versionName;
    }

    public static String formatMac(Byte[] mac) {
        if(mac == null || mac.length != 6) {
            return "??:??:??:??:??:??";
        } else {
            return String.format("%02X:%02X:%02X:%02X:%02X:%02X", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
        }
    }

    public void toXml(ZipOutputStream zos) throws IOException {
        zos.write(("<device name='" + Escaping.XmlString(nameProperty().get()) + "' version='" + Escaping.XmlString(versionNameProperty().get()) + "'>").getBytes(StandardCharsets.UTF_8));
        if(version.get() != null) {
            version.get().toXml(zos);
        }
        for(Port port : ports) {
            port.toXml(zos);
        }

        //TODO: VLans; nothing actually uses them at present
        //private final HashMap<Integer, VLan> vlans;

        zos.write("</device>".getBytes(StandardCharsets.UTF_8));
    }

}
