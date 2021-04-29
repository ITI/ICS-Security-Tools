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


public class DsizeFilter implements Filter<BigInteger> {

    private ObjectFactory factory;
    private BigInteger dSize;
    private SimpleObjectProperty<JAXBElement<BigInteger>> element;

    public DsizeFilter(JAXBElement<BigInteger> value) {
        factory = new ObjectFactory();
        element = new SimpleObjectProperty<>();
        if (null == value) {
            dSize = new BigInteger("0");
            element.setValue(factory.createFingerprintFilterDsize(dSize));
        } else {
            dSize = value.getValue();
            element.setValue(value);
        }
    }

    public DsizeFilter() {
        this(null);
    }


    @Override
    public HBox getInput() {
        HBox input = new HBox();

        Label dSizeLabel = new Label("DSize:");
        TextField dsizeField = new TextField(dSize.toString());

        dsizeField.textProperty().addListener((observable, oldValue, newValue) ->{
            if (!oldValue.equals(newValue)) {
                //don't allow wrong entries
                try {
                    BigInteger newSize = new BigInteger(newValue);
                    dSize = newSize;
                    element.setValue(factory.createFingerprintFilterDsize(dSize));
                } catch (NumberFormatException e) {
                    if (dsizeField.getText().isEmpty()) {
                        dsizeField.setText("0");
                        FingerPrintGui.selectAll(dsizeField);
                    } else {
                        dsizeField.setText(oldValue);
                    }
                }
            }
        });

        dsizeField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                FingerPrintGui.selectAll(dsizeField);
            }
        });

        input.setAlignment(Pos.CENTER_RIGHT);
        input.setSpacing(2);
        input.getChildren().addAll(dSizeLabel, dsizeField);

        return input;
    }

    @Override
    public FilterType getType() {
        return FilterType.DSIZE;
    }

    @Override
    public SimpleObjectProperty<JAXBElement<BigInteger>> elementProperty() {
        return element;
    }
}
