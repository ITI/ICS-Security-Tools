package ui.graphing.physical;

import core.document.graph.PhysicalEdge;
import core.document.graph.PhysicalNode;
import core.document.serialization.xml.XmlElement;
import javafx.beans.binding.When;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import ui.custom.fx.ActiveMenuItem;
import ui.dialog.ColorPickerDialogFx;
import ui.graphing.Cell;
import ui.graphing.Edge;
import ui.graphing.FactoryCurvedEdges;

import java.util.List;
import java.util.Optional;

public class FactoryCustomizablePhysicalCurvedEdges extends FactoryCurvedEdges<PhysicalNode, PhysicalEdge> {
    private final SimpleObjectProperty<Color> defaultTrunkColor;
    private final SimpleObjectProperty<Color> defaultColor;
    private final MenuItem miSetDefaultTrunkColor;
    private final MenuItem miSetDefaultColor;

    public FactoryCustomizablePhysicalCurvedEdges() {
        super(true);

        defaultTrunkColor = new SimpleObjectProperty<>(Color.GREEN);
        defaultColor = new SimpleObjectProperty<>(Color.BLUE);

        Rectangle previewTrunk = new Rectangle(14.0, 14.0);
        previewTrunk.setStrokeWidth(1.0);
        previewTrunk.setStroke(Color.BLACK);
        previewTrunk.fillProperty().bind(defaultTrunkColor);
        Rectangle previewNonTrunk = new Rectangle(14.0, 14.0);
        previewNonTrunk.setStrokeWidth(1.0);
        previewNonTrunk.setStroke(Color.BLACK);
        previewNonTrunk.fillProperty().bind(defaultColor);

        miSetDefaultTrunkColor = new ActiveMenuItem("Change Default Trunk Color", previewTrunk, event -> {
            final ColorPickerDialogFx dialogColorPicker = new ColorPickerDialogFx();

            dialogColorPicker.setColor(defaultTrunkColor.get());
            Optional<ButtonType> result = dialogColorPicker.showAndWait();
            if(result.isPresent() && result.get().equals(ButtonType.OK)) {
                defaultTrunkColor.set(dialogColorPicker.getSelectedColor());
            }
        });
        miSetDefaultColor = new ActiveMenuItem("Change Default Non-Trunk Color", previewNonTrunk, event -> {
            final ColorPickerDialogFx dialogColorPicker = new ColorPickerDialogFx();

            dialogColorPicker.setColor(defaultColor.get());
            Optional<ButtonType> result = dialogColorPicker.showAndWait();
            if(result.isPresent() && result.get().equals(ButtonType.OK)) {
                defaultColor.set(dialogColorPicker.getSelectedColor());
            }
        });
    }

    public ObjectProperty<Color> defaultTrunkColorProperty() {
        return this.defaultTrunkColor;
    }

    public ObjectProperty<Color> defaultColorProperty() {
        return this.defaultColor;
    }

    @Override
    public List<MenuItem> getFactoryMenuItems() {
        List<MenuItem> result = super.getFactoryMenuItems();

        result.add(miSetDefaultTrunkColor);
        result.add(miSetDefaultColor);

        return result;
    }

    @Override
    protected Edge<PhysicalNode> BuildNewEdge(PhysicalEdge edge, Cell<PhysicalNode> source, Cell<PhysicalNode> destination) {
        if(source instanceof CellCloud && destination instanceof CellCloud) {
            return new Edge<>(source, destination);
        } else {
            return new EdgePhysicalCurved(source, destination, edge);
        }
    }
    @Override
    protected void ConfigureNewEdge(Edge<PhysicalNode> edgeNew) {
        //The edges joining the two halves of a cloud will not be EdgePhysicalCurved objects.
        if(edgeNew instanceof  EdgePhysicalCurved) {
            EdgePhysicalCurved edge = (EdgePhysicalCurved) edgeNew;
            edge.defaultColorProperty().bind(new When(edge.getEdge().isTrunkProperty()).then(defaultTrunkColor).otherwise(defaultColor));
        }

        super.ConfigureNewEdge(edgeNew);
    }

    @Override
    public XmlElement toXml() {
        XmlElement element = super.toXml();

        element.addAttribute("trunkcolor").setValue(defaultTrunkColor.get().toString().substring(2, 8));
        element.addAttribute("wirecolor").setValue(defaultColor.get().toString().substring(2, 8));

        return element;
    }
}
