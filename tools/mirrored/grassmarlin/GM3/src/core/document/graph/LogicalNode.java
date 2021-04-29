package core.document.graph;

import com.sun.javafx.binding.ExpressionHelper;
import core.document.serialization.xml.XmlElement;
import core.knowledgebase.GeoIp;
import core.knowledgebase.Manufacturer;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import org.apache.commons.lang3.ArrayUtils;
import ui.custom.fx.LazyProperty;
import util.Cidr;
import util.Mac;

import java.util.*;
import java.util.stream.Collectors;

public class LogicalNode implements INode<LogicalNode>, ObservableValue<LogicalNode> {
    public static final String GROUP_SUBNET = "Network";
    public static final String GROUP_COUNTRY = "Country";
    public static final String GROUP_ROLE = "Role";
    public static final String GROUP_CATEGORY = "Category";


    private ExpressionHelper<LogicalNode> helper = null;

    private final Cidr cidr;
    private final byte[] mac;

    private final LazyProperty<String> country;

    private final HashMap<Object, HashMap<String, Set<ComputedProperty>>> fingerprintPayloadResults;
    private final HashMap<String, Set<ComputedProperty>> annotations;

    private final SimpleStringProperty title;
    private final SimpleStringProperty subtitle;
    private final SimpleBooleanProperty dirty;

    //private final ObservableList<Cidr> networks;
    private final LazyProperty<Cidr> network;

    public LogicalNode(final Cidr ip, final byte[] mac, final ObservableList<Cidr> networks) {
        //NOTE: 2 LogicalNode objects are constructed for every incoming packet.  the execution time of this constructor method is very relevant to the performance of the import process.

        this.cidr = ip;
        this.mac = mac;

        this.fingerprintPayloadResults = new HashMap<>();
        this.annotations = new HashMap<>();
        this.country = new LazyProperty<>(() -> GeoIp.getCountryName(LogicalNode.this.cidr) );
        this.network = new LazyProperty<>(() -> {
            final ArrayList<Cidr> nets = new ArrayList<>(networks);
            for (Cidr network : nets) {
                if (network.contains(cidr)) {
                    return network;
                }
            }
            return null;
        });

        title = new SimpleStringProperty(ip.toString());
        subtitle = new SimpleStringProperty(null);
        dirty = new SimpleBooleanProperty(false);
    }

    public Cidr getIp() {
        return cidr;
    }
    public String getCountry() {
        return country.get();
    }

    @Override
    public StringProperty titleProperty() {
        return title;
    }
    @Override
    public StringProperty subtitleProperty() {
        return subtitle;
    }
    public ObjectProperty<String> countryProperty() {
        return country;
    }
    public LazyProperty<Cidr> networkProperty() {
        return network;
    }
    @Override
    public BooleanProperty dirtyProperty() {
        return dirty;
    }

    @Override
    public synchronized Map<String, String> getGroups() {
        HashMap<String, String> mapNew = new LinkedHashMap<>();

        Cidr net = network.get();
        if(net != null) {
            mapNew.put(GROUP_SUBNET, net.toString());
        }
        mapNew.put(GROUP_COUNTRY, country.get());

        for(Map.Entry<String, Set<ComputedProperty>> entry: annotations.entrySet()) {
            mapNew.put(entry.getKey(), entry.getValue().stream().map(property -> property.getValue() + " (" + property.getConfidence() + ")").collect(Collectors.joining("\n")));
        }
        if(annotations.containsKey("MAC")) {
            mapNew.put("Manufacturer", annotations.get("MAC").stream()
                    .map(property -> Manufacturer.forMac(new Mac(property.getValue())))
                    .filter(manufacturer -> manufacturer != null)
                    .distinct()
                    .collect(Collectors.joining("\n")));
        }

        HashSet<String> keys = new HashSet<>();
        for(Map.Entry<Object, HashMap<String, Set<ComputedProperty>>> entry : fingerprintPayloadResults.entrySet()) {
            keys.addAll(entry.getValue().keySet());
            for(Map.Entry<String, Set<ComputedProperty>> entryInner : entry.getValue().entrySet()) {
                mapNew.put(entry.getKey().toString() + "." + entryInner.getKey(), entryInner.getValue().stream().map(property -> property.getValue() + " (" + property.getConfidence() + ")").collect(Collectors.joining("\n")));
            }
        }

        for(String key : keys) {
            List<ComputedProperty> properties = fingerprintPayloadResults.values().stream()
                    .map(map -> map.get(key))
                    .filter(value -> value != null)
                    .flatMap(set -> set.stream())
                    .collect(Collectors.toList());
            if (!properties.isEmpty()) {
                final int bestConfidence = properties.stream().mapToInt(prop -> prop.getConfidence()).min().getAsInt();
                mapNew.put(key, properties.stream()
                        .filter(prop -> prop.getConfidence() == bestConfidence)
                        .map(prop -> prop.getValue())
                        .collect(Collectors.joining("\n")));
            }
        }

        return mapNew;
    }

    public synchronized void addAnnotation(final Object fingerprint, final String field, final ComputedProperty value) {
        Set<ComputedProperty> container = getContainerForPath(fingerprint, field);
        container.add(value);
        ExpressionHelper.fireValueChangedEvent(helper);
    }
    public synchronized void addAnnotations(final Object fingerprint, final Map<String, ComputedProperty> annotations) {
        annotations.forEach((key, value) -> getContainerForPath(fingerprint, key).add(value));
        ExpressionHelper.fireValueChangedEvent(helper);
    }
    public synchronized void setAnnotation(Object fingerprint, String field, ComputedProperty value) {
        Set<ComputedProperty> container = getContainerForPath(fingerprint, field);
        container.clear();
        container.add(value);
    }
    private Set<ComputedProperty> getContainerForPath(Object fingerprint, String field) {
        HashMap<String, Set<ComputedProperty>> container;
        if(fingerprint == null) {
            container = annotations;
        } else {
            container = fingerprintPayloadResults.get(fingerprint);
            if(container == null) {
                container = new HashMap<>();
                fingerprintPayloadResults.put(fingerprint, container);
            }
        }

        Set<ComputedProperty> set = container.get(field);
        if(set == null) {
            set = new HashSet<>();
            container.put(field, set);
        }

        return set;
    }

    @Override
    public int hashCode() {
        return cidr.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if(other == null) {
            return false;
        }
        if(other instanceof LogicalNode) {
            return cidr.equals(((LogicalNode)other).cidr);
        }
        return false;
    }

    @Override
    public int compareTo(LogicalNode other) {
        return this.cidr.hashCode() - other.cidr.hashCode();
    }

    @Override
    public String toString() {
        return cidr.toString();
    }

    public static String formatMac(byte[] mac) {
        return Arrays.stream(ArrayUtils.toObject(mac)).map(Integer::toHexString).map(str -> str.length() == 1 ? "0" + str : str.substring(str.length() - 2)).collect(Collectors.joining(":"));
    }
    public static Byte[] parseMac(String mac) {
        return Arrays.stream(mac.split(":"))
                .map(token -> Integer.parseInt(token, 16))
                .map(i -> i > 127 ? i.byteValue() : (byte)(-256 + i) )
                .collect(Collectors.toList()).toArray(new Byte[6]);
    }

    @Override
    public XmlElement toXml() {
        XmlElement xmlNode = INode.super.toXml();
        if (mac != null) {
            xmlNode.addAttribute("mac").setValue(formatMac(mac));
        }

        String country = getCountry();
        if(country != null) {
            xmlNode.addAttribute("country").setValue(country);
        }
        Cidr network = networkProperty().get();
        if(network != null) {
            xmlNode.addAttribute("network").setValue(network.toString());
        }

        //Replace the default groups with higher-accuracy versions, based on the fingerprinting data.
        xmlNode.getChildren().clear();

        for(Map.Entry<Object, HashMap<String, Set<ComputedProperty>>> entryOuter : fingerprintPayloadResults.entrySet()) {
            String fingerprint = entryOuter.getKey().toString();
            for(Map.Entry<String, Set<ComputedProperty>> entry : entryOuter.getValue().entrySet()) {
                if(entry.getValue().isEmpty()) {
                    //If the value is null, don't write the key, either.
                    continue;
                }

                XmlElement xmlGroup = new XmlElement("group").appendedTo(xmlNode);
                xmlGroup.addAttribute("fingerprint").setValue(fingerprint);
                xmlGroup.addAttribute("key").setValue(entry.getKey());

                for(ComputedProperty value : entry.getValue()) {
                    new XmlElement("v").appendedTo(xmlGroup).setTextContent(value.getValue()).addAttribute("confidence").setValue(Integer.toString(value.getConfidence()));
                }
            }
        }
        for(Map.Entry<String, Set<ComputedProperty>> entry : annotations.entrySet()) {
            if(entry.getValue().isEmpty()) {
                //If the value is null, don't write the key, either.
                continue;
            }

            XmlElement xmlGroup = new XmlElement("group");
            xmlGroup.addAttribute("fingerprint").setValue("");
            xmlGroup.addAttribute("key").setValue(entry.getKey());

            for(ComputedProperty value : entry.getValue()) {
                new XmlElement("v").appendedTo(xmlGroup).setTextContent(value.getValue()).addAttribute("confidence").setValue(Integer.toString(value.getConfidence()));
            }
        }

        return xmlNode;
    }

    @Override
    public LogicalNode getValue() {
        return this;
    }

    @Override
    public void addListener(InvalidationListener listener) {
        helper = ExpressionHelper.addListener(helper, this, listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        helper = ExpressionHelper.removeListener(helper, listener);
    }

    @Override
    public void addListener(ChangeListener<? super LogicalNode> listener) {
        helper = ExpressionHelper.addListener(helper, this, listener);
    }

    @Override
    public void removeListener(ChangeListener<? super LogicalNode> listener) {
        helper = ExpressionHelper.removeListener(helper, listener);
    }
}
