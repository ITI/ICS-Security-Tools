package ui.fingerprint.editorPanes;


import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import ui.fingerprint.FingerPrintGui;
import ui.fingerprint.tree.FPItem;

public class FingerprintInfoPane extends GridPane {

    private FPItem fp;
    private FingerPrintGui gui;

    public FingerprintInfoPane(FPItem fp, FingerPrintGui gui) {
        super();
        this.fp = fp;
        this.gui = gui;

        // Objects
        Label nameLabel = new Label("Name:");
        Label authorLabel = new Label("Author:");
        Label descLabel = new Label("Description:");
        //TODO: Implement tag control like internet tags with editable combo box entry and list of tags with superscript x for tag deletion
        Label tagsLabel = new Label("Tags:");

        TextField nameField = new TextField(fp.getName());
        TextField authorField = new TextField(fp.getAuthor());
        TextArea descArea = new TextArea(fp.getDescription());
        descArea.setWrapText(true);

        Button applyButton = new Button("Apply");
        applyButton.setDisable(true);
        Button resetButton = new Button("Reset");
        resetButton.setDisable(true);


        // Event Handling
        applyButton.setOnAction(event -> {
            if (!nameField.getText().equals(fp.getName())) {
                boolean nameChanged = this.gui.getDocument().updateFingerprintName(fp.getName(), nameField.getText(), fp.pathProperty().get());
                if (nameChanged) {
                    fp.setName(nameField.getText());
                } else {
                    nameField.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, null, null)));
                }
            }

            if (!authorField.getText().equals(fp.getAuthor())) {
                boolean authorChanged = this.gui.getDocument().updateFingerprintAuthor(fp.getName(), fp.pathProperty().get(), authorField.getText());
                if (authorChanged) {
                    fp.setAuthor(authorField.getText());
                }
            }

            if (!descArea.getText().equals(fp.getDescription())) {
                boolean descChanged = this.gui.getDocument().updateFingerprintDescription(fp.getName(), fp.pathProperty().get(), descArea.getText());
                if (descChanged) {
                    fp.setDescription(descArea.getText());
                }
            }

            if (!different(fp, nameField.getText(), authorField.getText(), descArea.getText())) {
                disableButtons(applyButton, resetButton);
            }
        });

        resetButton.setOnAction(event -> {
            nameField.setText(fp.getName());
            authorField.setText(fp.getAuthor());
            descArea.setText(fp.getDescription());
        });

        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            nameField.setBorder(null);
            if (different(fp, nameField.getText(), authorField.getText(), descArea.getText())) {
                enableButtons(applyButton, resetButton);
            } else {
                disableButtons(applyButton, resetButton);
            }
        });

        fp.valueProperty().addListener((observable1, oldValue1, newValue1) -> {
            boolean isDisabled = applyButton.isDisabled();
            nameField.setText(newValue1);
            if (isDisabled) {
                disableButtons(applyButton, resetButton);
            }
        });

        authorField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (different(fp, nameField.getText(), authorField.getText(), descArea.getText())) {
                enableButtons(applyButton, resetButton);
            } else {
                disableButtons(applyButton, resetButton);
            }
        });

        descArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (different(fp, nameField.getText(), authorField.getText(), descArea.getText())) {
                enableButtons(applyButton, resetButton);
            } else {
                disableButtons(applyButton, resetButton);
            }
        });

        EventHandler<KeyEvent> applyingFieldHandler = event -> {
            if (event.getCode() == KeyCode.ENTER) {
                applyButton.fire();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                resetButton.fire();
            }
        };

        nameField.setOnKeyPressed(applyingFieldHandler);
        authorField.setOnKeyPressed(applyingFieldHandler);



        //layout
        this.setHgap(5);
        this.setVgap(20);
        this.setPadding(new Insets(5, 10, 5, 10));

        this.add(nameLabel, 0, 0);
        this.add(nameField, 1, 0, 2, 1);
        this.add(authorLabel, 3, 0);
        this.add(authorField, 4, 0, 2, 1);
        this.add(descLabel, 0, 1);
        this.add(descArea, 1, 1, 5, 3);

        HBox buttonBox = new HBox(2, applyButton, resetButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        this.add(buttonBox, 5, 6);
    }

    private void enableButtons(Button... buttons) {
        for (Button button : buttons) {
            button.setDisable(false);
        }
    }

    private void disableButtons(Button... buttons) {
        for (Button button : buttons) {
            button.setDisable(true);
        }
    }

    private boolean different(FPItem fp, String name, String author, String desc) {
        return !(fp.getName().equals(name) && fp.getAuthor().equals(author) && fp.getDescription().equals(desc));
    }

}
