package ui.fingerprint.filters;

import core.fingerprint3.ObjectFactory;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlagsFilter implements Filter<String> {
    private final static String[] flagValues = new String[] {"NS", "CWR", "ECE", "URG", "ACK", "PSH", "RST", "SYN", "FIN"};

    private ObjectFactory factory;
    private List<String> flags;
    private SimpleObjectProperty<JAXBElement<String>> element;

    public FlagsFilter(JAXBElement<String> value) {
        factory = new ObjectFactory();
        element = new SimpleObjectProperty<>();
        flags = new ArrayList<>();
        if (null == value) {
            element.setValue(factory.createFingerprintFilterFlags(String.join(" ", flags)));
        } else {
            flags.addAll(Arrays.asList(value.getValue().split(" ")));
            element.setValue(value);
        }
    }

    public FlagsFilter() {
        this(null);
    }

    @Override
    public HBox getInput() {
        HBox input = new HBox();

        Label flagLabel = new Label("Flags:");

        HBox checks = new HBox(10);
        checks.setAlignment(Pos.CENTER);

        for (String value : flagValues) {
            CheckBox check = new CheckBox(value);
            check.setId(value);
            if (flags.contains(value)) {
                check.setSelected(true);
            }
            check.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (check.isSelected()) {
                    flags.add(check.getId());
                } else {
                    flags.remove(check.getId());
                }

                element.setValue(factory.createFingerprintFilterFlags(String.join(" ", flags)));
            });

            checks.getChildren().add(check);
        }

        input.setAlignment(Pos.CENTER_RIGHT);
        input.setSpacing(8);
        input.getChildren().addAll(flagLabel, checks);

        return input;
    }

    @Override
    public FilterType getType() {
        return FilterType.FLAGS;
    }

    @Override
    public SimpleObjectProperty<JAXBElement<String>> elementProperty() {
        return element;
    }
}
