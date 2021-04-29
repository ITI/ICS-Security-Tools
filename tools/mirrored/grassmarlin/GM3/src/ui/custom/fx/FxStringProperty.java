package ui.custom.fx;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;

/**
 * A SimpleStringProperty that performs updates in the FxApplicationThread
 */
public class FxStringProperty extends SimpleStringProperty {
    @Override
    protected void fireValueChangedEvent() {
        if(!Platform.isFxApplicationThread()) {
            Platform.runLater(this::fireValueChangedEvent);
        } else {
            super.fireValueChangedEvent();
        }
    }
}
