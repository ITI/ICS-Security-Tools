package ui.fingerprint.editorPanes;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import ui.fingerprint.filters.Filter;
import ui.fingerprint.filters.Filter.FilterType;
import ui.fingerprint.tree.FilterItem;

import javax.xml.bind.JAXBElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class FilterRow {
    
    private FilterItem boundItem;
    private Filter<?> filter;
    private ComboBox<FilterType> filterBox;
    private int row;

    public FilterRow(FilterItem item, JAXBElement value){
        this.boundItem = item;
        if (null != value) {
            try {
                Constructor<? extends Filter> constructor = item.getType().getImplementingClass().getConstructor(JAXBElement.class);
                filter = constructor.newInstance(value);
            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                //TODO proper error handling
            }
        }
    }

    public void insert(GridPane parent, int row) {
        this.row = row;
        filterBox = new ComboBox<>();
        Arrays.stream(FilterType.values())
                .sorted((type, otherType) -> type.getName().compareTo(otherType.getName()))
                .forEach(type -> filterBox.getItems().add(type));

        filterBox.setValue(boundItem.getType());

        filterBox.setConverter(new FilterStringConverter());
        filterBox.setCellFactory(new TooltipCellFactory());
        filterBox.setVisibleRowCount(5);
        filterBox.setEditable(false);
        filterBox.setOnAction(event -> {
            FilterType type = filterBox.getSelectionModel().getSelectedItem();

            boundItem.setType(type);

            try {
                filter = type.getImplementingClass().newInstance();
                addListener(filter);
                boundItem.updateValue(filter.elementProperty().get());
                HBox input = filter.getInput();
                input.setAlignment(Pos.CENTER_LEFT);
                Node replaceMe = null;
                for (Node node : parent.getChildren()) {
                    if (node instanceof HBox && GridPane.getColumnIndex(node) == 1 && GridPane.getRowIndex(node) == this.row) {
                        replaceMe = node;
                        //found it
                        break;
                    }
                }
                if (null != replaceMe) {
                    parent.getChildren().remove(replaceMe);
                }
                parent.add(input, 1, this.row);
            } catch (IllegalAccessException | InstantiationException e) {
                //TODO proper error handling
                e.printStackTrace();
            }
        });

        // initialize on default filter
        if (filter == null) {
            FilterType type = filterBox.getSelectionModel().getSelectedItem();

            try {
                filter = type.getImplementingClass().newInstance();
                addListener(filter);
            } catch (IllegalAccessException | InstantiationException e) {
                //TODO proper error handling
                e.printStackTrace();
            }

        }
        if (filter != null) {
            HBox input = filter.getInput();
            input.setAlignment(Pos.CENTER_LEFT);
            Node replaceMe = null;
            for (Node node : parent.getChildren()) {
                if (node instanceof HBox && GridPane.getColumnIndex(node) == 1 && GridPane.getRowIndex(node) == this.row) {
                    replaceMe = node;
                    break;
                }
            }
            if (null != replaceMe) {
                parent.getChildren().remove(replaceMe);
            }
            parent.add(input, 1, this.row);
        }

        parent.add(filterBox, 0, this.row);

    }

    private void addListener(Filter<?> filter) {
        if (null != filter.elementProperty()) {
            filter.elementProperty().addListener((observable, oldValue, newValue) -> boundItem.updateValue(newValue));
        }
    }

    public void setFocus() {
        this.filterBox.requestFocus();
    }

    public void setRow(int index) {
        this.row = index;
    }

    public void addTcpFilter(GridPane parent) {

    }

    private class FilterStringConverter extends StringConverter<FilterType> {
        @Override
        public String toString(FilterType type) {
            return type.getName();
        }

        @Override
        public FilterType fromString(String name) {
            FilterType returnType = null;
            for (FilterType type : FilterType.values()) {
                if (type.getName().equals(name)) {
                   returnType = type;
                }
            }

            return returnType;
        }
    }

    private class TooltipCellFactory implements Callback<ListView<FilterType>, ListCell<FilterType>> {
        @Override
        public ListCell<FilterType> call(ListView<FilterType> view) {
            return new ListCell<FilterType>() {
                @Override
                protected void updateItem(FilterType item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item == null || empty) {
                        setTooltip(null);
                        setText(null);
                    } else {
                        setTooltip(new Tooltip(item.getTooltip()));
                        setText(item.getName());
                    }
                }
            };
        }
    }

    public JAXBElement getElement() {
        return this.filter.elementProperty().get();
    }
}
