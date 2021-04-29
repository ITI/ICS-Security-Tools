package ui.graphing.logical;

import core.document.graph.LogicalNode;
import ui.custom.fx.ActiveMenuItem;
import ui.dialog.ConnectionDetailsDialogFx;
import ui.graphing.graphs.LogicalFilterGraph;

public class CellLogicalHidable extends CellLogical {
    public CellLogicalHidable(final LogicalFilterGraph owner, LogicalNode node) {
        super(node);

        super.menuItems.add(
                new ActiveMenuItem("Hide Node", event -> {
                    owner.setNodeVisibility(node, false);
                })
        );
    }
}
