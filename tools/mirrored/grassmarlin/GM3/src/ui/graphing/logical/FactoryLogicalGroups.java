package ui.graphing.logical;

import core.document.graph.LogicalEdge;
import core.document.graph.LogicalNode;
import ui.EmbeddedIcons;
import ui.graphing.CellGroup;
import ui.graphing.CellGroupCollapsible;
import ui.graphing.FactoryCollapsibleGroups;
import ui.graphing.Graph;

import java.util.HashMap;

public class FactoryLogicalGroups extends FactoryCollapsibleGroups<LogicalNode, LogicalEdge> {
    public FactoryLogicalGroups(Graph<LogicalNode, LogicalEdge> graphOwner, HashMap<String, EmbeddedIcons> images) {
        super(graphOwner, images);
    }

    @Override
    protected CellGroup<LogicalNode, LogicalEdge> BuildGroup(String groupBy, String name) {
        if(groupBy.equals(LogicalNode.GROUP_SUBNET)) {
            return new GroupSubnet(name, imageGroups.get(groupBy), graph);
        } else {
            return new CellGroupCollapsible<>(name, imageGroups.get(groupBy), graph);
        }
    }
}
