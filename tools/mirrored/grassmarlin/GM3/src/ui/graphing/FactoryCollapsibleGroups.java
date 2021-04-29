package ui.graphing;

import core.document.graph.IEdge;
import core.document.graph.INode;
import ui.EmbeddedIcons;
import ui.custom.fx.ActiveMenuItem;

import java.util.HashMap;

/**
 * A CellGroup factory which stores previously used groups and re-issues them when the same criteria is requested.
 * This allows group-specific properties to persist even when the groups cease to be displayed.
 *
 * Additionally, this provides the necessary hooks for group dragging and setting group context items.
 */
public class FactoryCollapsibleGroups<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> extends FactoryCachedGroups<TNode, TEdge> {
    protected final HashMap<String, EmbeddedIcons> imageGroups;

    public FactoryCollapsibleGroups(Graph<TNode, TEdge> graphOwner, HashMap<String, EmbeddedIcons> images) {
        super(graphOwner);

        imageGroups = new HashMap<>();
        images.entrySet().forEach(entry -> imageGroups.put(entry.getKey(), entry.getValue()));

        contextItems.add(new ActiveMenuItem("Expand All", event -> {
            for(HashMap<String, CellGroup<TNode, TEdge>> cachedBy : cache.values()) {
                for(CellGroup<TNode, TEdge> group : cachedBy.values()) {
                    if(group instanceof CellGroupCollapsible) {
                        ((CellGroupCollapsible<TNode, TEdge>)group).collapsedProperty().set(false);
                    }
                }
            }
        }));
        contextItems.add(new ActiveMenuItem("Collapse All", event -> {
            for (HashMap<String, CellGroup<TNode, TEdge>> cachedBy : cache.values()) {
                for (CellGroup<TNode, TEdge> group : cachedBy.values()) {
                    if(group instanceof CellGroupCollapsible) {
                        ((CellGroupCollapsible<TNode, TEdge>)group).collapsedProperty().set(true);
                    }
                }
            }
        }));
    }

    public void putGroupImage(String group, EmbeddedIcons image) {
        imageGroups.put(group, image);
    }

    @Override
    protected CellGroup<TNode, TEdge> BuildGroup(String groupBy, String name) {
        return new CellGroupCollapsible<>(name, imageGroups.get(groupBy), graph);
    }
}
