package core.document.graph;

/**
 * Class representing the Mesh Graph
 */
public class MeshGraph extends NetworkGraph<MeshNode, MeshEdge> {

    @Override
    public String getEntryName() {
        return "mesh.xml";
    }
}
