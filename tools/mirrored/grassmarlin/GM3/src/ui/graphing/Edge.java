package ui.graphing;

import core.document.graph.INode;
import core.document.serialization.xml.XmlElement;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import ui.custom.fx.FxStringProperty;

public class Edge<TNode extends INode<TNode>> extends Group {
    private Cell<TNode> source;
    private Cell<TNode> target;
    protected Text details = null;

    protected final Line connectionLinear;
    protected final DoubleProperty edgeThickness;

    private final SimpleBooleanProperty deleted;
    private final SimpleBooleanProperty hidden;

    public Edge(Cell<TNode> source, Cell<TNode> target) {
        this.edgeThickness = new SimpleDoubleProperty(1.0);

        this.source = source;
        this.target = target;

        source.addEdge(this);
        target.addEdge(this);

        deleted = new SimpleBooleanProperty(false);
        hidden = new SimpleBooleanProperty(false);
        visibleProperty().bind(deleted.not().and(hidden.not()));

        connectionLinear = new Line();
        connectionLinear.startXProperty().bind(source.edgeXProperty());
        connectionLinear.startYProperty().bind(source.edgeYProperty());
        connectionLinear.endXProperty().bind(target.edgeXProperty());
        connectionLinear.endYProperty().bind(target.edgeYProperty());

        connectionLinear.strokeWidthProperty().bind(edgeThickness);
        connectionLinear.setStroke(Color.BLACK);

        getChildren().addAll(connectionLinear);
    }

    public void remapCells(Cell<TNode> oldEndpoint, Cell<TNode> newEndpoint) {
        if(source == oldEndpoint) {
            source = newEndpoint;
        } else {
            target = newEndpoint;
        }
        connectionLinear.startXProperty().bind(source.edgeXProperty());
        connectionLinear.startYProperty().bind(source.edgeYProperty());
        connectionLinear.endXProperty().bind(target.edgeXProperty());
        connectionLinear.endYProperty().bind(target.edgeYProperty());
    }

    public BooleanProperty deletedProperty() {
        return deleted;
    }
    public BooleanProperty hiddenProperty() {
        return hidden;
    }
    public DoubleProperty weightProperty() {
        return edgeThickness;
    }

    public void setDetails(FxStringProperty details) {
        if(this.details == null) {
            this.details = new Text();
            this.details.xProperty().bind(connectionLinear.startXProperty().add(connectionLinear.endXProperty()).divide(2.0));
            this.details.yProperty().bind(connectionLinear.startYProperty().add(connectionLinear.endYProperty()).divide(2.0));
            this.getChildren().add(this.details);
        }
        this.details.textProperty().bind(details);
    }

    public Cell<TNode> getSource() {
        return this.source;
    }
    public Cell<TNode> getTarget() {
        return this.target;
    }

    public XmlElement toXml(int index) {
        XmlElement edge = new XmlElement("edge");

        edge.addAttribute("ref").setValue(Integer.toString(index));

        return edge;
    }
}
