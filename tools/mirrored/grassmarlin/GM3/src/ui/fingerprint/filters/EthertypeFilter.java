package ui.fingerprint.filters;

import core.fingerprint3.ObjectFactory;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import ui.fingerprint.FingerPrintGui;

import javax.xml.bind.JAXBElement;

public class EthertypeFilter implements Filter<Integer> {
    private final static int DEFAULT_VALUE = 0x0800; //IPv4
    private final static int MAX_VALUE = 65535;
    private final static int MIN_VALUE = 0;

    ObjectFactory factory;
    int type;
    SimpleObjectProperty<JAXBElement<Integer>> element;

    public EthertypeFilter(JAXBElement<Integer> value) {
        factory = new ObjectFactory();
        element = new SimpleObjectProperty<>();
        if (null == value) {
            type = DEFAULT_VALUE;
            element.setValue(factory.createFingerprintFilterEthertype(type));
        } else {
            type = value.getValue();
            element.setValue(value);
        }
    }

    public EthertypeFilter() {
        this(null);
    }


    @Override
    public HBox getInput() {
        HBox input = new HBox();

        Label typeLabel = new Label("Value:");
        TextField typeField = new TextField(Integer.toString(type));

        typeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                //don't allow wrong entries
                try {
                    int newType = Integer.parseInt(newValue);
                    if (newType > MAX_VALUE || newType < MIN_VALUE) {
                        typeField.setText(oldValue);
                    } else {
                        type = newType;
                        element.setValue(factory.createFingerprintFilterEthertype(type));
                    }
                } catch (NumberFormatException e) {
                    if (typeField.getText().isEmpty()) {
                        typeField.setText("0");
                        FingerPrintGui.selectAll(typeField);
                    } else {
                        typeField.setText(oldValue);
                    }
                }
            }
        });

        typeField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                FingerPrintGui.selectAll(typeField);
            }
        });

        input.setAlignment(Pos.CENTER_RIGHT);
        input.setSpacing(2);
        input.getChildren().addAll(typeLabel, typeField);

        return input;
    }

    @Override
    public FilterType getType() {
        return FilterType.ETHERTYPE;
    }

    @Override
    public SimpleObjectProperty<JAXBElement<Integer>> elementProperty() {
        return element;
    }
}
