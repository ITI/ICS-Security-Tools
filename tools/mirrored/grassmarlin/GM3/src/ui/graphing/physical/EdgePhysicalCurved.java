package ui.graphing.physical;

import com.sun.javafx.collections.ObservableSetWrapper;
import core.document.graph.PhysicalEdge;
import core.document.graph.PhysicalNode;
import core.document.serialization.xml.XmlElement;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.When;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import ui.custom.fx.ActiveMenuItem;
import ui.custom.fx.FxStringFromCollectionBinding;
import ui.custom.fx.FxStringProperty;
import ui.dialog.ColorPickerDialogFx;
import ui.graphing.Cell;
import ui.graphing.CellLayer;
import ui.graphing.CurvedEdge;

import java.util.*;
import java.util.stream.Collectors;

public class EdgePhysicalCurved extends CurvedEdge<PhysicalNode> implements CellLayer.ICanHazContextMenu {
    private SimpleObjectProperty<Color> color;
    private SimpleObjectProperty<Color> colorDefault;
    private final PhysicalEdge edge;

    private final MenuItem miSetColor;
    private final CheckMenuItem miUseDefaultColor;

    public EdgePhysicalCurved(Cell<PhysicalNode> source, Cell<PhysicalNode> target, PhysicalEdge edge) {
        super(source, target);

        this.edge = edge;
        this.color = new SimpleObjectProperty<>(Color.BLACK);
        this.colorDefault = new SimpleObjectProperty<>(Color.BLACK);

        miUseDefaultColor = new CheckMenuItem("Use Default Color");
        miUseDefaultColor.setSelected(true);

        final ObjectBinding<Color> colorEvaluated = new When(miUseDefaultColor.selectedProperty()).then(colorDefault).otherwise(color);

        Rectangle previewSetColor = new Rectangle(14.0, 14.0);
        previewSetColor.setStrokeWidth(1.0);
        previewSetColor.setStroke(Color.BLACK);
        previewSetColor.fillProperty().bind(colorEvaluated);

        miSetColor = new ActiveMenuItem("Change Connection Color (" + source.getNode().titleProperty().get() + " <-> " + target.getNode().titleProperty().get() + ")", previewSetColor, event -> {
            final ColorPickerDialogFx dialogColorPicker = new ColorPickerDialogFx();

            dialogColorPicker.setColor(color.get());
            Optional<ButtonType> result = dialogColorPicker.showAndWait();
            if(result.isPresent() && result.get().equals(ButtonType.OK)) {
                color.set(dialogColorPicker.getSelectedColor());
                miUseDefaultColor.setSelected(false);
            }
        });

        connectionCurve.strokeProperty().bind(colorEvaluated);
        connectionLinear.strokeProperty().bind(colorEvaluated);
        FxStringProperty strDetails = new FxStringProperty();
        Set<Integer> vlans = new HashSet<>(edge.getSource().getVLans());
        vlans.retainAll(edge.getDestination().getVLans());
        strDetails.bind(new FxStringFromCollectionBinding<>(new ObservableSetWrapper<>(vlans), Collectors.joining(", ", "VLans: [", "]"), i -> i.toString()));
        setDetails(strDetails);
    }

    public ObjectProperty<Color> defaultColorProperty() {
        return colorDefault;
    }
    public ObjectProperty<Color> colorProperty() {
        return color;
    }
    public PhysicalEdge getEdge() {
        return edge;
    }
    public void useDefaultColor(boolean useDefault) {
        miUseDefaultColor.setSelected(useDefault);
    }

    @Override
    public List<MenuItem> getContextMenuItems() {
        ArrayList<MenuItem> result = new ArrayList<>(2);
        result.add(miSetColor);
        result.add(miUseDefaultColor);

        return result;
    }

    @Override
    public XmlElement toXml(int index) {
        XmlElement edge = super.toXml(index);

        edge.addAttribute("usedefaultcolor").setValue(Boolean.toString(miUseDefaultColor.isSelected()));
        edge.addAttribute("color").setValue(color.get().toString().substring(2, 8));

        return edge;
    }
}
