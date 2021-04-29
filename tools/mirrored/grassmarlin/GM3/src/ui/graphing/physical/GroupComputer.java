package ui.graphing.physical;

import core.document.graph.PhysicalEdge;
import core.document.graph.PhysicalNode;
import core.document.serialization.xml.XmlElement;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.paint.Color;
import ui.EmbeddedIcons;
import ui.custom.fx.ActiveMenuItem;
import ui.graphing.Cell;
import ui.graphing.Graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GroupComputer extends LayoutManagedGroup<PhysicalNode, PhysicalEdge> {
    private final Label lblWorkstationName;

    public GroupComputer(String name, Graph<PhysicalNode, PhysicalEdge> owner) {
        super(name, EmbeddedIcons.Vista_Computer, owner);

        super.fillColor.set(Color.BLUE);

        lblWorkstationName = new Label(name);
        lblWorkstationName.layoutXProperty().bind(offsetXProperty());
        lblWorkstationName.layoutYProperty().bind(offsetYProperty());
        lblWorkstationName.visibleProperty().bind(miCollapseGroup.selectedProperty().not());
        getChildren().add(lblWorkstationName);

        miChangeColor.setText("Change Workstation Color");
        miCollapseGroup.setText("Collapse Device");
        final MenuItem miRenameWorkstation = new ActiveMenuItem("Rename Computer", evt -> {
            //TODO: Display a better dialog to get the new workstation name.
            TextInputDialog dlgRename = new TextInputDialog(nameProperty().get());
            dlgRename.setTitle("Rename Computer");
            dlgRename.setContentText("Rename " + nameProperty().get() + " to: ");
            Optional<String> result = dlgRename.showAndWait();
            if(result.isPresent() && result.get() != null && !result.get().equals(nameProperty().get())) {
                //clear before adding so as to avoid any multi-parent conflicts.
                List<Cell<PhysicalNode>> tempMembers = new ArrayList<>(getMembers());
                getMembers().clear();
                for(Cell<PhysicalNode> member : tempMembers) {
                    if(member instanceof CellNic) {
                        ((CellNic)member).getNic().deviceProperty().set(result.get());
                        graph.reprocessNode(member.getNode());
                    }
                }
            }
        });
        contextItems.add(miRenameWorkstation);
    }

    public StringProperty workstationNameProperty() {
        return lblWorkstationName.textProperty();
    }


    @Override
    protected void memberAdded(final Cell<PhysicalNode> member) {
        super.memberAdded(member);
    }
    @Override
    protected void memberRemoved(final Cell<PhysicalNode> member) {
        member.layoutXProperty().unbind();
        member.layoutYProperty().unbind();

        super.memberRemoved(member);
    }
    @Override
    protected void membersChanged() {
        layoutChildren();

        super.membersChanged();
    }

    protected void layoutChildren() {
        DoubleProperty yBase = offsetYProperty();
        lblWorkstationName.autosize();
        DoubleBinding y = yBase.add(lblWorkstationName.heightProperty());
        for(Cell<PhysicalNode> cell : members) {
            if(cell.layoutXProperty().isBound()) {
                cell.layoutXProperty().unbind();
            }
            if(cell.layoutYProperty().isBound()) {
                cell.layoutYProperty().unbind();
            }
            cell.layoutXProperty().bind(offsetXProperty().add(8.0));
            cell.layoutYProperty().bind(y);

            y = y.add(cell.heightProperty());
        }
    }

    /**
     * Same as superclass, but set the initial constraints to the bounding box of the label.
     */
    @Override
    protected void buildRectangleHull() {
        layoutChildren();
        //Set initial size to the label, w/ padding
        double xMin = lblWorkstationName.getLayoutX() - PADDING_WIDTH;
        double xMax = lblWorkstationName.getLayoutX() + lblWorkstationName.getWidth() + PADDING_WIDTH;
        double yMin = lblWorkstationName.getLayoutY() - PADDING_WIDTH;
        double yMax = lblWorkstationName.getLayoutY() + lblWorkstationName.getHeight() + PADDING_WIDTH;

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
        XmlElement element = super.toXml();

        element.addAttribute("type").setValue("computer");

        return element;
    }
}
