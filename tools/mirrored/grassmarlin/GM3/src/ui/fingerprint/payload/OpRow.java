package ui.fingerprint.payload;


import core.fingerprint3.ObjectFactory;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import ui.fingerprint.editorPanes.PayloadEditorPane;
import ui.fingerprint.tree.PayloadItem;

import java.util.ArrayList;
import java.util.List;


public abstract class OpRow implements ParentBox {
    public static final int NEW_ROW_INDEX = -1;

    protected static final int MIN_OFFSET = -65535;
    protected static final int MAX_OFFSET = 65535;
    protected static final int DEFAULT_OFFSET = 0;
    protected static final boolean DEFAULT_RELATIVE = false;
    protected static final int MIN_BYTES = 1;
    protected static final int MAX_BYTES = 10;
    protected static final int DEFAULT_BYTES = 1;

    private PayloadItem.OpType type;
    private List<OpRow> children;
    private VBox childrenBox;
    protected ParentBox parent;
    protected ObjectFactory factory;

    public OpRow(PayloadItem.OpType type) {
        this.type = type;
        this.children = new ArrayList<>();
        this.childrenBox = new VBox();
        this.factory = new ObjectFactory();
    }

    public PayloadItem.OpType getType() {
        return this.type;
    }

    /**
     *
     * @param parent the parentBox to insert this row into
     * @param index the index of the row this row is replacing, -1 if adding new row
     */
    public void insert(ParentBox parent, int index) {
        this.parent = parent;

        VBox row = new VBox();
        row.setSpacing(4);

        HBox selectionBox = new HBox();
        selectionBox.setAlignment(Pos.CENTER_LEFT);
        selectionBox.setSpacing(2);

        ComboBox<PayloadItem.OpType> opBox = new ComboBox<>(parent.getAvailableOps());


        opBox.setValue(this.getType());
        opBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            int thisIndex = parent.getChildrenBox().getChildren().indexOf(row);

            OpRow newRow = OpRowFactory.get(newValue);

            parent.addChild(newRow);
            parent.removeChild(this);

            newRow.insert(parent, thisIndex);

            if (this instanceof EmptyRow) {
                this.insert(parent, NEW_ROW_INDEX);
            }

            if (newValue == PayloadItem.OpType.ALWAYS) {
                if (parent instanceof PayloadEditorPane) {
                    ((PayloadEditorPane) parent).HasAlwaysProperty().setValue(true);
                }
            } else if (oldValue == PayloadItem.OpType.ALWAYS) {
                if (parent instanceof PayloadEditorPane) {
                    ((PayloadEditorPane) parent).HasAlwaysProperty().setValue(false);
                }
            }
        });
        opBox.setCellFactory(lv -> {
            ListCell<PayloadItem.OpType> cell = new ListCell<PayloadItem.OpType>(){
                @Override
                public void updateItem(PayloadItem.OpType type, boolean empty) {
                    super.updateItem(type, empty);
                    if (empty) {
                        setText(null);
                    } else {
                        setText(type.name());
                    }
                }
            };
            if (parent instanceof PayloadEditorPane) {
                cell.disableProperty().bind(((PayloadEditorPane) parent).HasAlwaysProperty().and(cell.itemProperty().isEqualTo(PayloadItem.OpType.ALWAYS)));
                cell.opacityProperty().bind(Bindings.when(cell.disabledProperty()).then(0.4).otherwise(1.0));
            }

            return cell;
        });

        HBox input = getInput();

        Button removeButton = new Button("-");
        removeButton.setOnAction(event -> {
            parent.getChildrenBox().getChildren().removeAll(row);
            parent.removeChild(this);
            update();
        });


        if (this instanceof EmptyRow) {
            removeButton.setDisable(true);
        }

        selectionBox.getChildren().addAll(removeButton, opBox, input);



        // 20 is the indent applied to children
        HBox indentBox = new HBox(20);
        Pane indentPane = new Pane();
        VBox childrenBox = getChildrenBox();

        indentBox.setAlignment(Pos.CENTER_LEFT);
        // indent children
        indentBox.getChildren().addAll(indentPane, childrenBox);

        children.forEach(child -> child.insert(this, NEW_ROW_INDEX));

        row.getChildren().addAll(selectionBox, indentBox);

        if (index < 0) {
            parent.getChildrenBox().getChildren().addAll(row);
        } else {
            parent.getChildrenBox().getChildren().set(index, row);
        }

        if (!isLoading()) {
            update();
        }
    }

    @Override
    public VBox getChildrenBox() {
        return this.childrenBox;
    }

    @Override
    public void addChild(OpRow child) {
        this.children.add(child);
    }

    @Override
    public void removeChild(OpRow child) {
        this.children.remove(child);
    }

    @Override
    public void update() {
        this.parent.update();
    }

    @Override
    public boolean isLoading() {
        // if we don't have a parent then we are definitely loading
        return this.parent == null || parent.isLoading();
    }

    protected List<OpRow> getChildren(){
        return this.children;
    }

    public abstract HBox getInput();

    public abstract Object getOperation();

}
