package ui.dialog.importmanager;

import core.importmodule.ImportItem;
import core.importmodule.ImportProcessors;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import util.FileUnits;

import java.io.File;


public class SelectImportTypeDialog extends Dialog {
    private Label uiSelectedFile;
    private CheckBox uiAdditionalFiles;
    private ComboBox<ImportProcessors.ProcessorWrapper> uiImportType;

    public SelectImportTypeDialog() {

        initComponents();
    }

    private void initComponents() {
        this.setTitle("Select Import Type");
        this.initModality(Modality.APPLICATION_MODAL);
        this.initStyle(StageStyle.UTILITY);

        GridPane fields = new GridPane();
        fields.setVgap(10);
        fields.setHgap(10);
        fields.setPadding(new Insets(25, 25, 25, 25));

        fields.add(new Label("The following file does not match an Import Fingerprint.  Please indicate how it should be handled."), 0, 0);
        uiSelectedFile = new Label("\n");
        fields.add(uiSelectedFile, 0, 1);

        uiImportType = new ComboBox<>();
        uiImportType.getItems().addAll(ImportProcessors.getProcessors());
        uiImportType.setValue(uiImportType.getItems().get(0));
        fields.add(uiImportType, 0, 2);

        uiAdditionalFiles = new CheckBox("Apply to the next _ files");
        uiAdditionalFiles.setVisible(false);
        fields.add(uiAdditionalFiles, 0, 3);

        this.getDialogPane().setContent(fields);

        this.getDialogPane().getButtonTypes().addAll(new ButtonType("Close", ButtonBar.ButtonData.OK_DONE));
    }

    public void setCurrentFile(File fileSelectedFile) {
        uiSelectedFile.setText(fileSelectedFile.getAbsolutePath() + "\n" + FileUnits.formatSize(fileSelectedFile.length()));
    }
    public void setFilesRemaining(int count) {
        uiAdditionalFiles.setText("Apply to the next " + count + " file" + (count == 1 ? "" : "s"));
        uiAdditionalFiles.setVisible(count > 0);
    }
    public ImportProcessors.ProcessorWrapper getImportType() {
        return uiImportType.getValue();
    }
    public boolean isAppliedToRemainingItems() {
        return uiAdditionalFiles.isSelected();
    }
}
