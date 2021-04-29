package core.document.graph;

import core.document.serialization.xml.XmlElement;
import core.document.serialization.xml.XmlText;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

import java.util.Map;

public interface INode<TSelf> extends Comparable<TSelf> {
    StringProperty titleProperty();
    StringProperty subtitleProperty();
    BooleanProperty dirtyProperty();
    Map<String, String> getGroups();

    default XmlElement toXml() {
        XmlElement xmlNode = new XmlElement("node");
        xmlNode.addAttribute("title").setValue(titleProperty().get());

        for(Map.Entry<String, String> entry : getGroups().entrySet()) {
            XmlElement xmlGroup = new XmlElement("group");
            xmlNode.getChildren().add(xmlGroup);

            xmlGroup.addAttribute("name").setValue(entry.getKey());
            xmlGroup.getChildren().add(new XmlText(entry.getValue()));
        }

        return xmlNode;
    }
}
