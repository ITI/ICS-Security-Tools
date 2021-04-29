package ui.graphing;

import com.sun.javafx.collections.ObservableListWrapper;
import core.document.graph.IEdge;
import core.document.graph.INode;
import core.document.serialization.xml.XmlElement;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.NonInvertibleTransformException;
import ui.custom.fx.ActiveMenuItem;
import ui.dialog.ColorPickerDialogFx;

import java.util.*;
import java.util.stream.Collectors;

public class CellGroup<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> extends Group implements CellLayer.ICanHazContextMenu {
    public interface IHasNodesForCellLayer {
        Collection<Node> getCellLayerContents();
    }

    protected static final ColorPickerDialogFx dialogColorPicker = new ColorPickerDialogFx();

    protected final ObservableList<Cell<TNode>> members;
    protected final Polygon polyHull = new Polygon();
    protected final SimpleDoubleProperty polyCenterX;
    protected final SimpleDoubleProperty polyCenterY;
    protected final SimpleStringProperty name;
    protected final Graph<TNode, TEdge> graph;

    protected final List<MenuItem> contextItems;
    protected final MenuItem miChangeColor;

    private final ChangeListener<? super Number> listener = this::Handle_CoordinateChanged;

    //TODO: Configurable colors for stroke, fill, etc.
    private SimpleBooleanProperty isSelected;
    protected final static double OPACITY = 0.4;
    protected final static double STROKE_WIDTH = 2.0;
    protected final static double PADDING_WIDTH = 4.0;
    protected final static Color COLOR_SELECTED = Color.YELLOW;
    protected final static Color COLOR_DEFAULT = Color.GRAY;
    protected final SimpleObjectProperty<Color> fillColor;

    public CellGroup(String name, Graph<TNode, TEdge> graph) {
        this.members = new ObservableListWrapper<>(new LinkedList<>());
        this.members.addListener(this::Handle_ListChanged);
        this.graph = graph;

        polyCenterX = new SimpleDoubleProperty(0.0);
        polyCenterY = new SimpleDoubleProperty(0.0);

        fillColor = new SimpleObjectProperty<>(Color.TRANSPARENT);
        this.name = new SimpleStringProperty(name);

        initComponents();

        contextItems = new LinkedList<>();
        Rectangle colorPreview = new Rectangle(14.0, 14.0);
        colorPreview.setStrokeWidth(1.0);
        colorPreview.setStroke(Color.BLACK);
        colorPreview.fillProperty().bind(fillColor);
        miChangeColor = new ActiveMenuItem("Change Group Color", colorPreview, event -> {
            Color colorOpaque = Color.color(fillColor.get().getRed(), fillColor.get().getGreen(), fillColor.get().getBlue());
            dialogColorPicker.setColor(colorOpaque);
            Optional<ButtonType> result = dialogColorPicker.showAndWait();
            if(result.isPresent() && result.get().equals(ButtonType.OK)) {
                fillColor.set(dialogColorPicker.getSelectedColor());
            }
        });

        contextItems.add(miChangeColor);
    }

    private void initComponents() {
        this.getChildren().add(polyHull);
        polyHull.setStrokeWidth(STROKE_WIDTH);
        polyHull.setStroke(COLOR_DEFAULT);
        polyHull.setStrokeLineJoin(StrokeLineJoin.ROUND);

        fillColor.addListener((observable, oldValue, newValue) -> {
            //Set the opacity if it doesn't match the set value; shortcut to make setting color to named defaults easy.
            if (newValue.getOpacity() != OPACITY) {
                fillColor.set(Color.color(newValue.getRed(), newValue.getGreen(), newValue.getBlue(), OPACITY));
            }
        });
        fillColor.set(Color.RED);
        polyHull.fillProperty().bind(fillColor);

        //TODO: Entering/Exiting items should also select the group in which they are contained.
        polyHull.setOnMouseEntered(event -> isSelected.set(true));
        polyHull.setOnMouseExited(event -> isSelected.set(false));

        isSelected = new SimpleBooleanProperty(false);
        isSelected.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                polyHull.setStroke(COLOR_SELECTED);
            } else {
                polyHull.setStroke(COLOR_DEFAULT);
            }
        });
    }

    protected void memberAdded(final Cell<TNode> member) {
        member.containerProperty().set(this);

        member.layoutXProperty().addListener(listener);
        member.layoutYProperty().addListener(listener);
        member.widthProperty().addListener(listener);
        member.heightProperty().addListener(listener);
    }
    protected void memberRemoved(final Cell<TNode> member) {
        member.containerProperty().set(null);

        member.layoutXProperty().removeListener(listener);
        member.layoutYProperty().removeListener(listener);
        member.widthProperty().removeListener(listener);
        member.heightProperty().removeListener(listener);
    }
    protected void membersChanged() {
        //If empty, then hide the group.  Otherwise, show it.
        setVisible(!members.isEmpty());
        if (!graph.isLoading()) {
            rebuildHull();
        }
    }

    public void clear() {
        //clearing the members will unbind handlers from all the objects.
        members.clear();
    }

    protected void Handle_ListChanged(ListChangeListener.Change<? extends Cell<TNode>> c) {
        while(c.next()) {
            for(Cell<TNode> member : c.getRemoved()) {
                memberRemoved(member);
            }
            for(Cell<TNode> member : c.getAddedSubList()) {
                memberAdded(member);
            }
        }

        Platform.runLater(() -> {
            membersChanged();
        });
    }

    protected void Handle_CoordinateChanged(ObservableValue<? extends Number> o, Number oldValue, Number newValue) {
        rebuildHull();
    }

    protected static <TNode extends INode<TNode>> List<Double> BuildHullForCells(List<Cell<TNode>> cells, double padding) {
        ArrayList<Point2D> points = new ArrayList<>(cells.size() * 4);
        for(Cell<TNode> cell : cells) {
            points.add(new Point2D(cell.getLayoutX() - padding, cell.getLayoutY() - padding));
            points.add(new Point2D(cell.getLayoutX() + cell.getWidth() + padding, cell.getLayoutY() - padding));
            points.add(new Point2D(cell.getLayoutX() + cell.getWidth() + padding, cell.getLayoutY() + cell.getHeight() + padding));
            points.add(new Point2D(cell.getLayoutX() - padding, cell.getLayoutY() + cell.getHeight() + padding));
        }
        return BuildHullFor(points);
    }
    protected static List<Double> BuildHullFor(List<Point2D> points) {
        Stack<Point2D> ptsPolygon = new Stack<>();
        points = points.stream().filter(pt -> !Double.isNaN(pt.getX()) && !Double.isNaN(pt.getY())).distinct().collect(Collectors.toList());
        // The typical x1 - x2 solution for compare doesn't work since we need to retain the precision but the result has to be an integer.
        // The Y result is inverted because we're not on a Cartesian plane.
        try {
            points.sort((o1, o2) ->
                    (o1.getX() == o2.getX()) ?
                            (o1.getY() == o2.getY() ?
                                    0 :
                                    (o1.getY() > o2.getY() ?
                                            -1 :
                                            1)) :
                            o1.getX() < o2.getX() ? -1 : 1);
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        if(points.isEmpty()) {
            return null;
        }

        //Discard co-linear points.
        for(int idxPoint = 0; idxPoint < points.size(); idxPoint++) {
            while(ptsPolygon.size() >= 2 &&
                    ptsPolygon.get(ptsPolygon.size() - 1).subtract(ptsPolygon.get(ptsPolygon.size() - 2))
                            .crossProduct(
                                    points.get(idxPoint).subtract(ptsPolygon.get(ptsPolygon.size() - 2))).getZ() <= 0) {
                ptsPolygon.pop();
            }
            ptsPolygon.push(points.get(idxPoint));
        }
        for(int idxPoint = points.size() - 2; idxPoint >= 0; idxPoint--) {
            while(ptsPolygon.size() >= 2 && ptsPolygon.get(ptsPolygon.size() - 1).subtract(ptsPolygon.get(ptsPolygon.size() - 2)).crossProduct(points.get(idxPoint).subtract(ptsPolygon.get(ptsPolygon.size() - 2))).getZ() <= 0) {
                ptsPolygon.pop();
            }
            ptsPolygon.push(points.get(idxPoint));
        }
        ptsPolygon.pop();   //Last point will be a duplicate of the first

        List<Double> result = new ArrayList<>(ptsPolygon.size() * 2);
        for(Point2D pt : ptsPolygon) {
            result.add(pt.getX());
            result.add(pt.getY());
        }

        return result;
    }

    /**
     * Due to the complexity of rebuilding the hull, we're not going to bind properties; that would be ridiculous.
     * Instead, we are going to attach listeners to each member and call rebuild on update to either the member list or
     * the layoutX/Y,width,height properties.
     */
    public void rebuildHull() {
        ArrayList<Point2D> ptsSource = new ArrayList<>(members.size() * 4);

        //Monotone Chain algorithm for calculation of a convex hull. Sort time + O(N)
        boolean isAnythingVisible = false;
        for(Cell<TNode> member : members) {
            //If it is "deleted" then skip it altogether.
            if(!member.deletedProperty().get()) {
                //If it is invisible, then don't draw the hull around it but allow updates to the midpoint.
                if(member.isVisible()) {
                    isAnythingVisible = true;
                }
                ptsSource.add(new Point2D(member.getLayoutX() - PADDING_WIDTH, member.getLayoutY() - PADDING_WIDTH));
                ptsSource.add(new Point2D(member.getLayoutX() + member.getWidth() + PADDING_WIDTH, member.getLayoutY() - PADDING_WIDTH));
                ptsSource.add(new Point2D(member.getLayoutX() + member.getWidth() + PADDING_WIDTH, member.getLayoutY() + member.getHeight() + PADDING_WIDTH));
                ptsSource.add(new Point2D(member.getLayoutX() - PADDING_WIDTH, member.getLayoutY() + member.getHeight() + PADDING_WIDTH));
            }
        }

        // If everything is invisible (or there are no members) then hide the polygon.
        // Recalculate as long as we have any points.
        polyHull.setVisible(isAnythingVisible);
        if(ptsSource.isEmpty()) {
            return;
        }

        List<Double> ptsPolygon = BuildHullFor(ptsSource);
        ptsSource = null;
        polyHull.getPoints().clear();
        if(ptsPolygon != null) {
            polyHull.getPoints().addAll(ptsPolygon);
        }

        //Recalculate center
        Bounds bounds = polyHull.getBoundsInLocal();
        centerXProperty().set(bounds.getMinX() + bounds.getWidth() / 2.0);
        centerYProperty().set(bounds.getMinY() + bounds.getHeight() / 2.0);
    }

    public BooleanProperty isSelectedProperty() {
        return isSelected;
    }
    public ObjectProperty<Color> fillColorProperty() {
        return fillColor;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public ObservableList<Cell<TNode>> getMembers() {
        return members;
    }

    public DoubleProperty centerXProperty() {
        return polyCenterX;
    }
    public DoubleProperty centerYProperty() {
        return polyCenterY;
    }

    @Override
    public List<MenuItem> getContextMenuItems() {
        return contextItems;
    }

    public XmlElement toXml() {
        XmlElement xmlGroup = new XmlElement("group");
        xmlGroup.addAttribute("name").setValue(name.get());
        xmlGroup.addAttribute("color").setValue(fillColor.get().toString().substring(2, 8));

        return xmlGroup;
    }

    //<editor-fold defaultstate="collapsed" desc="Drag Support">
    protected Point2D originDrag = null;
    protected double xPrev, yPrev;
    protected boolean isDragged = false;
    @SuppressWarnings("unchecked")
    public void Handle_DragStart(MouseEvent event) {
        if (event.isPrimaryButtonDown() && (event.getSource() instanceof CellGroup)) {
            event.consume();
            isDragged = true;
            try {
                originDrag = this.getLocalToSceneTransform().inverseTransform(event.getSceneX(), event.getSceneY());
            } catch(NonInvertibleTransformException ex) {
                ex.printStackTrace();
                originDrag = new Point2D(event.getSceneX(), event.getSceneY());
            }
            xPrev = 0.0;
            yPrev = 0.0;
        }
    }
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
            this.getMembers().forEach(cell -> {
                cell.autoLayoutProperty().set(false);
                cell.setLayoutX(cell.getLayoutX() - xPrev + ptTranslated.getX());
                cell.setLayoutY(cell.getLayoutY() - yPrev + ptTranslated.getY());
            });
            //To avoid floating point rounding errors we will reverse this translation and apply a new absolute translation next time.
            xPrev = ptTranslated.getX();
            yPrev = ptTranslated.getY();
        } else {
            isDragged = false;
        }
    }
    public void Handle_DragEnd(MouseEvent event) {
        //Releasing any mouse button ends a drag
        isDragged = false;
    }
    public void Handle_DragEnd(KeyEvent event) {
        isDragged = false;
    }
    //</editor-fold>

    @Override
    public String toString() {
        return name.get();
    }
}
