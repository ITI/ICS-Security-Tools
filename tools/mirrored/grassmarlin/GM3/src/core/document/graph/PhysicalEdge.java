package core.document.graph;

import core.document.serialization.xml.XmlElement;
import core.importmodule.ImportItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipOutputStream;

public class PhysicalEdge extends AbstractBidirectionalEdge<PhysicalNode> {
    private final SimpleBooleanProperty isTrunk;

    public PhysicalEdge(PhysicalNode source, PhysicalNode destination) {
        super(source, destination);

        isTrunk = new SimpleBooleanProperty(false);
        if(source instanceof PhysicalPort && destination instanceof PhysicalPort) {
            isTrunk.bind(((PhysicalPort) source).trunkProperty().and(((PhysicalPort) destination).trunkProperty()));
        }
    }

    public BooleanProperty isTrunkProperty() {
        return isTrunk;
    }

    @Override
    public XmlElement toXml(List<PhysicalNode> nodes, List<ImportItem> items, ZipOutputStream zos) throws IOException {
        XmlElement edge = super.toXml(nodes, items, zos);

        zos.write(edge.toString().getBytes(StandardCharsets.UTF_8));

        return null;
    }
}
