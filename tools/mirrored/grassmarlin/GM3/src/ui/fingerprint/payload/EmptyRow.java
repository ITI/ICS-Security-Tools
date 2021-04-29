package ui.fingerprint.payload;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.HBox;
import ui.fingerprint.tree.PayloadItem;


public class EmptyRow extends OpRow {

    public EmptyRow() {
        super(null);
    }

    @Override
    public HBox getInput() {
        return new HBox();
    }

    @Override
    public Object getOperation() {
        return null;
    }

    @Override
     public ObservableList<PayloadItem.OpType> getAvailableOps() {
        return FXCollections.observableArrayList();
    }
}
