package ui.graphing.graphs;

import core.document.graph.IEdge;
import core.document.graph.INode;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import ui.graphing.Graph;

/**
 * Class that watches the Nodes, Edges, and Grouping of a UI Graph for changes and responds accordingly.
 * @param <TNode>          The type of node in the NetworkGraph backing the Graph.
 * @param <TEdge>          The type of edge in the NetworkGraph backing the Graph
 * @param <TNodeEntity>    The return type of the createNode method.  Generally the createEdge method in a derived class must call createNode to ensure the UI elements for the node exist, so this permits the return type to be set as needed.
 */
public abstract class GraphWatcher<TNode extends INode<TNode>, TEdge extends IEdge<TNode>, TNodeEntity> {
    protected final Graph<TNode, TEdge> graph;

    protected GraphWatcher(Graph<TNode, TEdge> graph) {
        this.graph = graph;

        graph.activeGroupProperty().addListener(this::Handle_GroupByChange);
        graph.getNodes().addListener(this::Handle_NodeChange);
        graph.getEdges().addListener(this::Handle_EdgeChange);
    }

    // Private handlers for change events.
    private void Handle_GroupByChange(ObservableValue<? extends String> o, String oldValue, String newValue) {
        groupByChanged(oldValue, newValue);
    }
    private void Handle_NodeChange(ListChangeListener.Change<? extends Graph.NodeWrapper<TNode>> change) {
        while (change.next()) {
            for(Graph.NodeWrapper<TNode> wrapper : change.getRemoved()) {
                removeNode(wrapper);
            }
            for(Graph.NodeWrapper<TNode> wrapper : change.getAddedSubList()) {
                createNode(wrapper);
            }
        }
    }
    private void Handle_EdgeChange(ListChangeListener.Change<? extends Graph.EdgeWrapper<TNode, TEdge>> change) {
        while(change.next()) {
            for(Graph.EdgeWrapper<TNode, TEdge> wrapper : change.getRemoved()) {
                removeEdge(wrapper);
            }
            for(Graph.EdgeWrapper<TNode, TEdge> wrapper : change.getAddedSubList()) {
                createEdge(wrapper);
            }
        }
    }

    //<editor-fold defaultstate="collapsed" desc="GraphWatcher implementation.">
    protected abstract void groupByChanged(String oldValue, String newValue);

    protected abstract TNodeEntity createNode(Graph.NodeWrapper<TNode> wrapper);
    protected abstract void removeNode(Graph.NodeWrapper<TNode> wrapper);

    protected abstract void createEdge(Graph.EdgeWrapper<TNode, TEdge> wrapper);
    protected abstract void removeEdge(Graph.EdgeWrapper<TNode, TEdge> wrapper);
    //</editor-fold>
}
