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

public class WindowFilter implements Filter<BigInteger> {
    private final static int MAX_VALUE = 1073725440;
    private final static int MIN_VALUE = 0;

    ObjectFactory factory;
    BigInteger value;
    SimpleObjectProperty<JAXBElement<BigInteger>> element;

    public WindowFilter(JAXBElement<BigInteger> value) {
        factory = new ObjectFactory();
        element = new SimpleObjectProperty<>();
        if (null == value) {
            this.value = new BigInteger("0");
            element.setValue(factory.createFingerprintFilterWindow(this.value));
        } else {
            this.value = value.getValue();
            element.setValue(value);
        }
    }

    public WindowFilter() {
        this(null);
    }


    @Override
    public HBox getInput() {
        HBox input = new HBox();

        Label windowLabel = new Label("Value:");
        TextField windowField = new TextField(value.toString());

        windowField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                //don't allow wrong entries
                try {
                    BigInteger newWindow = new BigInteger(newValue);
                    if (newWindow.intValue() > MAX_VALUE || newWindow.intValue() < MIN_VALUE) {
                        windowField.setText(oldValue);
                    } else {
                        value = newWindow;
                        element.setValue(factory.createFingerprintFilterWindow(value));
                    }
                } catch (NumberFormatException e) {
                    if (windowField.getText().isEmpty()) {
                        windowField.setText("0");
                        FingerPrintGui.selectAll(windowField);
                    } else {
                        windowField.setText(oldValue);
                    }
                }
            }
        });

        windowField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                FingerPrintGui.selectAll(windowField);
            }
        });

        input.setAlignment(Pos.CENTER_RIGHT);
        input.setSpacing(2);
        input.getChildren().addAll(windowLabel, windowField);

        return input;
    }

    @Override
    public FilterType getType() {
        return FilterType.WINDOW;
    }

    @Override
    public SimpleObjectProperty<JAXBElement<BigInteger>> elementProperty() {
        return element;
    }
}
