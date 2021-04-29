package ui.graphing.physical;

import core.document.graph.PhysicalEdge;
import core.document.graph.PhysicalNode;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineJoin;
import ui.graphing.Cell;
import ui.graphing.CellGroup;
import ui.graphing.Graph;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class GroupVLan extends CellGroup<PhysicalNode, PhysicalEdge> {
    private final SimpleObjectProperty<Color> borderColor;
    private final SimpleDoubleProperty borderWidth;

    private final ChangeListener<Boolean> Handler_VisibilityChanged = this::Handle_VisibilityChanged;

    public GroupVLan(int idVlan, Graph<PhysicalNode, PhysicalEdge> graph) {
        super(Integer.toString(idVlan), graph);

        miChangeColor.setText("Change VLAN" + idVlan + " Color");

        //Set default color based on the id.  Take the low 3 decimal digits and map [0,9] to [0.5,1.0]
        double red = ((idVlan % 1000) / 100) / 20.0 + 0.5;
        double green = ((idVlan % 100) / 10) / 20.0 + 0.5;
        double blue = (idVlan % 10) / 20.0 + 0.5;

        this.borderColor = new SimpleObjectProperty<>(COLOR_DEFAULT);
        this.borderWidth = new SimpleDoubleProperty(STROKE_WIDTH);
        fillColor.set(Color.rgb((int)(255 * red), (int)(255 * green), (int)(255 * blue)));

        this.setOnMouseEntered(event -> {
            borderColor.set(COLOR_SELECTED);
            borderWidth.set(STROKE_WIDTH + 2.0);
        });
        this.setOnMouseExited(event -> {
            borderColor.set(COLOR_DEFAULT);
            borderWidth.set(STROKE_WIDTH);
        });
    }

    //We need to attach the handlers to rebuild the hulls when components are moved / resized, but we don't want to set the container (since that mucks with dragging)
    @Override
    protected void memberAdded(final Cell<PhysicalNode> member) {
        super.memberAdded(member);

        member.visibleProperty().addListener(Handler_VisibilityChanged);
    }
    @Override
    protected void memberRemoved(final Cell<PhysicalNode> member) {
        super.memberRemoved(member);

        member.visibleProperty().removeListener(Handler_VisibilityChanged);
    }

    protected void Handle_VisibilityChanged(ObservableValue<? extends Boolean> o, boolean oldValue, boolean newValue) {
        rebuildHull();
    }

    private void traceEdges(Cell<PhysicalNode> base, List<Cell<PhysicalNode>> visited) {
        if(visited.contains(base)) {
            return;
        }
        visited.add(base);
        for(Cell<PhysicalNode> endpoint : base.getEdges().stream().map(edge -> (edge.getSource().equals(base) ? edge.getTarget() : edge.getSource())).collect(Collectors.toList())) {
            traceEdges(endpoint, visited);
        }
    }

    private final List<Polygon> vlanPolys = new ArrayList<>();

    @Override
    public void rebuildHull() {
        //Partition the members into disjoint sets and render a hull for each.
        List<List<Cell<PhysicalNode>>> groupings = new LinkedList<>();
        List<Cell<PhysicalNode>> visited = new LinkedList<>();
        for(Cell<PhysicalNode> member : this.getMembers()) {
            if(visited.contains(member)) {
                continue;
            }
            List<Cell<PhysicalNode>> grouping = new LinkedList<>();
            traceEdges(member, grouping);
            visited.addAll(grouping);
            groupings.add(grouping);
        }
        //Remove all hidden nodes from the groupings, and remove any empty groupings.
        List<List<Cell<PhysicalNode>>> finalGroupings = new LinkedList<>();
        for(List<Cell<PhysicalNode>> grouping : groupings) {
            List<Cell<PhysicalNode>> result = grouping.stream().filter(cell -> cell.isVisible()).collect(Collectors.toList());
            if(!result.isEmpty()) {
                finalGroupings.add(result);
            }
        }

        while(vlanPolys.size() > finalGroupings.size()) {
            Polygon poly = vlanPolys.get(vlanPolys.size() - 1);
            poly.fillProperty().unbind();
            poly.strokeProperty().unbind();
            poly.strokeWidthProperty().unbind();
            vlanPolys.remove(vlanPolys.size() - 1);
            getChildren().remove(poly);
        }
        while(vlanPolys.size() < finalGroupings.size()) {
            final Polygon polyHull = new Polygon();
            polyHull.fillProperty().bind(this.fillColor);
            polyHull.strokeProperty().bind(this.borderColor);
            polyHull.strokeWidthProperty().bind(borderWidth);
            polyHull.setStrokeLineJoin(StrokeLineJoin.ROUND);
            vlanPolys.add(polyHull);
            getChildren().add(polyHull);
        }
        int idxPoly = 0;
        for(List<Cell<PhysicalNode>> grouping : finalGroupings) {
            vlanPolys.get(idxPoly).getPoints().clear();
            List<Double> points = BuildHullForCells(grouping, 0);
            if(points != null) {
                vlanPolys.get(idxPoly++).getPoints().addAll(points);
            }
        }
    }
}
