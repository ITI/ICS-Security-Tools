package ui.graphing.physical;

import core.document.PhysicalDevice;
import core.document.PhysicalTopology;
import core.document.graph.PhysicalEdge;
import core.document.graph.PhysicalNode;
import javafx.scene.control.TreeItem;
import ui.EmbeddedIcons;
import ui.graphing.*;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class PhysicalGraphTreeController extends GraphTreeController<PhysicalNode, PhysicalEdge> {
    private final TreeItem<String> rootVlans;
    private final TreeItem<String> rootSwitches;
    private final TreeItem<String> rootWorkstations;
    private final TreeItem<String> rootClouds;

    private final HashMap<CellGroup, HashMap<Cell<PhysicalNode>, GraphTreeItem.GraphTreeNodeItem<PhysicalNode, PhysicalEdge>>> lookupVlanNodes;

    private final List<PhysicalDevice> devices;

    public PhysicalGraphTreeController(
            Graph<PhysicalNode, PhysicalEdge> graph,
            HashMap<String, EmbeddedIcons> imagesGroups,
            List<PhysicalDevice> devices,
            Consumer<GraphTreeItem.GraphTreeEdgeItem<PhysicalNode, PhysicalEdge>> callbackEdge,
            Consumer<GraphTreeItem.GraphTreeNodeItem<PhysicalNode, PhysicalEdge>> callbackNode) {
        super(graph, imagesGroups, callbackEdge, callbackNode);

        rootVlans = new TreeItem<>("VLANs", EmbeddedIcons.Vista_Network.getImage(16.0));
        rootSwitches = new TreeItem<>("Switches and Routers", EmbeddedIcons.Vista_NetworkMap.getImage(16.0));
        rootWorkstations = new TreeItem<>("Workstations", EmbeddedIcons.Vista_NetworkCenter.getImage(16.0));
        rootClouds = new TreeItem<>("Unknown Topology", EmbeddedIcons.Vista_NetworkInternet.getImage(16.0));

        this.devices = devices;
        this.lookupVlanNodes = new HashMap<>();

        treeRoot.getChildren().addAll(
                rootSwitches,
                rootVlans,
                rootWorkstations,
                rootClouds
        );
    }

    public GraphTreeItem.GraphTreeGroupItem<PhysicalNode, PhysicalEdge> createVlanGroup(CellGroup vlan) {
        //Build the item using the vlan group-set rather than owner to ensure there are no conflicts.
        GraphTreeItem.GraphTreeGroupItem<PhysicalNode, PhysicalEdge> item = getTreeItemFactory().TreeNodeForGroup(vlan.nameProperty().get(), "VLAN");
        if(item.requiresInitialization()) {
            rootVlans.getChildren().add(item);
            rootVlans.getChildren().sort((o1, o2) -> Integer.compare(Integer.parseInt(o1.getValue().split(" ")[0]), Integer.parseInt(o2.getValue().split(" ")[0])));
        }

        return item;
    }

    public GraphTreeItem.GraphTreeNodeItem<PhysicalNode, PhysicalEdge> createVlanNode(CellGroup vlan, Cell<PhysicalNode> node) {
        HashMap<Cell<PhysicalNode>, GraphTreeItem.GraphTreeNodeItem<PhysicalNode, PhysicalEdge>> lookupNodes = lookupVlanNodes.get(vlan);
        if(lookupNodes == null) {
            lookupNodes = new HashMap<>();
            lookupVlanNodes.put(vlan, lookupNodes);
        }

        GraphTreeItem.GraphTreeNodeItem<PhysicalNode, PhysicalEdge> item = lookupNodes.get(node);
        if(item == null) {
            item = new GraphTreeItem.GraphTreeNodeItem<>(node.getNode());
            item.valueProperty().unbind();
            item.setValue(node.getNode().getGroups().get(PhysicalNode.GROUP_OWNER) + ":" + item.getValue());
            lookupNodes.put(node, item);
        } else {
            item.initialize();
        }

        return item;
    }

    @Override
    protected void initializeGroup(GraphTreeItem.GraphTreeGroupItem<PhysicalNode, PhysicalEdge> nodeGroup) {
        String nameGroup = nodeGroup.getName();
        TreeItem<String> root = rootClouds;

        if(devices.stream().filter(device -> device.nameProperty().get().equals(nameGroup)).findAny().isPresent()) {
            root = rootSwitches;
        } else if(nameGroup.startsWith(PhysicalTopology.TOKEN_WORKSTATION)) {
            root = rootWorkstations;
        }

        root.getChildren().add(nodeGroup);
        root.getChildren().sort((o1, o2) -> ((GraphTreeItem<PhysicalNode, PhysicalEdge>) o1).compareTo((GraphTreeItem<PhysicalNode, PhysicalEdge>) o2));
    }

    @Override
    public void clear() {
        //clear then re-add so that any side effects of the superclass clear are preserved.
        super.clear();

        rootSwitches.getChildren().clear();
        rootVlans.getChildren().clear();
        rootWorkstations.getChildren().clear();
        rootClouds.getChildren().clear();

        treeRoot.getChildren().addAll(
                rootSwitches,
                rootVlans,
                rootWorkstations,
                rootClouds
        );
    }
}
