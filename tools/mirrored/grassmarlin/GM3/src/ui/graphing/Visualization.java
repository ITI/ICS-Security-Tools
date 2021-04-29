package ui.graphing;

import com.sun.javafx.collections.ObservableMapWrapper;
import core.document.graph.IEdge;
import core.document.graph.INode;
import core.document.serialization.xml.XmlElement;
import javafx.application.Platform;
import javafx.collections.ObservableMap;
import javafx.scene.control.MenuItem;
import ui.graphing.graphs.GraphWatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Visualization manages the creation and manipulation and mapping of Cell and Edge objects, which are the graphical
 * representations of TNode and TEdge objects from a NetworkGraph, as depicted in a ZoomableScrollPane.
 * @param <TNode> The type of node to which Cells will correspond.
 * @param <TEdge> the type of Edge to which Edges will correspond.
 */
public class Visualization<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> extends GraphWatcher<TNode, TEdge, Cell<TNode>> {
    // In the trivial cases, these will have no menu items, so the ability to provide trivial implementations as lambdas
    // or method references improves clarity.  For more complicated uses, you need to define an inner class or a full
    // factory class.
    @FunctionalInterface
    public interface INodeCellFactory<TNode extends INode<TNode>> {
        Cell<TNode> uiFor(TNode node);
        default List<MenuItem> getFactoryMenuItems() {
            return null;
        }
        default XmlElement toXml() { return null; }
    }
    @FunctionalInterface
    public interface IEdgeCellFactory<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> {
        Edge<TNode> uiFor(TEdge edge, Cell<TNode> source, Cell<TNode> destination);
        default List<MenuItem> getFactoryMenuItems() {
            return null;
        }
        default XmlElement toXml() { return null; }
    }
    @FunctionalInterface
    public interface IGroupCellFactory<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> {
        CellGroup<TNode, TEdge> getGroup(String groupBy, String group);
        default List<MenuItem> getFactoryMenuItems() { return null; }
        default void clearCache() { }
        default XmlElement toXml() { return null; }
    }

    // == Mapping of source items to renderable entities ======================
    protected final ObservableMap<TNode, Cell<TNode>> lookupCells;
    protected final ObservableMap<String, CellGroup<TNode, TEdge>> lookupGroups;
    protected final ObservableMap<TEdge, Edge> lookupEdges;

    // == Factories to produce new Cells/Edges for TNodes/TEdges ==============
    private INodeCellFactory<TNode> factoryNodes = this::DefaultNodeFactory;
    private IEdgeCellFactory<TNode, TEdge> factoryEdges = this::DefaultEdgeFactory;
    private IGroupCellFactory<TNode, TEdge> factoryGroups = this::DefaultGroupFactory;

    //The canvas contains the layers that will compose the visualization.
    protected Canvas canvas;

    private boolean enableLayout = true;
    public boolean suspendLayout() {
        boolean result = enableLayout;
        enableLayout = false;
        return result;
    }
    public void resumeLayout(boolean runNow) {
        enableLayout = true;
        if(runNow && layout != null) {
            layout.layoutAll(this);
        }
    }

    protected Layout<TNode, TEdge> layout = null;
    protected String groupCurrent;

    //public Visualization(StringProperty currentGroup) {
    public Visualization(Graph<TNode, TEdge> graph) {
        super(graph);

        lookupCells = new ObservableMapWrapper<>(new HashMap<>());
        lookupGroups = new ObservableMapWrapper<>(new HashMap<>());
        lookupEdges = new ObservableMapWrapper<>(new HashMap<>());

        canvas = new Canvas();

        this.groupCurrent = graph.activeGroupProperty().get();
    }

    public void clear() {
        if(!Platform.isFxApplicationThread()) {
            Platform.runLater(this::clear);
            return;
        }

        lookupCells.clear();
        lookupGroups.clear();
        lookupEdges.clear();
        //TODO: Other factories should support clearCache, even if default implementation does nothing.
        factoryGroups.clearCache();

        canvas.clear();
    }

    //<editor-fold defaultstate="collapsed" desc="GraphWatcher implementation.">
    @Override
    protected void groupByChanged(String oldValue, String newValue) {
        //This is used when creating new Nodes to assign group membership
        groupCurrent = newValue;

        //Remove all existing groups
        canvas.clearGroups();
        lookupGroups.values().forEach(group -> group.clear());  // Unbind the event listeners
        lookupGroups.clear(); // clear the Group Name -> Group mapping

        //Add new groups for each distinct group
        List<String> groups = lookupCells.keySet().stream()
                .map(node -> node.getGroups().get(newValue))
                .filter(group -> group != null)
                .distinct()
                .collect(Collectors.toList());
        for (String group : groups) {
            CellGroup<TNode, TEdge> groupObject = factoryGroups.getGroup(newValue, group);
            lookupGroups.put(group, groupObject);
            groupObject.getMembers().addAll(lookupCells.entrySet().stream()
                    .filter(entry -> group.equals(entry.getKey().getGroups().get(newValue)))
                    .map(entry -> entry.getValue())
                    .collect(Collectors.toList()));
        }
        for(CellGroup<TNode, TEdge> group : lookupGroups.values()) {
            canvas.addGroup(group);
        }
    }

    @Override
    protected Cell<TNode> createNode(Graph.NodeWrapper<TNode> wrapper) {
        Cell<TNode> uiAdded = lookupCells.get(wrapper.getNode());
        if(uiAdded == null) {
            //Call on the cell factory to build a cell for this
            uiAdded = factoryNodes.uiFor(wrapper.getNode());
            uiAdded.addContextMenuItems(wrapper.getContextItems());
            lookupCells.put(wrapper.getNode(), uiAdded);

            //Run the cell through the layout engine before adding it to minimize layout-related artifacts.
            //Generally the size is not set correctly when it hits the layout engine, and a node is only
            // added right before an edge, and the layout engine will re-layout both endpoints of a new edge, so while
            // we know this is half redundant and half insufficient, we do it anyway because it should be right.
            if(enableLayout && layout != null) {
                Cell<TNode> uiLayout = uiAdded;
                Platform.runLater(() -> layout.layoutSingle(this, uiLayout, wrapper.getNode()));
            }

            canvas.getCellLayer().getChildren().add(uiAdded);

            //Check for membership in a group
            String nameGroup = wrapper.getNode().getGroups().get(groupCurrent);
            if(nameGroup != null) {
                CellGroup<TNode, TEdge> objGroup = lookupGroups.get(nameGroup);
                if(objGroup == null ) {
                    objGroup = factoryGroups.getGroup(groupCurrent, nameGroup);
                    lookupGroups.put(nameGroup, objGroup);
                    objGroup.getMembers().add(uiAdded);
                    canvas.addGroup(objGroup);
                } else {
                    objGroup.getMembers().add(uiAdded);
                }
            }
        } else {
            if(uiAdded.containerProperty().get() != null) {
                uiAdded.containerProperty().get().getMembers().remove(uiAdded);
            }
            uiAdded.deletedProperty().set(false);
            //Check for membership in a group
            //If we're a member of the group and the group exists, then rebuild the hull.
            //Creating a group here can cause issues when working with the LogicalGraph variants (because of the shared network list)
            String nameGroup = wrapper.getNode().getGroups().get(groupCurrent);
            if(nameGroup != null) {
                CellGroup<TNode, TEdge> objGroup = lookupGroups.get(nameGroup);
                if(objGroup == null ) {
                    objGroup = factoryGroups.getGroup(groupCurrent, nameGroup);
                    lookupGroups.put(nameGroup, objGroup);
                    canvas.addGroup(objGroup);
                }
                if(!objGroup.getMembers().contains(uiAdded)) {
                    objGroup.getMembers().add(uiAdded);
                } else {
                    //We toggled visibility without affecting contents.
                    objGroup.rebuildHull();
                }
            }

        }
        return uiAdded;
    }
    @Override
    protected void removeNode(Graph.NodeWrapper<TNode> wrapper) {
        Cell cellRemoved = lookupCells.get(wrapper.getNode());
        if(cellRemoved != null) {
            cellRemoved.deletedProperty().set(true);
        }
    }

    @Override
    protected void createEdge(Graph.EdgeWrapper<TNode, TEdge> wrapper) {
        TEdge edge = wrapper.getEdge();
        Edge uiAdded = lookupEdges.get(edge);
        if(uiAdded == null) {
            //Call on the edge factory to build a ui edge for this
            //Because of the multithreading in how the graph elements are sourced, we might be creating an edge before the nodes.
            //In theory, we have to be in the FX Thread to create the nodes and edges, so there are no
            //concurrency risks within this code, but the cost of a simple synchronized call to GetOrBuildNode, called once for
            //each node and twice per edge, is very low and ensures data integrity.
            final Cell<TNode> cellSource = createNode(wrapper.getSource());
            final Cell<TNode> cellDestination = createNode(wrapper.getDestination());

            uiAdded = factoryEdges.uiFor(edge, cellSource, cellDestination);
            lookupEdges.put(edge, uiAdded);
            canvas.getEdgeLayer().getChildren().add(uiAdded);

            //Regardless of whether or not we just created the nodes, run them through the layout.
            //This is most obviously necessary in the RadialGenerations Layout.
            if(enableLayout) {
                if (cellSource.autoLayoutProperty().get()) {
                    if (layout != null) {
                        layout.layoutSingle(this, cellSource, edge.getSource());
                    }
                }
                if (cellDestination.autoLayoutProperty().get()) {
                    if (layout != null) {
                        layout.layoutSingle(this, cellDestination, edge.getDestination());
                    }
                }
            }
        } else {
            uiAdded.deletedProperty().set(false);
        }
    }
    @Override
    protected void removeEdge(Graph.EdgeWrapper<TNode, TEdge> wrapper) {
        Edge edgeVisual = lookupEdges.get(wrapper.getEdge());
        if(edgeVisual != null) {
            edgeVisual.deletedProperty().set(true);
        }
    }
    //</editor-fold>


    public List<Cell<TNode>> getAllCellsForLayout() {
        return new ArrayList<>(lookupCells.values().stream()
                .filter(cell -> cell.autoLayoutProperty().get())
                .collect(Collectors.toList()));
    }
    public List<Cell<TNode>> getAllCellsForLayout(String group) {
        if(group == null) {
            return lookupCells.entrySet().stream()
                    .filter(entry -> entry.getValue().autoLayoutProperty().get() && entry.getKey().getGroups().get(groupCurrent) == null)
                    .map(entry -> entry.getValue())
                    .collect(Collectors.toList());
        } else {
            return lookupCells.entrySet().stream()
                    .filter(entry -> entry.getValue().autoLayoutProperty().get() && group.equals(entry.getKey().getGroups().get(groupCurrent)))
                    .map(entry -> entry.getValue())
                    .collect(Collectors.toList());
        }
    }
    public List<Map.Entry<TNode, Cell<TNode>>> getMappedCells() {
        return new ArrayList<>(lookupCells.entrySet());
    }
    public List<Map.Entry<TEdge, Edge>> getMappedEdges() {
        return new ArrayList<>(lookupEdges.entrySet());
    }
    public List<String> getAllGroupsForLayout() {
        return lookupCells.entrySet().stream()
                .filter(entry -> entry.getValue().autoLayoutProperty().get())
                .map(entry -> entry.getKey().getGroups().get(groupCurrent))
                .distinct()
                .collect(Collectors.toList());
    }
    public Cell<TNode> cellFor(TNode node) {
        return lookupCells.get(node);
    }
    public Edge edgeFor(TEdge edge) {
        return lookupEdges.get(edge);
    }
    public String getCurrentGroupBy() {
        return groupCurrent;
    }
    public CellGroup<TNode, TEdge> groupFor(String nameGroup) {
        return lookupGroups.get(nameGroup);
    }

    //TODO: Extend this to cover edges, groups, etc.
    public ObservableMap<TNode, Cell<TNode>> getObservableCells() {
        return lookupCells;
    }

    // == Default Factories ===================================================
    private Cell<TNode> DefaultNodeFactory(TNode node) {
        return new Cell<>(node);
    }
    private Edge<TNode> DefaultEdgeFactory(TEdge edge, Cell<TNode> source, Cell<TNode> destination) {
        return new Edge<>(source, destination);
    }
    private CellGroup<TNode, TEdge> DefaultGroupFactory(String groupBy, String groupName) {
        return new CellGroup<>(groupName, this.graph);
    }

    public void setNodeFactory(INodeCellFactory<TNode> factory) {
        factoryNodes = factory;
    }
    public void setEdgeFactory(IEdgeCellFactory<TNode, TEdge> factory) {
        factoryEdges = factory;
    }
    public void setGroupFactory(IGroupCellFactory<TNode, TEdge> factory) {
        factoryGroups = factory;
    }
    public INodeCellFactory<TNode> getNodeFactory() {
        return factoryNodes;
    }
    public IEdgeCellFactory<TNode, TEdge> getEdgeFactory() {
        return factoryEdges;
    }
    public IGroupCellFactory<TNode, TEdge> getGroupFactory() {
        return factoryGroups;
    }

    public void setLayout(Layout<TNode, TEdge> layout) {
        this.layout = layout;
    }
    public Layout<TNode, TEdge> getLayout() {
        return this.layout;
    }

    public Canvas getCanvas() {
        return canvas;
    }

}
