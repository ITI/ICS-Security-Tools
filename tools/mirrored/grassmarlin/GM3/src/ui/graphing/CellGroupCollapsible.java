package ui.graphing;

import core.Preferences;
import core.document.graph.IEdge;
import core.document.graph.INode;
import core.document.serialization.xml.XmlElement;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import ui.EmbeddedIcons;
import ui.custom.fx.ListSizeBinding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CellGroupCollapsible<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> extends CellGroup<TNode, TEdge> implements CellGroup.IHasNodesForCellLayer{
    protected class LabelPane extends StackPane implements CellLayer.ICanHazContextMenu {
        @Override
        public List<MenuItem> getContextMenuItems() {
            if(CellGroupCollapsible.this.collapsed.get()) {
                return CellGroupCollapsible.this.getContextMenuItems();
            } else {
                //The context items will be returned by the group layer since it is visible, so we don't need to add a copy.
                return null;
            }
        }
    }

    private final BooleanProperty collapsed;
    private final BooleanProperty showLabel;

    protected final CheckMenuItem miCollapseGroup;
    protected final CheckMenuItem miShowLabel;

    protected final StackPane paneLabel;

    public CellGroupCollapsible(String name, EmbeddedIcons icon, Graph<TNode, TEdge> graph) {
        super(name, graph);

        miCollapseGroup = new CheckMenuItem("Collapse Group");
        miCollapseGroup.setSelected(false);
        miShowLabel = new CheckMenuItem("Show Label when Expanded");
        miShowLabel.setSelected(false);

        contextItems.add(miCollapseGroup);
        contextItems.add(miShowLabel);

        collapsed = miCollapseGroup.selectedProperty();
        collapsed.addListener(this::Handle_CollapsedChanged);

        //Create a node to allow dragging / display the group name when the group is hidden.
        paneLabel = new LabelPane();
        Rectangle bkgLabel = new Rectangle();
        HBox contentLabel = new HBox();
        contentLabel.setPadding(new Insets(2.0, 2.0, 2.0, 2.0));
        if(icon != null) {
            contentLabel.getChildren().add(icon.getImage(16.0));
        }
        Label textLabel = new Label();
        contentLabel.getChildren().add(textLabel);

        textLabel.textProperty().bind(nameProperty().concat(" (").concat(new ListSizeBinding(members)).concat(")"));
        textLabel.textFillProperty().bind(Preferences.VisualizationNodeTextColor);

        bkgLabel.widthProperty().bind(paneLabel.widthProperty());
        bkgLabel.heightProperty().bind(paneLabel.heightProperty());
        bkgLabel.setArcWidth(5.0);
        bkgLabel.setArcHeight(5.0);
        bkgLabel.fillProperty().bind(Preferences.VisualizationNodeBackgroundColor);

        paneLabel.getChildren().addAll(bkgLabel, contentLabel);
        paneLabel.layoutXProperty().bind(centerXProperty().subtract(paneLabel.widthProperty().divide(2.0)));
        paneLabel.layoutYProperty().bind(centerYProperty().subtract(paneLabel.heightProperty().divide(2.0)));
        //Drag group support
        paneLabel.addEventHandler(MouseEvent.MOUSE_PRESSED, this::Handle_RedirectedMouseEvent);
        paneLabel.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::Handle_RedirectedMouseEvent);
        paneLabel.addEventHandler(MouseEvent.MOUSE_RELEASED, this::Handle_RedirectedMouseEvent);
        paneLabel.addEventHandler(MouseEvent.MOUSE_CLICKED, this::Handle_RedirectedMouseEvent);

        paneLabel.visibleProperty().bind(visibleProperty().and(miShowLabel.selectedProperty().or(collapsed)));
        showLabel = miShowLabel.selectedProperty();
    }

    private void Handle_RedirectedMouseEvent(MouseEvent event) {
        this.fireEvent(event.copyFor(event.getSource(), this));
        event.consume();
    }

    protected void Handle_CollapsedChanged(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
        if(newValue) {
            for(Cell<TNode> cell : members) {
                cell.RedirectTo(centerXProperty(), centerYProperty());
            }
        } else {
            for(Cell<TNode> cell : members) {
                cell.RedirectTo(null, null);
            }
        }
        rebuildHull();
    }


    @Override
    public Collection<Node> getCellLayerContents() {
        ArrayList<Node> result = new ArrayList<>(1);
        result.add(paneLabel);
        return result;
    }

    @Override
    protected void memberAdded(Cell<TNode> member) {
        super.memberAdded(member);

        member.hiddenProperty().bind(collapsed);
        if(collapsed.get()) {
            member.RedirectTo(centerXProperty(), centerYProperty());
        } else {
            member.RedirectTo(null, null);
        }
    }
    @Override
    protected void memberRemoved(Cell<TNode> member) {
        super.memberRemoved(member);

        member.hiddenProperty().unbind();
        member.hiddenProperty().set(false);
        member.RedirectTo(null, null);
    }

    public BooleanProperty collapsedProperty() {
        return collapsed;
    }

    public BooleanProperty showLabelProperty() {
        return showLabel;
    }

    @Override
    public XmlElement toXml() {
        XmlElement xmlGroup = super.toXml();
        xmlGroup.addAttribute("collapse").setValue(Boolean.toString(collapsed.get()));
        xmlGroup.addAttribute("showlabel").setValue(Boolean.toString(showLabel.get()));

        return xmlGroup;
    }
}
