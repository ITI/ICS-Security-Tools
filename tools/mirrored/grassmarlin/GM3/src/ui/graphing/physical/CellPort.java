package ui.graphing.physical;

import core.document.graph.PhysicalNode;
import core.document.graph.PhysicalPort;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import ui.EmbeddedIcons;

public class CellPort extends ControlPointCell<PhysicalNode> {
    private static final double PORT_CONTROL_FACTOR = 3.0;
    private static class PortImageBinding extends ObjectBinding<EmbeddedIcons> {
        private final ObservableBooleanValue inverted;
        private final ObservableBooleanValue connected;

        public PortImageBinding(ObservableBooleanValue isInverted, ObservableBooleanValue isConnected) {
            super.bind(isInverted, isConnected);

            this.inverted = isInverted;
            this.connected = isConnected;
        }

        @Override
        public EmbeddedIcons computeValue() {
            if(inverted.get()) {
                if(connected.get()) {
                    return EmbeddedIcons.Port_Inverted_Connected;
                } else {
                    return EmbeddedIcons.Port_Inverted_Disconnected;
                }
            } else {
                if(connected.get()) {
                    return EmbeddedIcons.Port_Connected;
                } else {
                    return EmbeddedIcons.Port_Disconnected;
                }
            }
        }
    }

    private static class ImageBinding extends ObjectBinding<Image> {
        private final PortImageBinding port;

        public ImageBinding(PortImageBinding port) {
            super.bind(port);

            this.port = port;
        }

        @Override
        public Image computeValue() {
            return port.get().getRawImage();
        }
    }
    private static class ViewportBinding extends ObjectBinding<Rectangle2D> {
        private final PortImageBinding port;

        public ViewportBinding(PortImageBinding port) {
            super.bind(port);

            this.port = port;
        }

        @Override
        public Rectangle2D computeValue() {
            return port.get().getViewport();
        }
    }

    private final PhysicalPort port;
    private final SimpleBooleanProperty inverted;

    public CellPort(PhysicalPort port) {
        super(port);

        this.port = port;
        inverted = new SimpleBooleanProperty(false);

        //Suppress the normal content
        paneInternalContent.getChildren().clear();

        ImageView viewPort = new ImageView();
        viewPort.setFitWidth(GroupSwitch.IMAGE_WIDTH);
        viewPort.setFitHeight(GroupSwitch.IMAGE_HEIGHT);

        PortImageBinding binding = new PortImageBinding(inverted, port.connectedProperty());

        viewPort.imageProperty().bind(new ImageBinding(binding));
        viewPort.viewportProperty().bind(new ViewportBinding(binding));

        ImageView overlayDisabled = EmbeddedIcons.Vista_Blocked.getImage(GroupSwitch.IMAGE_HEIGHT);
        overlayDisabled.visibleProperty().bind(port.enabledProperty().not());
        ImageView overlayConfused = EmbeddedIcons.Vista_Warning.getImage(GroupSwitch.IMAGE_HEIGHT);
        if(port.getGroups().get(PhysicalPort.FIELD_VLAN_CONSISTENCY).equals("Mismatched")) {
            overlayConfused.visibleProperty().set(true);
        } else {
            overlayConfused.visibleProperty().bind(port.unknownConnectionProperty());
        }

        StackPane stackImages = new StackPane();
        stackImages.getChildren().addAll(viewPort, overlayDisabled, overlayConfused);

        boxExternalImages.getChildren().add(stackImages);

        controlPointProperty().bind(new When(invertedProperty())
                .then(new OffsetBinding(pointCenter, null, heightProperty().multiply(PORT_CONTROL_FACTOR)))
                .otherwise(new OffsetBinding(pointCenter, null, heightProperty().multiply(-PORT_CONTROL_FACTOR))));

        menuItems.remove(miAutoLayout);
        this.makeUndraggable();
    }

    public BooleanProperty invertedProperty() {
        return inverted;
    }
    public PhysicalPort getPort() {
        return port;
    }
}
