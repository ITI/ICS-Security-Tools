package core.document.serialization.xml;

import java.util.ArrayList;
import java.util.Collection;

public class XmlAttribute extends XmlNode {
    private final String name;
    private String value;

    XmlAttribute(String name) {
        this.name = name;
    }

    @Override
    public String getValue() {
        return value;
    }
    @Override
    public void setValue(String value) {
        this.value = value;
    }
    @Override
    public Collection<XmlAttribute> getAttributes() {
        return new ArrayList<>();
    }
    @Override
    public Collection<XmlElement> getChildren() {
        return new ArrayList<>();
    }
    @Override
    public String toString() {
        return String.format("%s='%s'", name, Escaping.XmlString(value));
    }
}
