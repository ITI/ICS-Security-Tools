package ui.graphing;

import core.document.graph.IEdge;
import core.document.graph.INode;
import core.document.serialization.xml.XmlElement;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class FactoryCurvedEdges<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> implements Visualization.IEdgeCellFactory<TNode, TEdge> {
    private final CheckMenuItem miUseCurves;
    private final CheckMenuItem miShowDetails;
    private final boolean allowDetails;

    /**
     * Return an Edge factory which allows edges to be rendered as curves instead of straight lines.
     * @param allowDetails If true, the details node of the edges can be displayed.
     */
    public FactoryCurvedEdges(boolean allowDetails) {
        miUseCurves = new CheckMenuItem("Use Curved Edges");  //Defaults off.
        miShowDetails = new CheckMenuItem("Show Connection Details");

        this.allowDetails = allowDetails;
    }

    protected Edge<TNode> BuildNewEdge(TEdge edge, Cell<TNode> source, Cell<TNode> destination) {
        return new CurvedEdge<>(source, destination);
    }
    protected void ConfigureNewEdge(Edge<TNode> edgeNew) {
        if(edgeNew instanceof CurvedEdge) {
            ((CurvedEdge<TNode>)edgeNew).bindVisualProperties(miUseCurves.selectedProperty(), miShowDetails.selectedProperty());
        }
    }

    @Override
    public Edge<TNode> uiFor(TEdge edge, Cell<TNode> source, Cell<TNode> destination) {
        Edge<TNode> result = BuildNewEdge(edge, source, destination);
        ConfigureNewEdge(result);
        return result;
    }
    @Override
    public List<MenuItem> getFactoryMenuItems() {
        ArrayList<MenuItem> result = new ArrayList<>(2);
        result.add(miUseCurves);
        if(allowDetails) {
            result.add(miShowDetails);
        }

        return result;
    }

    public BooleanProperty useCurvedLinesProperty() {
        return miUseCurves.selectedProperty();
    }
    public BooleanProperty showDetailsProperty() {
        return miShowDetails.selectedProperty();
    }

    @Override
    public XmlElement toXml() {
        XmlElement xmlFactory = new XmlElement("factory");
        xmlFactory.addAttribute("type").setValue("edge");
        xmlFactory.addAttribute("isCurved").setValue(Boolean.toString(useCurvedLinesProperty().get()));
        xmlFactory.addAttribute("showDetails").setValue(Boolean.toString(showDetailsProperty().get()));

        return xmlFactory;
    }
}
