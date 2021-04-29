package ui.fingerprint.filters;

import core.fingerprint3.Fingerprint;
import core.fingerprint3.ObjectFactory;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import ui.fingerprint.FingerPrintGui;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;

public class TtlWithinFilter implements Filter<Fingerprint.Filter.TTLWithin> {
    ObjectFactory factory;
    Fingerprint.Filter.TTLWithin value;
    SimpleObjectProperty<JAXBElement<Fingerprint.Filter.TTLWithin>> element;

    public TtlWithinFilter(JAXBElement<Fingerprint.Filter.TTLWithin> value) {
        factory = new ObjectFactory();
        element = new SimpleObjectProperty<>();
        if (null == value) {
            this.value = new Fingerprint.Filter.TTLWithin();
            this.value.setMax(new BigInteger("1"));
            this.value.setMin(new BigInteger("1"));
            element.setValue(factory.createFingerprintFilterTTLWithin(this.value));
        } else {
            this.value = value.getValue();
            element.setValue(value);
        }
    }

    public TtlWithinFilter() {
        this(null);
    }


    @Override
    public HBox getInput() {
        HBox input = new HBox();

        Label maxLabel = new Label("Max:");
        Label minLabel = new Label("Min:");

        TextField maxField = new TextField(value.getMax().toString());
        TextField minField = new TextField(value.getMin().toString());

        maxField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                //don't allow wrong entries
                try {
                    BigInteger newMax = new BigInteger(newValue);
                    value.setMax(newMax);
                    element.setValue(factory.createFingerprintFilterTTLWithin(value));
                } catch (NumberFormatException e) {
                    if (maxField.getText().isEmpty()) {
                        maxField.setText("0");
                        FingerPrintGui.selectAll(maxField);
                    } else {
                        maxField.setText(oldValue);
                    }
                }
            }
        });

        maxField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                FingerPrintGui.selectAll(maxField);
            }
        });

        minField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                //don't allow wrong entries
                try {
                    BigInteger newMin = new BigInteger(newValue);
                    value.setMin(newMin);
                    element.setValue(factory.createFingerprintFilterTTLWithin(value));
                } catch (NumberFormatException e) {
                    if (minField.getText().isEmpty()) {
                        minField.setText("0");
                        FingerPrintGui.selectAll(minField);
                    } else {
                        minField.setText(oldValue);
                    }
                }
            }
        });

        minField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                FingerPrintGui.selectAll(minField);
            }
        });

        input.setAlignment(Pos.CENTER_RIGHT);
        input.setSpacing(2);
        input.getChildren().addAll(minLabel, minField, maxLabel, maxField);

        return input;
    }

    @Override
    public FilterType getType() {
        return FilterType.TTLWITHIN;
    }

    @Override
    public SimpleObjectProperty<JAXBElement<Fingerprint.Filter.TTLWithin>> elementProperty() {
        return element;
    }
}
