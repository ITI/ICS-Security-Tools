package ui.graphing;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import ui.EmbeddedIcons;
import ui.custom.fx.ActiveMenuItem;

import java.util.ArrayList;
import java.util.List;

/**
 * The ZoomableScrollPane allows for panning and zooming controls to modify the display of a group of nodes.
 * As this is the top-most UI element within the graphical depiction of a Graph, it is also responsible for the display
 * of context menus.
 * The node and edge factories may provide menu items to modify the properties of nodes and edges they have created
 * (e.g. curves vs. straight lines).
 * Content items may also provide either their own context menu to replace the ZoomableScrollPane's, or may provide a
 * list of menuItems to setCellContextItems to be added.
 */
public class ZoomableScrollPane extends Pane {
    @FunctionalInterface
    public interface FnGetContextItems {
        List<List<MenuItem>> getContextMenuItems();
    }

    private static class DragContext {
        double x;
        double y;
    }

    protected final Group zoomGroup;
    private Scale scaleTransform;
    private Translate translateTransform;

    private final double scaleFactor = 1.1;
    //Track the current scale as an integer to avoid precision-related errors when zooming in and out.
    //The scaling is computed as getScaleValue() := Math.pow(scaleFactor, scaleLevel)
    private int scaleLevel = 0;
    private final int scaleMin = -48;   // 1.1^-48  ~= 0.01x
    private final int scaleMax = 48;    // 1.1^48   ~= 100x

    private final ContextMenu menuVisualization;
    private final CheckMenuItem miZoomAfterLayout;

    private final FnGetContextItems contextItemFactory;


    private List<MenuItem> paneContextItems;
    private final MenuItem miFitToWindow;

    final DragContext dragContext = new DragContext();

    public ZoomableScrollPane(FnGetContextItems contextItemFactory) {
        this.contextItemFactory = contextItemFactory;

        menuVisualization = new ContextMenu();
        menuVisualization.setAutoHide(true);
        paneContextItems = new ArrayList<>(5);
        miFitToWindow = new ActiveMenuItem("Fit to Window", event -> {
            ZoomableScrollPane.this.scaleToWindow();
        });
        paneContextItems.add(
                new ActiveMenuItem("Reset View", event -> {
                    ZoomableScrollPane.this.zoomReset();
                }));
        paneContextItems.add(
                new ActiveMenuItem("Zoom to Fit", event -> {
                    ZoomableScrollPane.this.zoomToFit();
                }));
        paneContextItems.add(
                miFitToWindow);
        paneContextItems.add(
                new ActiveMenuItem("Zoom In", EmbeddedIcons.Vista_ZoomIn, event -> {
                    ZoomableScrollPane.this.zoomTo(scaleLevel + 1);
                }));
        paneContextItems.add(
                new ActiveMenuItem("Zoom Out", EmbeddedIcons.Vista_ZoomOut, event -> {
                    ZoomableScrollPane.this.zoomTo(scaleLevel - 1);
                }));
        miZoomAfterLayout = new CheckMenuItem("Zoom to Fit After Node Added");
        miZoomAfterLayout.setSelected(true);
        paneContextItems.add(miZoomAfterLayout);

        menuVisualization.setOnShowing(event -> {
            //We know the paneContextItems are non-empty, so we don't need null/blank checks.
            menuVisualization.getItems().addAll(paneContextItems);

            for(List<MenuItem> items : this.contextItemFactory.getContextMenuItems()) {
                if(items != null && items.size() > 0) {
                    menuVisualization.getItems().add(new SeparatorMenuItem());
                    menuVisualization.getItems().addAll(items);
                }
            }
        });
        menuVisualization.setOnHiding(event -> {
            //Reset for next showing.
            menuVisualization.getItems().clear();
        });


        //This object is responsible for handling panning operations, and does so by manipulating translateTransform.
        this.setOnMousePressed(this::Handle_MousePressed);
        this.setOnMouseDragged(this::Handle_MouseDragged);
        this.setOnScroll(this::Handle_MouseWheelZoom);

        scaleTransform = new Scale(getScaleValue(), getScaleValue(), 0, 0);
        translateTransform = new Translate();
        //Modify the content so that it can handle zooming and panning.
        zoomGroup = new Group();
        zoomGroup.getTransforms().addAll(translateTransform, scaleTransform);

        getChildren().add(zoomGroup);
    }

    public void setCanvas(Group canvas) {
        zoomGroup.getChildren().clear();
        zoomGroup.getChildren().add(canvas);
    }

    public void allowCellMovement(boolean value) {
        miFitToWindow.setVisible(value);
    }

    public BooleanProperty zoomAfterLayoutProperty() {
        return miZoomAfterLayout.selectedProperty();
    }
    public boolean getZoomAfterLayout() {
        return miZoomAfterLayout.isSelected();
    }
    public void setZoomAfterLayout(boolean zoom) {
        miZoomAfterLayout.setSelected(zoom);
    }

    private boolean isBeingDragged = false;
    protected void Handle_MousePressed(MouseEvent event) {
        isBeingDragged = false;
        if(event.isPrimaryButtonDown()) {
            event.consume(); //Tastes like delicious, imaginary, deceitful cake.
            dragContext.x = translateTransform.getX() - event.getScreenX();
            dragContext.y = translateTransform.getY() - event.getScreenY();
            isBeingDragged = true;
        } else if(event.isSecondaryButtonDown()) {
            //By using the window variant, auto-close will apply to any loss of focus.
            //If we just pass this, clicking elsewhere in the ZoomableScrollPane will leave the menu open.
            menuVisualization.show(this.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        }
    }
    protected void Handle_MouseDragged(MouseEvent event) {
        if(event.isPrimaryButtonDown() && !event.isShiftDown() && isBeingDragged) {
            event.consume();
            // X,Y += The screen delta between the click and current
            translateTransform.setX(event.getScreenX() + dragContext.x);
            translateTransform.setY(event.getScreenY() + dragContext.y);
        } else {
            isBeingDragged = false;
        }
    }

    public int getScaleLevel(double Value) {
        //Floor and cast-to-int are not identical, as cast-to-int rounds closer to 0, which matters when dealing with negative values.
        return (int)Math.floor(Math.log(Value) / Math.log(scaleFactor));
    }
    public double getScaleValue(int Level) {
        return Math.pow(this.scaleFactor, (double)Level);
    }
    public double getScaleValue() {
        return Math.pow(this.scaleFactor, (double)this.scaleLevel);
    }

    public void centerOn(Cell target) {
        centerOn(target.getLayoutX() + target.getWidth() / 2.0, target.getLayoutY() + target.getHeight() / 2.0);
    }
    public void centerOn(double X, double Y) {
        // Translate to inverse of point on which it should focus
        // Since translate defines top-left, we want to take (point - half_screen), the additive inverse of which is (half_screen - point).
        translateTransform.setX((getWidth() / 2.0) - (X * getScaleValue()));
        translateTransform.setY((getHeight() / 2.0) - (Y * getScaleValue()));
    }
    public void zoomReset() {
        // Zoom to 1:1 and put 0,0 in the top left (default zoom and position)
        scaleLevel = 0;
        scaleTransform.setX(1.0);
        scaleTransform.setY(1.0);
        translateTransform.setX(0.0);
        translateTransform.setY(0.0);
    }
    public void zoomTo(int scaleLevel) {
        //Zoom around the center of the display.
        Bounds bounds = getBoundsInLocal();
        this.zoomTo(scaleLevel, bounds.getWidth() / 2.0, bounds.getHeight() / 2.0);
    }
    public void zoomTo(int scaleLevel, Point2D ptViewport) {
        zoomTo(scaleLevel, ptViewport.getX(), ptViewport.getY());
    }
    public void zoomTo(int scaleLevel, double viewportX, double viewportY) {
        try {
            //Clamp the scaleLevel to the permitted range.
            if(scaleLevel < scaleMin) {
                scaleLevel = scaleMin;
            } else if(scaleLevel > scaleMax) {
                scaleLevel = scaleMax;
            }

            // Viewport coordinates are screen coordinates relative to the top left of the ZoomableScrollPane
            // Each viewport coordinate can be translated to a world coordinate, which is the coordinate system used by the
            // visual elements; this is done by performing the transforms that are applied to zoomPane to the given viewport
            // point.
            Point2D worldCursorPre = scaleTransform.inverseTransform(translateTransform.inverseTransform(viewportX, viewportY));

            this.scaleLevel = scaleLevel;
            scaleTransform.setX(getScaleValue());
            scaleTransform.setY(getScaleValue());

            // Since we want the viewportX,Y point to remain unchanged, find the point under that location and update the
            // translate transformation by the difference.
            Point2D worldCursorPost = scaleTransform.inverseTransform(translateTransform.inverseTransform(viewportX, viewportY));

            translateTransform.setX(translateTransform.getX() + getScaleValue() * (worldCursorPost.getX() - worldCursorPre.getX()));
            translateTransform.setY(translateTransform.getY() + getScaleValue() * (worldCursorPost.getY() - worldCursorPre.getY()));
        } catch(NonInvertibleTransformException wontHappen) {
            // scaleTransform.inverseTransform might throw a NonInvertibleTransformException, but only does so if you
            //apply an inverse 3d transform to a 2d point.
        }

    }
    protected static class Point {
        public double x;
        public double y;

        public Point(double val) {
            x = val;
            y = val;
        }
    }
    protected Bounds calculateCellBounds() {
        /*HACK: zoomGroup contains the content Group which was passed to the constructor; this is internal and an
        acceptable cast... However, we need to find the cells, which happen to be on the third child (#2) of that group.
         */
        List<Node> cells = ((CellLayer)((Group)zoomGroup.getChildren().get(0)).getChildren().get(2)).getChildren();
        if(cells.isEmpty()) {
            return new BoundingBox(0, 0, 0, 0);
        } else {
            Point ptTopLeft = new Point(Double.MAX_VALUE);
            Point ptBottomRight = new Point(-Double.MAX_VALUE);

            cells.stream().filter(node -> node instanceof Cell).forEach(node -> {
                Cell cell = (Cell) node;
                if(!Double.isNaN(cell.getLayoutX())) {
                    ptTopLeft.x = Double.min(ptTopLeft.x, cell.getLayoutX());
                    if(!Double.isNaN(cell.getWidth())) {
                        ptBottomRight.x = Double.max(ptBottomRight.x, cell.getLayoutX() + cell.getWidth());
                    }
                }
                if(!Double.isNaN(cell.getLayoutY())) {
                    ptTopLeft.y = Double.min(ptTopLeft.y, cell.getLayoutY());
                    if(!Double.isNaN(cell.getHeight())) {
                        ptBottomRight.y = Double.max(ptBottomRight.y, cell.getLayoutY() + cell.getHeight());
                    }
                }
            });

            //If we had no cells pass the check, or there was no valid value for one of the bounds use default zoom.
            if(ptTopLeft.x == Double.MAX_VALUE || ptTopLeft.y == Double.MAX_VALUE || ptBottomRight.x == -Double.MAX_VALUE || ptBottomRight.x == -Double.MAX_VALUE) {
                return new BoundingBox(0, 0, 0, 0);
            }
            //Box is specified as (x,y,width,height)
            return new BoundingBox(ptTopLeft.x, ptTopLeft.y, ptBottomRight.x - ptTopLeft.x, ptBottomRight.y - ptTopLeft.y);
        }
    }

    /**
     * Like zoomToFit, this fills the screen with the content, but does so by repositioning the content rather than
     * changing the zoom and translation transforms.  This results in nodes remaining unscaled and moving closer or
     * farther from each other permitting, for example, more room for details to be displayed without overlapping.
     *
     * This will distort the graph as the X and Y axes are scaled independently.
     *
     * There is some error but the result is close enough that attention is being spent elsewhere.
     */
    public void scaleToWindow() {
        Bounds boundsWorld = calculateCellBounds();

        if(boundsWorld.getWidth() == 0.0 && boundsWorld.getHeight() == 0.0) {
            zoomReset();
        } else {
            //Get the (translated and scaled) coordinates for the top left and width/height
            Point2D ptTopLeft = new Point2D(
                    -translateTransform.getX() / scaleTransform.getX(),
                    -translateTransform.getY() / scaleTransform.getY()
            );
            double width = getWidth() / scaleTransform.getX();
            double height = getHeight() / scaleTransform.getY();

            //HACK: We know the first child of the zoomGroup is the canvas.
            Canvas canvas = (Canvas)zoomGroup.getChildren().get(0);
            CellLayer cells = canvas.getCellLayer();

            for(Node node : cells.getChildren()) {
                if(node instanceof Cell) {
                    Cell cell = (Cell)node;

                    // Start from the top left, normalized against the bounds.
                    double x = cell.getLayoutX() - boundsWorld.getMinX();
                    double y = cell.getLayoutY() - boundsWorld.getMinY();
                    x /= boundsWorld.getWidth();
                    y /= boundsWorld.getHeight();
                    // Rescale to viewport
                    x *= width;
                    y *= height;
                    //Set scaled value shifted by viewport location
                    cell.setLayoutX(x + ptTopLeft.getX());
                    cell.setLayoutY(y + ptTopLeft.getY());
                }
            }
        }
    }
    public void zoomToFit() {
        // We will use getWidth() and getHeight() to find the screen size of the viewport
        // Get the size of the content (in world units)  We need this solution to avoid forcing the inclusion of (0,0) in the bounds.
        Bounds boundsWorld = calculateCellBounds();

        //If there is no content, then width and height will both be 0.
        if(boundsWorld.getWidth() == 0.0 && boundsWorld.getHeight() == 0.0) {
            zoomReset();
        } else {
            // Set the scaling factor to the ratio of the above 2 items
            //Neither width nor height of boundsWorld should be 0, but we'll play it safe because it costs so little to double check.
            double scaleX = getScaleValue(scaleMax);
            if (boundsWorld.getWidth() > 0.0) {
                scaleX = getWidth() / boundsWorld.getWidth();
            }
            double scaleY = getScaleValue(scaleMax);
            if (boundsWorld.getHeight() > 0.0) {
                scaleY = getHeight() / boundsWorld.getHeight();
            }
            int scaleNew = getScaleLevel(Math.min(scaleX, scaleY));

            zoomTo(scaleNew);
            //Translate the newly-scaled graph so that it is (roughly) centered.
            //Top left is the origin, so we want the inverse of the top left of the content, then shift by half the difference in width/height.
            //TODO: Insert joke at the expense of Node.js about using a 3rd party library for a centering algorithm
            translateTransform.setX(-getScaleValue() * boundsWorld.getMinX() + (getWidth() - (boundsWorld.getWidth() * getScaleValue())) / 2.0);
            translateTransform.setY(-getScaleValue() * boundsWorld.getMinY() + (getHeight() - (boundsWorld.getHeight() * getScaleValue())) / 2.0);
        }
    }

    protected void Handle_MouseWheelZoom(ScrollEvent scrollEvent) {
        int scaleNew = scaleLevel;
        double delta = scrollEvent.getDeltaY();
        //Shift+scroll scrolls X instead of Y.
        if(scrollEvent.isShiftDown()) {
            delta = scrollEvent.getDeltaX();
        }

        if (delta < 0) {
            scaleNew--;
        } else if(delta > 0){
            scaleNew++;
        }
        Point2D ptEvent = this.screenToLocal(scrollEvent.getScreenX(), scrollEvent.getScreenY());
        zoomTo(scaleNew, ptEvent);
        scrollEvent.consume();
    }
}
