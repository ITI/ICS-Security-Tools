package ui.graphing;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


public class Canvas extends Group {
    private final CellLayer groups;
    private final CellLayer edges;
    private final CellLayer cells;

    protected final List<List<MenuItem>> contextMenus;

    //These are Nodes which are part of Groups that are added to the Cell Layer.
    private final List<Node> groupChildren;

    public Canvas() {
        groups = new CellLayer();
        edges = new CellLayer();
        cells = new CellLayer();

        this.getChildren().addAll(groups, edges, cells);

        groupChildren = new LinkedList<>();
        contextMenus = new LinkedList<>();

        this.setOnMousePressed(event -> {
            if (!event.isConsumed() && event.isSecondaryButtonDown() && !(event.getTarget() instanceof CellLayer)) {
                //Process child layers in order
                for (Node child : getChildren()) {
                    //Skip edges; edges, if they have context items, are available at the endpoints.
                    if (child instanceof CellLayer && child != edges) {
                        CellLayer layer = (CellLayer) child;
                        List<MenuItem> items = layer.contextMenuAt(event.getX(), event.getY());
                        if (items != null && !items.isEmpty()) {
                            contextMenus.add(items);
                        }
                    }
                }
            }
        });
    }

    /**
     * Groups needs a special-case clear since they are rebuilt when changing the grouping
     * Also, since groups can add content to the cell layer, it needs to handle that, too.
     */
    public void clearGroups() {
        groups.getChildren().clear();
        cells.getChildren().removeAll(groupChildren);
        groupChildren.clear();
    }
    public void clear() {
        groups.getChildren().clear();
        edges.getChildren().clear();
        cells.getChildren().clear();
    }

    public void addGroup(Node group) {
        groups.getChildren().add(group);
        if(group instanceof CellGroup.IHasNodesForCellLayer) {
            Collection<Node> newCells = ((CellGroup.IHasNodesForCellLayer)group).getCellLayerContents();
            groupChildren.addAll(newCells);
            cells.getChildren().addAll(newCells);
        }
    }

    public CellLayer getGroupLayer() {
        return groups;
    }
    public CellLayer getEdgeLayer() {
        return edges;
    }
    public CellLayer getCellLayer() {
        return cells;
    }

    public List<List<MenuItem>> getContextMenuItems() {
        //We build the list every* time a context menu is requested.
        //We don't build the list if the click targets the background--the Canvas does not receive the MousePressed event.
        //Since every press event is matched by a show event, we can clear the list in the show event to ensure it is erased for the next show.
        List<List<MenuItem>> result = new ArrayList<>(contextMenus);
        contextMenus.clear();
        return result;
    }
}
