package core.document.graph;

import core.document.serialization.xml.XmlElement;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Since a PhysicalPort and a PhysicalNic can only connect to a single point, they will connect to a PhysicalCloud when
 * the intervening topology is unknown but known (or suspected) to not be a direct connection
 */
public class PhysicalCloud  extends PhysicalNode {
    public static final String FIELD_ROLE = "Role";

    private final StringProperty cloudName;
    private final String subtype;
    private final int idxCloud;

    public PhysicalCloud(int idxCloud) {
        this(idxCloud, null);
    }
    public PhysicalCloud(int idxCloud, String subtype) {
        super(null);

        //We have to set the title to a non-null since we set a null MAC.
        this.cloudName = new SimpleStringProperty("Cloud " + idxCloud);
        this.subtype = subtype;
        this.idxCloud = idxCloud;

        this.titleProperty().bind(cloudName.concat(" " + (subtype == null ? "" : " (" + subtype + ")")));
    }

    public StringProperty cloudNameProperty() {
        return cloudName;
    }
    public String getSubtype() {
        return subtype;
    }

    @Override
    public Map<String, String> getGroups() {
        HashMap<String, String> mapProperties = new HashMap<>();
        mapProperties.put(GROUP_OWNER, cloudName.get());
        mapProperties.put(FIELD_ROLE, subtype);

        return mapProperties;
    }

    @Override
    public XmlElement toXml() {
        XmlElement cloud = super.toXml();

        cloud.addAttribute("type").setValue("cloud");
        cloud.addAttribute("subtype").setValue(this.subtype);
        cloud.addAttribute("index").setValue(Integer.toString(this.idxCloud));

        return cloud;
    }
}
