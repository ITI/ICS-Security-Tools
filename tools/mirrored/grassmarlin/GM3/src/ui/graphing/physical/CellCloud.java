package ui.graphing.physical;

import core.document.graph.PhysicalCloud;
import core.document.graph.PhysicalNode;
import javafx.scene.control.MenuItem;
import ui.LocalIcon;
import ui.graphing.Cell;
import ui.graphing.Edge;

import java.util.ArrayList;
import java.util.List;

public class CellCloud extends ControlPointCell<PhysicalNode> {
    public CellCloud(PhysicalCloud cloud) {
        super(cloud);

        //Suppress the normal content
        paneInternalContent.getChildren().clear();
        //Add the cloud image.
        //boxExternalImages.getChildren().add(new ImageView(Icons.Original_cloud.getFxImage("128")));
        boxExternalImages.getChildren().add(LocalIcon.forPath("images|physical|1403_Globe.png").getView(32.0));

        menuItems.remove(miAutoLayout);
    }

    @Override
    public void addEdge(Edge<PhysicalNode> edge) {
        super.addEdge(edge);

        Cell<PhysicalNode> nodeOther = (edge.getTarget() == this ? edge.getSource() : edge.getTarget());
        if(nodeOther instanceof CellCloud) {
            CellCloud cloudOther = (CellCloud)nodeOther;

            controlPointProperty().bind(new OffsetBinding(pointCenter,
                    layoutXProperty().subtract(cloudOther.layoutXProperty()).multiply(2.0),
                    layoutYProperty().subtract(cloudOther.layoutYProperty()).multiply(2.0)));
        }
    }

    @Override
    public List<MenuItem> getContextMenuItems() {
        //Do not add edge menu items for a cloud; it is likely that there will be prohibitively many edges.
        return new ArrayList<>(menuItems);
    }
}
