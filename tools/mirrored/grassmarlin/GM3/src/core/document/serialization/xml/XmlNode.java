package core.document.serialization.xml;

import java.util.Collection;

/**
 * XPath can return either an element or an attribute, so we need a common representation of the valid operations on either.
 */
public abstract class XmlNode {

    public abstract String getValue();
    public abstract void setValue(String value);
    public abstract Collection<XmlAttribute> getAttributes();
    public abstract Collection<XmlElement> getChildren();
}
