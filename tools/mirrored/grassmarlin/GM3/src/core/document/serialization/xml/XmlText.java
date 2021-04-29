package core.document.serialization.xml;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A text node is a node which does not contain a tag, only text.  It inherits from element so that it can appear as a sibling of XmlElements
 */
public class XmlText extends XmlElement {
    private String text;

    public XmlText(String text) {
        super("");
        this.text = text;
    }
    public XmlText() {
        super("");
        text = "";
    }

    @Override
    public String getValue() {
        return text;
    }
    @Override
    public void setValue(String value) {
        this.text = value;
    }
    @Override
    public XmlAttribute addAttribute(String name) {
        throw new NotImplementedException();
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
        return Escaping.XmlString(text);
    }
}
