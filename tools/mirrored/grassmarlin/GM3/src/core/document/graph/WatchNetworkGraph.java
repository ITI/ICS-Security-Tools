package core.document.graph;

import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;

import java.util.LinkedList;
import java.util.List;

/**
 * Network Graph that filters the contents of another NetworkGraph; used for watch functionality.
 */
public class WatchNetworkGraph<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> extends NetworkGraph<TNode, TEdge> {
    protected final TNode root;
    protected final IntegerProperty degrees;
    protected final NetworkGraph<TNode, TEdge> graphParent;

    public WatchNetworkGraph(NetworkGraph<TNode, TEdge> parent, TNode root, int degrees) {
        super();

        if(degrees < 1) {
            throw new IllegalArgumentException("Degrees must be positive.");
        }

        this.graphParent = parent;
        this.root = root;
        this.degrees = new SimpleIntegerProperty(degrees);

        //It would be great if we could just use filteredLists, but they only evaluate the predicate when the predicate
        // is changed or the list is modified (and then only against hte modified elements).
        //Since nodes are added before edges, we would have to reevaluate the node list manually in response to edge
        // invalidations, but we have to reevaluate all edges if degrees > 1 whenever an edge is added, so the
        // FilteredList model doesn't work, at least not without building a lot of dirty hacks.
        //Instead, we will add an invalidation handler on the edge graph of the parent and rebuild our node/edge lists.
        //This depends on the inability to remove nodes and edges from the parent--a situation that only happens when
        // cleared, which should close any watch tabs
        parent.edgesObservable.addListener(this::Handle_Invalidation);
        this.degrees.addListener(this::Handle_DegreesChanged);
    }

    public int indexOf(TNode node) {
        return graphParent.indexOf(node);
    }

    protected void Handle_DegreesChanged(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        if(newValue.intValue() < oldValue.intValue()) {
            this.clearTopology();
        }
        reparseTree();
    }
    protected void Handle_Invalidation(Observable source) {
        reparseTree();
    }

    public void reparseTree() {
        List<TNode> nodesVisited = new LinkedList<>();
        List<TNode> nodesCurrent = new LinkedList<>();

        //Degree 0
        nodesCurrent.add(root);
        addNode(root);

        for(int idxIteration = degrees.get(); idxIteration > 0; idxIteration--) {
            List<TNode> nextNodes = new LinkedList<>();
            for(TNode node : nodesCurrent) {
                if(nodesVisited.contains(node)) {
                    continue;
                }
                nodesVisited.add(node);

                //Add connected nodes; we'll handle duplicates next iteration.
                for(TEdge edge : graphParent.getEdgesInvolving(node)) {
                    TNode other;
                    if(edge.getSource().equals(node)) {
                        other = edge.getDestination();
                    } else {
                        other = edge.getSource();
                    }
                    nextNodes.add(other);
                    //Always add the node before the edge.
                    addNode(other);
                    addEdge(edge);
                }
            }
            nodesCurrent = nextNodes;
        }

        this.refresh();
    }

    public IntegerProperty degreesProperty() {
        return degrees;
    }
}
