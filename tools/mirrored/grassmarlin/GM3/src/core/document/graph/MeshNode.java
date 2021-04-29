/*
 *  Copyright (C) 2016
 *  This file is part of GRASSMARLIN.
 */
package core.document.graph;

import core.document.serialization.xml.XmlElement;
import javafx.beans.property.*;

import java.util.HashMap;
import java.util.Map;

public class MeshNode implements INode<MeshNode> {
    public static final String GROUP_PAN = "Pan";

    private final SimpleStringProperty title;
    private final SimpleBooleanProperty dirty;

    private int idNetwork;

    /**
     *  MeshNode for the Sniffles graph; indicates an address within a network (PAN).
     *
     *  @param title The name of the node.  Generally this is some form of address.
     *  @param idNetwork The PAN ID where this node exists.
     */
    public MeshNode(String title, int idNetwork) {
        this.title = new SimpleStringProperty(title);
        this.idNetwork = idNetwork;
        this.dirty = new SimpleBooleanProperty(false);
    }

    @Override
    public StringProperty titleProperty() {
        return title;
    }
    @Override
    public StringProperty subtitleProperty() {
        return new ReadOnlyStringWrapper(getNetworkAsHex());
    }
    @Override
    public BooleanProperty dirtyProperty() {
        return dirty;
    }
    public int getNetwork() {
        return idNetwork;
    }

    public String getNetworkAsHex() {
        return Integer.toHexString(this.idNetwork);
    }

    @Override
    public Map<String, String> getGroups() {
        HashMap<String, String> groups = new HashMap<>();
        groups.put(GROUP_PAN, Integer.toString(idNetwork));
        return groups;
    }

    @Override
    public int compareTo(MeshNode rhs) {
        if(rhs == null) {
            return 1;
        }
        if(this.equals(rhs)) {
            return 0;
        }
        return this.title.get().compareTo(rhs.title.get());
    }

    @Override
    public int hashCode() {
        return this.titleProperty().get().hashCode();
    }

    @Override
    public boolean equals(Object rhs) {
        if(rhs instanceof MeshNode) {
            MeshNode other = (MeshNode)rhs;
            // A Network id of -1 means that we didn't know the pan Id of the MeshNode
            // because it was a source and the packets only have the destination pan
            return (this.title.get().equals(other.title.get())) && (this.idNetwork == other.idNetwork || this.idNetwork == -1 || other.idNetwork == -1);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return String.format("[%d]%s", idNetwork, title.get());
    }

    @Override
    public XmlElement toXml() {
        XmlElement node = new XmlElement("node");

        node.addAttribute("title").setValue(this.title.get());
        node.addAttribute("pan").setValue(getGroups().get(GROUP_PAN));

        return node;
    }
}
