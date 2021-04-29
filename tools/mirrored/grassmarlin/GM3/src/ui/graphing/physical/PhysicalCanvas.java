package ui.graphing.physical;

import ui.graphing.Canvas;
import ui.graphing.CellLayer;

public class PhysicalCanvas extends Canvas {
    private final CellLayer vlans;

    public PhysicalCanvas() {
        vlans = new CellLayer();

        //Edges render over nodes, groups are below vlans.
        this.getChildren().clear();
        this.getChildren().addAll(vlans, getGroupLayer(), getCellLayer(), getEdgeLayer());
    }

    public CellLayer getVlans() {
        return vlans;
    }

    @Override
    public void clear() {
        super.clear();
        vlans.getChildren().clear();
    }
}
