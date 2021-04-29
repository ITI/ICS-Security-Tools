package ui.graphing.physical;

import core.document.graph.PhysicalEdge;
import core.document.graph.PhysicalNode;
import ui.graphing.CellGroup;
import ui.graphing.CellLayer;
import ui.graphing.FactoryCachedGroups;
import ui.graphing.Graph;

public class FactoryVLanGroups extends FactoryCachedGroups<PhysicalNode, PhysicalEdge> {
    private final CellLayer layerVlans;

    public FactoryVLanGroups(Graph<PhysicalNode, PhysicalEdge> graphOwner, CellLayer layerVlans) {
        super(graphOwner);

        this.layerVlans = layerVlans;
    }

    @Override
    protected CellGroup<PhysicalNode, PhysicalEdge> BuildGroup(String groupBy, String name) {
        return new GroupVLan(Integer.parseInt(name), graph);
    }

    @Override
    protected void ConfigureGroup(CellGroup group) {
        layerVlans.getChildren().add(group);
    }
}
