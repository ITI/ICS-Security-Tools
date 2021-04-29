package ui.graphing;

import core.Preferences;
import core.document.graph.INode;
import core.document.serialization.xml.XmlElement;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class Cell<TNode extends INode<TNode>> extends Pane implements CellLayer.ICanHazContextMenu, IDraggable {
    private final SimpleObjectProperty<CellGroup> container;

    private final List<Edge<TNode>> edges = new ArrayList<>();

    //Coordinates to be used for edge endpoints.
    protected final ReadOnlyDoubleWrapper edgeX;
    protected final ReadOnlyDoubleWrapper edgeY;
    private final SimpleDoubleProperty edgeRedirectX;
    private final SimpleDoubleProperty edgeRedirectY;

    private final BooleanProperty selected;
    private final SimpleBooleanProperty deleted;
    private final SimpleBooleanProperty hidden;
    protected final TNode node;

    protected final List<MenuItem> menuItems;
    protected final CheckMenuItem miAutoLayout;

    protected final Timeline timelineHighlightModified;
    protected final Rectangle polyHighlight;
    protected final Rectangle bkgInternal;

    //Containers that might be used by subclasses.
    protected final HBox boxExternalImages;
    protected final HBox boxInternalImages;
    protected final HBox boxInternalContent;
    protected final StackPane paneInternalContent;

    public Cell(TNode node) {
        this.container = new SimpleObjectProperty<>(null);
        this.node = node;
        deleted = new SimpleBooleanProperty(false);
        hidden = new SimpleBooleanProperty(false);
        visibleProperty().bind(deleted.not().and(hidden.not()));

        selected = new SimpleBooleanProperty(false);
        menuItems = new LinkedList<>();
        miAutoLayout = new CheckMenuItem("Allow Auto-Layout");
        miAutoLayout.setSelected(true);
        menuItems.add(miAutoLayout);

        //Edges will be drawn to the center of the node.
        edgeX = new ReadOnlyDoubleWrapper();
        edgeX.bind(
                this.layoutXProperty().add(
                        this.widthProperty().divide(2.0)
                ));
        edgeY = new ReadOnlyDoubleWrapper();
        edgeY.bind(
                this.layoutYProperty().add(
                        this.heightProperty().divide(2.0)
                ));
        edgeRedirectX = new SimpleDoubleProperty();
        edgeRedirectY = new SimpleDoubleProperty();
        edgeRedirectX.bind(edgeX);
        edgeRedirectY.bind(edgeY);

        Label lblTest = new Label();
        lblTest.textProperty().bind(node.titleProperty());
        lblTest.textFillProperty().bind(Preferences.VisualizationNodeTextColor);

        // The root node is a StackPane so we can put the highlight over the entire node (and sized to match)
        StackPane paneRoot = new StackPane();
        //The highlight polygon; it will be animated to fade to transparent.
        polyHighlight = new Rectangle();
        polyHighlight.widthProperty().bind(paneRoot.widthProperty());
        polyHighlight.heightProperty().bind(paneRoot.heightProperty());
        polyHighlight.fillProperty().bind(Preferences.NodeNewColor);

        //boxRoot contains the external images and internal content
        HBox boxRoot = new HBox();
        boxExternalImages = new HBox();
        boxExternalImages.setPadding(new Insets(1.0, 2.0, 1.0, 0.0));
        boxExternalImages.setAlignment(Pos.TOP_RIGHT);
        //The internal content has the background and contains internal images and a title.
        paneInternalContent = new StackPane();
        paneInternalContent.setPadding(new Insets(1.0, 3.0, 1.0, 1.0));
        boxInternalContent = new HBox();
        bkgInternal = new Rectangle();
        bkgInternal.widthProperty().bind(paneInternalContent.widthProperty());
        bkgInternal.heightProperty().bind(paneInternalContent.heightProperty());
        bkgInternal.setArcWidth(5.0);
        bkgInternal.setArcHeight(5.0);
        bkgInternal.fillProperty().bind(Preferences.VisualizationNodeBackgroundColor);
        boxInternalImages = new HBox();
        boxInternalImages.setPadding(new Insets(0.0, 2.0, 0.0, 2.0));

        paneInternalContent.getChildren().addAll(bkgInternal, boxInternalContent);

        boxInternalImages.setAlignment(Pos.TOP_RIGHT);
        VBox boxTextData = new VBox();
        boxTextData.setPadding(new Insets(0.0, 2.0, 0.0, 2.0));
        boxInternalContent.getChildren().addAll(boxInternalImages, boxTextData);
        boxTextData.getChildren().addAll(lblTest);
        boxRoot.getChildren().addAll(boxExternalImages, paneInternalContent);

        paneRoot.getChildren().addAll(boxRoot, polyHighlight);
        this.getChildren().addAll(paneRoot);

        //Initially, we use this timeline to fade from "new" to "default", but when we activate it in the future, it will instead play "modified" to "default"
        timelineHighlightModified = new Timeline();
        timelineHighlightModified.getKeyFrames().addAll(
                new KeyFrame(Duration.seconds(1.0), new KeyValue(polyHighlight.opacityProperty(), 0.0))
        );
        timelineHighlightModified.play();


        selected.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                bkgInternal.fillProperty().bind(Preferences.VisualizationNodeSelectedBackgroundColor);
            } else {
                bkgInternal.fillProperty().bind(Preferences.VisualizationNodeBackgroundColor);
            }
        });

        this.addEventHandler(MouseEvent.MOUSE_CLICKED, this::Handler_MouseClick);
        this.makeDraggable();

        //Bind visual events to invalidation of save state.
        this.layoutXProperty().addListener(this::Handle_Invalidation);
        this.layoutYProperty().addListener(this::Handle_Invalidation);
    }

    protected void Handle_Invalidation(Observable source) {
        this.node.dirtyProperty().set(true);
    }

    public BooleanProperty deletedProperty() {
        return deleted;
    }
    public BooleanProperty hiddenProperty() {
        return hidden;
    }

    public void HighlightForChange() {
        polyHighlight.fillProperty().bind(Preferences.NodeModifiedColor);
        polyHighlight.setOpacity(1.0);
        timelineHighlightModified.playFromStart();

    }

    protected void Handler_MouseClick(MouseEvent event) {
        if(event.getClickCount() == 1 && event.getButton() == MouseButton.PRIMARY) {
            //Allow selection/highlight to be removed, but don't allow it to be set (this looks odd)
            selected.set(false);
            event.consume();
        }
    }

    public void addEdge(Edge<TNode> edge) {
        edges.add(edge);
    }
    public List<Edge<TNode>> getEdges() {
        return edges;
    }

    public DoubleProperty edgeXProperty() {
        return edgeRedirectX;
    }
    public DoubleProperty edgeYProperty() {
        return edgeRedirectY;
    }
    public void RedirectTo(DoubleProperty x, DoubleProperty y) {
        if(x == null || y == null) {
            edgeRedirectX.bind(edgeX);
            edgeRedirectY.bind(edgeY);
        } else {
            edgeRedirectX.bind(x);
            edgeRedirectY.bind(y);
        }
    }

    public BooleanProperty autoLayoutProperty() {
        return miAutoLayout.selectedProperty();
    }
    public BooleanProperty selectedProperty() {
        return selected;
    }

    @Override
    public List<MenuItem> getContextMenuItems() {
        ArrayList<MenuItem> result = new ArrayList<>(menuItems);

        for(Edge edge : this.getEdges()) {
            if(edge instanceof CellLayer.ICanHazContextMenu) {
                if(!result.isEmpty()) {
                    result.add(new SeparatorMenuItem());
                }
                result.addAll( ((CellLayer.ICanHazContextMenu)edge).getContextMenuItems() );
            }
        }

        return result;
    }

    public void addContextMenuItem(MenuItem item) {
        menuItems.add(item);
    }
    public void addContextMenuItems(Collection<MenuItem> items) {
        menuItems.addAll(items);
    }

    public TNode getNode() {
        return node;
    }

    public ObjectProperty<CellGroup> containerProperty() {
        return container;
    }

    public XmlElement toXml(int ref) {
        XmlElement cell = new XmlElement("cell");

        cell.addAttribute("y").setValue(Double.toString(getLayoutY()));
        cell.addAttribute("x").setValue(Double.toString(getLayoutX()));
        cell.addAttribute("ref").setValue(Integer.toString(ref));
        cell.addAttribute("autoLayout").setValue(Boolean.toString(autoLayoutProperty().get()));

        return cell;
    }

    @Override
    public String toString() {
        return this.node.titleProperty().get();
    }
}
