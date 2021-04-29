package ui.custom.fx;

import javafx.beans.binding.StringBinding;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collector;

public class FxStringFromCollectionBinding<T> extends StringBinding {
    private final Collection<T> source;
    private final Collector<CharSequence, ?, String> collector;
    private final Function<T, CharSequence> mapper;

    public FxStringFromCollectionBinding(ObservableSet<T> source, Collector<CharSequence, ?, String> collector, Function<T, CharSequence> mapper) {
        this((Collection<T>)source, collector, mapper);
        super.bind(source);
    }
    public FxStringFromCollectionBinding(ObservableList<T> source, Collector<CharSequence, ?, String> collector, Function<T, CharSequence> mapper) {
        this((Collection<T>)source, collector, mapper);
        super.bind(source);
    }

    private FxStringFromCollectionBinding(Collection<T> source, Collector<CharSequence, ?, String> collector, Function<T, CharSequence> mapper) {
        super();

        this.source = source;
        this.collector = collector;
        this.mapper = mapper;
    }

    @Override
    public String computeValue() {
        return source.stream().map(mapper).collect(collector);
    }
}
