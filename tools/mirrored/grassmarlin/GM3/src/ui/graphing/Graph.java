package ui.graphing;

import com.sun.javafx.collections.ObservableListWrapper;
import core.document.graph.*;
import core.document.serialization.xml.Escaping;
import core.document.serialization.xml.XmlElement;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import ui.EmbeddedIcons;
import ui.custom.fx.ActiveMenuItem;
import ui.dialog.NodeDetailsDialogFx;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

public abstract class Graph<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> {
    //<editor-fold default-state="collapsed" desc="Internal classes">
    public static class NodeWrapper<TNode extends INode<TNode>> {
        private final TNode node;
        private final List<MenuItem> contextItems;

        public NodeWrapper(TNode node) {
            this.node = node;
            this.contextItems = new LinkedList<>();
        }

        public TNode getNode() {
            return node;
        }
        public List<MenuItem> getContextItems() {
            return contextItems;
        }

        @Override
        public int hashCode() {
            return node.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if(other instanceof NodeWrapper) {
                return node.equals(((NodeWrapper<?>)other).node);
            }
            return false;
        }
    }
    public static class EdgeWrapper<TNode extends INode <TNode>, TEdge extends IEdge<TNode>> {
        private final TEdge edge;
        private final NodeWrapper<TNode> source;
        private final NodeWrapper<TNode> destination;

        public EdgeWrapper(TEdge edge, NodeWrapper<TNode> source, NodeWrapper<TNode> destination) {
            this.edge = edge;
            this.source = source;
            this.destination = destination;
        }

        public TEdge getEdge() {
            return edge;
        }
        public NodeWrapper<TNode> getSource() {
            return source;
        }
        public NodeWrapper<TNode> getDestination() {
            return destination;
        }
    }

    protected static HashMap<String, EmbeddedIcons> imagesGroups = new HashMap<String, EmbeddedIcons>() {{
        put(LogicalNode.GROUP_SUBNET, EmbeddedIcons.Vista_Network);
        put(LogicalNode.GROUP_COUNTRY, EmbeddedIcons.Vista_Flag_Red);
        put(MeshNode.GROUP_PAN, EmbeddedIcons.Vista_Network);
    }};
    //</editor-fold>

    //Data Elements
    protected final NetworkGraph<TNode, TEdge> graph;

    protected final ObservableList<NodeWrapper<TNode>> uiNodes;
    protected final ObservableList<EdgeWrapper<TNode, TEdge>> uiEdges;
    protected final SimpleStringProperty activeGroup;

    protected final StringProperty title;
    protected boolean loading;

    //Controller Elements
    protected GraphTreeController<TNode, TEdge> treeController;
    private Visualization<TNode, TEdge> visualization;

    //UI Elements
    private final ZoomableScrollPane scrollPane;
    private final Menu menuGroupBy;
    private final NodeDetailsDialogFx<TNode> dlgDetails;
    protected final List<MenuItem> menuGraph;

    //Event hooks
    private final MapChangeListener<TNode, Cell<TNode>> Handler_VisualizationChanged = this::Handle_VisualizationCellsChanged;

    public Graph(NetworkGraph<TNode, TEdge> graphSource, String activeGroup) {
        this.graph = graphSource;

        this.uiNodes = new ObservableListWrapper<>(new LinkedList<>());
        this.uiEdges = new ObservableListWrapper<>(new LinkedList<>());
        this.activeGroup = new SimpleStringProperty(activeGroup);

        this.title = new SimpleStringProperty("Untitled Graph");
        this.loading = false;

        this.treeController = new GraphTreeController<>(this, imagesGroups, this::Handle_NewTreeEdge, this::Handle_NewTreeNode);
        this.menuGroupBy = new Menu("Group By");
        this.menuGraph = new ArrayList<>(1);
        this.dlgDetails = new NodeDetailsDialogFx<>();

        // == Initialize the ZoomableScrollPane / Visualization ===============
        //The scrollPane wraps the canvas and is what is referenced outside the Graph.
        this.scrollPane = new ZoomableScrollPane(this::Handle_ContextMenuGeneration);

        this.visualization = new Visualization<>(this);
        this.visualization.getObservableCells().addListener(Handler_VisualizationChanged);
        scrollPane.setCanvas(visualization.getCanvas());

        initComponents();

        //Only bind handlers after the Graph has been fully initialized.
        this.graph.getNodes().addListener(this::Handle_NodeChange);
        this.graph.getEdges().addListener(this::Handle_EdgeChange);
        this.graph.OnGraphCleared.addHandler((source, arguments) -> {
            //Clear cached data
            this.visualization.clear();
            this.treeController.clear();
        });
        this.graph.OnGroupingInvalidated.addHandler((source, arguments) -> {
            String grouping = this.activeGroup.get();
            this.activeGroup.set(null);
            this.activeGroup.set(grouping);
        });
    }

    private void initComponents() {
        //Ensure there is at least one item initially
        this.menuGroupBy.getItems().add(new SeparatorMenuItem());
        this.menuGroupBy.setOnShowing(event -> {
            menuGroupBy.getItems().clear();
            menuGroupBy.getItems().addAll(
                    graph.getNodes().stream()
                            .flatMap(node -> node.getGroups().keySet().stream())
                            .distinct()
                            .map(group -> {
                                EmbeddedIcons imageGroup = imagesGroups.get(group);
                                ImageView imageView = null;
                                if (imageGroup != null) {
                                    imageView = imageGroup.getImage(16.0);
                                }
                                CheckMenuItem result = new CheckMenuItem(group, imageView);
                                if (Graph.this.activeGroup.get().equals(group)) {
                                    result.setSelected(true);
                                }
                                result.setOnAction(evt -> Graph.this.activeGroup.set(group));
                                return result;
                            })
                            .collect(Collectors.toList())
            );
            //If there is only a single element, disable it.
            if (menuGroupBy.getItems().size() == 0) {
                MenuItem miPlacebo = new MenuItem("No Groups Available");
                miPlacebo.setDisable(true);
                menuGroupBy.getItems().add(miPlacebo);
            } else if (menuGroupBy.getItems().size() == 1) {
                menuGroupBy.getItems().get(0).setDisable(true);
            }
        });

        menuGraph.add(menuGroupBy);
    }

    public void SelectNode(TNode node) {
        treeController.selectNode(node);
        visualization.getMappedCells().stream().forEach(entry -> entry.getValue().selectedProperty().set(entry.getKey().equals(node)));
    }

    public boolean isLoading() {
        return this.loading;
    }
    protected void doneLoading() {
        //Nothing to do in base case
    }

    //<editor-fold defaultstate="collapsed" desc="processXXX methods, intended to be overridden in derived classes, which are used to respond to the creation of graph or visual elements.">

    // Called when a new node is added to the graph; the wrapper contains elements that are common to the tree and visualization.
    protected void processNewNode(final NodeWrapper<TNode> node) {
        node.getContextItems().clear();
        node.getContextItems().add(new ActiveMenuItem("View Details for " + node.getNode().titleProperty().get(), EmbeddedIcons.Vista_Report, event -> {
            dlgDetails.setNode(node.getNode());
            dlgDetails.showAndWait();
        }));
        node.getContextItems().add(new ActiveMenuItem("Center in View", (event) -> {
            Graph.this.getVisualizationView().centerOn(Graph.this.getVisualization().cellFor(node.getNode()));
        }));
    }

    //Visualization / ScrollPane
    protected void processNewVisualNode(final TNode node, final Cell<TNode> cell) {
        //No activity in the base case.
    }
    protected void processRemoveVisualNode(final TNode node, final Cell<TNode> cell) {
        //No activity in the base case.
    }
    protected void processNewVisualEdge(final Edge edge) {
        //No activity in the base case.
    }

    //Tree
    protected void processNewTreeNode(GraphTreeItem.GraphTreeNodeItem<TNode, TEdge> item) {
        //No activity in the base case.
    }
    protected void processNewTreeEdge(GraphTreeItem.GraphTreeEdgeItem<TNode, TEdge> item) {
        //No activity in the base case.
    }

    /**
     * Find all of the groups of Context Menu items for this location.  This includes lists from factory objects as well as the Canvas.
     * @return A list of lists of menu items; each list will be separated from the others when the context menu is constructed.
     */
    protected List<List<MenuItem>> processContextMenu() {
        LinkedList<List<MenuItem>> result = new LinkedList<>();
        result.add(visualization.getEdgeFactory().getFactoryMenuItems());
        result.add(visualization.getNodeFactory().getFactoryMenuItems());
        result.add(visualization.getGroupFactory().getFactoryMenuItems());
        //The Canvas will handle all layers, which include node, edge, vlans (on Physical Graph), and groups.
        result.addAll(getVisualization().getCanvas().getContextMenuItems());
        result.add(menuGraph);
        return result;
    }
    //</editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Internal methods that are used to trigger the processXXX methods.">

    private void Handle_VisualizationCellsChanged(MapChangeListener.Change<? extends TNode,? extends Cell<TNode>> change) {
        if(change.wasRemoved()) {
            processRemoveVisualNode(change.getKey(), change.getValueRemoved());
        }
        if(change.wasAdded()) {
            processNewVisualNode(change.getKey(), change.getValueAdded());
        }
    }

    //TODO: Move these to handlers for Observable collections.
    protected void Handle_NewTreeNode(GraphTreeItem.GraphTreeNodeItem<TNode, TEdge> item) {
        processNewTreeNode(item);
    }
    protected void Handle_NewTreeEdge(GraphTreeItem.GraphTreeEdgeItem<TNode, TEdge> item) {
        processNewTreeEdge(item);
    }

    private NodeWrapper<TNode> Handle_WrapNode(TNode node) {
        NodeWrapper<TNode> result = new NodeWrapper<>(node);
        processNewNode(result);
        return result;
    }
    private EdgeWrapper<TNode, TEdge> Handle_WrapEdge(TEdge edge) {
        return new EdgeWrapper<>(edge, Handle_WrapNode(edge.getSource()), Handle_WrapNode(edge.getDestination()));
    }

    private List<List<MenuItem>> Handle_ContextMenuGeneration() {
        return processContextMenu();
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Event Hooks">
    // Internal handlers for list modification
    private void Handle_NodeChange(ListChangeListener.Change<? extends TNode> c) {
        final boolean isLayoutRequired = visualization.suspendLayout();
        try {
            while (c.next()) {
                this.loading = true;
                uiNodes.removeIf(wrapper -> c.getRemoved().contains(wrapper.getNode()));
                c.getRemoved().forEach(node -> getGroupFactory().getGroup(activeGroup.get(), node.getGroups().get(activeGroup.get())).rebuildHull());

                uiNodes.addAll(c.getAddedSubList().stream().map(this::Handle_WrapNode).collect(Collectors.toList()));

                this.loading = false;
                this.doneLoading();
            }
        } finally {
            //Do not run the layout if it was already suspended; this happens as part of loading.
            if(isLayoutRequired) {
                visualization.resumeLayout(true);
            }
            if (scrollPane.getZoomAfterLayout()) {
                Platform.runLater(() -> scrollPane.zoomToFit());
            }
        }
    }

    /**
     * To be used when a node changes group membership.
     * @param node
     */
    public void reprocessNode(final TNode node) {
        final NodeWrapper<TNode> wrapper = uiNodes.stream().filter(wrap -> wrap.getNode().equals(node)).findAny().get();
        final List<EdgeWrapper<TNode, TEdge>> edges = uiEdges.stream().filter(edge -> edge.getEdge().getSource().equals(node) || edge.getEdge().getDestination().equals(node)).collect(Collectors.toList());

        uiEdges.removeAll(edges);
        uiNodes.remove(wrapper);

        uiNodes.add(wrapper);
        uiEdges.addAll(edges);
    }
    private void Handle_EdgeChange(ListChangeListener.Change<? extends TEdge> c) {
        final boolean isLayoutRequired = visualization.suspendLayout();
        try {
            while (c.next()) {
                this.loading = true;
                uiEdges.removeIf(wrapper -> c.getRemoved().contains(wrapper.getEdge()));

                uiEdges.addAll(c.getAddedSubList().stream().map(this::Handle_WrapEdge).collect(Collectors.toList()));
                this.loading = false;
            }
        } finally {
            // If layout was already suspended (which happens during loading) we do not want to run it now.
            if(isLayoutRequired) {
                visualization.resumeLayout(true);
            }
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Accessors">
    public NetworkGraph<TNode, TEdge> getGraph() {
        return graph;
    }

    /**
     * Do not make this public; at best a subclass can access it, but the main UI classes should use methods on the Graph
     * to access the corresponding methods in the Visualization.
     * @return The visualization that maps the graph nodes to visual entities for the scrollPane.
     */
    protected Visualization<TNode, TEdge> getVisualization() {
        return this.visualization;
    }
    protected void setVisualization(Visualization<TNode, TEdge> visualization) {
        this.visualization.getObservableCells().removeListener(Handler_VisualizationChanged);
        if(visualization != null) {
            this.visualization = visualization;
            this.visualization.getObservableCells().addListener(Handler_VisualizationChanged);
            this.scrollPane.setCanvas(visualization.getCanvas());
        }
    }

    public ObservableList<NodeWrapper<TNode>> getNodes() {
        return uiNodes;
    }
    public ObservableList<EdgeWrapper<TNode, TEdge>> getEdges() {
        return uiEdges;
    }

    /**
     * The Active Group is the current attribute by which the nodes are being grouped.
     * @return A property which is the active group.
     */
    public StringProperty activeGroupProperty() {
        return activeGroup;
    }

    public void setLayout(Layout<TNode, TEdge> layout) {
        this.visualization.setLayout(layout);
    }
    public Layout getLayout() {
        return this.visualization.getLayout();
    }

    public void setEdgeFactory(Visualization.IEdgeCellFactory<TNode, TEdge> factory) {
        this.visualization.setEdgeFactory(factory);
    }
    public Visualization.IEdgeCellFactory<TNode, TEdge> getEdgeFactory() {
        return visualization.getEdgeFactory();
    }
    public void setCellFactory(Visualization.INodeCellFactory<TNode> factory) {
        this.visualization.setNodeFactory(factory);
    }
    public Visualization.INodeCellFactory<TNode> getCellFactory() {
        return this.visualization.getNodeFactory();
    }
    public void setGroupFactory(Visualization.IGroupCellFactory<TNode, TEdge> factory) {
        this.visualization.setGroupFactory(factory);
    }
    public Visualization.IGroupCellFactory<TNode, TEdge> getGroupFactory() {
        return visualization.getGroupFactory();
    }
    public void setTreeFactory(GraphTreeController.FactoryTreeItems<TNode, TEdge> treeFactory) {
        treeController.setTreeItemFactory(treeFactory);
        treeFactory.setOwner(this);
    }
    public GraphTreeController.FactoryTreeItems<TNode, TEdge> getTreeFactory() {
        return treeController.getTreeItemFactory();
    }

    public ZoomableScrollPane getVisualizationView() {
        return this.scrollPane;
    }
    public TreeView<String> getTreeView() {
        return treeController.getTreeView();
    }
    public StringProperty titleProperty() {
        return title;
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Layout Support">
    public void suspendLayout() {
        getVisualization().suspendLayout();
    }
    public void resumeLayout(boolean runNow) {
        getVisualization().resumeLayout(runNow);
    }

    public void setZoomAfterLayout(boolean value) {
        scrollPane.setZoomAfterLayout(value);
    }

    public void ExecuteLayout(boolean layoutAll) {
        if(layoutAll) {
            visualization.getMappedCells().stream()
                    .forEach(entry -> entry.getValue().autoLayoutProperty().set(true));
        }
        visualization.getLayout().layoutAll(visualization);
    }

    public List<String> getGroups() {
        return getGroups(activeGroup.get());
    }
    public List<String> getGroups(String category) {
        ArrayList<TNode> nodes = new ArrayList<>();
        graph.GetGraphContents(nodes, null);

        //Find the distinct group names
        return nodes.stream()
                .map(node -> node.getGroups().get(category))
                .filter(g -> g != null)
                .distinct()
                .collect(Collectors.toList());
    }

    public Point2D locationOf(TNode node) {
        Cell cell = visualization.cellFor(node);
        return new Point2D(cell.getLayoutX(), cell.getLayoutY());
    }
    public Rectangle2D layoutOf(TNode node) {
        Cell cell = visualization.cellFor(node);
        return new Rectangle2D(cell.getLayoutX(), cell.getLayoutY(), cell.getWidth(), cell.getHeight());
    }
    public Cell<TNode> cellFor(TNode node) {
        return visualization.cellFor(node);
    }
    public Edge edgeFor(TEdge edge) {
        return visualization.edgeFor(edge);
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Serialization">
    public String getEntryName() {
        return UUID.randomUUID().toString() + "_graph.xml";
    }

    public abstract void toXml(ZipOutputStream zos) throws IOException;

    protected void writeOpenTag(ZipOutputStream zos, String tag, String title) throws IOException {
        zos.write("<".getBytes(StandardCharsets.UTF_8));
        zos.write(tag.getBytes(StandardCharsets.UTF_8));
        zos.write(" title='".getBytes(StandardCharsets.UTF_8));
        zos.write(Escaping.XmlString(title).getBytes(StandardCharsets.UTF_8));
        zos.write("' group='".getBytes(StandardCharsets.UTF_8));
        zos.write(Escaping.XmlString(getVisualization().getCurrentGroupBy()).getBytes(StandardCharsets.UTF_8));
        zos.write("' zoomAfterLayout='".getBytes(StandardCharsets.UTF_8));
        zos.write(Boolean.toString(scrollPane.getZoomAfterLayout()).getBytes(StandardCharsets.UTF_8));
        zos.write("' >".getBytes(StandardCharsets.UTF_8));
    }

    protected void writeContents(ZipOutputStream zos) throws IOException {
        // Record cell positions / other state
        for(Map.Entry<TNode, Cell<TNode>> entry : getVisualization().getMappedCells()) {
            int idxNode = graph.indexOf(entry.getKey());
            zos.write(entry.getValue().toXml(idxNode).toString().getBytes(StandardCharsets.UTF_8));
        }
        // Record edge state
        for(Map.Entry<TEdge, Edge> entry : getVisualization().getMappedEdges()) {
            int idxEdge = graph.indexOf(entry.getKey());
            zos.write(entry.getValue().toXml(idxEdge).toString().getBytes(StandardCharsets.UTF_8));
        }
        // Record factory states (this should include cached objects, which is how group states are preserved).
        XmlElement nodes = getVisualization().getNodeFactory().toXml();
        if(nodes != null) {
            zos.write(nodes.toString().getBytes(StandardCharsets.UTF_8));
        }
        XmlElement edges = getVisualization().getEdgeFactory().toXml();
        if(edges != null) {
            zos.write(edges.toString().getBytes(StandardCharsets.UTF_8));
        }
        XmlElement groups = getVisualization().getGroupFactory().toXml();
        if(groups != null) {
            zos.write(groups.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    protected void writeCloseTag(ZipOutputStream zos, String tag) throws IOException {
        zos.write("</".getBytes(StandardCharsets.UTF_8));
        zos.write(tag.getBytes(StandardCharsets.UTF_8));
        zos.write(">".getBytes(StandardCharsets.UTF_8));
    }

    protected void toXml(ZipOutputStream zos, String tag, String title) throws IOException {
        writeOpenTag(zos, tag, title);
        writeContents(zos);
        writeCloseTag(zos, tag);
    }
    //</editor-fold>
}
