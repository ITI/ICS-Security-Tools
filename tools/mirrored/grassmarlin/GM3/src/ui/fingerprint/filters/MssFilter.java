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

public class MssFilter implements Filter<BigInteger> {
    private final static int MIN_VALUE = 0;
    private final static int DEFAULT_VALUE = 1460; // Standard 1500 byte MTU minus 40 byte TCP Header

    private ObjectFactory factory;
    private BigInteger value;
    private SimpleObjectProperty<JAXBElement<BigInteger>> element;


    public MssFilter(JAXBElement<BigInteger> value) {
        factory = new ObjectFactory();
        element = new SimpleObjectProperty<>();
        if (null == value) {
            this.value = new BigInteger(Integer.toString(DEFAULT_VALUE));
            element.setValue(factory.createFingerprintFilterMSS(this.value));
        } else {
            this.value = value.getValue();
            element.setValue(value);
        }
    }

    public MssFilter() {
        this(null);
    }


    @Override
    public HBox getInput() {
        HBox input = new HBox();

        Label mssLabel = new Label("MSS:");
        TextField mssField = new TextField(value.toString());

        mssField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                //don't allow wrong entries
                try {
                    int newMss = Integer.parseInt(newValue);
                    if (newMss < MIN_VALUE) {
                        mssField.setText(oldValue);
                    } else {
                        value = new BigInteger(newValue);
                        element.setValue(factory.createFingerprintFilterMSS(value));
                    }
                } catch (NumberFormatException e) {
                    if (mssField.getText().isEmpty()) {
                        mssField.setText("0");
                        FingerPrintGui.selectAll(mssField);
                    } else {
                        mssField.setText(oldValue);
                    }
                }
            }
        });

        mssField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                FingerPrintGui.selectAll(mssField);
            }
        });

        input.setAlignment(Pos.CENTER_RIGHT);
        input.setSpacing(2);
        input.getChildren().addAll(mssLabel, mssField);

        return input;
    }

    @Override
    public FilterType getType() {
        return FilterType.MSS;
    }

    @Override
    public SimpleObjectProperty<JAXBElement<BigInteger>> elementProperty() {
        return element;
    }
}
