package core.document.graph;

import core.document.serialization.xml.XmlElement;
import core.importmodule.ImportItem;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipOutputStream;

/**
 * Interface to permit typed access to edges of a node without needing to resolve to a specific type.
 * Additionally, a hook to handle merging of edges is present; for the non-trivial case, this allows metrics to be aggregated across multiple edges.
 * @param <TNode> The type of nodes associated with this edge.
 */
public interface IEdge<TNode> {
    TNode getSource();
    TNode getDestination();
    void setSource(TNode source);
    void setDestination(TNode destination);

    XmlElement toXml(List<TNode> nodes, List<ImportItem> items, ZipOutputStream zos) throws IOException;
}
