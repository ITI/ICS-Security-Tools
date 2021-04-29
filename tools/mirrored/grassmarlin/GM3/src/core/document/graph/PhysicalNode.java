package core.document.graph;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import util.Mac;

import java.util.*;
import java.util.stream.Collectors;

public abstract class PhysicalNode implements INode<PhysicalNode> {
    public static final String GROUP_OWNER = "Owner";
    public static final String FIELD_NAME = "Title";
    public static final String FIELD_MAC = "MAC";
    public static final String FIELD_VLAN = "VLan(s)";

    protected Mac mac;
    private final SimpleStringProperty title;
    private final SimpleStringProperty subtitle;
    private final SimpleBooleanProperty dirty;
    private final Set<Integer> vlans;

    /**
     * Create a workstation node for the MAC address.
     * @param mac
     */
    public PhysicalNode(Mac mac) {
        this.mac = mac;

        this.title = new SimpleStringProperty();
        if (this.mac != null) {
            this.title.setValue(this.mac.toString());
        }
        this.subtitle = new SimpleStringProperty();
        this.dirty = new SimpleBooleanProperty(false);
        this.vlans = new HashSet<>();
    }

    public Set<Integer> getVLans() {
        return vlans;
    }

    // == INode Interface =====================================================
    @Override
    public Map<String, String> getGroups() {
        HashMap<String, String> result = new HashMap<>();
        result.put(FIELD_NAME, title.get());
        result.put(FIELD_MAC, mac.toString());
        if(!vlans.isEmpty()) {
            result.put(FIELD_VLAN, vlans.stream().sorted().map(numeric -> Integer.toString(numeric)).collect(Collectors.joining(", ")));
        }

        return result;
    }

    @Override
    public BooleanProperty dirtyProperty() {
        return dirty;
    }

    @Override
    public StringProperty titleProperty() {
        return title;
    }

    @Override
    public StringProperty subtitleProperty() {
        return subtitle;
    }

    // == Comparable Interface ================================================
    public int compareTo(PhysicalNode other) {
        if(other == null) {
            return 1;
        }
        return title.get().compareTo(other.title.get());
    }

    @Override
    public int hashCode() {
        return this.title.get().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if(other == null || !(other instanceof PhysicalNode)) {
            return false;
        }
        return title.get().equals(((PhysicalNode)other).title.get());
    }
}
