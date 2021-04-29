package ui.graphing.logical;

import core.document.graph.LogicalEdge;
import core.document.graph.LogicalNode;
import core.document.serialization.xml.XmlElement;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import ui.custom.fx.Log10DoubleBinding;
import ui.graphing.Cell;
import ui.graphing.Edge;
import ui.graphing.FactoryCurvedEdges;

import java.util.List;

public class FactoryCurvedEdgesLogical extends FactoryCurvedEdges<LogicalNode, LogicalEdge> {
    private final CheckMenuItem miUseWeightedLines = new CheckMenuItem("Weight Edges By Byte Count");
    private final DoubleProperty defaultWeight = new SimpleDoubleProperty(1.0);

    public FactoryCurvedEdgesLogical() {
        super(true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Edge uiFor(LogicalEdge edge, Cell source, Cell destination) {
        //We know that the result is a CurvedEdge because that is what the CurvedEdge factory always returns.
        Edge result = super.uiFor(edge, source, destination);
        result.setDetails(edge.edgeDetailsProperty());

        //Weight is log10(bytes sent+bytes received) / 100.
        result.weightProperty().bind(new When(miUseWeightedLines.selectedProperty()).then(Log10DoubleBinding.of(
                edge.getDetailsToSource().bytesProperty().add(edge.getDetailsToDestination().bytesProperty()).divide(100.0)
        )).otherwise(defaultWeight));

        // When a new packet is reported, the bytes properties will change (the bytes includes header info, so a packet can never have 0 size).
        // When the edge receives a packet, play the modify animation on the endpoints.
        edge.getDetailsToDestination().bytesProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                source.HighlightForChange();
                destination.HighlightForChange();
            });
        });
        edge.getDetailsToSource().bytesProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                source.HighlightForChange();
                destination.HighlightForChange();
            });
        });

        return result;
    }

    public BooleanProperty useWeightedLinesProperty() {
        return miUseWeightedLines.selectedProperty();
    }

    @Override
    public List<MenuItem> getFactoryMenuItems() {
        List<MenuItem> result = super.getFactoryMenuItems();
        result.add(miUseWeightedLines);
        return result;
    }

    @Override
    public XmlElement toXml() {
        XmlElement xmlEdge = super.toXml();
        xmlEdge.addAttribute("isWeighted").setValue(Boolean.toString(miUseWeightedLines.isSelected()));

        return xmlEdge;
    }
}
