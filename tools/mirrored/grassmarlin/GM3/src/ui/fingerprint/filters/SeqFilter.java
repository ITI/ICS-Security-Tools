package ui.fingerprint.filters;

import core.fingerprint3.ObjectFactory;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import ui.fingerprint.FingerPrintGui;

import javax.xml.bind.JAXBElement;

public class SeqFilter implements Filter<Long> {
    ObjectFactory factory;
    Long value;
    SimpleObjectProperty<JAXBElement<Long>> element;

    public SeqFilter(JAXBElement<Long> value) {
        factory = new ObjectFactory();
        element = new SimpleObjectProperty<>();
        if (null == value) {
            this.value = 0L;
            element.setValue(factory.createFingerprintFilterSeq(this.value));
        } else {
            this.value = value.getValue();
            element.setValue(value);
        }
    }

    public SeqFilter() {
        this(null);
    }


    @Override
    public HBox getInput() {
        HBox input = new HBox();

        Label seqLabel = new Label("Sequence Number:");
        TextField seqField = new TextField(Long.toString(value));

        seqField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                //don't allow wrong entries
                try {
                    Long newSeqNum = Long.parseLong(newValue);
                    value = newSeqNum;
                    element.setValue(factory.createFingerprintFilterSeq(value));
                } catch (NumberFormatException e) {
                    if (seqField.getText().isEmpty()) {
                        seqField.setText("0");
                        FingerPrintGui.selectAll(seqField);
                    } else {
                        seqField.setText(oldValue);
                    }
                }
            }
        });

        seqField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                FingerPrintGui.selectAll(seqField);
            }
        });

        input.setSpacing(2);
        input.setAlignment(Pos.CENTER_RIGHT);
        input.getChildren().addAll(seqLabel, seqField);

        return input;
    }

    @Override
    public FilterType getType() {
        return FilterType.SEQ;
    }

    @Override
    public SimpleObjectProperty<JAXBElement<Long>> elementProperty() {
        return element;
    }
}
