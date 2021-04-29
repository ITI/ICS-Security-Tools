package ui.graphing;

import core.document.graph.IEdge;
import core.document.graph.INode;
import javafx.application.Platform;
import javafx.scene.control.*;
import ui.EmbeddedIcons;
import ui.graphing.graphs.GraphWatcher;

import java.util.*;
import java.util.function.Consumer;

public class GraphTreeController<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> extends GraphWatcher<TNode, TEdge, GraphTreeItem.GraphTreeNodeItem<TNode, TEdge>> {
    public interface FactoryTreeItems<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> {
        GraphTreeItem.GraphTreeGroupItem<TNode, TEdge> TreeNodeForGroup(String nameGroup, String groupBy);
        GraphTreeItem.GraphTreeNodeItem<TNode, TEdge> TreeNodeForNode(TNode node);
        GraphTreeItem.GraphTreeEdgeItem<TNode, TEdge> TreeNodeForEdge(TEdge edge, TNode parent);

        void putGroupImage(String group, EmbeddedIcons image);
        void setOwner(Graph<TNode, TEdge> owner);
        void clearCache();
    }

    private static class Internal_TreeCell extends TreeCell<String> {
        @Override
        public void updateItem(String value, boolean isEmpty) {
            super.updateItem(value, isEmpty);

            if(isEmpty) {
                setText(null);
                setGraphic(null);
            } else {
                setText(getTreeItem().getValue());
                setGraphic(getTreeItem().getGraphic());
            }
        }
    }

    // The <String> type is misleading; it is the least convoluted way to build a hybrid tree.  In reality, the majority of the nodes are GraphTreeItems
    protected final TreeItem<String> treeRoot;
    protected final TreeView<String> treeView;

    protected FactoryTreeItems<TNode, TEdge> treeItemFactory;

    protected final Consumer<GraphTreeItem.GraphTreeEdgeItem<TNode, TEdge>> callbackEdge;
    protected final Consumer<GraphTreeItem.GraphTreeNodeItem<TNode, TEdge>> callbackNode;

    public GraphTreeController(final Graph<TNode, TEdge> graph, HashMap<String, EmbeddedIcons> imagesGroups, Consumer<GraphTreeItem.GraphTreeEdgeItem<TNode, TEdge>> callbackEdge, Consumer<GraphTreeItem.GraphTreeNodeItem<TNode, TEdge>> callbackNode) {
        super(graph);

        treeRoot = new TreeItem<>();
        treeItemFactory = new FactoryTreeItemsDefault<>(imagesGroups);

        this.callbackEdge = callbackEdge;
        this.callbackNode = callbackNode;

        treeView = new TreeView<>(treeRoot);

        initComponents();
    }

    private void initComponents() {
        treeView.setShowRoot(false);
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeView.setCellFactory(this::Factory_TreeCells);
    }

    public void clear() {
        if(treeItemFactory != null) {
            treeItemFactory.clearCache();
        }
        treeRoot.getChildren().clear();
    }

    private void recursiveDelete(TreeItem<String> root) {
        List<GraphTreeItem<?, ?>> lstToDelete = new LinkedList<>();
        for(TreeItem<String> item : root.getChildren()) {
            if(item instanceof GraphTreeItem) {
                lstToDelete.add((GraphTreeItem<?, ?>)item);
            }
            recursiveDelete(item);
        }
        for(GraphTreeItem<?, ?> item : lstToDelete) {
            item.delete();
        }
        root.getChildren().clear();
    }

    //<editor-fold defaultstate="collapsed" desc="GraphWatcher implementation.">
    @Override
    protected void groupByChanged(String oldValue, String newValue) {
        recursiveDelete(treeRoot);

        graph.getNodes().forEach(wrapper -> this.createNode(wrapper));
        graph.getEdges().forEach(wrapper -> this.createEdge(wrapper));
    }
    @Override
    protected GraphTreeItem.GraphTreeNodeItem<TNode, TEdge> createNode(Graph.NodeWrapper<TNode> node) {
        final String group = graph.activeGroupProperty().get();
        final String nameGroup = node.getNode().getGroups().get(group);

        final GraphTreeItem.GraphTreeGroupItem<TNode, TEdge> nodeGroup = treeItemFactory.TreeNodeForGroup(nameGroup, group);
        if(nodeGroup.requiresInitialization()) {
            nodeGroup.initialize();
            initializeGroup(nodeGroup);
            //TODO: Set temporary highlight (new) on nodeGroup
        } else {
            //TODO: Set temporary highlight (update) on nodeGroup

        }

        final GraphTreeItem.GraphTreeNodeItem<TNode, TEdge> nodeNode = treeItemFactory.TreeNodeForNode(node.getNode());
        if(nodeNode.requiresInitialization()) {
            nodeNode.initialize();
            initializeNode(node, nodeNode, nodeGroup);
            //TODO: Set temporary highlight (new) on nodeNode
        } else {
            //TODO: Set temporary highlight (update) on nodeNode
        }

        return nodeNode;
    }
    @Override
    protected void removeNode(Graph.NodeWrapper<TNode> node) {
        final GraphTreeItem.GraphTreeNodeItem<TNode, TEdge> nodeNode = treeItemFactory.TreeNodeForNode(node.getNode());
        final GraphTreeItem.GraphTreeGroupItem<TNode, TEdge> nodeGroup = (GraphTreeItem.GraphTreeGroupItem<TNode, TEdge>)nodeNode.getParent();
        nodeNode.delete();
        if(nodeGroup != null && nodeGroup.getChildren().isEmpty()) {
            nodeGroup.getParent().getChildren().remove(nodeGroup);
        }
    }
    @Override
    protected void createEdge(Graph.EdgeWrapper<TNode, TEdge> edgeAdded) {
        // We need to get/build the tree nodes for each endpoint
        GraphTreeItem.GraphTreeNodeItem<TNode, TEdge> treeSource = createNode(edgeAdded.getSource());
        GraphTreeItem.GraphTreeNodeItem<TNode, TEdge> treeDestination = createNode(edgeAdded.getDestination());
        //Now add the child nodes for the edge
        final TEdge edge = edgeAdded.getEdge();
        GraphTreeItem.GraphTreeEdgeItem<TNode, TEdge> treeChildOfSource = treeItemFactory.TreeNodeForEdge(edge, edge.getSource());
        GraphTreeItem.GraphTreeEdgeItem<TNode, TEdge> treeChildOfDestination = treeItemFactory.TreeNodeForEdge(edge, edge.getDestination());
        if(treeChildOfSource.requiresInitialization()) {
            treeChildOfSource.initialize();
            initializeEdge(treeChildOfSource, treeSource);
            //TODO: Set temporary highlight (new) on treeChildOfSource
        } else {
            //TODO: Set temporary highlight (update) on treeChildOfSource
        }
        if(treeChildOfDestination.requiresInitialization()) {
            treeChildOfDestination.initialize();
            initializeEdge(treeChildOfDestination, treeDestination);
            //TODO: Set temporary highlight (new) on treeChildOfDestination
        } else {
            //TODO: Set temporary highlight (update) on treeChildOfDestination
        }
    }
    @Override
    protected void removeEdge(Graph.EdgeWrapper<TNode, TEdge> wrapper) {
        // Find both tree items associated with this edge and delete them.
        GraphTreeItem.GraphTreeEdgeItem<TNode, TEdge> treeForSource = treeItemFactory.TreeNodeForEdge(wrapper.getEdge(), wrapper.getEdge().getSource());
        treeForSource.delete();
        GraphTreeItem.GraphTreeEdgeItem<TNode, TEdge> treeForDestination = treeItemFactory.TreeNodeForEdge(wrapper.getEdge(), wrapper.getEdge().getDestination());
        treeForDestination.delete();
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Overridable GraphTreeItem initialization methods.">
    protected void initializeGroup(GraphTreeItem.GraphTreeGroupItem<TNode, TEdge> nodeGroup) {
        treeRoot.getChildren().add(nodeGroup);
        treeRoot.getChildren().sort((o1, o2) -> ((GraphTreeItem<TNode, TEdge>) o1).compareTo((GraphTreeItem<TNode, TEdge>) o2));
    }
    protected void initializeNode(Graph.NodeWrapper<TNode> node, GraphTreeItem.GraphTreeNodeItem<TNode, TEdge> treeNode, GraphTreeItem.GraphTreeGroupItem<TNode, TEdge> treeGroup) {
        if(node.getContextItems() != null) {
            treeNode.addContextMenuItems(node.getContextItems());
        }
        treeGroup.getChildren().add(treeNode);
        treeGroup.getChildren().sort((o1, o2) -> ((GraphTreeItem<TNode, TEdge>)o1).compareTo((GraphTreeItem<TNode, TEdge>)o2));

        if(callbackNode != null) {
            callbackNode.accept(treeNode);
        }
    }
    protected void initializeEdge(GraphTreeItem.GraphTreeEdgeItem<TNode, TEdge> treeEdge, GraphTreeItem.GraphTreeNodeItem<TNode, TEdge> treeNode) {
        treeNode.getChildren().add(treeEdge);
        treeNode.getChildren().sort((o1, o2) -> ((GraphTreeItem<TNode, TEdge>) o1).compareTo((GraphTreeItem<TNode, TEdge>) o2));
        if(callbackEdge != null) {
            callbackEdge.accept(treeEdge);
        }
    }
    //</editor-fold>

    @SuppressWarnings("unchecked")
    public void selectNode(TNode node) {
        treeView.getSelectionModel().clearSelection();
        if(treeItemFactory == null) {
            return;
        }
        final GraphTreeItem<TNode, TEdge> nodeTree = treeItemFactory.TreeNodeForNode(node);
        treeView.getSelectionModel().select(nodeTree);
        int idxRootSelected = treeView.getSelectionModel().getSelectedIndex();
        for(TreeItem edgeItem : new ArrayList<>(nodeTree.getChildren())) {
            if(edgeItem instanceof GraphTreeItem.GraphTreeEdgeItem) {
                TNode nodeOther = ((GraphTreeItem.GraphTreeEdgeItem<TNode, TEdge>)edgeItem).getNode();
                TEdge edge = ((GraphTreeItem.GraphTreeEdgeItem<TNode, TEdge>)edgeItem).getEdge();

                GraphTreeItem<TNode, TEdge> nodeEdge = treeItemFactory.TreeNodeForEdge(edge, nodeOther);
                treeView.getSelectionModel().select(nodeEdge);
            }
        }
        //We need to wait for the other selection actions to complete before scrolling, otherwise the scroll events implicitly created by changing the selection will override this.
        Platform.runLater(() -> treeView.scrollTo(idxRootSelected));
    }

    // Tree Cell Factory
    private TreeCell<String> Factory_TreeCells(TreeView<String> tree) {
        TreeCell<String> result = new Internal_TreeCell();

        //If there are no menu items, there is no OnShowing event, so we keep a separator in the menu while it is hidden.
        ContextMenu menu = new ContextMenu();
        result.setContextMenu(menu);
        result.getContextMenu().getItems().add(new SeparatorMenuItem());
        result.getContextMenu().setOnShowing(event -> {
            //Detect contained item and build the appropriate menu; since cells are reused, we either have to change the menu when the item is set or when the menu is shown.
            //Setting when shown allows us to reuse menu items from the visualization.
            result.getContextMenu().getItems().clear();
            TreeItem<String> contents = result.getTreeItem();
            List<Collection<MenuItem>> menus = new LinkedList<>();
            while(contents != null && contents instanceof GraphTreeItem) {
                Collection<MenuItem> itemsTreeNode = ((GraphTreeItem<TNode, TEdge>) contents).getContextMenuItems();
                menus.add(itemsTreeNode);
                contents = contents.getParent();
            }
            menu.getItems();
            for(Collection<MenuItem> items : menus) {
                if(items == null || items.isEmpty()) {
                    continue;
                }
                if(!menu.getItems().isEmpty()) {
                    menu.getItems().add(new SeparatorMenuItem());
                }
                menu.getItems().addAll(items);
            }
        });
        result.getContextMenu().setOnHidden(event -> {
            result.getContextMenu().getItems().clear();
            result.getContextMenu().getItems().add(new SeparatorMenuItem());
        });

        return result;
    }

    public TreeView<String> getTreeView() {
        return treeView;
    }

    // == Factory accessors
    public FactoryTreeItems<TNode, TEdge> getTreeItemFactory() {
        return treeItemFactory;
    }
    public void setTreeItemFactory(FactoryTreeItems<TNode, TEdge> factory) {
        treeItemFactory = factory;
    }
}
