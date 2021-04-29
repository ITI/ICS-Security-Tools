package ui.custom.fx;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;

public class FxThresholdDoubleProperty extends SimpleDoubleProperty {
    private final double dThreshold;
    private double dLast;

    public FxThresholdDoubleProperty(double threshold) {
        super();

        dThreshold = threshold;
        dLast = 0.0;
    }

    public FxThresholdDoubleProperty(double threshold, double value) {
        super(value);

        dThreshold = threshold;
        dLast = value;
    }

    public void setForceUpdate(double value) {
        dLast = Double.NaN;
        set(value);
    }

    @Override
    public void set(double value) {
        super.set(value);
    }

    @Override
    public double get() {
        return super.get();
    }

    @Override
    protected void fireValueChangedEvent() {
        if(Double.isNaN(dLast) || Math.abs(get() - dLast) >= dThreshold) {
            dLast = get();

            if (!Platform.isFxApplicationThread()) {
                Platform.runLater(this::fireValueChangedEvent);
            } else {
                super.fireValueChangedEvent();
            }
        }
    }
}
