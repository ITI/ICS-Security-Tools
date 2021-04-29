package ui.graphing.physical;

import core.document.graph.PhysicalNic;
import core.document.graph.PhysicalNode;
import ui.LocalIcon;
import ui.custom.fx.EnumSelectionMenuItem;

public class CellNic extends ControlPointCell<PhysicalNode> {
    protected enum EConnectionPoint {
        Top("Top"),
        Bottom("Bottom"),
        Left("Left"),
        Right("Right"),
        Center("Center")
        ;

        private final String displayText;

        EConnectionPoint(String displayText) {
            this.displayText = displayText;
        }

        @Override
        public String toString() {
            return displayText;
        }
    }

    private final PhysicalNic nic;

    public CellNic(PhysicalNic nic) {
        super(nic);

        this.nic = nic;

        //Add the Computer image.
        //TODO: This should be a NIC, not a computer...  but the image for that is not part of the image library we have on hand at the moment.
        boxInternalImages.getChildren().add(LocalIcon.forPath("images|physical|Computer.png").getView(16.0));

        EnumSelectionMenuItem<EConnectionPoint> miSelectEndpoint = new EnumSelectionMenuItem<>("Connect edges to", EConnectionPoint.Right);
        miSelectEndpoint.selectedValueProperty().addListener((observable, oldValue, newValue) -> {
            switch (newValue) {
                case Top:
                    edgeX.bind(layoutXProperty().add(widthProperty().divide(2.0)));
                    edgeY.bind(layoutYProperty());
                    break;
                case Bottom:
                    edgeX.bind(layoutXProperty().add(widthProperty().divide(2.0)));
                    edgeY.bind(layoutYProperty().add(heightProperty()));
                    break;
                case Left:
                    edgeX.bind(layoutXProperty());
                    edgeY.bind(layoutYProperty().add(heightProperty().divide(2.0)));
                    break;
                case Right:
                    edgeX.bind(layoutXProperty().add(widthProperty()));
                    edgeY.bind(layoutYProperty().add(heightProperty().divide(2.0)));
                    break;
                case Center:
                    edgeX.bind(layoutXProperty().add(widthProperty().divide(2.0)));
                    edgeY.bind(layoutYProperty().add(heightProperty().divide(2.0)));
                    break;
            }
        });
        //Set the bindings to match the default menu selection
        edgeX.bind(layoutXProperty().add(widthProperty()));
        edgeY.bind(layoutYProperty().add(heightProperty().divide(2.0)));

        controlPointProperty().bind(new OffsetBinding(pointCenter,
                edgeX.subtract(layoutXProperty().add(widthProperty().divide(2.0))).multiply(2.0),
                edgeY.subtract(layoutYProperty().add(heightProperty().divide(2.0))).multiply(2.0)
        ));

        menuItems.add(miSelectEndpoint);

        menuItems.remove(miAutoLayout);
        this.makeUndraggable();
    }

    public PhysicalNic getNic() {
        return nic;
    }
}
