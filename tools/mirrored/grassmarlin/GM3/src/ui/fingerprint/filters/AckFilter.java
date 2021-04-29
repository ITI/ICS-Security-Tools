package ui.fingerprint.filters;


import core.fingerprint3.ObjectFactory;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import ui.fingerprint.FingerPrintGui;

import javax.xml.bind.JAXBElement;

public class AckFilter implements Filter<Long>{
    private static long MAX_VALUE = Long.MAX_VALUE;
    private static long MIN_VALUE = Long.MIN_VALUE;

    ObjectFactory factory;
    Long ack;
    SimpleObjectProperty<JAXBElement<Long>> element;

    public AckFilter(JAXBElement<Long> value) {
        factory = new ObjectFactory();
        element = new SimpleObjectProperty<>();
        if (null == value) {
            ack = 0L;
            element.setValue(factory.createFingerprintFilterAck(ack));
        } else {
            ack = value.getValue();
            element.setValue(value);
        }
    }

    public AckFilter() {
        this(null);
    }

    @Override
    public HBox getInput() {
        HBox input = new HBox(2);

        Label ackLabel = new Label("ACK Number:");
        TextField ackField = new TextField(Long.toString(ack));

        ackField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                //don't allow wrong entries
                try {
                    long newAck = Long.parseLong(newValue);
                    if (newAck > MAX_VALUE || newAck < MIN_VALUE) {
                        ackField.setText(oldValue);
                    } else {
                        ack = newAck;
                        element.setValue(factory.createFingerprintFilterAck(ack));
                    }
                } catch (NumberFormatException e) {
                    if (ackField.getText().isEmpty()) {
                        ackField.setText("0");
                        FingerPrintGui.selectAll(ackField);
                    } else {
                        ackField.setText(oldValue);
                    }
                }
            }
        });

        ackField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                FingerPrintGui.selectAll(ackField);
            }
        });

        input.setAlignment(Pos.CENTER_RIGHT);
        input.getChildren().addAll(ackLabel, ackField);
        return input;
    }

    @Override
    public FilterType getType() {
        return FilterType.ACK;
    }

    @Override
    public SimpleObjectProperty<JAXBElement<Long>> elementProperty() {
        return element;
    }
}
