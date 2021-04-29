package ui.fingerprint.payload;

import core.fingerprint3.Anchor;
import core.fingerprint3.Cursor;
import core.fingerprint3.Position;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import ui.fingerprint.tree.PayloadItem;

public class AnchorRow extends OpRow {

    private Cursor cursor;
    private Position position;
    private int offset;
    private boolean relative;

    public AnchorRow() {
        this(null);
    }

    public AnchorRow(Anchor anchor) {
        super(PayloadItem.OpType.ANCHOR);

        if (anchor != null) {
            this.cursor = anchor.getCursor() != null ? anchor.getCursor() : Cursor.MAIN;
            this.position = anchor.getPosition() != null ? anchor.getPosition() : Position.START_OF_PAYLOAD;
            this.offset = anchor.getOffset() != null ? anchor.getOffset() : DEFAULT_OFFSET;
            this.relative = anchor.isRelative();
        } else {
            this.cursor = Cursor.MAIN;
            this.position = Position.START_OF_PAYLOAD;
            this.offset = DEFAULT_OFFSET;
            this.relative = DEFAULT_RELATIVE;
        }
    }

    @Override
    public HBox getInput() {
        HBox inputBox = new HBox(4);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        HBox cursorBox = new HBox(2);
        cursorBox.setAlignment(Pos.CENTER_LEFT);
        Label cursorLabel = new Label("Cursor:");
        ChoiceBox<Cursor> cursorChoice = new ChoiceBox<>(FXCollections.observableArrayList(Cursor.values()));
        cursorChoice.setValue(this.cursor);
        cursorChoice.valueProperty().addListener(observable -> {
            this.cursor = cursorChoice.getValue();
            update();
        });
        cursorBox.getChildren().addAll(cursorLabel, cursorChoice);

        HBox positionBox = new HBox(2);
        positionBox.setAlignment(Pos.CENTER_LEFT);
        Label positionLabel = new Label("Position:");
        ChoiceBox<Position> positionChoice = new ChoiceBox<>(FXCollections.observableArrayList(Position.values()));
        positionChoice.setValue(this.position);
        positionChoice.setDisable(this.relative);
        positionChoice.valueProperty().addListener(observable -> {
            this.position = positionChoice.getValue();
            update();
        });
        positionBox.getChildren().addAll(positionLabel, positionChoice);

        HBox offsetBox = new HBox(2);
        offsetBox.setAlignment(Pos.CENTER_LEFT);
        Label offsetLabel = new Label("Offset:");
        TextField offsetField = new TextField(Integer.toString(this.offset));
        //size appropriately
        offsetField.setPrefColumnCount(3);
        offsetField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                offsetField.setText("0");
                Platform.runLater(offsetField::selectAll);
            } else {
                try {
                    int temp = Integer.parseInt(offsetField.getText());
                    if (temp < MIN_OFFSET || temp > MAX_OFFSET) {
                        offsetField.setText(oldValue);
                    } else {
                        this.offset = temp;
                        update();
                    }
                } catch (NumberFormatException e) {
                    offsetField.setText(oldValue);
                }
            }
        });
        offsetBox.getChildren().addAll(offsetLabel, offsetField);

        HBox relativeBox = new HBox(2);
        relativeBox.setAlignment(Pos.CENTER_LEFT);
        Label relativeLabel = new Label("Relative:");
        ChoiceBox<Boolean> relativeChoice = new ChoiceBox<>(FXCollections.observableArrayList(true, false));
        relativeChoice.setValue(this.relative);
        relativeChoice.valueProperty().addListener(observable -> {
            this.relative = relativeChoice.getValue();
            positionChoice.setDisable(this.relative);
            update();
        });
        relativeBox.getChildren().addAll(relativeLabel, relativeChoice);


        inputBox.getChildren().addAll(cursorBox, positionBox, offsetBox, relativeBox);

        return inputBox;
    }

    @Override
    public Object getOperation() {
        Anchor anchor = factory.createAnchor();

        anchor.setCursor(this.cursor);
        anchor.setPosition(this.position);
        anchor.setOffset(this.offset);
        anchor.setRelative(this.relative);

        return anchor;
    }

    @Override
    public ObservableList<PayloadItem.OpType> getAvailableOps() {
        ObservableList<PayloadItem.OpType> opList = FXCollections.observableArrayList(PayloadItem.OpType.values());
        opList.removeAll(PayloadItem.OpType.ALWAYS);

        return opList;
    }
}
