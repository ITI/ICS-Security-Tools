package ui.fingerprint.filters;

import core.fingerprint3.ObjectFactory;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import ui.fingerprint.FingerPrintGui;

import javax.xml.bind.JAXBElement;

public class DestPortFilter implements Filter<Integer> {
    private final static int DEFAULT_PORT = 80;
    private final static int MAX_VALUE = 65535;
    private final static int MIN_VALUE = 0;

    private int port;
    private ObjectFactory factory;
    private SimpleObjectProperty<JAXBElement<Integer>> element;

    public DestPortFilter(JAXBElement<Integer> value) {
        factory = new ObjectFactory();
        element = new SimpleObjectProperty<>();
        if (null == value) {
            port = DEFAULT_PORT;
            element.setValue(factory.createFingerprintFilterDstPort(port));
        } else {
            port = value.getValue();
            element.setValue(value);
        }
    }

    public DestPortFilter() {
        this(null);
    }

    @Override
    public HBox getInput() {
        HBox inputBox = new HBox();

        Label portLabel = new Label("Port:");
        TextField portField = new TextField();
        portField.setText(Integer.toString(port));

        portField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                //don't allow wrong entries
                try {
                    int newPort = Integer.parseInt(newValue);
                    if (newPort > MAX_VALUE || newPort < MIN_VALUE) {
                        portField.setText(oldValue);
                    } else {
                        port = newPort;
                        element.setValue(factory.createFingerprintFilterDstPort(port));
                    }
                } catch (NumberFormatException e) {
                    if (portField.getText().isEmpty()) {
                        portField.setText("0");
                        FingerPrintGui.selectAll(portField);
                    } else {
                        portField.setText(oldValue);
                    }
                }
            }
        });

        portField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                FingerPrintGui.selectAll(portField);
            }
        });

        inputBox.setAlignment(Pos.CENTER_RIGHT);
        inputBox.setSpacing(2);
        inputBox.getChildren().addAll(portLabel, portField);
        return inputBox;
    }

    @Override
    public FilterType getType() {
        return FilterType.DSTPORT;
    }

    @Override
    public SimpleObjectProperty<JAXBElement<Integer>> elementProperty() {
        return this.element;
    }

}
