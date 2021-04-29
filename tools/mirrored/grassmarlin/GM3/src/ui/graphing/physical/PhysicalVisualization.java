package ui.graphing.physical;

import core.document.graph.PhysicalEdge;
import core.document.graph.PhysicalNode;
import ui.graphing.CellLayer;
import ui.graphing.Graph;
import ui.graphing.Visualization;

/**
 * The PhysicalVisualization supports two aggregate layers; the normal
 * group layer, which displays devices and the vlan layer, which groups
 * MACs and clouds by vlan.
 */
public class PhysicalVisualization extends Visualization<PhysicalNode, PhysicalEdge> {
    public PhysicalVisualization(Graph<PhysicalNode, PhysicalEdge> graph) {
        super(graph);

        //We need a separate 4-layer canvas
        this.canvas = new PhysicalCanvas();
    }

    public CellLayer getVLanLayer() {
        return ((PhysicalCanvas)this.canvas).getVlans();
    }
}
