package ui.fingerprint.payload;


import core.fingerprint3.AndThen;
import core.fingerprint3.ByteTestFunction;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import ui.fingerprint.tree.PayloadItem;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class ByteTestRow extends OpRow {


    private Test op;
    private BigInteger compareValue;
    private int postOffset;
    private boolean relative;
    private Endian endian;
    private int offset;
    private int bytes;

    public ByteTestRow() {
        this(null);
    }

    public ByteTestRow(ByteTestFunction bt) {
        super(PayloadItem.OpType.BYTE_TEST);

        if (bt != null) {
            Optional<BigInteger> value = new ArrayList<>(Arrays.asList(bt.getGT(), bt.getLT(), bt.getGTE(), bt.getLTE(), bt.getEQ(), bt.getAND(), bt.getOR())).stream()
                    .filter(bi -> bi != null)
                    .findFirst();
            this.compareValue = value.orElse(new BigInteger("0"));
            this.postOffset = bt.getPostOffset();
            this.relative = bt.isRelative();
            this.endian = bt.getEndian() != null ? Endian.valueOf(bt.getEndian()) : Endian.getDefault();
            this.offset = bt.getOffset();
            this.bytes = bt.getBytes();

            if (bt.getAndThen() != null && bt.getAndThen().getMatchOrByteTestOrIsDataAt().size() > 0) {
                bt.getAndThen().getMatchOrByteTestOrIsDataAt().forEach(op -> {
                    OpRow childRow = OpRowFactory.get(op);
                    if (childRow != null) {
                        this.addChild(childRow);
                    }
                });
            }
        } else {
            this.compareValue = new BigInteger("0");
            this.postOffset = DEFAULT_OFFSET;
            this.relative = DEFAULT_RELATIVE;
            this.endian = Endian.getDefault();
            this.offset = DEFAULT_OFFSET;
            this.bytes = DEFAULT_BYTES;
        }

        this.getChildren().add(new EmptyRow());
    }

    @Override
    public HBox getInput() {
        HBox inputBox = new HBox(4);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        HBox testBox = new HBox(2);
        testBox.setAlignment(Pos.CENTER_LEFT);
        Label testLabel = new Label("Test:");
        ChoiceBox<Test> operatorChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(Test.values()));
        //The AND and OR Tests have no current meaning
        operatorChoiceBox.getItems().removeAll(Test.AND, Test.OR);
        operatorChoiceBox.valueProperty().addListener(observable -> {
            this.op = operatorChoiceBox.getValue();
            update();
        });
        TextField valueField = new TextField(this.compareValue.toString());
        //don't need full standard width
        valueField.setPrefColumnCount(5);
        valueField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (newValue.isEmpty()) {
                    valueField.setText("0");
                    Platform.runLater(valueField::selectAll);
                } else {
                    this.compareValue = new BigInteger(newValue);
                }
                update();
            } catch (NumberFormatException e) {
                valueField.setText(oldValue);
            }
        });
        testBox.getChildren().addAll(testLabel, operatorChoiceBox, valueField);

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
            this.relative =relativeChoice.getValue();
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


        inputBox.getChildren().addAll(testBox, offsetBox, postOffsetBox, relativeBox, endianBox, bytesBox);

        return inputBox;
    }

    @Override
    public Object getOperation() {
        ByteTestFunction test = factory.createByteTestFunction();
        if (this.op != null) {
            switch (this.op) {
                case GT:
                    test.setGT(this.compareValue);
                    break;
                case LT:
                    test.setLT(this.compareValue);
                    break;
                case GTE:
                    test.setGTE(this.compareValue);
                    break;
                case LTE:
                    test.setLTE(this.compareValue);
                    break;
                case EQ:
                    test.setEQ(this.compareValue);
                    break;
                case AND:
                    test.setAND(this.compareValue);
                    break;
                case OR:
                    test.setOR(this.compareValue);
                    break;
            }
        }

        test.setOffset(this.offset);
        test.setPostOffset(this.postOffset);
        test.setRelative(this.relative);
        test.setEndian(this.endian.name());
        test.setBytes(this.bytes);

        if (this.getChildren().size() > 0) {
            if (!(this.getChildren().get(0) instanceof EmptyRow)) {
                AndThen then = factory.createAndThen();
                this.getChildren().forEach(child -> {
                    if (!(child instanceof EmptyRow)) {
                        then.getMatchOrByteTestOrIsDataAt().add(child.getOperation());
                    }
                });
                test.setAndThen(then);
            }
        }

        return test;
    }

    @Override
    public ObservableList<PayloadItem.OpType> getAvailableOps() {
        ObservableList<PayloadItem.OpType> opList = FXCollections.observableArrayList(PayloadItem.OpType.values());
        opList.removeAll(PayloadItem.OpType.ALWAYS);

        return opList;
    }
}
