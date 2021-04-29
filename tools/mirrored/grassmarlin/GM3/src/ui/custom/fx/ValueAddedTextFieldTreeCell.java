package ui.custom.fx;


import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.util.StringConverter;

import java.lang.ref.WeakReference;


/**
 * A {@link javafx.scene.control.cell.TextFieldTreeCell} that takes a {@link javafx.beans.binding.StringBinding}
 * the value of which will be concatenated to the end of actual value of the tree item for display
 */
public class ValueAddedTextFieldTreeCell<T> extends TreeCell<T> {

    static int TREE_VIEW_HBOX_GRAPHIC_PADDING = 3;

    private ObservableValue<String> toConcat;
    private Paint concatFill;
    private OverrunStyle concatOverrun;

    private TextField textField;
    private HBox hbox;

    // --- converter
    private ObjectProperty<StringConverter<T>> converter = new SimpleObjectProperty<>(this, "converter");

    private WeakReference<TreeItem<T>> treeItemRef;

    private InvalidationListener treeItemGraphicListener = observable -> {
        updateDisplay(getItem(), isEmpty());
    };

    private InvalidationListener treeItemListener = new InvalidationListener() {
        @Override public void invalidated(Observable observable) {
            TreeItem<T> oldTreeItem = treeItemRef == null ? null : treeItemRef.get();
            if (oldTreeItem != null) {
                oldTreeItem.graphicProperty().removeListener(weakTreeItemGraphicListener);
            }

            TreeItem<T> newTreeItem = getTreeItem();
            if (newTreeItem != null) {
                newTreeItem.graphicProperty().addListener(weakTreeItemGraphicListener);
                treeItemRef = new WeakReference<>(newTreeItem);
            }
        }
    };

    private WeakInvalidationListener weakTreeItemGraphicListener =
            new WeakInvalidationListener(treeItemGraphicListener);

    private WeakInvalidationListener weakTreeItemListener =
            new WeakInvalidationListener(treeItemListener);

    /**
     * The {@link StringConverter} property.
     */
    public final ObjectProperty<StringConverter<T>> converterProperty() {
        return converter;
    }

    /**
     * Sets the {@link StringConverter} to be used in this cell.
     */
    public final void setConverter(StringConverter<T> value) {
        converterProperty().set(value);
    }

    /**
     * Returns the {@link StringConverter} used in this cell.
     */
    public final StringConverter<T> getConverter() {
        return converterProperty().get();
    }

    /**
     * Creates a ValueAddedTextFieldTreeCell that provides a {@link TextField} when put
     * into editing mode that allows editing of the cell content. This method
     * will work on any TreeView instance, regardless of its generic type.
     * However, to enable this, a {@link StringConverter} must be provided that
     * will convert the given String (from what the user typed in) into an
     * instance of type T. This item will then be passed along to the
     * {@link TreeView#onEditCommitProperty()} callback.
     *
     * @param converter A {@link StringConverter converter} that can convert
     *      the given String (from what the user typed in) into an instance of
     *      type T.
     */
    public ValueAddedTextFieldTreeCell(StringConverter<T> converter) {
        setConverter(converter);
        this.concatFill = this.getTextFill();
        this.concatOverrun = this.getTextOverrun();

        treeItemProperty().addListener(weakTreeItemListener);

        if (getTreeItem() != null) {
            getTreeItem().graphicProperty().addListener(weakTreeItemGraphicListener);
        }
    }

    public void setBinding(ObservableValue<String> toConcat) {
        if (toConcat == null) {
            this.textProperty().unbind();
            this.toConcat = toConcat;
        } else {
            this.toConcat = toConcat;
        }
    }

    public void setConcatFill(Paint concatFill) {
        this.concatFill = concatFill;
    }

    public void setConcatOverrun(OverrunStyle style) {
        this.concatOverrun = style;
    }


    void updateDisplay(T item, boolean empty) {
        textProperty().unbind();
        setText(null);
        if (item == null || empty) {
            hbox = null;
            setGraphic(null);
        } else {
            // update the graphic if one is set in the TreeItem
            TreeItem<T> treeItem = getTreeItem();
            Label concatText = new Label("");
            concatText.setTextFill(this.concatFill);
            concatText.setTextOverrun(this.concatOverrun);
            if (toConcat != null) {
                concatText.textProperty().bind(toConcat);
            }
            if (hbox == null) {
                hbox = new HBox(3);
            }
            if (treeItem != null && treeItem.getGraphic() != null) {
                if (item instanceof Node) {

                    // the item is a Node, and the graphic exists, so
                    // we must insert both into an HBox and present that
                    // to the user (see RT-15910)

                    hbox.getChildren().setAll(treeItem.getGraphic(), (Node)item, concatText);
                    setGraphic(hbox);
                } else {
                    Text itemText = new Text(item.toString());
                    hbox.getChildren().setAll(treeItem.getGraphic(), itemText, concatText);
                    setGraphic(hbox);
                }
            } else {
                if (item instanceof Node) {

                    hbox.getChildren().setAll((Node)item, concatText);
                    setGraphic(hbox);
                } else {
                    Text itemText = new Text(item.toString());
                    hbox.getChildren().setAll(itemText, concatText);
                    setGraphic(hbox);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override public void startEdit() {
        if (! isEditable() || ! getTreeView().isEditable()) {
            return;
        }
            super.startEdit();

        if (isEditing()) {
            StringConverter<T> converter = getConverter();
            if (textField == null) {
                textField = createTextField(this, converter);
            }
            if (hbox == null) {
                hbox = new HBox(TREE_VIEW_HBOX_GRAPHIC_PADDING);
            }

            startEdit(this, converter, hbox, getTreeItemGraphic(), textField);
        }
    }

    /** {@inheritDoc} */
    @Override public void cancelEdit() {
        super.cancelEdit();
        cancelEdit(this, getConverter(), getTreeItemGraphic());
        this.updateDisplay(this.getItem(), this.isEmpty());
    }

    /** {@inheritDoc} */
    @Override public void updateItem(T item, boolean empty) {
        this.textProperty().unbind();
        updateItem(this, getConverter(), hbox, getTreeItemGraphic(), textField);
        updateDisplay(item, empty);
        super.updateItem(item, empty);
    }



    /***************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private Node getTreeItemGraphic() {
        TreeItem<T> treeItem = getTreeItem();
        return treeItem == null ? null : treeItem.getGraphic();
    }


    // Pulled from Cell Utils
    /***************************************************************************
     *                                                                         *
     * TextField convenience                                                   *
     *                                                                         *
     **************************************************************************/


    static <T> void updateItem(final Cell<T> cell,
                               final StringConverter<T> converter,
                               final HBox hbox,
                               final Node graphic,
                               final TextField textField) {
        if (cell.isEmpty()) {
            cell.setText(null);
            cell.setGraphic(null);
        } else {
            if (cell.isEditing()) {
                if (textField != null) {
                    textField.setText(getItemText(cell, converter));
                }
                cell.setText(null);

                if (graphic != null) {
                    hbox.getChildren().setAll(graphic, textField);
                    cell.setGraphic(hbox);
                } else {
                    cell.setGraphic(textField);
                }
            } else {
                cell.setText(getItemText(cell, converter));
                cell.setGraphic(graphic);
            }
        }
    }

    static <T> void startEdit(final Cell<T> cell,
                              final StringConverter<T> converter,
                              final HBox hbox,
                              final Node graphic,
                              final TextField textField) {
        if (textField != null) {
            textField.setText(getItemText(cell, converter));
        }
        cell.textProperty().unbind();
        cell.setText(null);

        if (graphic != null) {
            hbox.getChildren().setAll(graphic, textField);
            cell.setGraphic(hbox);
        } else {
            cell.setGraphic(textField);
        }

        textField.selectAll();

        // requesting focus so that key input can immediately go into the
        // TextField (see RT-28132)
        textField.requestFocus();
    }

    static <T> void cancelEdit(Cell<T> cell, final StringConverter<T> converter, Node graphic) {
        cell.setText(getItemText(cell, converter));
        cell.setGraphic(graphic);
    }

    static <T> TextField createTextField(final Cell<T> cell, final StringConverter<T> converter) {
        final TextField textField = new TextField(getItemText(cell, converter));

        // Use onAction here rather than onKeyReleased (with check for Enter),
        // as otherwise we encounter RT-34685
        textField.setOnAction(event -> {
            if (converter == null) {
                throw new IllegalStateException(
                        "Attempting to convert text input into Object, but provided "
                                + "StringConverter is null. Be sure to set a StringConverter "
                                + "in your cell factory.");
            }
            cell.commitEdit(converter.fromString(textField.getText()));
            event.consume();
        });
        textField.setOnKeyReleased(t -> {
            if (t.getCode() == KeyCode.ESCAPE) {
                cell.cancelEdit();
                t.consume();
            }
        });
        return textField;
    }

    private static <T> String getItemText(Cell<T> cell, StringConverter<T> converter) {
        return converter == null ?
                cell.getItem() == null ? "" : cell.getItem().toString() :
                converter.toString(cell.getItem());
    }
}
