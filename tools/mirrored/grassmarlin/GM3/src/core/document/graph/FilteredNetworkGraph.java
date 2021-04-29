package core.document.graph;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class FilteredNetworkGraph extends LogicalGraph {
    protected final NetworkGraph<LogicalNode, LogicalEdge> root;
    private final ObservableListWrapper<LogicalNode> nodesHidden;

    public FilteredNetworkGraph(LogicalGraph root) {
        super(root.getCidrList());
        this.root = root;

        nodesHidden  = new ObservableListWrapper<>(new LinkedList<>());

        root.nodesObservable.addListener(this::Handle_NodeListChanged);
        root.edgesObservable.addListener(this::Handle_EdgeListChanged);
        nodesHidden.addListener(this::Handle_HiddenListChanged);
    }

    public void initialize() {
        addNodes(root.nodesObservable);
        updateEdges();

        refresh();
    }

    @Override
    protected void createSubnets(final List<LogicalNode> nodesNew) {
        //Do not create subnets from a FilteredNetworkGraph
    }

    private void Handle_NodeListChanged(ListChangeListener.Change<? extends LogicalNode> change) {
        LinkedList<LogicalNode> nodesRemoved = new LinkedList<>();
        LinkedList<LogicalNode> nodesAdded = new LinkedList<>();

        while(change.next()) {
            nodesRemoved.addAll(change.getRemoved());
            nodesAdded.addAll(change.getAddedSubList());
        }

        removeNodes(nodesRemoved);
        addNodes(nodesAdded);
        updateEdges();

        refresh();
    }
    private void Handle_HiddenListChanged(ListChangeListener.Change<? extends LogicalNode> change) {
        LinkedList<LogicalNode> nodesRemoved = new LinkedList<>();
        LinkedList<LogicalNode> nodesAdded = new LinkedList<>();

        while(change.next()) {
            nodesAdded.addAll(change.getRemoved());
            nodesRemoved.addAll(change.getAddedSubList());
        }

        removeNodes(nodesRemoved);
        addNodes(nodesAdded);
        updateEdges();

        refresh();
    }
    private void Handle_EdgeListChanged(ListChangeListener.Change<? extends LogicalEdge> change) {
        updateEdges();
        refresh();
    }

    protected void updateEdges() {
        //Only use edges committed in the root, but check against uncommitted nodes.
        List<LogicalEdge> edgesAll = new LinkedList<>(root.edgesObservable);
        List<LogicalEdge> edgesFiltered = edgesAll.stream().filter(edge -> nodes.containsKey(edge.getSource()) && nodes.containsKey(edge.getDestination())).collect(Collectors.toList());

        List<LogicalEdge> edgesRemoved = new LinkedList<>(edges.keySet());
        edgesRemoved.removeAll(edgesFiltered);
        List<LogicalEdge> edgesAdded = new LinkedList<>(edgesFiltered);
        edgesAdded.removeAll(edges.keySet());

        addEdges(edgesAdded);
        removeEdges(edgesRemoved);
    }

    @Override
    public void clearTopology() {
        //We don't call the superclass implementation since the superclass clears the node lists and we will propagate that from the ObservableLists in root.
        nodesHidden.clear();
    }

    @Override
    public int indexOf(LogicalNode node) {
        return this.root.indexOf(node);
    }

    @Override
    public int indexOf(LogicalEdge edge) {
        return this.root.indexOf(edge);
    }

    @Override
    public List<LogicalNode> getRawNodeList() {
        return this.root.getRawNodeList();
    }

    public ObservableList<LogicalNode> getHiddenNodes() {
        return nodesHidden;
    }
}
