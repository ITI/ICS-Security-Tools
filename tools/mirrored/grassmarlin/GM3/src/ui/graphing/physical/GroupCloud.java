package ui.graphing.physical;

import core.document.graph.PhysicalEdge;
import core.document.graph.PhysicalNode;
import core.document.serialization.xml.XmlElement;
import ui.EmbeddedIcons;
import ui.graphing.CellGroupCollapsible;
import ui.graphing.Graph;


public class GroupCloud extends CellGroupCollapsible<PhysicalNode, PhysicalEdge> {
    public GroupCloud(String name, EmbeddedIcons icon, Graph<PhysicalNode, PhysicalEdge> graph) {
        super(name, icon, graph);
    }

    @Override
    public XmlElement toXml() {
        XmlElement group = super.toXml();

        group.addAttribute("type").setValue("cloud");

        return group;
    }
}
