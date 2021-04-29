package ui.dialog;

import core.Preferences;
import core.Version;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import ui.EmbeddedIcons;

import java.util.List;

public class ReleaseNotesDialogFx extends Dialog {
    private final static double PADDING_NOTES = 24.0;
    private final SimpleStringProperty displayedVersion;
    private final ListView<String> display;

    public ReleaseNotesDialogFx() {
        displayedVersion = new SimpleStringProperty(Version.APPLICATION_VERSION);
        display = new ListView<>();


        initComponents();
    }

    private void initComponents() {
        setTitle("Release Notes");
        Window stage = super.getDialogPane().getScene().getWindow();
        if(stage instanceof Stage) {
            ((Stage)stage).getIcons().add(EmbeddedIcons.Logo.getRawImage());
            ((Stage)stage).getIcons().add(EmbeddedIcons.Logo_Small.getRawImage());
        }

        display.setCellFactory(param ->
                        new ListCell<String>() {
                            @Override
                            public void updateItem(String content, boolean isEmpty) {
                                super.updateItem(content, isEmpty);
                                if (!isEmpty) {
                                    Text text = new Text(content);
                                    text.wrappingWidthProperty().bind(display.widthProperty().subtract(PADDING_NOTES));
                                    setGraphic(text);
                                } else {
                                    setGraphic(null);
                                }
                            }
                        }
        );
        display.getItems().addAll(Version.PATCH_NOTES.get(displayedVersion.get()));

        final Label lblHeader = new Label("Release Notes may be viewed at any time with the Help -> Release Notes menu item.");
        final Label lblVersion = new Label();
        lblVersion.textProperty().bind(displayedVersion.concat(":"));
        final Button btnPrevVersion = new Button("Newer");
        btnPrevVersion.setOnAction(this::Handle_PrevVersion);
        final Button btnNextVersion = new Button("Older");
        btnNextVersion.setOnAction(this::Handle_NextVersion);
        final CheckBox ckShowNotesOnStartup = new CheckBox("Only show version notes for a new version.");
        ckShowNotesOnStartup.selectedProperty().bindBidirectional(Preferences.SuppressUnchangedVersionNotes);

        displayedVersion.addListener((observable, oldValue, newValue) -> {
            List<String> notes = Version.PATCH_NOTES.get(newValue);
            display.getItems().clear();
            if (notes != null) {
                display.getItems().addAll(notes);
            }
        });

        GridPane layout = new GridPane();
        layout.add(lblHeader, 0, 0, 3, 1);
        layout.add(lblVersion, 0, 1);
        layout.add(display, 0, 2, 3, 1);
        layout.add(btnNextVersion, 1, 3);
        layout.add(btnPrevVersion, 2, 3);
        layout.add(ckShowNotesOnStartup, 0, 4);

        getDialogPane().setContent(layout);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }

    private void Handle_PrevVersion(ActionEvent event) {
        String verCurrent = displayedVersion.get();
        String verPrevious = null;

        for(String ver : Version.PATCH_NOTES.keySet()) {
            if(ver.equals(verCurrent)) {
                if(verPrevious == null) {
                    //There is no previous entry.
                    return;
                }
                displayedVersion.set(verPrevious);
            }
            verPrevious = ver;
        }
    }
    private void Handle_NextVersion(ActionEvent event) {
        String verCurrent = displayedVersion.get();
        String verPrevious = null;

        for(String ver : Version.PATCH_NOTES.keySet()) {
            if(verPrevious != null && verPrevious.equals(verCurrent)) {
                displayedVersion.set(ver);
            }
            verPrevious = ver;
        }
    }

    public StringProperty displayedVersionProperty() {
        return displayedVersion;
    }
}
