package ui.fingerprint.filters;

import core.fingerprint3.ObjectFactory;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import javax.xml.bind.JAXBElement;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

public class TransportProtoFilter implements Filter<Short> {
    private final static short TCP_PROTO_NUM = 6;
    private Map<Short, String> supportedProtocols;

    ObjectFactory factory;
    short proto;
    SimpleObjectProperty<JAXBElement<Short>> element;

    public TransportProtoFilter(JAXBElement<Short> value) {
        supportedProtocols = new HashMap<>();
        supportedProtocols.put((short)4, "IPv4");
        supportedProtocols.put((short)6, "TCP");
        supportedProtocols.put((short)17, "UDP");

        factory = new ObjectFactory();
        element = new SimpleObjectProperty<>();
        if (null == value) {
            proto = TCP_PROTO_NUM;
            element.setValue(factory.createFingerprintFilterTransportProtocol(proto));
        } else {
            proto = value.getValue();
            element.setValue(value);
        }
    }

    public TransportProtoFilter() {
        this(null);
    }


    @Override
    public HBox getInput() {
        HBox input =  new HBox();

        Label protoLabel = new Label("Protocol:");
        ChoiceBox<Short> protoBox = new ChoiceBox<>(FXCollections.observableArrayList(supportedProtocols.keySet()));
        protoBox.setConverter(new ProtocolConverter());
        protoBox.setValue(this.proto);

        protoBox.valueProperty().addListener(change -> {
            proto = protoBox.getValue();
            element.setValue(factory.createFingerprintFilterTransportProtocol(proto));
        });

        input.setSpacing(2);
        input.setAlignment(Pos.CENTER_RIGHT);
        input.getChildren().addAll(protoLabel, protoBox);

        return input;
    }

    @Override
    public FilterType getType() {
        return FilterType.TRANSPORTPROTOCOL;
    }

    @Override
    public SimpleObjectProperty<JAXBElement<Short>> elementProperty() {
        return element;
    }

    private class ProtocolConverter extends StringConverter<Short> {

        @Override
        public String toString(Short protoNum) {
            return supportedProtocols.get(protoNum);
        }

        @Override
        public Short fromString(String protoName) {
            OptionalInt protoNum = supportedProtocols.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(protoName))
                    .mapToInt(entry -> entry.getKey())
                    .findFirst();

            return (short)protoNum.getAsInt();
        }
    }
}
