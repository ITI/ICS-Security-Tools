package core;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class IdentityCellValue<T> implements Callback<TableColumn.CellDataFeatures<T, T>, ObservableValue<T>> {
    public IdentityCellValue() {
        //No initialization required
    }

    @Override
    public ObservableValue<T> call(TableColumn.CellDataFeatures<T, T> i) {
        return new ReadOnlyObjectWrapper<>(i.getValue());
    }
}
