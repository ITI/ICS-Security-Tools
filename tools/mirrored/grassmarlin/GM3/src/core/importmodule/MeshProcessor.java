package core.importmodule;

import core.document.Session;
import core.document.graph.IEdge;
import core.document.graph.MeshNode;
import core.exec.IEEE802154Data;

import java.util.function.Function;

/**
 * Processing pipeline for Mesh graph data
 */
public class MeshProcessor {

    private final Session session;
    private final Function<IEEE802154Data, IEdge<MeshNode>> graph;

    public MeshProcessor(Session session, Function<IEEE802154Data, IEdge<MeshNode>> graph) {
        this.session = session;
        this.graph = graph;
    }

    public void process(IEEE802154Data data) {
        graph.apply(data);
    }
}
