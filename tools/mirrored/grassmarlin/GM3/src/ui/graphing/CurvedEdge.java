package ui.graphing;

import core.document.graph.INode;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import ui.custom.fx.FxStringProperty;
import ui.graphing.physical.ControlPointCell;

public class CurvedEdge<TNode extends INode<TNode>> extends Edge<TNode> {
    /**
     * The control points are based on the vertices of a parallelogram where two sides are parallel to either the X or Y
     * axis and are of length equal to CONTROL_FACTOR times the distance between the two endpoints on that axis.  Which
     * axis is used is based on whether the X or Y difference between the endpoints is greater.
     */
    protected final static double CONTROL_FACTOR = 0.8;

    protected final CubicCurve connectionCurve;
    protected final BooleanProperty showDetails;

    public CurvedEdge(Cell<TNode> source, Cell<TNode> target) {
        super(source, target);
        this.showDetails = new SimpleBooleanProperty(false);

        connectionCurve = new CubicCurve();
        connectionCurve.startXProperty().bind(getSource().edgeXProperty());
        connectionCurve.startYProperty().bind(getSource().edgeYProperty());
        connectionCurve.endXProperty().bind(getTarget().edgeXProperty());
        connectionCurve.endYProperty().bind(getTarget().edgeYProperty());

        if(getSource() instanceof ControlPointCell) {
            connectionCurve.controlX1Property().bind(new ControlPointCell.XBinding( ((ControlPointCell<TNode>)getSource()).controlPointProperty() ));
            connectionCurve.controlY1Property().bind(new ControlPointCell.YBinding( ((ControlPointCell<TNode>)getSource()).controlPointProperty() ));
        } else {
            connectionCurve.controlX1Property().bind(new ControlX1Property(getSource(), getTarget()));
            connectionCurve.controlY1Property().bind(new ControlY1Property(getSource(), getTarget()));
        }
        if(getTarget() instanceof ControlPointCell) {
            connectionCurve.controlX2Property().bind(new ControlPointCell.XBinding( ((ControlPointCell<TNode>)getTarget()).controlPointProperty() ));
            connectionCurve.controlY2Property().bind(new ControlPointCell.YBinding( ((ControlPointCell<TNode>)getTarget()).controlPointProperty() ));
        } else {
            connectionCurve.controlX2Property().bind(new ControlX2Property(getSource(), getTarget()));
            connectionCurve.controlY2Property().bind(new ControlY2Property(getSource(), getTarget()));
        }

        //Misc line traits that apply regardless of format:
        connectionCurve.strokeWidthProperty().bind(edgeThickness);
        connectionCurve.setFill(Color.TRANSPARENT);
        connectionCurve.setStroke(Color.BLACK);

        getChildren().add(connectionCurve);
    }

    public void bindVisualProperties(BooleanProperty useCurvedLines, BooleanProperty showDetails) {
        this.connectionLinear.visibleProperty().bind(useCurvedLines.not());
        this.connectionCurve.visibleProperty().bind(useCurvedLines);

        this.showDetails.bind(showDetails);
    }

    @Override
    public void remapCells(Cell<TNode> oldEndpoint, Cell<TNode> newEndpoint) {
        super.remapCells(oldEndpoint, newEndpoint);

        connectionCurve.startXProperty().bind(getSource().edgeXProperty());
        connectionCurve.startYProperty().bind(getSource().edgeYProperty());
        connectionCurve.endXProperty().bind(getTarget().edgeXProperty());
        connectionCurve.endYProperty().bind(getTarget().edgeYProperty());

        if(getSource() instanceof ControlPointCell) {
            connectionCurve.controlX1Property().bind(new ControlPointCell.XBinding( ((ControlPointCell<TNode>)getSource()).controlPointProperty() ));
            connectionCurve.controlY1Property().bind(new ControlPointCell.YBinding( ((ControlPointCell<TNode>)getSource()).controlPointProperty() ));
        } else {
            connectionCurve.controlX1Property().bind(new ControlX1Property(getSource(), getTarget()));
            connectionCurve.controlY1Property().bind(new ControlY1Property(getSource(), getTarget()));
        }
        if(getTarget() instanceof ControlPointCell) {
            connectionCurve.controlX2Property().bind(new ControlPointCell.XBinding( ((ControlPointCell<TNode>)getTarget()).controlPointProperty() ));
            connectionCurve.controlY2Property().bind(new ControlPointCell.YBinding( ((ControlPointCell<TNode>)getTarget()).controlPointProperty() ));
        } else {
            connectionCurve.controlX2Property().bind(new ControlX2Property(getSource(), getTarget()));
            connectionCurve.controlY2Property().bind(new ControlY2Property(getSource(), getTarget()));
        }
    }

    @Override
    public void setDetails(FxStringProperty details) {
        super.setDetails(details);
        this.details.visibleProperty().bind(showDetails);
    }

    private static abstract class ComputedControlProperty extends SimpleDoubleProperty {
        DoubleProperty x1;
        DoubleProperty x2;
        DoubleProperty y1;
        DoubleProperty y2;

        ComputedControlProperty(Cell<?> source, Cell<?> target) {
            x1 = new SimpleDoubleProperty();
            x2 = new SimpleDoubleProperty();
            y1 = new SimpleDoubleProperty();
            y2 = new SimpleDoubleProperty();

            x1.bind(source.edgeXProperty());
            x2.bind(target.edgeXProperty());
            y1.bind(source.edgeYProperty());
            y2.bind(target.edgeYProperty());
        }

        @Override
        public abstract double get();
    }

    private static class ControlX1Property extends ComputedControlProperty {

        ControlX1Property(Cell<?> source, Cell<?> target) {
            super(source, target);
        }

        @Override
        public double get() {
            double deltaX = Math.abs(x1.get() - x2.get());
            double deltaY = Math.abs(y1.get() - y2.get());

            if (deltaX < deltaY) {
                return x1.get();
            } else {
                return CONTROL_FACTOR * (x2.get() - x1.get()) + x1.get();
            }
        }
    }

    private static class ControlX2Property extends ComputedControlProperty {

        ControlX2Property(Cell<?> source, Cell<?> target) {
            super(source, target);
        }

        @Override
        public double get() {
            double deltaX = Math.abs(x1.get() - x2.get());
            double deltaY = Math.abs(y1.get() - y2.get());

            if (deltaX < deltaY) {
                return x2.get();
            } else {
                return CONTROL_FACTOR * (x1.get() - x2.get()) + x2.get();
            }
        }
    }

    private static class ControlY1Property extends ComputedControlProperty {

        ControlY1Property(Cell<?> source, Cell<?> target) {
            super(source, target);
        }

        @Override
        public double get() {
            double deltaX = Math.abs(x1.get() - x2.get());
            double deltaY = Math.abs(y1.get() - y2.get());

            if (deltaX < deltaY) {
                return CONTROL_FACTOR * (y2.get() - y1.get()) + y1.get();
            } else {
                return y1.get();
            }
        }
    }

    private static class ControlY2Property extends ComputedControlProperty {

        ControlY2Property(Cell<?> source, Cell<?> target) {
            super(source, target);
        }

        @Override
        public double get() {
            double deltaX = Math.abs(x1.get() - x2.get());
            double deltaY = Math.abs(y1.get() - y2.get());

            if (deltaX < deltaY) {
                return CONTROL_FACTOR * (y1.get() - y2.get()) + y2.get();
            } else {
                return y2.get();
            }
        }
    }
}
