package ui.graphing.graphs;

import core.document.graph.MeshEdge;
import core.document.graph.MeshNode;
import core.document.graph.NetworkGraph;
import ui.graphing.*;

import java.io.IOException;
import java.util.zip.ZipOutputStream;

public class MeshGraph extends Graph<MeshNode, MeshEdge> {
    public MeshGraph(NetworkGraph<MeshNode, MeshEdge> graph) {
        super(graph, MeshNode.GROUP_PAN);

        setCellFactory(new FactoryLayoutableCells<MeshNode>(this) {
            @Override
            public Cell<MeshNode> uiFor(MeshNode meshNode) {
                return new Cell<>(meshNode);
            }
        });
        setEdgeFactory(new FactoryCurvedEdges<>(false));
        setGroupFactory(new FactoryCollapsibleGroups<>(this, imagesGroups));

        setLayout(new LayoutHub<>());

        title.set("Sniffles");
    }

    @Override
    public void toXml(ZipOutputStream zos) throws IOException {
        super.toXml(zos, "mesh_graph", "primary");
    }
}
