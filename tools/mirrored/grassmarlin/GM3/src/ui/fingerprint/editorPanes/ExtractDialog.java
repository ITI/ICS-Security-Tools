package ui.fingerprint.editorPanes;

import core.fingerprint3.ContentType;
import core.fingerprint3.Extract;
import core.fingerprint3.Position;
import core.fingerprint3.Post;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ui.fingerprint.FingerPrintGui;
import ui.fingerprint.payload.Endian;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExtractDialog extends Dialog<ExtractDialog> {

    private static final int DEFAULT_MAX_LENGTH = 1024;
    private static final int MAX_MAX_LENGTH = 65535;
    private static final int MIN_MAX_LENGTH = 0;

    private enum Lookup {
        BACNET,
        ENIPVENDOR,
        ENIPDEVICE
    }

    private enum EditablePosition {
        START_OF_PAYLOAD,
        END_OF_PAYLOAD,
        CURSOR_START,
        CURSOR_MAIN,
        CURSOR_END,
        CUSTOM;

        public Position toPosition() {
            Position result;
            try {
                result = Position.fromValue(this.name());
            } catch (IllegalArgumentException e) {
                result = null;
            }
            return result;
        }
    }

    private List<Extract> extractList;

    public ExtractDialog() {
        this(null);
    }

    public ExtractDialog(List<Extract> extracts) {
        super();

        extractList = new ArrayList<>();

        VBox content = new VBox(5);

        if (extracts != null) {
            extracts.forEach(extract -> content.getChildren().add(new ExtractRow(extract, content)));
        }

        HBox addBox = new HBox(2);
        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();
        Button addButton = new Button("+");
        addButton.setDisable(true);
        addButton.setOnAction(event -> {
            if (nameField.getText() != null && !nameField.getText().isEmpty()) {
                Extract extract = new Extract();
                extract.setName(nameField.getText());
                ExtractRow newRow = new ExtractRow(extract, content);
                content.getChildren().add(content.getChildren().indexOf(addBox), newRow);
                nameField.setText("");
                addButton.setDisable(true);

                Platform.runLater(newRow::requestFocus);
            }
        });
        addButton.setOnKeyPressed(key -> {
            if (key.getCode() == KeyCode.ENTER) {
                addButton.fire();
                key.consume();
            }
        });

        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.trim().isEmpty()) {
                addButton.setDisable(false);
            } else {
                addButton.setDisable(true);
            }
        });
        nameField.setOnKeyPressed(key -> {
            if (key.getCode() == KeyCode.ENTER) {
                addButton.fire();
                key.consume();
            }
        });

        addBox.setAlignment(Pos.CENTER_LEFT);
        addBox.getChildren().addAll(nameLabel, nameField, addButton);

        content.getChildren().addAll(addBox);

        this.getDialogPane().setContent(content);

        this.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        this.getDialogPane().setPrefWidth(1150);
        this.getDialogPane().setPrefHeight(500);
        this.setResizable(true);

        this.setResultConverter(button -> {
            if (button == ButtonType.CLOSE) {
                List<Extract> extractList = content.getChildren().stream()
                        .filter(child -> child instanceof ExtractRow)
                        .map(row -> ((ExtractRow) row).getExtract())
                        .filter(extract -> extract != null)
                        .collect(Collectors.toList());
                this.extractList = extractList;
            }
            return  this;
        });
    }

    public List<Extract> getExtractList() {
        return this.extractList;
    }

    private class ExtractRow extends HBox{
        private String name;
        private String from;
        private String to;
        private int maxLength;
        private Endian endian;
        private Post post;

        public ExtractRow(Extract extract, VBox parent) {
            super();

            if (extract != null) {
                this.name = extract.getName();
                this.from = extract.getFrom();
                this.to = extract.getTo();
                this.maxLength = extract.getMaxLength();
                this.endian = extract.getEndian() != null ? Endian.valueOf(extract.getEndian()) : Endian.getDefault();
                this.post = extract.getPost();
            }

            if (this.post == null) {
                this.post = new Post();
            }

            this.setAlignment(Pos.CENTER_LEFT);
            this.setSpacing(10);

            HBox nameBox = new HBox(2);
            Label nameLabel = new Label("Name:");
            TextField nameField = new TextField(name);
            nameField.backgroundProperty().bind(parent.backgroundProperty());
            nameField.setEditable(false);
            nameField.setBorder(null);
            nameBox.setAlignment(Pos.CENTER_LEFT);
            nameBox.getChildren().addAll(nameLabel, nameField);

            VBox postBox = new VBox(2);
            HBox convertBox = new HBox(2);
            HBox lookupBox = new HBox(2);
            Label postLabel = new Label("Post:");
            Label convertLabel = new Label("Convert:");
            ChoiceBox<ContentType> convertChoice = new ChoiceBox<>(FXCollections.observableArrayList(ContentType.values()));
            convertChoice.setValue(this.post != null ? this.post.getConvert() : null);
            convertChoice.valueProperty().addListener(observable1 -> this.post.setConvert(convertChoice.getValue()));
            Label lookupLabel = new Label("Lookup:");
            ChoiceBox<Lookup> lookupChoice = new ChoiceBox<>(FXCollections.observableArrayList(Lookup.values()));
            lookupChoice.setValue(this.post != null && this.post.getLookup() != null ? Lookup.valueOf(this.post.getLookup()) : null);
            lookupChoice.valueProperty().addListener(observable1 -> this.post.setLookup(lookupChoice.getValue().name()));

            convertBox.getChildren().addAll(convertLabel, convertChoice);
            lookupBox.getChildren().addAll(lookupLabel, lookupChoice);

            postBox.getChildren().addAll(convertBox, lookupBox);

            VBox positionBox = new VBox(2);
            HBox fromBox = new HBox(2);
            Label fromLabel = new Label("From:");
            ChoiceBox<EditablePosition> fromPositionBox = new ChoiceBox<>(FXCollections.observableArrayList(EditablePosition.values()));
            fromPositionBox.setValue(EditablePosition.START_OF_PAYLOAD);
            TextField fromPositionField = new TextField();

            //set the from options appropriately from the passed in extract object
            try {
                Long.parseLong(this.from);
                fromPositionBox.setValue(EditablePosition.CUSTOM);
                fromPositionField.setText(this.from);
                fromPositionField.setDisable(false);
            } catch (NumberFormatException e) {
                fromPositionBox.setValue(this.from != null ? EditablePosition.valueOf(this.from) : null);
                fromPositionField.setDisable(true);
            }

            fromPositionBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == EditablePosition.CUSTOM) {
                    fromPositionField.setDisable(false);
                } else {
                    this.from = newValue.name();
                    fromPositionField.setDisable(true);
                }
            });

            fromPositionField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null || newValue.isEmpty()) {
                    fromPositionField.setText("0");
                    FingerPrintGui.selectAll(fromPositionField);
                } else {
                    try {
                        long index = Long.parseLong(newValue);
                        if (index < 0) {
                            fromPositionField.setText(oldValue);
                        } else {
                            this.from = Long.toString(index);
                        }
                    } catch (NumberFormatException e) {
                        fromPositionField.setText(oldValue);
                    }
                }
            });

            fromBox.setAlignment(Pos.CENTER_LEFT);
            fromBox.getChildren().addAll(fromLabel, fromPositionBox, fromPositionField);

            HBox toBox = new HBox(2);
            Label toLabel = new Label("To:");
            ChoiceBox<EditablePosition> toPositionBox = new ChoiceBox<>(FXCollections.observableArrayList(EditablePosition.values()));
            toPositionBox.setValue(EditablePosition.END_OF_PAYLOAD);
            TextField toPositionField = new TextField();

            //set the to options appropriately from the passed in extract object
            try {
                Long.parseLong(this.to);
                toPositionBox.setValue(EditablePosition.CUSTOM);
                toPositionField.setText(this.to);
                toPositionField.setDisable(false);
            } catch (NumberFormatException e) {
                toPositionBox.setValue(this.to != null ? EditablePosition.valueOf(this.to) : null);
                toPositionField.setDisable(true);
            }

            toPositionBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == EditablePosition.CUSTOM) {
                    toPositionField.setDisable(false);
                } else {
                    toPositionField.setDisable(true);
                    this.to = newValue.name();
                }
            });

            toPositionField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null || newValue.isEmpty()) {
                    toPositionField.setText("0");
                    FingerPrintGui.selectAll(toPositionField);
                } else {
                    try {
                        long index = Long.parseLong(newValue);
                        if (index < 0) {
                            toPositionField.setText(oldValue);
                        } else {
                            this.to = Long.toString(index);
                        }
                    } catch (NumberFormatException e) {
                        toPositionField.setText(oldValue);
                    }
                }
            });

            toLabel.prefWidthProperty().bind(fromLabel.widthProperty());
            toBox.setAlignment(Pos.CENTER_LEFT);
            toBox.getChildren().addAll(toLabel, toPositionBox, toPositionField);

            positionBox.getChildren().addAll(fromBox, toBox);

            HBox maxLengthBox = new HBox(2);
            Label maxLengthLabel = new Label("Max Length:");
            TextField maxLengthField = new TextField(Integer.toString(this.maxLength));
            maxLengthField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null || newValue.isEmpty()) {
                    maxLengthField.setText("0");
                    maxLengthField.selectAll();
                } else {
                    try {
                        int length = Integer.parseInt(newValue);
                        if (length < MIN_MAX_LENGTH || length > MAX_MAX_LENGTH) {
                            maxLengthField.setText(oldValue);
                        } else {
                            this.maxLength = length;
                        }
                    } catch (NumberFormatException e) {
                        maxLengthField.setText(oldValue);
                    }
                }
            });

            maxLengthBox.setAlignment(Pos.CENTER_LEFT);
            maxLengthBox.getChildren().addAll(maxLengthLabel, maxLengthField);

            HBox endianBox = new HBox(2);
            Label endianLabel = new Label("Endian:");
            ChoiceBox<Endian> endianChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(Endian.values()));
            endianChoiceBox.setValue(this.endian);
            endianChoiceBox.valueProperty().addListener(observable -> this.endian = endianChoiceBox.getValue());
            endianBox.setAlignment(Pos.CENTER_LEFT);
            endianBox.getChildren().addAll(endianLabel, endianChoiceBox);

            Pane spacer = new Pane();

            Button removeButton = new Button("-");
            removeButton.setOnAction(event -> parent.getChildren().remove(this));

            Pane separator = new Pane();
            separator.setPrefWidth(10);
            this.getChildren().addAll(nameBox, postLabel, postBox, separator, positionBox, maxLengthBox, endianBox, spacer, removeButton);
            HBox.setHgrow(spacer, Priority.ALWAYS);
        }

        public Extract getExtract() {
            Extract ret = new Extract();

            ret.setName(this.name);
            ret.setFrom(this.from);
            ret.setTo(this.to);
            ret.setPost(this.post);
            ret.setMaxLength(this.maxLength);
            ret.setEndian(this.endian.name());

            return ret;
        }
    }
}
