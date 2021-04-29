package ui.custom.fx;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import java.util.function.Supplier;

public class LazyProperty<T> extends SimpleObjectProperty<T> {
    private boolean isLoaded;
    private final Supplier<T> fnGet;

    public LazyProperty(final Supplier<T> method) {
        super(null, method.toString());

        fnGet = method;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T get() {
        if(!isLoaded && !isBound()) {
            this.set(fnGet.get());
        }
        return super.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void set(T newValue) {
        isLoaded = true;
        super.set(newValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void bind(final ObservableValue<? extends T> newObservable) {
        isLoaded = true;
        super.bind(newObservable);
    }

    public void clear() {
        isLoaded = false;
    }

}
