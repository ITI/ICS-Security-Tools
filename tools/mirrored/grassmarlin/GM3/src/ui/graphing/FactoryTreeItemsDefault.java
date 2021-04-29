package ui.graphing;

import core.document.graph.IEdge;
import core.document.graph.INode;
import ui.EmbeddedIcons;

import java.util.HashMap;

public class FactoryTreeItemsDefault<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> implements GraphTreeController.FactoryTreeItems<TNode, TEdge> {
    protected final HashMap<String, EmbeddedIcons> imageGroups;
    protected Graph<TNode, TEdge> owner;

    //Caching
    protected class EdgeEndpointPair {
        public GraphTreeItem.GraphTreeEdgeItem<TNode, TEdge> source;
        public GraphTreeItem.GraphTreeEdgeItem<TNode, TEdge> destination;
    }
    protected final HashMap<String, HashMap<String, GraphTreeItem.GraphTreeGroupItem<TNode, TEdge>>> lookupGroups;
    protected final HashMap<TNode, GraphTreeItem.GraphTreeNodeItem<TNode, TEdge>> lookupNodes;
    protected final HashMap<TEdge, EdgeEndpointPair> lookupEdges;

    public FactoryTreeItemsDefault(HashMap<String, EmbeddedIcons> images) {
        //Images for groups as mapped to groupBy strings.
        imageGroups = new HashMap<>();
        images.entrySet().forEach(entry -> imageGroups.put(entry.getKey(), entry.getValue()));

        //Caching
        lookupGroups = new HashMap<>();
        lookupNodes = new HashMap<>();
        lookupEdges = new HashMap<>();
    }

    // == FactoryTreeItems Interface ==========================================
    public void clearCache() {
        lookupGroups.clear();
        lookupNodes.clear();
        lookupEdges.clear();
    }

    public void setOwner(Graph<TNode, TEdge> owner) {
        this.owner = owner;
    }

    public void putGroupImage(String group, EmbeddedIcons image) {
        imageGroups.put(group, image);
    }

    public GraphTreeItem.GraphTreeGroupItem<TNode, TEdge> TreeNodeForGroup(String nameGroup, String groupBy) {
        HashMap<String, GraphTreeItem.GraphTreeGroupItem<TNode, TEdge>> mapGroups = lookupGroups.get(groupBy);
        if(mapGroups == null) {
            mapGroups = new HashMap<>();
            lookupGroups.put(groupBy, mapGroups);
        }
        GraphTreeItem.GraphTreeGroupItem<TNode, TEdge> node = mapGroups.get(nameGroup);
        if(node == null) {
            node = new GraphTreeItem.GraphTreeGroupItem<>(nameGroup, imageGroups.get(groupBy));
            mapGroups.put(nameGroup, node);
        }
        return node;
    }
    public GraphTreeItem.GraphTreeNodeItem<TNode, TEdge> TreeNodeForNode(TNode node) {
        GraphTreeItem.GraphTreeNodeItem<TNode, TEdge> result = lookupNodes.get(node);
        if(result == null) {
            result = new GraphTreeItem.GraphTreeNodeItem<>(node);
            lookupNodes.put(node, result);
        }
        return result;
    }
    public GraphTreeItem.GraphTreeEdgeItem<TNode, TEdge> TreeNodeForEdge(TEdge edge, TNode parent) {
        EdgeEndpointPair pair = lookupEdges.get(edge);
        if(pair == null) {
            pair = new EdgeEndpointPair();
            lookupEdges.put(edge, pair);
        }

        if(edge.getSource().equals(parent)) {
            if(pair.destination == null) {
                pair.destination = new GraphTreeItem.GraphTreeEdgeItem<>(edge.getDestination(), edge);
            }
            return pair.destination;
        } else {
            if(pair.source == null) {
                pair.source = new GraphTreeItem.GraphTreeEdgeItem<>(edge.getSource(), edge);
            }
            return pair.source;
        }
    }
}
