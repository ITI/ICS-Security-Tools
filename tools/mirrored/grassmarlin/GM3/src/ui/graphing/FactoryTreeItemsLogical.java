package ui.graphing;

import core.document.graph.LogicalEdge;
import core.document.graph.LogicalNode;
import javafx.scene.control.MenuItem;
import ui.EmbeddedIcons;
import ui.custom.fx.ActiveMenuItem;
import util.Cidr;

import java.util.ArrayList;
import java.util.HashMap;

public class FactoryTreeItemsLogical extends FactoryTreeItemsDefault<LogicalNode, LogicalEdge>{
    /**
     * When grouped by Cidr (subnet), we don't want to alpha-sort.
     */
    public static class GraphTreeGroupItemLogicalCidr extends GraphTreeItem.GraphTreeGroupItem<LogicalNode, LogicalEdge> {
        protected final Cidr cidrGroup;

        public GraphTreeGroupItemLogicalCidr(String name, EmbeddedIcons image) {
            super(name, image);

            if(name.equals("(Unknown)")) {
                cidrGroup = null;
            } else {
                cidrGroup = new Cidr(name);
            }

            //TODO: Menu items for managing Cidr-based groups.
        }

        @Override
        public int compareTo(GraphTreeItem<LogicalNode, LogicalEdge> other) {
            if(other instanceof GraphTreeGroupItemLogicalCidr) {
                if(cidrGroup == null && ((GraphTreeGroupItemLogicalCidr) other).cidrGroup == null) {
                    return 0;
                }
                if(cidrGroup == null) {
                    return -1;
                }
                return cidrGroup.compareTo(((GraphTreeGroupItemLogicalCidr) other).cidrGroup);
            } else {
                return super.compareTo(other);
            }
        }
    }

    public FactoryTreeItemsLogical(HashMap<String, EmbeddedIcons> images) {
        super(images);
    }

    public GraphTreeItem.GraphTreeGroupItem<LogicalNode, LogicalEdge> TreeNodeForGroup(String nameGroup, String groupBy) {
        if(nameGroup == null) {
            nameGroup = "(Unknown)";
        }
        if(LogicalNode.GROUP_SUBNET.equals(groupBy)) {
            //Copied from superclass and modified for this case.
            //When grouped by subnet, use the LogicalCidr variant group node so that the sort order can be set appropriately (also we can set menu items appropriately for working with subnets, which is normally not a thing you can do with groups)
            HashMap<String, GraphTreeItem.GraphTreeGroupItem<LogicalNode, LogicalEdge>> mapGroups = lookupGroups.get(groupBy);
            if(mapGroups == null) {
                mapGroups = new HashMap<>();
                lookupGroups.put(groupBy, mapGroups);
            }
            GraphTreeItem.GraphTreeGroupItem<LogicalNode, LogicalEdge> node = mapGroups.get(nameGroup);
            if(node == null) {
                node = new GraphTreeGroupItemLogicalCidr(nameGroup, imageGroups.get(groupBy));
                mapGroups.put(nameGroup, node);
            }
            return node;
        } else {
            return super.TreeNodeForGroup(nameGroup, groupBy);
        }
    }
    public GraphTreeItem.GraphTreeNodeItem<LogicalNode, LogicalEdge> TreeNodeForNode(LogicalNode node) {
        GraphTreeItem.GraphTreeNodeItem<LogicalNode, LogicalEdge> nodeNew = super.TreeNodeForNode(node);

        if(nodeNew.requiresInitialization()) {
            if(nodeNew.getContextMenuItems() != null) {
                nodeNew.getContextMenuItems().clear();
            }
            //This doesn't make sense in the physical view, so it is reproduced separately for Logical and Mesh Views.
            ActiveMenuItem miSelect = new ActiveMenuItem("Select All Instances of " + node.titleProperty().get(), (event) -> {
                owner.SelectNode(nodeNew.getNode());
            });
            ArrayList<MenuItem> items = new ArrayList<>(1);
            items.add(miSelect);

            nodeNew.addContextMenuItems(items);
        }
        return nodeNew;
    }
    public GraphTreeItem.GraphTreeEdgeItem<LogicalNode, LogicalEdge> TreeNodeForEdge(LogicalEdge edge, LogicalNode parent) {
        GraphTreeItem.GraphTreeEdgeItem<LogicalNode, LogicalEdge> nodeNew = super.TreeNodeForEdge(edge, parent);

        if(nodeNew.requiresInitialization()) {
            if(nodeNew.getContextMenuItems() != null) {
                nodeNew.getContextMenuItems().clear();
            }
            if (edge.getSource().equals(parent)) {
                nodeNew.valueProperty().bind(
                        nodeNew.getNode().titleProperty()
                                .concat(":  ")
                                .concat(nodeNew.getEdge().getDetailsToSource().bytesProperty().asString().concat(" bytes received / "))
                                .concat(nodeNew.getEdge().getDetailsToDestination().bytesProperty().asString().concat(" bytes sent"))
                );
            } else {
                nodeNew.valueProperty().bind(
                        nodeNew.getNode().titleProperty()
                                .concat(":  ")
                                .concat(nodeNew.getEdge().getDetailsToDestination().bytesProperty().asString().concat(" bytes received / "))
                                .concat(nodeNew.getEdge().getDetailsToSource().bytesProperty().asString().concat(" bytes sent"))
                );
            }

            //This doesn't make sense in the physical view, so it is reproduced separately for Logical and Mesh Views.
            ActiveMenuItem miSelect = new ActiveMenuItem("Select All Instances of " + nodeNew.getNode().titleProperty().get(), (event) -> {
                owner.SelectNode(nodeNew.getNode());
            });
            ArrayList<MenuItem> items = new ArrayList<>(1);
            items.add(miSelect);

            nodeNew.addContextMenuItems(items);
        }

        return nodeNew;
    }
}
