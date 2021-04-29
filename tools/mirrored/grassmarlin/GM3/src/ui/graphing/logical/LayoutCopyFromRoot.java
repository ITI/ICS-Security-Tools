package ui.graphing.logical;

import core.document.graph.IEdge;
import core.document.graph.INode;
import javafx.beans.InvalidationListener;
import ui.graphing.Cell;
import ui.graphing.Graph;
import ui.graphing.Layout;
import ui.graphing.Visualization;

import java.util.HashMap;

public class LayoutCopyFromRoot<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> implements Layout<TNode, TEdge> {
    private final Graph<TNode, TEdge> root;
    private final HashMap<Cell<TNode>, InvalidationListener> listenersX;
    private final HashMap<Cell<TNode>, InvalidationListener> listenersY;

    public LayoutCopyFromRoot(Graph<TNode, TEdge> root) {
        this.root = root;
        this.listenersX = new HashMap<>();
        this.listenersY = new HashMap<>();
    }

    protected void copyCell(Cell<TNode> source, Cell<TNode> target) {
        if(source != null && target != null) {
            if(target.autoLayoutProperty().get()) {
                target.setLayoutX(source.getLayoutX());
                target.setLayoutY(source.getLayoutY());

                if(listenersX.containsKey(source)) {
                    source.layoutXProperty().removeListener(listenersX.get(source));
                }
                if(listenersY.containsKey(source)) {
                    source.layoutYProperty().removeListener(listenersY.get(source));
                }
                InvalidationListener listener = observable -> layoutSingle(null, target, target.getNode());
                listenersX.put(source, listener);
                listenersY.put(source, listener);
                source.layoutXProperty().addListener(listener);
                source.layoutYProperty().addListener(listener);
            }
        }
    }

    @Override
    public void layoutAll(Visualization<TNode, TEdge> visualization) {
        for(Cell<TNode> cell : visualization.getAllCellsForLayout()) {
            Cell<TNode> cellRoot = root.cellFor(cell.getNode());
            copyCell(cellRoot, cell);
        }
    }

    @Override
    public void layoutSingle(Visualization<TNode, TEdge> visualization, Cell<TNode> cell, TNode node) {
        Cell<TNode> cellRoot = root.cellFor(node);
        copyCell(cellRoot, cell);
    }
}
