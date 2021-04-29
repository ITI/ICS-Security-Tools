package ui.custom.fx;

import javafx.beans.binding.IntegerBinding;
import javafx.collections.ObservableList;

public class ListSizeBinding extends IntegerBinding {
    private final ObservableList<?> list;

    public ListSizeBinding(ObservableList<?> list) {
        this.list = list;

        super.bind(list);
    }

    @Override
    public int computeValue() {
        return list.size();
    }
}
