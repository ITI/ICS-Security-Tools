package ui.graphing.physical;

import core.document.PhysicalDevice;
import core.document.PhysicalTopology;
import core.document.graph.PhysicalEdge;
import core.document.graph.PhysicalNode;
import javafx.collections.ObservableList;
import ui.EmbeddedIcons;
import ui.graphing.CellGroup;
import ui.graphing.FactoryCollapsibleGroups;
import ui.graphing.Graph;

import java.util.HashMap;

public class FactoryDeviceGroups extends FactoryCollapsibleGroups<PhysicalNode, PhysicalEdge> {
    private final ObservableList<PhysicalDevice> devices;

    public FactoryDeviceGroups(Graph<PhysicalNode, PhysicalEdge> graphOwner, HashMap<String, EmbeddedIcons> images, ObservableList<PhysicalDevice> devices) {
        super(graphOwner, images);

        this.devices = devices;
    }


    @Override
    protected CellGroup<PhysicalNode, PhysicalEdge> BuildGroup(String groupBy, String name) {
        if(devices.stream().anyMatch(device -> device.nameProperty().get().equals(name))) {
            return new GroupSwitch(name, graph);
        } else if(name.startsWith(PhysicalTopology.TOKEN_WORKSTATION)) {
            return new GroupComputer(name.substring(PhysicalTopology.TOKEN_WORKSTATION.length()), graph);
        } else {
            // Clouds should support relocating nodes within the aggregate / calculate a hull that is not necessarily rectangular.
            return new GroupCloud(name, imageGroups.get(groupBy), graph);
        }
    }
}
