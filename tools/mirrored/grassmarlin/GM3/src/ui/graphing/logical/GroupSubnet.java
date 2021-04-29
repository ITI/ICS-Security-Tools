package ui.graphing.logical;

import core.document.graph.LogicalEdge;
import core.document.graph.LogicalGraph;
import core.document.graph.LogicalNode;
import ui.EmbeddedIcons;
import ui.custom.fx.ActiveMenuItem;
import ui.graphing.CellGroupCollapsible;
import ui.graphing.Graph;
import util.Cidr;

public class GroupSubnet extends CellGroupCollapsible<LogicalNode, LogicalEdge> {
    public GroupSubnet(String name, EmbeddedIcons icon, Graph<LogicalNode, LogicalEdge> graph) {
        super(name, icon, graph);

        super.getContextMenuItems().add(new ActiveMenuItem("Remove Subnet", event -> {
            ((LogicalGraph) graph.getGraph()).getCidrList().remove(new Cidr(name));
        }));
    }
}
