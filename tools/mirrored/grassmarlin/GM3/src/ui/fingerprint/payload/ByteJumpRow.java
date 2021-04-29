package ui.fingerprint.payload;

import core.fingerprint3.AndThen;
import core.fingerprint3.ByteJumpFunction;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import ui.fingerprint.tree.PayloadItem;

import java.util.regex.Pattern;

public class ByteJumpRow extends OpRow {

    private static final String calcRegex = "((\\d+)|x)([-+/*]((\\d+)|x))*";

    private String calc;
    private Pattern calcPattern;
    private int offset;
    private int postOffset;
    private boolean relative;
    private Endian endian;
    private int bytes;

    public ByteJumpRow() {
        this(null);
    }

    public ByteJumpRow(ByteJumpFunction jump) {
        super(PayloadItem.OpType.BYTE_JUMP);
        calcPattern = Pattern.compile(calcRegex);

        if (jump != null) {
            this.calc = jump.getCalc();
            this.offset = jump.getOffset() != null ? jump.getOffset() : DEFAULT_OFFSET;
            this.postOffset = jump.getPostOffset() != null ? jump.getPostOffset() : DEFAULT_OFFSET;
            this.relative = jump.isRelative();
            this.endian = jump.getEndian() != null ? Endian.valueOf(jump.getEndian()) : Endian.getDefault();
            this.bytes = jump.getBytes();

            if (jump.getAndThen() != null && jump.getAndThen().getMatchOrByteTestOrIsDataAt().size() > 0) {
                jump.getAndThen().getMatchOrByteTestOrIsDataAt().forEach(op -> {
                    OpRow childRow = OpRowFactory.get(op);
                    if (childRow != null) {
                        this.addChild(childRow);
                    }
                });
            }
        } else {
            this.calc = "";
            this.offset = DEFAULT_OFFSET;
            this.postOffset = DEFAULT_OFFSET;
            this.endian = Endian.getDefault();
            this.bytes = DEFAULT_BYTES;
            this.relative = DEFAULT_RELATIVE;
        }

        this.getChildren().add(new EmptyRow());
    }

    @Override
    public HBox getInput() {
        HBox inputBox = new HBox(4);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        HBox calcBox = new HBox(2);
        calcBox.setAlignment(Pos.CENTER_LEFT);
        Label calcLabel = new Label("Calc:");
        TextField calcField = new TextField(this.calc);
        calcField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == false) {
                String calcText = calcField.getText() != null ? calcField.getText().replaceAll("\\s", "") : null;
                // the != check is checking for if they are both null
                if (this.calc != calcText) {
                    if (calcText != null && !calcText.equals(this.calc)) {
                        if (calcPattern.matcher(calcText).matches() || calcText.isEmpty() || calcText == null) {
                            this.calc = calcText;
                            update();
                        } else {
                            Platform.runLater(calcField::requestFocus);
                        }
                    } else if (calcText == null) {
                        this.calc = null;
                        update();
                    }
                }
            }
        });
        calcBox.getChildren().addAll(calcLabel, calcField);

        HBox postOffsetBox = new HBox(2);
        postOffsetBox.setAlignment(Pos.CENTER_LEFT);
        Label postOffsetLabel = new Label("Post Offset:");
        TextField postOffsetField = new TextField(Integer.toString(this.postOffset));
        // size to fit max allowed value
        postOffsetField.setPrefColumnCount(3);
        postOffsetField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (newValue.isEmpty()) {
                    postOffsetField.setText("0");
                    Platform.runLater(postOffsetField::selectAll);
                } else {
                    int temp = Integer.parseInt(postOffsetField.getText());
                    if (temp < MIN_OFFSET || temp > MAX_OFFSET) {
                        postOffsetField.setText(oldValue);
                    } else {
                        this.postOffset = Integer.parseInt(postOffsetField.getText());
                        update();
                    }
                }
            } catch (NumberFormatException e) {
                postOffsetField.setText(oldValue);
            }
        });
        postOffsetBox.getChildren().addAll(postOffsetLabel, postOffsetField);

        HBox relativeBox = new HBox(2);
        relativeBox.setAlignment(Pos.CENTER_LEFT);
        Label relativeLabel = new Label("Relative:");
        ChoiceBox<Boolean> relativeChoice = new ChoiceBox<>(FXCollections.observableArrayList(true, false));
        relativeChoice.setValue(this.relative);
        relativeChoice.valueProperty().addListener(observable -> {
            this.relative = relativeChoice.getValue();
            update();
        });
        relativeBox.getChildren().addAll(relativeLabel, relativeChoice);

        HBox endianBox = new HBox(2);
        endianBox.setAlignment(Pos.CENTER_LEFT);
        Label endianLabel = new Label("Endian:");
        ChoiceBox<Endian> endianChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(Endian.values()));
        endianChoiceBox.setValue(this.endian);
        endianChoiceBox.valueProperty().addListener(observable -> {
            this.endian = endianChoiceBox.getValue();
            update();
        });
        endianBox.getChildren().addAll(endianLabel, endianChoiceBox);

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

        HBox bytesBox = new HBox(2);
        bytesBox.setAlignment(Pos.CENTER_LEFT);
        Label bytesLabel = new Label("Bytes:");
        TextField bytesField = new TextField(Integer.toString(this.bytes));
        //size for max value
        bytesField.setPrefColumnCount(2);
        bytesField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                bytesField.setText(Integer.toString(DEFAULT_BYTES));
                Platform.runLater(bytesField::selectAll);
            } else {
                try {
                    int temp = Integer.parseInt(bytesField.getText());
                    if (temp < MIN_BYTES || temp > MAX_BYTES) {
                        bytesField.setText(oldValue);
                    } else {
                        this.bytes = temp;
                        update();
                    }
                } catch (NumberFormatException e) {
                    bytesField.setText(oldValue);
                }
            }
        });
        bytesBox.getChildren().addAll(bytesLabel, bytesField);



        inputBox.getChildren().addAll(calcBox, offsetBox, postOffsetBox, relativeBox, endianBox, bytesBox);

        return inputBox;
    }

    @Override
    public Object getOperation() {
        ByteJumpFunction jump = factory.createByteJumpFunction();

        jump.setCalc(this.calc);
        jump.setOffset(this.offset);
        jump.setPostOffset(this.postOffset);
        jump.setRelative(this.relative);
        jump.setEndian(this.endian.name());
        jump.setBytes(this.bytes);

        if (this.getChildren().size() > 0) {
            if (!(this.getChildren().get(0) instanceof EmptyRow)) {
                AndThen then = factory.createAndThen();
                this.getChildren().forEach(child -> {
                    if (!(child instanceof EmptyRow)) {
                        then.getMatchOrByteTestOrIsDataAt().add(child.getOperation());
                    }
                });
                jump.setAndThen(then);
            }
        }

        return jump;
    }

    @Override
    public ObservableList<PayloadItem.OpType> getAvailableOps() {
        ObservableList<PayloadItem.OpType> opList = FXCollections.observableArrayList(PayloadItem.OpType.values());
        opList.removeAll(PayloadItem.OpType.ALWAYS);

        return opList;
    }
}
