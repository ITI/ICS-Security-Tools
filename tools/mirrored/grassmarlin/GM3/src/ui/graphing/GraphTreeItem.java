package ui.graphing;

import core.document.graph.IEdge;
import core.document.graph.INode;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import ui.EmbeddedIcons;
import ui.custom.fx.ActiveMenuItem;
import ui.custom.fx.ListSizeBinding;

import java.util.*;

public abstract class GraphTreeItem<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> extends TreeItem<String> implements Comparable<GraphTreeItem<TNode, TEdge>>{
    public static class GraphTreeGroupItem<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> extends GraphTreeItem<TNode, TEdge> {
        public GraphTreeGroupItem(String name, EmbeddedIcons image) {
            super(name, image);

            List<MenuItem> menu = new LinkedList<>();

            menu.add(new ActiveMenuItem("Expand All Groups", (event -> {
                this.getParent().getChildren().stream().forEach(group -> group.setExpanded(true));
            })));
            menu.add(new ActiveMenuItem("Collapse All Groups", (event -> {
                this.getParent().getChildren().stream().forEach(group -> group.setExpanded(false));
            })));
            addContextMenuItems(menu);
        }
    }
    public static class GraphTreeNodeItem<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> extends GraphTreeItem<TNode, TEdge> {
        private final TNode node;

        public GraphTreeNodeItem(TNode item) {
            super();
            this.valueProperty().bind(item.titleProperty());
            this.node = item;
        }

        public TNode getNode() {
            return node;
        }

        @Override
        public void delete() {
            deleteParentIfEmpty();
            super.delete();
        }

        protected void deleteParentIfEmpty() {
            TreeItem<String> parent = getParent();
            if(parent != null && parent.getParent() != null) {
                if(parent.getChildren().size() == 1) {
                    if(parent instanceof GraphTreeItem) {
                        ((GraphTreeItem<?, ?>)parent).delete();
                    } else {
                        parent.getParent().getChildren().remove(parent);
                    }
                }
            }
        }
        @Override
        public int compareTo(GraphTreeItem<TNode, TEdge> rhs) {
            if(rhs instanceof GraphTreeNodeItem) {
                return node.compareTo( ((GraphTreeNodeItem<TNode, TEdge>)rhs).node );
            } else {
                return super.compareTo(rhs);
            }
        }
    }
    public static class GraphTreeEdgeItem<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> extends GraphTreeNodeItem<TNode, TEdge> {
        private final TEdge edge;

        public GraphTreeEdgeItem(TNode item, TEdge edge) {
            super(item);
            this.edge = edge;
        }

        @Override
        protected void deleteParentIfEmpty() {
            //Do not delete an empty parent.
        }
        public TEdge getEdge() {
            return edge;
        }
    }

    private boolean initialized = false;
    private String name;

    protected GraphTreeItem() {
        super();
    }
    protected GraphTreeItem(String title, EmbeddedIcons image) {
        super(title, image == null ? null : image.getImage(16.0));

        this.name = title;

        valueProperty().bind(new ReadOnlyStringWrapper(title).concat(" (").concat(new ListSizeBinding(getChildren())).concat(" item[s])"));
    }

    public String getName() {
        return this.name;
    }

    public boolean requiresInitialization() {
        return !initialized;
    }
    public void initialize() {
        initialized = true;
    }
    public void delete() {
        getChildren().clear();

        initialized = false;
        /*
        if(getParent() != null) {
            getParent().getChildren().remove(this);
        }
        */
    }

    private Collection<MenuItem> contextMenu;
    public Collection<MenuItem> getContextMenuItems() {
        return contextMenu;
    }
    public void addContextMenuItems(Collection<MenuItem> menu) {
        if(this.contextMenu == null) {
            //Drop duplicate menu items.
            contextMenu = new LinkedHashSet<>();
        }
        this.contextMenu.addAll(menu);
    }

    @Override
    public int compareTo(GraphTreeItem<TNode, TEdge> rhs) {
        if(getValue() == null) {
            if(rhs.getValue() == null) {
                return 0;
            } else {
                return -1;
            }
        }
        if(rhs.getValue() == null) {
            return 1;
        }
        return getValue().compareTo(rhs.getValue());
    }
}
