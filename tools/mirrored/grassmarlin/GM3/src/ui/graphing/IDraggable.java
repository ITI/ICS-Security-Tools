package ui.graphing;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.NonInvertibleTransformException;

public interface IDraggable {
    DragContext dragContext = new DragContext();

    default void makeDraggable() {
        if(this instanceof Node) {
            final Node node = (Node)this;
            node.setOnMousePressed(this::Handle_MousePressed);
            node.setOnMouseDragged(this::Handle_MouseDragged);
        }
    }
    default void makeUndraggable() {
        if(this instanceof Node) {
            final Node node = (Node)this;
            node.setOnMousePressed(this::Handle_MouseEventRedirectToParent);
            node.setOnMouseDragged(this::Handle_MouseEventRedirectToParent);
        }
    }

    default void Handle_MousePressed(MouseEvent event) {
        if(event.isPrimaryButtonDown()) {
            if(event.getSource() instanceof Cell) {
                event.consume();
                try {
                    // The cell applies a translation to center itself so that the code laying out the cell does not
                    //need to compensate for centering.
                    // Because of this, we can't use the Cell's transforms, but must use the parent's transforms.
                    // This is relatively obvious (in hindsight) but is a deviation from the group-dragging code in
                    //CellGroup where we can use the group's transforms, since no transforms are applied to the group.
                    dragContext.ptOrigin = ((Node)this).getParent().getLocalToSceneTransform().inverseTransform(event.getSceneX(), event.getSceneY());
                } catch (NonInvertibleTransformException ex) {
                    ex.printStackTrace();
                    dragContext.ptOrigin = new Point2D(event.getSceneX(), event.getSceneY());
                }
                dragContext.ptPrevious = new Point2D(0, 0);
            }
        }
    }
    default void Handle_MouseEventRedirectToParent(MouseEvent event) {
        if(event.isPrimaryButtonDown()) {
            //Redirect to the containing group.
            if(event.getSource() instanceof Cell) {
                Cell cellSource = (Cell)event.getSource();
                if(cellSource.containerProperty().get() instanceof Node) {
                    ((Node)cellSource.containerProperty().get()).fireEvent(event);
                    event.consume();
                }
            }
        }
    }

    default void Handle_MouseDragged(MouseEvent event) {
        //The primary button has to be down
        //The drag target only has to match if we are going to process the drag; if dragging is disabled then that check will be handled elsewhere.
        if(event.isPrimaryButtonDown() && (event.getSource() == this)) {
            event.consume();
            if(this instanceof Cell) {
                ((Cell<?>)this).autoLayoutProperty().set(false);
            }

            Point2D ptTemp = new Point2D(event.getSceneX(), event.getSceneY());
            try {
                ptTemp = ((Node)this).getParent().getLocalToSceneTransform().inverseTransform(ptTemp);

            } catch(NonInvertibleTransformException ex) {
                ex.printStackTrace();
                //we just won't be able to account for the translation.  There may be some distortion, but it will still work.
            }
            final Point2D ptTranslated = ptTemp.subtract(dragContext.ptOrigin);

            ((Node)this).setLayoutX(((Node)this).getLayoutX() - dragContext.ptPrevious.getX() + ptTranslated.getX());
            ((Node)this).setLayoutY(((Node)this).getLayoutY() - dragContext.ptPrevious.getY() + ptTranslated.getY());

            //To avoid floating point rounding errors we will reverse this translation and apply a new absolute translation next time.
            dragContext.ptPrevious = ptTranslated;
        }
    }

    class DragContext {
        Point2D ptOrigin;
        Point2D ptPrevious;
    }
}
