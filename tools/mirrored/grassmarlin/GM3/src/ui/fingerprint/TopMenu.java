package ui.fingerprint;


import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.stage.FileChooser;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.nio.file.Path;

public class TopMenu extends MenuBar{

    FingerPrintGui gui;

    public TopMenu(FingerPrintGui gui) {
        super();
        this.gui = gui;
        createMenu();
    }

    private void createMenu() {
        Menu fileMenu = new Menu("_File");

        MenuItem openItem = new MenuItem("_Open...");
        openItem.setOnAction(this::showOpenDialog);
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCodeCombination.SHORTCUT_DOWN));

        MenuItem saveItem = new MenuItem("_Save");
        saveItem.setOnAction(this.gui::saveFingerprintWODialog);
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCodeCombination.SHORTCUT_DOWN));
        saveItem.disableProperty().bind(this.gui.isSelectedDirtyProperty().not());

        MenuItem saveAsItem = new MenuItem("Save _As...");
        saveAsItem.setOnAction(this.gui::saveFingerprintWDialog);

        MenuItem newItem = new MenuItem("_New Fingerprint");
        newItem.setOnAction(this::createFingerprint);
        newItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCodeCombination.SHORTCUT_DOWN));

        MenuItem exitItem = new MenuItem("E_xit");
        exitItem.setOnAction(this::handleExit);
        exitItem.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCodeCombination.SHORTCUT_DOWN));

        fileMenu.getItems().addAll(newItem, new SeparatorMenuItem(), openItem, saveItem, saveAsItem, new SeparatorMenuItem(), exitItem);
        fileMenu.setOnShowing(event -> {
            boolean isSelected = this.gui.getSelectedFPItem() != null;
            saveAsItem.setDisable(!isSelected);
        });



        Menu fingerprintMenu = new Menu("F_ingerprints");

        MenuItem enableAllItem = new MenuItem("Enable _All");
        enableAllItem.setOnAction(this.gui::enableAll);
        enableAllItem.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCodeCombination.SHORTCUT_DOWN, KeyCodeCombination.SHIFT_DOWN));

        MenuItem enableItem = new MenuItem("_Enable");
        enableItem.setOnAction(this.gui::enableSelected);
        enableItem.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCodeCombination.SHORTCUT_DOWN));

        MenuItem disableAllItem = new MenuItem("_Disable All");
        disableAllItem.setOnAction(this.gui::disableAll);
        disableAllItem.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCodeCombination.SHORTCUT_DOWN, KeyCodeCombination.SHIFT_DOWN));

        MenuItem disableItem = new MenuItem("Di_sable");
        disableItem.setOnAction(this.gui::disableSelected);
        disableItem.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCodeCombination.SHORTCUT_DOWN));

        fingerprintMenu.getItems().addAll(enableAllItem, enableItem, disableAllItem, disableItem);

        this.getMenus().addAll(fileMenu, fingerprintMenu);
    }

    private void showOpenDialog(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        Path initialDir = FingerPrintGui.getDefaultFingerprintDir();
        if (null != initialDir) {
            chooser.setInitialDirectory(initialDir.toFile());
        }

        chooser.setTitle("Open...");
        FileChooser.ExtensionFilter xmlFilter = new FileChooser.ExtensionFilter("Fingerprint", "*.xml");
        FileChooser.ExtensionFilter everything = new FileChooser.ExtensionFilter("All", "*.*");
        chooser.getExtensionFilters().addAll(xmlFilter, everything);
        chooser.setSelectedExtensionFilter(xmlFilter);
        File toLoad = chooser.showOpenDialog(this.getScene().getWindow());

        if (null != toLoad) {
            try {
                if (this.gui.getDocument().alreadyLoaded(null, toLoad.toPath())) {
                    Alert loadedAlert = new Alert(Alert.AlertType.INFORMATION);
                    loadedAlert.setTitle("Already Open");
                    loadedAlert.setHeaderText("Fingerprint Already Open");
                    loadedAlert.showAndWait();
                } else {
                    this.gui.getDocument().load(toLoad.toPath());
                }
            } catch (JAXBException e) {
                Alert error = new Alert(Alert.AlertType.ERROR, "Unable to load file");
                error.showAndWait();
            }
        }
    }

    private void createFingerprint(ActionEvent event) {
        this.gui.newFingerprint();
    }

    private void handleExit(ActionEvent event) {
        this.gui.exit();
    }
}
