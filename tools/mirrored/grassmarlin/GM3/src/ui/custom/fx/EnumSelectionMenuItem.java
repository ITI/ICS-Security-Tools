package ui.custom.fx;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public class EnumSelectionMenuItem<TEnum extends Enum<TEnum>> extends Menu {
    private final SimpleObjectProperty<TEnum> selectedValue;

    public EnumSelectionMenuItem(String prompt, TEnum initialValue) {
        this(prompt, null, initialValue);
    }
    public EnumSelectionMenuItem(String prompt, Node graphic, TEnum initialValue) {
        super(prompt, graphic);

        selectedValue = new SimpleObjectProperty<>(initialValue);

        for(TEnum value : initialValue.getDeclaringClass().getEnumConstants()) {
            CheckMenuItem miValue = new CheckMenuItem(value.toString());
            miValue.setOnAction(event -> {
                selectedValue.set(value);
            });

            this.getItems().add(miValue);
        }

        this.setOnShowing(event -> {
            for(MenuItem child : getItems()) {
                if(child instanceof CheckMenuItem) {
                    ((CheckMenuItem)child).setSelected(child.getText().equals(selectedValue.get().toString()));
                }
            }
        });
    }

    public ObjectProperty<TEnum> selectedValueProperty() {
        return selectedValue;
    }
}
