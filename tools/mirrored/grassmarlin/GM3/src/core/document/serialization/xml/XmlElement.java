package core.document.serialization.xml;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class XmlElement extends XmlNode {
    private final LinkedHashMap<String, XmlAttribute> attributes;
    private final LinkedList<XmlElement> children;
    private final String name;

    public XmlElement(String name) {
        this.name = name;
        this.children = new LinkedList<>();
        this.attributes = new LinkedHashMap<>();
    }

    public XmlAttribute addAttribute(String name) {
        XmlAttribute attribute = new XmlAttribute(name);
        attributes.put(name, attribute);
        return attribute;
    }

    public XmlElement appendedTo(XmlElement parent) {
        parent.getChildren().add(this);
        return this;
    }

    public XmlElement setTextContent(String content) {
        this.children.clear();
        this.children.add(new XmlText(content));
        return this;
    }

    @Override
    public String getValue() {
        throw new NotImplementedException();
    }
    public void setValue(String value) {
        throw new NotImplementedException();
    }
    @Override
    public Collection<XmlAttribute> getAttributes() {
        return attributes.values();
    }
    @Override
    public Collection<XmlElement> getChildren() {
        return children;
    }
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("<").append(name);
        for(XmlAttribute attribute : attributes.values()) {
            result.append(" ").append(attribute);
        }

        if(children.isEmpty()) {
            result.append(" />");
        } else {
            result.append(">");

            for(XmlElement child : children) {
                result.append(child);
            }

            result.append("</").append(name).append(">");
        }

        return result.toString();
    }

    public String openTag() {
        StringBuilder tag = new StringBuilder();
        tag.append("<").append(name);
        for (XmlAttribute attribute : attributes.values()) {
            tag.append(" ").append(attribute);
        }
        tag.append(">");

        return tag.toString();
    }

    public String childrenToString() {
        StringBuilder childrenString = new StringBuilder();

        for (XmlElement child : children) {
            childrenString.append(child);
            childrenString.append(System.lineSeparator());
        }

        return childrenString.toString();
    }

    public String closeTag() {
        return String.format("</%s>", name);
    }
}
