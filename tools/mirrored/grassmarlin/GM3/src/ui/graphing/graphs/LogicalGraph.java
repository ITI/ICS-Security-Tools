package ui.graphing.graphs;

import core.document.graph.LogicalEdge;
import core.document.graph.LogicalNode;
import core.document.graph.NetworkGraph;
import core.logging.Logger;
import core.logging.Severity;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import ui.custom.fx.ActiveMenuItem;
import ui.dialog.ConnectionDetailsDialogFx;
import ui.dialog.ManageLogicalNetworksDialogFx;
import ui.graphing.Cell;
import ui.graphing.FactoryLayoutableCells;
import ui.graphing.FactoryTreeItemsLogical;
import ui.graphing.Graph;
import ui.graphing.logical.CellLogical;
import ui.graphing.logical.FactoryCurvedEdgesLogical;
import ui.graphing.logical.FactoryLogicalGroups;
import ui.graphing.logical.LayoutForceDirectedGroups;
import util.Cidr;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipOutputStream;

public class LogicalGraph extends Graph<LogicalNode, LogicalEdge> {
    public interface ICreateWatchWindow {
        void createWatch(LogicalNode root, int levels);
    }

    protected final ConnectionDetailsDialogFx dlgConnectionDetails;
    protected final ICreateWatchWindow fnCreateWatch;
    protected final List<Cidr> cidrs;

    public LogicalGraph(final NetworkGraph<LogicalNode, LogicalEdge> graph, final ICreateWatchWindow fnCreateWatch, final List<Cidr> cidrs) {
        super(graph, LogicalNode.GROUP_SUBNET);

        title.set("Logical Graph");

        // Ideally we don't need one for each watch window, etc., however the resource use is minimal and allows for customization in the future, should such be deemed desirable.
        dlgConnectionDetails = new ConnectionDetailsDialogFx(graph);

        // == Tree Factory
        setTreeFactory(new FactoryTreeItemsLogical(imagesGroups));

        // == Edge Factory
        setEdgeFactory(new FactoryCurvedEdgesLogical());

        // == Group Factory
        setGroupFactory(new FactoryLogicalGroups(this, imagesGroups));

        // == Layout
        //setLayout(new LayoutHub<>());
        setLayout(new LayoutForceDirectedGroups<>());

        // == Cell Factory
        setCellFactory(new FactoryLayoutableCells<LogicalNode>(this) {
            @Override
            public Cell<LogicalNode> uiFor(LogicalNode logicalNode) {
                return new CellLogical(logicalNode);
            }
        });

        // MenuItems
        CheckMenuItem miShowGroups = new CheckMenuItem("Show Group Regions");
        miShowGroups.selectedProperty().set(true);
        getVisualization().getCanvas().getGroupLayer().visibleProperty().bind(miShowGroups.selectedProperty());
        menuGraph.add(miShowGroups);


        this.fnCreateWatch = fnCreateWatch;
        this.cidrs = cidrs;

        // == Cell Factory
        // If we have a method to create a watch window, set the cell factory to include the relevant menu item.
        setCellFactory(new FactoryLayoutableCells<LogicalNode>(this) {
            @Override
            public Cell<LogicalNode> uiFor(LogicalNode logicalNode) {
                CellLogical result = new CellLogical(logicalNode);

                result.addContextMenuItem(new ActiveMenuItem("Watch Connections", event -> {
                    fnCreateWatch.createWatch(logicalNode, 1);
                }));
                return result;
            }
        });

        MenuItem miAddSubnet = new ActiveMenuItem("Add Subnet Group", event -> {
            ManageLogicalNetworksDialogFx.getInstance().showAndWait();
        });
        menuGraph.add(miAddSubnet);
    }

    @Override
    protected void processNewNode(final NodeWrapper<LogicalNode> node) {
        super.processNewNode(node);

        node.getContextItems().add(
                new ActiveMenuItem("View Frames...", event -> {
                    dlgConnectionDetails.setRootNode(node.getNode());
                    dlgConnectionDetails.showAndWait();
                })
        );
    }

    @Override
    public void toXml(ZipOutputStream zos) throws IOException {
        super.toXml(zos, "logical_graph", "primary");
    }
}
