package core.document.graph;

import core.document.serialization.xml.XmlElement;
import core.importmodule.ImportItem;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipOutputStream;

public abstract class AbstractBidirectionalEdge<TNode> implements IEdge<TNode>, Comparable<AbstractBidirectionalEdge<TNode>> {
    private TNode source;
    private TNode destination;

    protected AbstractBidirectionalEdge(TNode source, TNode destination) {
        this.source = source;
        this.destination = destination;
    }

    // == IEdge
    @Override
    public TNode getSource() {
        return source;
    }
    @Override
    public TNode getDestination() {
        return destination;
    }

    @Override
    public void setSource(TNode source) {
        this.source = source;
    }
    @Override
    public void setDestination(TNode destination) {
        this.destination = destination;
    }

    // == Comparable
    @Override
    public int compareTo(AbstractBidirectionalEdge<TNode> rhs) {
        if(rhs == null) {
            return 1;
        }
        if(this.equals(rhs)) {
            return 0;
        }
        return this.toString().compareTo(rhs.toString());
    }

    // == Object
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof AbstractBidirectionalEdge) {
            AbstractBidirectionalEdge rhs = (AbstractBidirectionalEdge)obj;
            return ((this.source.equals(rhs.source) && rhs.source.equals(this.source) && this.destination.equals(rhs.destination) && rhs.destination.equals(this.destination))
                    ||
                    (this.source.equals(rhs.destination) && rhs.destination.equals(this.source) && this.destination.equals(rhs.source) && rhs.source.equals(this.destination)));
        } else {
            return super.equals(obj);
        }
    }

    /**
     * NOTE: If this method is ever changed, core.importmodule.TaskDispatcher.LogicalEdgeFactory.LogicalEdgePlaceholder must be updated as well.
     * @return
     */
    @Override
    public int hashCode() {
        return source.hashCode() ^ destination.hashCode();
    }


    @Override
    public String toString() {
        String s = source.toString();
        String d = destination.toString();

        if(s.compareTo(d) < 0) {
            String t = s;
            s = d;
            d = t;
        }
        //Use sorted ordering to name nodes to ensure consistency when different edges still evaluate as equal.

        return "("
                .concat(s)
                .concat(",")
                .concat(d)
                .concat(")");
    }

    @Override
    public XmlElement toXml(List<TNode> nodes, List<ImportItem> items, ZipOutputStream zos) throws IOException{
        XmlElement xmlEdge = new XmlElement("edge");
        xmlEdge.addAttribute("from").setValue(Integer.toString(nodes.indexOf(getSource())));
        xmlEdge.addAttribute("to").setValue(Integer.toString(nodes.indexOf(getDestination())));

        return xmlEdge;
    }
}
