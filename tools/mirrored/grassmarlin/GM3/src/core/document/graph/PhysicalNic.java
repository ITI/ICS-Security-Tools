package core.document.graph;

import core.document.PhysicalTopology;
import core.document.serialization.xml.XmlElement;
import core.knowledgebase.Manufacturer;
import core.knowledgebase.Reference;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import util.Cidr;
import util.Mac;

import java.util.Map;

/**
 * Any device where the MAC is known but device properties are not.
 */
public class PhysicalNic extends PhysicalNode {
    private final SimpleStringProperty device;
    private final SimpleStringProperty name;
    private final SimpleObjectProperty<Cidr> ipDevice;
    private final SimpleStringProperty vendor;

    private static final String FIELD_IP = "Ip";
    private static final String FIELD_VENDOR = "Vendor";

    public PhysicalNic(Mac mac) {
        super(mac);

        this.name = new SimpleStringProperty("NIC: " + mac);
        this.device = new SimpleStringProperty(name.get());
        this.ipDevice = new SimpleObjectProperty<>(null);
        this.vendor = new SimpleStringProperty(Manufacturer.forMac(mac));

        titleProperty().bind(name);
    }

    @Override
    public Map<String, String> getGroups() {
        Map<String, String> mapProperties = super.getGroups();
        mapProperties.put(GROUP_OWNER, PhysicalTopology.TOKEN_WORKSTATION + device.get());
        if(ipDevice.get() != null) {
            mapProperties.put(FIELD_IP, ipDevice.get().toString());
        }
        if(vendor.get() != null) {
            mapProperties.put(FIELD_VENDOR, vendor.get());
        }

        return mapProperties;
    }

    // == Accessors ===========================================================
    public StringProperty vendorProperty() {
        return vendor;
    }
    public StringProperty deviceProperty() {
        return device;
    }
    public StringProperty nameProperty() {
        return name;
    }
    public ObjectProperty<Cidr> ipProperty() {
        return ipDevice;
    }

    // == Serialization =======================================================
    @Override
    public XmlElement toXml() {
        XmlElement xmlNic = super.toXml();
        xmlNic.addAttribute("type").setValue("nic");
        if(vendor.get() != null) {
            xmlNic.addAttribute("vendor").setValue(vendor.get());
        }
        xmlNic.addAttribute("device").setValue(device.get());

        return xmlNic;
    }
}
