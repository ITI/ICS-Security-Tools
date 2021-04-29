package ui.graphing.graphs;

import core.document.graph.LogicalEdge;
import core.document.graph.LogicalNode;
import core.document.graph.WatchNetworkGraph;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import ui.custom.fx.ActiveMenuItem;
import ui.graphing.Cell;
import ui.graphing.FactoryLayoutableCells;
import ui.graphing.LayoutRadialGenerations;
import ui.graphing.logical.CellLogical;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipOutputStream;

public class LogicalWatchGraph extends LogicalGraph {
    public LogicalWatchGraph(core.document.graph.LogicalGraph graph, LogicalNode root, int degrees) {
        super(new WatchNetworkGraph<>(graph, root, degrees), null, graph.getCidrList());

        //we need a cell factory that allows the watch degree to change.
        setCellFactory(new FactoryLayoutableCells<LogicalNode>(this) {
            @Override
            public Cell<LogicalNode> uiFor(LogicalNode logicalNode) {
                return new CellLogical(logicalNode);
            }

            @Override
            public List<MenuItem> getFactoryMenuItems() {
                List<MenuItem> result = super.getFactoryMenuItems();

                if (result == null) {
                    result = new LinkedList<>();
                }

                Menu miSetDegrees = new Menu("Set Watch Degrees");
                miSetDegrees.getItems().addAll(
                        new ActiveMenuItem("1", event -> ((WatchNetworkGraph<LogicalNode, LogicalEdge>) LogicalWatchGraph.this.graph).degreesProperty().set(1)),
                        new ActiveMenuItem("2", event -> ((WatchNetworkGraph<LogicalNode, LogicalEdge>) LogicalWatchGraph.this.graph).degreesProperty().set(2)),
                        new ActiveMenuItem("3", event -> ((WatchNetworkGraph<LogicalNode, LogicalEdge>) LogicalWatchGraph.this.graph).degreesProperty().set(3)),
                        new ActiveMenuItem("4", event -> ((WatchNetworkGraph<LogicalNode, LogicalEdge>) LogicalWatchGraph.this.graph).degreesProperty().set(4)),
                        new ActiveMenuItem("5", event -> ((WatchNetworkGraph<LogicalNode, LogicalEdge>) LogicalWatchGraph.this.graph).degreesProperty().set(5)),
                        new ActiveMenuItem("6", event -> ((WatchNetworkGraph<LogicalNode, LogicalEdge>) LogicalWatchGraph.this.graph).degreesProperty().set(6)),
                        new ActiveMenuItem("7", event -> ((WatchNetworkGraph<LogicalNode, LogicalEdge>) LogicalWatchGraph.this.graph).degreesProperty().set(7)),
                        new ActiveMenuItem("8", event -> ((WatchNetworkGraph<LogicalNode, LogicalEdge>) LogicalWatchGraph.this.graph).degreesProperty().set(8)),
                        new ActiveMenuItem("9", event -> ((WatchNetworkGraph<LogicalNode, LogicalEdge>) LogicalWatchGraph.this.graph).degreesProperty().set(9)),
                        new ActiveMenuItem("10", event -> ((WatchNetworkGraph<LogicalNode, LogicalEdge>) LogicalWatchGraph.this.graph).degreesProperty().set(10))
                );

                result.add(miSetDegrees);

                return result;
            }
        });

        //Override the default layout; watch windows do a radial layout.
        setLayout(new LayoutRadialGenerations<>(root));

        //Now that the UI hooks are ready, perform the initial parsing.
        ((WatchNetworkGraph<LogicalNode, LogicalEdge>)LogicalWatchGraph.this.graph).reparseTree();

        title.bind(
                new ReadOnlyStringWrapper("Watch (")
                        .concat(((WatchNetworkGraph<LogicalNode, LogicalEdge>)LogicalWatchGraph.this.graph).degreesProperty().asString())
                        .concat("): ")
                        .concat(root.titleProperty()));
    }

    @Override
    public void toXml(ZipOutputStream zos) throws IOException {
        super.toXml(zos, "logical_watch",
                graph.indexOf(((LayoutRadialGenerations<LogicalNode, LogicalEdge>)getLayout()).getRoot())
                + ":"
                + ((WatchNetworkGraph<LogicalNode, LogicalEdge>)graph).degreesProperty().get()
        );
    }
}
