package ui.custom.fx;

import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;

/**
 * A SimpleLongProperty which fires events in the Fx Application Thread.
 */
public class FxLongProperty extends SimpleLongProperty{
    public FxLongProperty(long initialValue) {
        super(initialValue);
    }
    @Override
    protected void fireValueChangedEvent() {
        if(!Platform.isFxApplicationThread()) {
            Platform.runLater(this::fireValueChangedEvent);
        } else {
            super.fireValueChangedEvent();
        }
    }
}
