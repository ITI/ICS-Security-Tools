package ui.graphing.graphs;

import core.document.graph.*;
import core.document.serialization.xml.XmlElement;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import ui.EmbeddedIcons;
import ui.custom.fx.ActiveMenuItem;
import ui.custom.fx.DynamicSubMenu;
import ui.graphing.Cell;
import ui.graphing.FactoryLayoutableCells;
import ui.graphing.logical.CellLogicalHidable;
import ui.graphing.logical.LayoutCopyFromRoot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

public class LogicalFilterGraph extends LogicalGraph {
    public LogicalFilterGraph(final LogicalGraph root) {
        super(new FilteredNetworkGraph((core.document.graph.LogicalGraph) root.getGraph()), root.fnCreateWatch, root.cidrs);

        title.set("Filtered View");

        this.setLayout(new LayoutCopyFromRoot<>(root));

        setCellFactory(new FactoryLayoutableCells<LogicalNode>(this) {
            @Override
            public Cell<LogicalNode> uiFor(LogicalNode logicalNode) {
                return new CellLogicalHidable(LogicalFilterGraph.this, logicalNode);
            }
        });

        // Menu Item for restoring hidden nodes
        menuGraph.add(new DynamicSubMenu("Unhide Nodes", EmbeddedIcons.Vista_Filter, () ->
                ((FilteredNetworkGraph) getGraph()).getHiddenNodes().stream()
                        .map(node -> new ActiveMenuItem(node.getIp().toString(), event -> setNodeVisibility(node, true)))
                        .collect(Collectors.toList())
        ));

        if(!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> {
                ((FilteredNetworkGraph) getGraph()).initialize();
                getVisualizationView().zoomToFit();
            });
        } else {
            ((FilteredNetworkGraph) getGraph()).initialize();
            Platform.runLater(getVisualizationView()::zoomToFit);
        }
    }

    public void setNodeVisibility(LogicalNode node, boolean visible) {
        ObservableList<LogicalNode> hidden = ((FilteredNetworkGraph)getGraph()).getHiddenNodes();
        if(visible) {
            hidden.remove(node);
        } else {
            if(!hidden.contains(node)) {
                hidden.add(node);
            }
        }
    }

    @Override
    public void toXml(ZipOutputStream zos) throws IOException{
        String tag = "logical_filter_graph";
        super.writeOpenTag(zos, tag, this.title.get());
        super.writeContents(zos);
        zos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
        zos.write("<hidden_nodes>".getBytes(StandardCharsets.UTF_8));
        zos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
        for (LogicalNode node : ((FilteredNetworkGraph) getGraph()).getHiddenNodes()) {
            XmlElement element = new XmlElement("node");
            int ref = getGraph().indexOf(node);
            element.addAttribute("ref").setValue(Integer.toString(ref));
            zos.write(element.toString().getBytes(StandardCharsets.UTF_8));
            zos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
        }
        zos.write("</hidden_nodes>".getBytes(StandardCharsets.UTF_8));
        super.writeCloseTag(zos, tag);
    }
}
