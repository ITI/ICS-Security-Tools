package ui.graphing;

import core.document.graph.IEdge;
import core.document.graph.INode;
import core.document.serialization.xml.XmlElement;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FactoryCachedGroups<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> implements Visualization.IGroupCellFactory<TNode, TEdge> {
    protected final HashMap<String, HashMap<String, CellGroup<TNode, TEdge>>> cache;
    protected final Graph<TNode, TEdge> graph;
    protected final List<MenuItem> contextItems;

    public FactoryCachedGroups(Graph<TNode, TEdge> graphOwner) {
        cache = new HashMap<>();

        this.graph = graphOwner;
        this.contextItems = new ArrayList<>();
    }

    protected CellGroup<TNode, TEdge> BuildGroup(String groupBy, String name) {
        return new CellGroup<>(name, graph);
    }
    protected void ConfigureGroup(CellGroup group) {
        //Drag group support
        group.addEventHandler(MouseEvent.MOUSE_PRESSED, group::Handle_DragStart);
        group.addEventHandler(MouseEvent.MOUSE_DRAGGED, group::Handle_Drag);
        group.addEventHandler(MouseEvent.MOUSE_RELEASED, group::Handle_DragEnd);
        group.addEventHandler(KeyEvent.KEY_RELEASED, group::Handle_DragEnd);

        //Expand/contract group
        group.addEventHandler(ScrollEvent.SCROLL, this::Handle_ScrollEvent);
    }

    protected void Handle_ScrollEvent(ScrollEvent event) {
        if(event.isControlDown() && event.getSource() instanceof CellGroup) {
            event.consume();

            final double multiplier;
            double delta = event.getDeltaY();
            if(event.isShiftDown()) {
                delta = event.getDeltaX();
            }
            if(delta < 0) {
                multiplier = 1.25;
            } else if(delta > 0) {
                multiplier = 0.8;
            } else {
                return;
            }

            @SuppressWarnings("unchecked") CellGroup<TNode, TEdge> group = (CellGroup<TNode, TEdge>)event.getSource();
            double xOrigin = group.centerXProperty().get();
            double yOrigin = group.centerYProperty().get();

            for(Cell<TNode> cell : group.getMembers()) {
                try {
                    double xDelta = cell.getLayoutX() + cell.getWidth() / 2.0 - xOrigin;
                    double yDelta = cell.getLayoutY() + cell.getHeight() / 2.0 - yOrigin;

                    cell.setLayoutX(xOrigin + xDelta * multiplier - cell.getWidth() / 2.0);
                    cell.setLayoutY(yOrigin + yDelta * multiplier - cell.getHeight() / 2.0);
                } catch(RuntimeException ex) {
                    //Cannot modify bound property; ignore and continue.
                }
            }
        }
    }

    @Override
    public List<MenuItem> getFactoryMenuItems() {
        return contextItems;
    }

    @Override
    public void clearCache() {
        cache.clear();
    }

    @Override
    public CellGroup<TNode, TEdge> getGroup(String groupBy, String group) {
        if(!cache.containsKey(groupBy)) {
            cache.put(groupBy, new HashMap<>());
        }
        HashMap<String, CellGroup<TNode, TEdge>> cacheGroups = cache.get(groupBy);
        if(!cacheGroups.containsKey(group)) {
            CellGroup<TNode, TEdge> groupNew = BuildGroup(groupBy, group);
            ConfigureGroup(groupNew);
            cacheGroups.put(group, groupNew);
        }
        return cacheGroups.get(group);
    }

    @Override
    public XmlElement toXml() {
        XmlElement xmlFactory = new XmlElement("factory");
        xmlFactory.addAttribute("type").setValue("group");

        for(String groupBy : cache.keySet()) {
            XmlElement xmlGroupBy = new XmlElement("groupBy");
            xmlGroupBy.addAttribute("name").setValue(groupBy);

            for(CellGroup group : cache.get(groupBy).values())  {
                xmlGroupBy.getChildren().add(group.toXml());
            }

            xmlFactory.getChildren().add(xmlGroupBy);
        }

        return xmlFactory;
    }
}
