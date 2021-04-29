package ui.graphing;

import core.document.graph.INode;
import javafx.scene.control.MenuItem;
import ui.custom.fx.ActiveMenuItem;

import java.util.LinkedList;
import java.util.List;

public abstract class FactoryLayoutableCells<TNode extends INode<TNode>> implements Visualization.INodeCellFactory<TNode> {
    protected final Graph owner;

    public FactoryLayoutableCells(Graph owner) {
        this.owner = owner;
    }

    @Override
    public List<MenuItem> getFactoryMenuItems() {
        LinkedList<MenuItem> result = new LinkedList<>();

        result.add(new ActiveMenuItem("Run Layout Now", event -> {
            owner.ExecuteLayout(false);
        }));
        result.add(new ActiveMenuItem("Run Layout on All Nodes Now", event -> {
            owner.ExecuteLayout(true);
        }));

        return result;
    }
}
