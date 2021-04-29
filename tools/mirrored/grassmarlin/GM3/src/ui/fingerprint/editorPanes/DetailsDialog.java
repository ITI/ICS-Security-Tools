package ui.fingerprint.editorPanes;


import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import ui.fingerprint.payload.Category;
import ui.fingerprint.payload.Role;

import java.util.HashMap;
import java.util.Map;

public class DetailsDialog extends Dialog<DetailsDialog> {

    private Category category;
    private Role role;
    private Map<String, String> details;

    public DetailsDialog() {
        this(null, null, null);
    }

    public DetailsDialog(Category category, Role role, Map<String, String> details) {
        super();

        this.category = category;
        this.role = role;
        this.details = details == null ? new HashMap<>() : new HashMap<>(details);
        this.setResizable(true);
        this.setTitle("Details");


        VBox content = new VBox();
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFocusTraversable(false);
        // if this event is not consumed here than clicking on
        // the background causes the text fields to lose theirs
        scroll.setOnMouseClicked(event -> event.consume());

        HBox entryBox = new HBox(2);
        Label catLabel = new Label("Category:");
        ComboBox<Category> catBox = new ComboBox<>(FXCollections.observableArrayList(Category.values()));
        if (this.category != null) {
            catBox.setValue(this.category);
        }
        Label roleLabel = new Label("Role:");
        ComboBox<Role> roleBox = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        if (this.role != null) {
            roleBox.setValue(this.role);
        }
        entryBox.getChildren().addAll(catLabel, catBox, roleLabel, roleBox);



        VBox detailsBox = new VBox(4);

        HBox addBox = new HBox();
        Label detailsLabel = new Label("Details:");
        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();
        Button addButton = new Button("+");
        addButton.setDisable(true);
        addButton.setOnAction(event -> {
            if (nameField.getText() != null && !nameField.getText().isEmpty()) {
                DetailRow newRow = new DetailRow(detailsBox, nameField.getText(), "");
                detailsBox.getChildren().add(detailsBox.getChildren().indexOf(addBox), newRow);
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

        detailsBox.getChildren().add(detailsLabel);

        this.details.forEach((name, value) -> detailsBox.getChildren().add(new DetailRow(detailsBox, name, value)));

        detailsBox.getChildren().add(addBox);


        content.getChildren().addAll(entryBox, detailsBox);

        this.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.CLOSE) {
                this.category = catBox.getValue();
                this.role = roleBox.getValue();
                this.details = new HashMap<>();
                detailsBox.getChildren().forEach(row -> {
                    if (row instanceof DetailRow) {
                        DetailRow detailRow = (DetailRow) row;
                        if (detailRow.getName() != null && detailRow.getValue().isNotEmpty().get()){
                            this.details.put(detailRow.getName(), detailRow.getValue().get());
                        }
                    }
                });
            }
            return this;
        });

        this.getDialogPane().setContent(scroll);
        this.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        this.getDialogPane().setPrefHeight(300);
        this.getDialogPane().setPrefWidth(450);
    }

    public String getCategory() {
        return this.category != null ? this.category.name() : null;
    }

    public String getRole() {
        return this.role != null ? this.role.name() : null;
    }

    public Map<String, String> getDetails() {
        return this.details;
    }


    private class DetailRow extends HBox {
        private String name;
        private SimpleStringProperty valueProperty;

        public DetailRow(VBox parent, String name, String value) {
            super();
            this.name = name;
            this.valueProperty = new SimpleStringProperty(value);

            this.setAlignment(Pos.CENTER_LEFT);
            this.setSpacing(10);

            HBox nameBox = new HBox(2);
            HBox valueBox = new HBox(2);

            Label nameLabel = new Label("Name:");
            TextField nameText = new TextField(name);
            nameText.setEditable(false);
            nameText.backgroundProperty().bind(parent.backgroundProperty());
            nameText.setBorder(new Border(new BorderStroke(null, null, null, null)));
            nameText.setFocusTraversable(false);

            nameBox.setAlignment(Pos.CENTER_LEFT);
            nameBox.getChildren().addAll(nameLabel, nameText);

            Label valueLabel = new Label("Value:");
            TextField valueField = new TextField(value);
            valueProperty.bind(valueField.textProperty());

            valueBox.setAlignment(Pos.CENTER_LEFT);
            valueBox.getChildren().addAll(valueLabel, valueField);

            Button removeButton = new Button ("-");

            removeButton.setOnAction(event -> parent.getChildren().remove(this));

            Pane spacer = new Pane();

            this.getChildren().addAll(nameBox, valueBox, spacer, removeButton);
            setHgrow(spacer, Priority.ALWAYS);
        }

        @Override
        public void requestFocus() {
            this.getChildren().forEach(child -> {
                if (child instanceof HBox) {
                    ((HBox)child).getChildren().forEach(child2 -> {
                        if (child2 instanceof TextField) {
                            child2.requestFocus();
                        }
                    });
                }
            });
        }

        public String getName() {
            return this.name;
        }

        public StringProperty getValue() {
            return this.valueProperty;
        }

    }
}
