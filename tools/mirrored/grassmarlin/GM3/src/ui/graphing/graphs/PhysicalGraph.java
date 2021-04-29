package ui.graphing.graphs;

import core.document.PhysicalTopology;
import core.document.graph.*;
import javafx.application.Platform;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.TreeItem;
import ui.EmbeddedIcons;
import ui.graphing.*;
import ui.graphing.physical.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.zip.ZipOutputStream;

public class PhysicalGraph extends Graph<PhysicalNode, PhysicalEdge> {
    private final PhysicalVisualization physicalVisualization;
    private final Visualization.IGroupCellFactory<PhysicalNode, PhysicalEdge> factoryVLans;
    private final HashMap<CellGroup, TreeItem<String>> lookupVlanGroups;

    public PhysicalGraph(NetworkGraph<PhysicalNode, PhysicalEdge> graph, PhysicalTopology topology) {
        super(graph, PhysicalNode.GROUP_OWNER);
        title.set("Physical Graph");

        //The PhysicalVisualization adds a 4th layer (devices) and reorders the other layers.
        //The device layer functions as a second group layer.
        this.physicalVisualization = new PhysicalVisualization(this);
        this.setVisualization(physicalVisualization);
        this.lookupVlanGroups = new HashMap<>();

        this.treeController = new PhysicalGraphTreeController(this, imagesGroups, topology.getDevices(), this::Handle_NewTreeEdge, this::Handle_NewTreeNode);

        //Set default factories.
        HashMap<String, EmbeddedIcons> imagesGroups = new HashMap<>();
        imagesGroups.put(PhysicalNode.GROUP_OWNER, EmbeddedIcons.Vista_Network);

        setCellFactory(new FactoryCells());
        setEdgeFactory(new FactoryCustomizablePhysicalCurvedEdges());
        FactoryDeviceGroups factoryDevices = new FactoryDeviceGroups(this, imagesGroups, topology.getDevices());
        setGroupFactory(factoryDevices);
        factoryVLans = new FactoryVLanGroups(this, physicalVisualization.getVLanLayer());

        this.setLayout(new LayoutPhysicalRadial(factoryDevices));

        //Disallow node dragging; nodes are set relative to the group for the Physical Graph
        getVisualizationView().allowCellMovement(false);

        //Add hook to clear the device factory cache on a clear command.
        graph.OnGraphCleared.addHandler((source, arguments) -> {
            if (!Platform.isFxApplicationThread()) {
                Platform.runLater(factoryVLans::clearCache);
            } else {
                factoryVLans.clearCache();
            }
        });

        //By default a Graph permits the user to change the grouping through the context menu.
        //We want to remove that and, instead, allow the ability to toggle the VLAN layer
        CheckMenuItem miShowVLans = new CheckMenuItem("Show VLANs");
        miShowVLans.selectedProperty().set(false);
        physicalVisualization.getVLanLayer().visibleProperty().bind(miShowVLans.selectedProperty());
        menuGraph.clear();
        menuGraph.add(miShowVLans);
    }

    @Override
    protected void processNewVisualNode(final PhysicalNode node, final Cell<PhysicalNode> cell) {
        super.processNewVisualNode(node, cell);

        // Process the vlan groups
        final Set<Integer> vlans;
        if(cell.getNode() instanceof PhysicalPort) {
            vlans = ((PhysicalPort)cell.getNode()).getAllVlans();
        } else {
            vlans = cell.getNode().getVLans();
        }
        for(Integer vlan : vlans) {
            CellGroup<PhysicalNode, PhysicalEdge> groupVlan = factoryVLans.getGroup("VLAN", Integer.toString(vlan));
            groupVlan.getMembers().add(cell);
            TreeItem<String> treeVlan = lookupVlanGroups.get(groupVlan);
            if(treeVlan == null) {
                treeVlan = ((PhysicalGraphTreeController)treeController).createVlanGroup(groupVlan);
                lookupVlanGroups.put(groupVlan, treeVlan);
            }

            GraphTreeItem.GraphTreeNodeItem<PhysicalNode, PhysicalEdge> item = ((PhysicalGraphTreeController)treeController).createVlanNode(groupVlan, cell);
            treeVlan.getChildren().add(item);

            //TODO: Add context menu items to item
            // - Center in View
            // - Show details
        }
    }
    @Override
    protected void processRemoveVisualNode(final PhysicalNode node, final Cell<PhysicalNode> cell) {
        super.processRemoveVisualNode(node, cell);

        //TODO: Remove from VLANs
    }

    @Override
    protected  void doneLoading() {
        this.lookupVlanGroups.keySet().forEach(group -> group.rebuildHull());
    }

    @Override
    protected void processNewNode(final NodeWrapper<PhysicalNode> node) {
        super.processNewNode(node);

        //TODO: Add hooks for rename computer, etc.
    }

    @Override
    public void reprocessNode(final PhysicalNode node) {
        // Process the vlan groups
        final Set<Integer> vlans;
        final Cell<PhysicalNode> cell = getVisualization().cellFor(node);
        if(cell.getNode() instanceof PhysicalPort) {
            vlans = ((PhysicalPort)cell.getNode()).getAllVlans();
        } else {
            vlans = cell.getNode().getVLans();
        }
        for(Integer vlan : vlans) {
            final CellGroup groupVlan = factoryVLans.getGroup("VLAN", Integer.toString(vlan));

            groupVlan.getMembers().remove(cell);
        }

        // Run the superclass last since that will update the mapping that is used to find the cell from the node.
        super.reprocessNode(node);
    }

    @Override
    public void toXml(ZipOutputStream zos) throws IOException {
        super.toXml(zos, "physical_graph", "primary");
    }
}
