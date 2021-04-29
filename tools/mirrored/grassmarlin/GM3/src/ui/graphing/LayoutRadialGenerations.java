package ui.graphing;

import core.document.graph.IEdge;
import core.document.graph.INode;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class LayoutRadialGenerations<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> extends LayoutHub<TNode, TEdge>  {
    private final TNode root;
    private final double radiusMinIncrement = 150.0;

    public LayoutRadialGenerations(TNode root) {
        this.root = root;
    }
    @Override
    public void layoutAll(Visualization<TNode, TEdge> visualization) {
        Point2D ptOrigin = new Point2D(0.0, 0.0);
        List<TNode> visited = new ArrayList<>();
        List<TNode> current = new LinkedList<>();

        current.add(root);
        double radiusCurrent = 0.0;
        while(!current.isEmpty()) {
            // We need to know what cells exist at this generation, but we only want to layout those that allow auto-layout.
            List<Cell<TNode>> cellsCurrent = current.stream().map(node -> visualization.cellFor(node)).distinct().collect(Collectors.toList());
            List<Cell<TNode>> cellsCurrentForLayout = cellsCurrent.stream().filter(cell -> cell.autoLayoutProperty().get()).collect(Collectors.toList());
            cellsCurrentForLayout.sort((o1, o2) -> o1.getNode().compareTo(o2.getNode()));

            LayoutAround(ptOrigin, radiusCurrent, Math.PI / 2.0, cellsCurrentForLayout, false);

            visited.addAll(current);
            current.clear();

            cellsCurrent.forEach(cell -> {
                current.addAll(cell.getEdges().stream()
                        .map(edge -> edge.getSource().equals(cell) ? edge.getTarget().getNode() : edge.getSource().getNode())
                        .filter(node -> !visited.contains(node))
                        .collect(Collectors.toList()));
            });

            //TODO: Calculate min radius to display the new current elements, set radiusCurrent to the maximum of the calculated limit and the current + the min.
            radiusCurrent += radiusMinIncrement;
        }
    }

    public TNode getRoot() {
        return this.root;
    }
}
