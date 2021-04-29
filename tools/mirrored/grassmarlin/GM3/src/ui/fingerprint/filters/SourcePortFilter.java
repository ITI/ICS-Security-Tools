package ui.fingerprint.filters;

import core.fingerprint3.ObjectFactory;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import ui.fingerprint.FingerPrintGui;

import javax.xml.bind.JAXBElement;


public class SourcePortFilter implements Filter<Integer> {
    private final static int MAX_VALUE = 65535;
    private final static int MIN_VALUE = 0;

    private ObjectFactory factory;
    private int port;
    private SimpleObjectProperty<JAXBElement<Integer>> element;

    public SourcePortFilter(JAXBElement<Integer> value) {
        factory = new ObjectFactory();
        element = new SimpleObjectProperty<>();
        if (null == value) {
            port = 0;
            element.setValue(factory.createFingerprintFilterSrcPort(port));
        } else {
            port = value.getValue();
            element.setValue(value);
        }
    }

    public SourcePortFilter() {
        this(null);
    }


    @Override
    public HBox getInput() {
        HBox input = new HBox();

        Label portLabel = new Label("Port:");
        TextField portField = new TextField(Integer.toString(port));

        portField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                //don't allow wrong entries
                try {
                    int newPort = Integer.parseInt(newValue);
                    if (newPort > MAX_VALUE || newPort < MIN_VALUE) {
                        portField.setText(oldValue);
                    } else {
                        port = newPort;
                        element.setValue(factory.createFingerprintFilterSrcPort(port));
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

        input.setAlignment(Pos.CENTER_RIGHT);
        input.setSpacing(2);
        input.getChildren().addAll(portLabel, portField);

        return input;
    }

    @Override
    public FilterType getType() {
        return FilterType.SRCPORT;
    }

    @Override
    public SimpleObjectProperty<JAXBElement<Integer>> elementProperty() {
        return element;
    }
}
