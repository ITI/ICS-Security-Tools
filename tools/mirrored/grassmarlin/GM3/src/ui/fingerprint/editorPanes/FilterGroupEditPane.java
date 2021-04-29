package ui.fingerprint.editorPanes;

import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import ui.fingerprint.FingerPrintGui;
import ui.fingerprint.filters.Filter;
import ui.fingerprint.tree.FPItem;
import ui.fingerprint.tree.FilterGroupItem;
import ui.fingerprint.tree.FilterItem;

import java.util.List;
import java.util.stream.Collectors;

public class FilterGroupEditPane extends GridPane {

    private FilterGroupItem boundItem;
    private FingerPrintGui gui;

    public FilterGroupEditPane(FilterGroupItem boundItem, FingerPrintGui gui) {
        this.boundItem = boundItem;
        this.gui = gui;

        this.setVgap(5);
        this.setHgap(2);
        this.setPadding(new Insets(5));

        this.init();

        InvalidationListener childListener = observable -> {
            this.getChildren().clear();
            this.init();
        };

        this.boundItem.getChildren().addListener(childListener);
    }

    private void init() {
        if (boundItem.getChildren().isEmpty()) {
            this.gui.addFilter(boundItem, false);
            ((FilterItem) boundItem.getChildren().get(0)).getRow().insert(this, 0);
            this.add(getButtons(), 2, 0);
        } else {
            for (int i = 0; i < boundItem.getChildren().size(); i++) {
                TreeItem<String> child = boundItem.getChildren().get(i);
                if (child instanceof FilterItem) {
                    FilterItem filter = ((FilterItem) child);
                    filter.getRow().insert(this, i);
                    this.add(getButtons(), 2, i);
                }
            }
        }
    }

    private HBox getButtons() {
        HBox buttonBox = new HBox(1);
        Button addButton = new Button("+");
        Button delButton = new Button("-");

        addButton.setOnAction(event -> {
            FPItem fp = this.gui.getFPItem(this.boundItem);
            int storedIndex = gui.getDocument().addFilter(fp.getName(), fp.pathProperty().get(),
                    gui.getPayloadItem(this.boundItem).getName(), this.boundItem.getName(), gui.getDefaultFilterElement());

            int currentIndex = GridPane.getRowIndex(buttonBox);
            int newRowIndex = currentIndex + 1;

            if (storedIndex >= 0) {
                FilterItem newItem = new FilterItem(Filter.FilterType.DSTPORT, storedIndex, gui);
                //insert after row that contains the button that was pressed;
                this.getChildren().forEach(child -> {
                    int row = GridPane.getRowIndex(child);
                    if (row > currentIndex) {
                        GridPane.setRowIndex(child, row + 1);
                        ((FilterItem) boundItem.getChildren().get(row)).setRowIndex(row + 1);
                    }
                });
                newItem.getRow().insert(this, newRowIndex);
                this.add(getButtons(), 2, newRowIndex);
                boundItem.getChildren().add(newRowIndex, newItem);
            }
        });

        delButton.setOnAction(event -> {
            int currentIndex = GridPane.getRowIndex(buttonBox);
            List<Node> toRemove = this.getChildren().stream()
                            .filter(child -> GridPane.getRowIndex(child) == currentIndex)
                            .collect(Collectors.toList());
            this.getChildren().removeAll(toRemove);
            boundItem.getChildren().remove(currentIndex);

            this.getChildren().forEach(child -> {
                int row = GridPane.getRowIndex(child);
                if (row > currentIndex) {
                    GridPane.setRowIndex(child, row - 1);
                }
            });
        });

        buttonBox.getChildren().addAll(addButton, delButton);

        return buttonBox;
    }
}
