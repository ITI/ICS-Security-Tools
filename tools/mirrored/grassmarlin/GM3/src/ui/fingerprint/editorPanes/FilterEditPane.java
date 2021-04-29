package ui.fingerprint.editorPanes;

import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import ui.fingerprint.FingerPrintGui;
import ui.fingerprint.tree.FilterItem;


public class FilterEditPane extends GridPane {

    private FilterItem fi;
    private FingerPrintGui gui;

    public FilterEditPane(FilterItem item, FingerPrintGui gui) {
        super();
        this.fi = item;
        this.gui = gui;

        this.setHgap(10);
        this.setPadding(new Insets(30, 5, 30, 5));

        fi.getRow().insert(this, 0);
    }
}
