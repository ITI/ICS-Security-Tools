package ui.graphing.physical;

import core.document.graph.INode;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import ui.graphing.Cell;

public class ControlPointCell<TNode extends INode<TNode>> extends Cell<TNode> {
    protected static class CenteredBinding extends ObjectBinding<Point2D> {
        private Node root;

        public CenteredBinding(Node root) {
            super.bind(root.layoutBoundsProperty(), root.layoutXProperty(), root.layoutYProperty());

            this.root = root;
        }

        @Override
        public Point2D computeValue() {
            Bounds bounds = root.getLayoutBounds();
            return new Point2D(root.getLayoutX() + bounds.getWidth() / 2.0, root.getLayoutY() + bounds.getHeight() / 2.0);
        }
    }
    protected static class OffsetBinding extends ObjectBinding<Point2D> {
        private ObservableValue<Point2D> base;
        private ObservableValue<Number> offsetX;
        private ObservableValue<Number> offsetY;

        public OffsetBinding(ObservableValue<Point2D> base, ObservableValue<Number> offsetX, ObservableValue<Number> offsetY) {
            bind(base);
            if(offsetX != null) {
                bind(offsetX);
            }
            if(offsetY != null) {
                bind(offsetY);
            }

            this.base = base;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }

        @Override
        public Point2D computeValue() {
            return base.getValue().add(offsetX == null ? 0.0 : offsetX.getValue().doubleValue(), offsetY == null ? 0.0 : offsetY.getValue().doubleValue());
        }
    }
    public static class XBinding extends DoubleBinding {
        private ObservableValue<Point2D> point;

        public XBinding(ObservableValue<Point2D> point) {
            bind(point);
            this.point = point;
        }

        @Override
        public double computeValue() {
            return point.getValue().getX();
        }
    }
    public static class YBinding extends DoubleBinding {
        private ObservableValue<Point2D> point;

        public YBinding(ObservableValue<Point2D> point) {
            bind(point);
            this.point = point;
        }

        @Override
        public double computeValue() {
            return point.getValue().getY();
        }
    }

    private final SimpleObjectProperty<Point2D> controlPoint;
    protected final CenteredBinding pointCenter;

    public ControlPointCell(TNode node) {
        super(node);

        pointCenter = new CenteredBinding(this);
        controlPoint = new SimpleObjectProperty<>();
        controlPoint.bind(pointCenter);
    }

    public ObjectProperty<Point2D> controlPointProperty() {
        return controlPoint;
    }
}
