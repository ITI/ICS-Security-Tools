package ui.fingerprint.payload;


import core.fingerprint3.AndThen;
import core.fingerprint3.ContentType;
import core.fingerprint3.MatchFunction;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import ui.fingerprint.tree.PayloadItem;

import java.util.ArrayList;
import java.util.List;

public class MatchRow extends OpRow {

    private static final String PATTERN = "PATTERN";
    private static final boolean DEFAULT_REVERSE = true;
    private static final boolean DEFAULT_NO_CASE = false;
    private static final int MAX_DEPTH = 65535;
    private static final int MIN_DEPTH = 0;
    private static final int DEFAULT_DEPTH = 0;
    private static final int MAX_WITHIN = 65535;
    private static final int MIN_WITHIN = 0;
    private static final int DEFAULT_WITHIN = 0;
    private static final boolean DEFAULT_MOVE_CURSORS = true;

    private ContentType contentType;
    private String content;
    private int offset;
    private boolean reverse;
    private boolean noCase;
    private int depth;
    private boolean relative;
    private int within;
    private boolean moveCursors;

    private List<String> types;

    public MatchRow() {
        this(null);
    }

    public MatchRow(MatchFunction match) {
        super(PayloadItem.OpType.MATCH);

        if (match != null) {
            this.contentType = match.getContent() != null ? match.getContent().getType() : null;
            this.content = this.contentType != null ? match.getContent().getValue() : match.getPattern();
            this.offset = match.getOffset();
            this.reverse = match.isReverse();
            this.noCase = match.isNoCase();
            this.depth = match.getDepth();
            this.relative = match.isRelative();
            this.within = match.getWithin();
            this.moveCursors = match.isMoveCursors();

            if (match.getAndThen() != null && match.getAndThen().getMatchOrByteTestOrIsDataAt().size() > 0) {
                match.getAndThen().getMatchOrByteTestOrIsDataAt().forEach(op -> {
                    OpRow childRow = OpRowFactory.get(op);
                    if (childRow != null) {
                        this.addChild(childRow);
                    }
                });
            }
        } else {
            this.offset = DEFAULT_OFFSET;
            this.reverse = DEFAULT_REVERSE;
            this.noCase = DEFAULT_NO_CASE;
            this.depth = DEFAULT_DEPTH;
            this.relative = DEFAULT_RELATIVE;
            this.within = DEFAULT_WITHIN;
            this.moveCursors = DEFAULT_MOVE_CURSORS;
        }

        types = new ArrayList<>();
        for (ContentType type : ContentType.values()) {
            types.add(type.name());
        }
        //PATTERN type matches pattern element in xsd
        //All others match the content element
        types.add(PATTERN);

        this.getChildren().add(new EmptyRow());
    }

    @Override
    public ObservableList<PayloadItem.OpType> getAvailableOps() {
        ObservableList<PayloadItem.OpType> ops = FXCollections.observableArrayList(PayloadItem.OpType.values());
        ops.removeAll(PayloadItem.OpType.ALWAYS);

        return ops;
    }

    @Override
    public HBox getInput() {
        HBox inputBox = new HBox(4);

        HBox typeBox = new HBox(2);
        typeBox.setAlignment(Pos.CENTER_LEFT);
        Label typeLabel = new Label("Type:");
        HBox typeInputBox = new HBox();
        ChoiceBox<String> typeChoice = new ChoiceBox<>(FXCollections.observableArrayList(types));
        typeChoice.setValue(this.contentType != null ? this.contentType.name() : PATTERN);
        TextField contentField = new TextField(this.content);
        contentField.textProperty().addListener(observable -> {
            this.content = contentField.getText();
            update();
        });
        typeChoice.valueProperty().addListener(change -> {
            if (typeChoice.getValue().equals(PATTERN)) {
                this.contentType = null;
            } else {
                this.contentType = ContentType.valueOf(typeChoice.getValue());
            }

            Platform.runLater(() -> {
                contentField.requestFocus();
                contentField.selectAll();
            });
            update();
        });
        typeInputBox.getChildren().addAll(typeChoice, contentField);
        typeBox.getChildren().addAll(typeLabel, typeInputBox);

//        private boolean moveCursors;

        HBox offsetBox = new HBox(2);
        offsetBox.setAlignment(Pos.CENTER_LEFT);
        Label offsetLabel = new Label("Offset:");
        TextField offsetField = new TextField(Integer.toString(this.offset));
        //sizing to fit the max number for offset
        offsetField.setPrefColumnCount(3);
        offsetField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (offsetField.getText().isEmpty()) {
                    offsetField.setText(Integer.toString(DEFAULT_OFFSET));
                    Platform.runLater(offsetField::selectAll);
                } else {
                    int newOffset = Integer.parseInt(newValue);
                    if (newOffset < MIN_OFFSET || newOffset > MAX_OFFSET) {
                        offsetField.setText(oldValue);
                    } else {
                        this.offset = newOffset;
                        update();
                    }
                }
            } catch (NumberFormatException e) {
                offsetField.setText(oldValue);
            }
        });
        offsetBox.getChildren().addAll(offsetLabel, offsetField);

        HBox reverseBox = new HBox(2);
        reverseBox.setAlignment(Pos.CENTER_LEFT);
        Label reverseLabel = new Label("Reverse:");
        ChoiceBox<Boolean> reverseChoice = new ChoiceBox<>(FXCollections.observableArrayList(true, false));
        reverseChoice.setValue(this.reverse);
        reverseChoice.valueProperty().addListener(change -> {
            this.reverse = reverseChoice.getValue();
            update();
        });
        reverseBox.getChildren().addAll(reverseLabel, reverseChoice);

        HBox noCaseBox = new HBox(2);
        noCaseBox.setAlignment(Pos.CENTER_LEFT);
        Label noCaseLabel = new Label("No Case:");
        ChoiceBox<Boolean> noCaseChoice = new ChoiceBox<>(FXCollections.observableArrayList(true, false));
        noCaseChoice.setValue(this.noCase);
        noCaseChoice.valueProperty().addListener(change -> {
            this.noCase = reverseChoice.getValue();
            update();
        });
        noCaseBox.getChildren().addAll(noCaseLabel, noCaseChoice);

        HBox depthBox = new HBox(2);
        depthBox.setAlignment(Pos.CENTER_LEFT);
        Label depthLabel = new Label("Depth:");
        TextField depthField = new TextField(Integer.toString(this.depth));
        //sizing to fit the max number for depth
        depthField.setPrefColumnCount(3);
        depthField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (depthField.getText().isEmpty()) {
                    depthField.setText(Integer.toString(DEFAULT_DEPTH));
                    Platform.runLater(depthField::selectAll);
                } else {
                    int newDepth = Integer.parseInt(newValue);
                    if (newDepth < MIN_DEPTH || newDepth > MAX_DEPTH) {
                        depthField.setText(oldValue);
                    } else {
                        this.depth = newDepth;
                        update();
                    }
                }
            } catch (NumberFormatException e) {
                depthField.setText(oldValue);
            }
        });
        depthBox.getChildren().addAll(depthLabel, depthField);

        HBox relativeBox = new HBox(2);
        relativeBox.setAlignment(Pos.CENTER_LEFT);
        Label relativeLabel = new Label("Relative:");
        ChoiceBox<Boolean> relativeChoice = new ChoiceBox<>(FXCollections.observableArrayList(true, false));
        relativeChoice.setValue(this.relative);
        relativeChoice.valueProperty().addListener(change -> {
            this.relative = relativeChoice.getValue();
            update();
        });
        relativeBox.getChildren().addAll(relativeLabel, relativeChoice);

        HBox withinBox = new HBox(2);
        withinBox.setAlignment(Pos.CENTER_LEFT);
        Label withinLabel = new Label("Within:");
        TextField withinField = new TextField(Integer.toString(this.within));
        //sizing to fit the max number for within
        withinField.setPrefColumnCount(3);
        withinField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (withinField.getText().isEmpty()) {
                    withinField.setText(Integer.toString(DEFAULT_WITHIN));
                    Platform.runLater(withinField::selectAll);
                } else {
                    int newWithin = Integer.parseInt(newValue);
                    if (newWithin < MIN_WITHIN || newWithin > MAX_WITHIN) {
                        withinField.setText(oldValue);
                    } else {
                        this.within = newWithin;
                        update();
                    }
                }
            } catch (NumberFormatException e) {
                withinField.setText(oldValue);
            }
        });
        withinBox.getChildren().addAll(withinLabel, withinField);

        HBox moveCursorsBox = new HBox(2);
        moveCursorsBox.setAlignment(Pos.CENTER_LEFT);
        Label moveCursorsLabel = new Label("Move Cursors:");
        ChoiceBox<Boolean> moveCursorsChoice = new ChoiceBox<>(FXCollections.observableArrayList(true, false));
        moveCursorsChoice.setValue(this.moveCursors);
        moveCursorsChoice.valueProperty().addListener(change -> {
            this.moveCursors = moveCursorsChoice.getValue();
            update();
        });
        moveCursorsBox.getChildren().addAll(moveCursorsLabel, moveCursorsChoice);


        inputBox.setAlignment(Pos.CENTER_LEFT);
        inputBox.getChildren().addAll(typeBox, offsetBox, reverseBox, noCaseBox, depthBox, relativeBox, withinBox, moveCursorsBox);

        return inputBox;
    }

    @Override
    public Object getOperation() {
        MatchFunction match = factory.createMatchFunction();
        if (this.contentType == null) {
            match.setPattern(this.content);
        } else {
            MatchFunction.Content content = factory.createMatchFunctionContent();
            content.setType(this.contentType);
            if (this.contentType == ContentType.HEX) {
                if (this.content != null) {
                    content.setValue(this.content.toUpperCase());
                } else {
                    content.setValue(null);
                }
            } else {
                if (this.content != null) {
                    content.setValue(this.content);
                } else {
                    content.setValue(null);
                }
            }
            match.setContent(content);
        }

        match.setOffset(this.offset);
        match.setReverse(this.reverse);
        match.setNoCase(this.noCase);
        match.setDepth(this.depth);
        match.setRelative(this.relative);
        match.setWithin(this.within);
        match.setMoveCursors(this.moveCursors);

        if (this.getChildren().size() > 0) {
            if (!(this.getChildren().get(0) instanceof EmptyRow)) {
                AndThen then = factory.createAndThen();
                this.getChildren().forEach(child -> {
                    if (!(child instanceof EmptyRow)) {
                        then.getMatchOrByteTestOrIsDataAt().add(child.getOperation());
                    }
                });
                match.setAndThen(then);
            }
        }

        return match;
    }
}
