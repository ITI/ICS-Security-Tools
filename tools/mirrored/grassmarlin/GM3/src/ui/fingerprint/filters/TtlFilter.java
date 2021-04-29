package ui.fingerprint.filters;

import core.fingerprint3.ObjectFactory;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import ui.fingerprint.FingerPrintGui;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;

public class TtlFilter implements Filter<BigInteger> {
    private final static int MIN_VALUE = 0;
    private final static int DEFAULT_VALUE = 1;

    ObjectFactory factory;
    BigInteger ttl;
    SimpleObjectProperty<JAXBElement<BigInteger>> element;

    public TtlFilter(JAXBElement<BigInteger> value) {
        factory = new ObjectFactory();
        element = new SimpleObjectProperty<>();
        if (null == value) {
            ttl = new BigInteger(Integer.toString(DEFAULT_VALUE));
            element.setValue(factory.createFingerprintFilterTTL(ttl));
        } else {
            ttl = value.getValue();
            element.setValue(value);
        }
    }

    public TtlFilter() {
        this(null);
    }


    @Override
    public HBox getInput() {
        HBox input = new HBox();

        Label ttlLabel = new Label("TTL:");
        TextField ttlField = new TextField(ttl.toString());

        ttlField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                //don't allow wrong entries
                try {
                    int newTtl = Integer.parseInt(newValue);
                    if (newTtl < MIN_VALUE) {
                        ttlField.setText(oldValue);
                    } else {
                        ttl = new BigInteger(Integer.toString(newTtl));
                        element.setValue(factory.createFingerprintFilterTTL(ttl));
                    }
                } catch (NumberFormatException e) {
                    if (ttlField.getText().isEmpty()) {
                        ttlField.setText("0");
                        FingerPrintGui.selectAll(ttlField);
                    } else {
                        ttlField.setText(oldValue);
                    }
                }
            }
        });

        ttlField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                FingerPrintGui.selectAll(ttlField);
            }
        });

        input.setAlignment(Pos.CENTER_RIGHT);
        input.setSpacing(2);
        input.getChildren().addAll(ttlLabel, ttlField);

        return input;
    }

    @Override
    public FilterType getType() {
        return FilterType.TTL;
    }

    @Override
    public SimpleObjectProperty<JAXBElement<BigInteger>> elementProperty() {
        return element;
    }
}
