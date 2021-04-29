package ui.graphing.physical;

import core.document.graph.IEdge;
import core.document.graph.INode;
import core.document.serialization.xml.XmlElement;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.NonInvertibleTransformException;
import ui.EmbeddedIcons;
import ui.graphing.Cell;
import ui.graphing.CellGroupCollapsible;
import ui.graphing.Graph;

public abstract class LayoutManagedGroup<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> extends CellGroupCollapsible<TNode, TEdge> {
    private final SimpleDoubleProperty offsetX;
    private final SimpleDoubleProperty offsetY;

    private final SimpleBooleanProperty rectangularHull;

    public LayoutManagedGroup(String name, EmbeddedIcons icon, Graph<TNode, TEdge> graph) {
        super(name, icon, graph);

        offsetX = new SimpleDoubleProperty(0.0);
        offsetY = new SimpleDoubleProperty(0.0);

        rectangularHull = new SimpleBooleanProperty(true);

        offsetXProperty().addListener(this::Handle_Moved);
        offsetYProperty().addListener(this::Handle_Moved);

        contextItems.remove(miShowLabel);
    }

    protected void Handle_Moved(Observable value) {
        rebuildHull();
    }

    public DoubleProperty offsetXProperty() {
        return offsetX;
    }
    public DoubleProperty offsetYProperty() {
        return offsetY;
    }

    public BooleanProperty rectangularHullProperty() {
        return rectangularHull;
    }

    @Override
    public void Handle_Drag(MouseEvent event) {
        if (event.isPrimaryButtonDown() && isDragged) {
            event.consume();

            Point2D ptTemp = new Point2D(event.getSceneX(), event.getSceneY());
            try {
                ptTemp = this.getLocalToSceneTransform().inverseTransform(ptTemp);
            } catch(NonInvertibleTransformException ex) {
                ex.printStackTrace();
                //we just won't be able to account for the translation.  There may be some distortion, but it will still work.
            }
            final Point2D ptTranslated = ptTemp.subtract(originDrag);

            // We change the offset on the group rather than reposition the elements.
            offsetXProperty().set(offsetXProperty().get() - xPrev + ptTranslated.getX());
            offsetYProperty().set(offsetYProperty().get() - yPrev + ptTranslated.getY());

            //To avoid floating point rounding errors we will reverse this translation and apply a new absolute translation next time.
            xPrev = ptTranslated.getX();
            yPrev = ptTranslated.getY();
        } else {
            isDragged = false;
        }
    }

    @Override
    public void rebuildHull() {
        if(rectangularHull.get()) {
            buildRectangleHull();
        } else {
            super.rebuildHull();
        }
    }

    protected void buildRectangleHull() {
        double xMin = Double.MAX_VALUE;
        double xMax = -Double.MAX_VALUE;
        double yMin = Double.MAX_VALUE;
        double yMax = -Double.MAX_VALUE;

        boolean isAnythingVisible = false;
        for (Cell member : members) {
            if (member.isVisible()) {
                isAnythingVisible = true;
            }

            xMin = Math.min(xMin, member.getLayoutX() - PADDING_WIDTH);
            xMax = Math.max(xMax, member.getLayoutX() + member.getWidth() + PADDING_WIDTH);
            yMin = Math.min(yMin, member.getLayoutY() - PADDING_WIDTH);
            yMax = Math.max(yMax, member.getLayoutY() + member.getHeight() + PADDING_WIDTH);
        }

        // If everything is invisible (or there are no members) then hide the polygon.
        // We still need to recalculate the center.
        polyHull.setVisible(isAnythingVisible);

        polyHull.getPoints().clear();
        polyHull.getPoints().addAll(
                xMin, yMin,
                xMax, yMin,
                xMax, yMax,
                xMin, yMax
        );

        //Recalculate center
        centerXProperty().set((xMin + xMax) / 2.0);
        centerYProperty().set((yMin + yMax) / 2.0);
    }

    @Override
    public XmlElement toXml() {
        XmlElement xmlGroup = super.toXml();
        xmlGroup.addAttribute("x").setValue(Double.toString(offsetX.get()));
        xmlGroup.addAttribute("y").setValue(Double.toString(offsetY.get()));
        xmlGroup.addAttribute("rect").setValue(Boolean.toString(rectangularHull.get()));

        return xmlGroup;
    }
}
