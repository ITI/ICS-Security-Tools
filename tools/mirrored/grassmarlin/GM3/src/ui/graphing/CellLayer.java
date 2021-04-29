package ui.graphing;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CellLayer extends Group {
    @FunctionalInterface
    public interface ICanHazContextMenu {
        /**
         * Returns the context menu items associated with this element.  This method may be called multiple times, so it
         * should avoid building new menu items each invocation.  While returning a cached list is ideal, the overhead
         * for building a list tends to be relatively minimal.
         * @return null if no Context Menu is present, otherwise a list of MenuItems for this node.
         */
        List<MenuItem> getContextMenuItems();
    }

    private final SimpleBooleanProperty overlappingAllowed;

    public CellLayer() {
        overlappingAllowed = new SimpleBooleanProperty(true);
    }

    public List<Node> nodeAt(double localX, double localY) {
        List<Node> children = getChildren();
        List<Node> results = new LinkedList<>();

        for (int i=0, max=children.size(); i<max; i++) {
            final Node node = children.get(i);
            javafx.geometry.Point2D ptTemp = new javafx.geometry.Point2D((float)localX, (float)localY);
            ptTemp = node.parentToLocal(ptTemp);
            if(ptTemp == null) {
                continue;
            }

            if (node.visibleProperty().get() && node.contains(ptTemp.getX(), ptTemp.getY())) {
                results.add(node);
                if(!overlappingAllowed.get()) {
                    return results;
                }
            }
        }
        if(results.isEmpty()) {
            return null;
        } else {
            return results;
        }
    }

    public List<MenuItem> contextMenuAt(double localX, double localY) {
        if(!isVisible()) {
            return null;
        }
        List<Node> nodes = nodeAt(localX, localY);
        if(nodes == null || nodes.isEmpty()) {
            return null;
        } else if(nodes.size() == 1) {
            Node node = nodes.get(0);
            if(node instanceof ICanHazContextMenu) {
                return ((ICanHazContextMenu)node).getContextMenuItems();
            }
        } else {
            List<MenuItem> result = new ArrayList<>(nodes.size());
            for(Node node : nodes) {
                if(node instanceof ICanHazContextMenu) {
                    List<MenuItem> items = ((ICanHazContextMenu)node).getContextMenuItems();
                    if(items != null && !items.isEmpty()) {
                        Menu menuNode = new Menu(node.toString());
                        //Remove from old parent to suppress a warning in the following addAll
                        for(MenuItem item : items) {
                            if(item.getParentMenu() != null) {
                                item.getParentMenu().getItems().remove(item);
                            }
                        }
                        menuNode.getItems().addAll(items);
                        result.add(menuNode);
                    }
                }
            }
            if(result.isEmpty()) {
                return null;
            } else if(result.size() == 1) {
                return ((Menu)result.get(0)).getItems();
            } else {
                return result;
            }
        }

        return null;
    }
}
