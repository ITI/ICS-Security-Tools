package ui.custom.fx;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.NumberBinding;

public class Log10DoubleBinding extends DoubleBinding {
    public static DoubleBinding of(NumberBinding source) {
        return new Log10DoubleBinding(source);
    }

    private NumberBinding source;

    public Log10DoubleBinding(NumberBinding source) {
        super.bind(source);
        this.source = source;
    }

    @Override
    public double computeValue() {
        return Math.max(Math.log10(source.getValue().doubleValue()), 1);
    }
}
