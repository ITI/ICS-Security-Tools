package ui.dialog.importmanager;

import core.Configuration;
import core.Version;
import core.document.Event;
import core.document.Session;
import core.importmodule.ImportItem;
import core.importmodule.ImportProcessors;
import core.logging.Logger;
import core.logging.Severity;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.When;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import ui.GrassMarlinFx;
import ui.custom.fx.ActiveMenuItem;
import ui.custom.fx.DynamicSubMenu;
import ui.custom.fx.LogViewer;
import util.FileUnits;
import util.Launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ImportDialog extends Dialog {
    private static DateTimeFormatter FORMAT_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static class PreliminaryImportItem {
        private final File source;
        private SimpleObjectProperty<ImportProcessors.ProcessorWrapper> type;

        public PreliminaryImportItem(File source) {
            this.source = source;
            type = new SimpleObjectProperty<>(null);
        }

        public final String getPath() {
            return source.getPath();
        }

        public final String getSize() {
            return FileUnits.formatSize(source.length());
        }

        public final ImportProcessors.ProcessorWrapper getType() {
            return type.get();
        }
        public void setType(ImportProcessors.ProcessorWrapper value) {
            type.set(value);
        }
        public ObjectProperty<ImportProcessors.ProcessorWrapper> typeProperty() {
            return type;
        }
    }

    private SimpleObjectProperty<Session> document;

    private FileChooser chooserFile;
    private FileChooser chooserQuicklist;
    private DirectoryChooser chooserFolder;

    private TableView<PreliminaryImportItem> tblPending;
    private TableView<ImportItem> tblRunning;

    protected GrassMarlinFx gui;
    protected SelectImportTypeDialog dialogSelectImportType;

    public ImportDialog(ObjectProperty<Session> document, GrassMarlinFx gui) {
        this.document = new SimpleObjectProperty<>();
        this.document.bind(document);
        this.gui = gui;

        dialogSelectImportType = new SelectImportTypeDialog();

        initComponents();

        //Only bind after this dialog has been initialized
        this.document.addListener(this::Handle_DocumentChange);
        Handle_DocumentChange(null, null, document.get());
    }

    private void initComponents() {
        this.setTitle("Import");
        this.initModality(Modality.NONE);
        this.initStyle(StageStyle.UTILITY);

        //These elements are constructed here because they are referenced in event handlers below.
        final Button btnQuicklistSave = new Button("Save Quicklist");
        final Button btnImport = new Button("Import Selected");

        //TODO: File choosers should use a default path
        chooserFile = new FileChooser();
        chooserFile.setTitle("Add Files");
        chooserFile.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", "*"),
                new FileChooser.ExtensionFilter("PCAP Files (*.pcap, *.pcapng)", "*.pcap", "*.pcapng"),
                new FileChooser.ExtensionFilter("Bro2Conn Files (*.conn.*)", "*.conn.*"),
                new FileChooser.ExtensionFilter("GrassMarlin 3.X Export Files (*.xml)", "*.xml")
        );
        chooserFolder = new DirectoryChooser();
        chooserFolder.setTitle("Add Folder");
        chooserQuicklist = new FileChooser();
        chooserQuicklist.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Quicklists (*.g3)", "*.g3"),
                new FileChooser.ExtensionFilter("All Files", "*")
        );

        GridPane fields = new GridPane();
        fields.setVgap(10);
        fields.setHgap(10);
        fields.setPadding(new Insets(25, 25, 25, 25));

        // == Pending Imports Label (0,0) 5x1
        final Label lblPendingImports = new Label("Pending Imports");
        fields.add(lblPendingImports, 0, 0, 5, 1);
        // == Pending Imports List  (0,1) 5x1
        tblPending = new TableView<>();
        // It would be nice if you could reuse columns in multiple tables, but that causes problems; so these are reproduced below as well.
        TableColumn<PreliminaryImportItem, String> colFile = new TableColumn<>("File Name");
        colFile.setCellValueFactory(new PropertyValueFactory<>("path"));
        colFile.setPrefWidth(240.0);
        TableColumn<PreliminaryImportItem, String> colSize = new TableColumn<>("Size");
        colSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        TableColumn<PreliminaryImportItem, ImportProcessors.ProcessorWrapper> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        tblPending.setRowFactory(view -> {
            final TableRow<PreliminaryImportItem> row = new TableRow<>();
            final ContextMenu menuRow = new ContextMenu();
            menuRow.getItems().addAll(
                    new DynamicSubMenu("Set Type", (Node)null, () ->
                        ImportProcessors.getProcessors().stream().map(processor ->
                            new ActiveMenuItem(processor.toString(), (event) -> {
                                row.getItem().setType(processor);
                            })
                        ).collect(Collectors.toList())
                    ),
                    new ActiveMenuItem("Remove", event -> {
                        document.get().removePendingImport(row.getItem());
                    })
            );

            row.contextMenuProperty().bind(new When(row.emptyProperty()).then((ContextMenu)null).otherwise(menuRow));
            return row;
        });

        tblPending.setPrefHeight(360.0);
        tblPending.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tblPending.getColumns().addAll(colFile, colSize, colType);
        tblPending.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<PreliminaryImportItem>() {
            @Override
            public void onChanged(Change<? extends PreliminaryImportItem> c) {
                //Pull a snapshot in case somebody is making edits in another thread; at least we will be internally consistent.
                //Also, please don't edit the selection of a private UI element from a different thread.  Ever.
                boolean isEmpty = c.getList().isEmpty();
                btnQuicklistSave.setDisable(isEmpty);
                btnImport.setDisable(isEmpty);
            }
        });
        tblPending.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.DELETE) {
                document.get().removePendingImports(tblPending.getSelectionModel().getSelectedItems());
                tblPending.getSelectionModel().clearSelection();
            }
        });
        fields.add(tblPending, 0, 1, 5, 1);

        //== Add Files Button (0,2)
        Button btnFiles = new Button("Add Files");
        btnFiles.setOnAction(event -> {
            addPreliminaryImportItemsForFiles(chooserFile.showOpenMultipleDialog(super.getOwner()));
        });
        fields.add(btnFiles, 0, 2);
        // == Load Quicklist (1,2)
        Button btnQuicklistLoad = new Button("Load Quicklist");
        btnQuicklistLoad.setOnAction(event -> {
            Launcher.CreateUserDirectories();
            chooserQuicklist.setTitle("Load Quicklist");
            Path defaultDir = Paths.get(Configuration.getPreferenceString(Configuration.Fields.DIR_QUICKLIST));
            Path parentDir = defaultDir.getParent();
            if (Files.exists(parentDir)) {
                if (Files.notExists(defaultDir)) {
                    try {
                        Files.createDirectory(defaultDir);
                    } catch (IOException ioe) {
                        Logger.log(this, Severity.Warning, "Unable to create non-existant quicklist directory " + defaultDir);
                    }
                }
                if (Files.exists(defaultDir)) {
                    chooserQuicklist.setInitialDirectory(defaultDir.toFile());
                }
            }
            File chosen = chooserQuicklist.showOpenDialog(super.getOwner());
            if(chosen != null && chosen.exists()) {
                LoadQuicklist(chosen);
            }
        });
        fields.add(btnQuicklistLoad, 1, 2);
        // == Save Quicklist (2,2)
        btnQuicklistSave.setDisable(true);
        btnQuicklistSave.setOnAction(event -> {
            Launcher.CreateUserDirectories();
            chooserQuicklist.setTitle("Save Quicklist");
            chooserQuicklist.setInitialDirectory(new File(Configuration.getPreferenceString(Configuration.Fields.DIR_QUICKLIST)));
            chooserQuicklist.setInitialFileName("import_" + java.time.Instant.now().atZone(java.time.ZoneId.of("Z")).format(FORMAT_TIMESTAMP) + ".g3");

            File chosen = chooserQuicklist.showSaveDialog(super.getOwner());
            List<PreliminaryImportItem> lstItems = tblPending.getSelectionModel().getSelectedItems();
            if (chosen != null && !lstItems.isEmpty()) {
                SaveQuicklist(lstItems, chosen);
            } else {
                Logger.log(this, Severity.Warning, "No items were found in the quicklist.");
            }
        });
        fields.add(btnQuicklistSave, 2, 2);
        // == Import Command (4,2)
        btnImport.setDisable(true); //No selection by default
        btnImport.setAlignment(Pos.CENTER_RIGHT); //In case there is extra width in this column.
        btnImport.setOnAction(event -> {
            this.beginImport(tblPending.getSelectionModel().getSelectedItems());
            tblPending.getSelectionModel().clearSelection();
        });
        fields.add(btnImport, 4, 2);

        // == Running and Completed Imports Label (0,3) 6x1
        final Label lblCompletedImports = new Label("Running and Completed Imports");
        fields.add(lblCompletedImports, 0, 3, 6, 1);
        // == Active Imports Table (0,4) 6x1
        tblRunning = new TableView();
        TableColumn<ImportItem, String> colProgress = new TableColumn("Progress");
        //colProgress.setCellValueFactory(new PropertyValueFactory<Double, Integer>("progress"));
        final SimpleStringProperty propRunning = new SimpleStringProperty("Running...");
        colProgress.setCellValueFactory(cellDataFeatures -> {
            if(cellDataFeatures.getValue() == null) {
                return new SimpleStringProperty("");
            } else {
                return Bindings.when(cellDataFeatures.getValue().progressProperty().lessThan(0.0))
                        .then(propRunning)
                        .otherwise(
                                Bindings.when(cellDataFeatures.getValue().progressProperty().greaterThanOrEqualTo(1.0))
                                    .then("Complete")
                                    .otherwise(cellDataFeatures.getValue().progressProperty().multiply(100.0).asString("%.02f%%"))
                        );
            }
        });


        TableColumn colRunningFile = new TableColumn("File");
        colRunningFile.setPrefWidth(240.0);
        colRunningFile.setCellValueFactory(new PropertyValueFactory<ImportItem, String>("path"));
        TableColumn colRunningSize = new TableColumn("Size");
        colRunningSize.setCellValueFactory(new PropertyValueFactory<ImportItem, String>("displaySize"));
        tblRunning.getColumns().addAll(colProgress, colRunningFile, colRunningSize);
        tblRunning.setPrefHeight(180.0);
        colProgress.setSortType(TableColumn.SortType.ASCENDING);
        tblRunning.getSortOrder().add(colProgress);

        //Timer will update the running table every 1/8th of a second.  Lacking this, completed items sometimes are left looking as if they are incomplete.
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                tblRunning.refresh();
            }
        }, 1000, 125);

        fields.add(tblRunning, 0, 4, 6, 1);

        // Event Log (5,1) 1x2
        LogViewer ctrTemp = new LogViewer();
        fields.add(ctrTemp, 5, 1, 1, 2);

        this.getDialogPane().setContent(fields);
        this.getDialogPane().getButtonTypes().addAll(new ButtonType("Close", ButtonBar.ButtonData.OK_DONE));
    }

    private void Handle_DocumentChange(ObservableValue<? extends Session> source, Session docOld, Session docNew) {
        if(docOld != null) {
            docOld.OnDocumentCleared.removeHandler(this::Handle_DocumentCleared);
        }
        if(docNew != null) {
            docNew.OnDocumentCleared.addHandler(this::Handle_DocumentCleared);
            tblRunning.setItems(docNew.getImports());
            tblPending.setItems(docNew.getPendingImports());
        } else {
            tblRunning.setItems(null);
        }

    }

    /**
     * When the session is cleared, we need to remove all ingested content and, for usability, move them to the preliminary dialog and select them.
     * @param source
     * @param args
     */
    protected void Handle_DocumentCleared(Event<Session.DocumentUpdatedEventArgs> source, Session.DocumentUpdatedEventArgs args) {
        List<PreliminaryImportItem> newItems =
                args.session.getImports().stream().map(completed_import -> {
                    PreliminaryImportItem result = new PreliminaryImportItem(completed_import.getPath().toFile());
                    result.setType(ImportProcessors.processorForClass(completed_import.getType()));
                    return result;
                }).collect(Collectors.toList());
        document.get().addPendingImports(newItems);
        tblPending.getSelectionModel().clearSelection();
        newItems.forEach(tblPending.getSelectionModel()::select);
    }


    protected void beginImport(List<PreliminaryImportItem> selectedItems) {
        // Since we remove from the source list as they are processed, this will affect the contents of selectedItems.
        //Make a copy of selectedItems first so that changes don't propagate, resulting in skipped imports.
        ArrayList<PreliminaryImportItem> lstToImport = new ArrayList<>(selectedItems);
        for(PreliminaryImportItem item : lstToImport) {
            try {
                ImportItem itemNew = ImportProcessors.newItem(item.getType().getProcessor(), Paths.get(item.getPath()), GrassMarlinFx.getRunningFingerprints());
                if(itemNew == null) {
                    Logger.log(this, Severity.Error, "Unable to start import of [" + item.getPath() + "]: Unable to initialize import routine.");
                    continue;
                }
                document.get().ProcessImport(itemNew);
                document.get().removePendingImport(item);
            } catch(Exception e) {
                if(item != null) {
                    Logger.log(this, Severity.Error, "Unable to start import of [" + item.getPath() + "]: " + e.getMessage());
                } else {
                    Logger.log(this, Severity.Error, "Unable to start import: " + e.getMessage());
                }
            }
        }
    }

    private void LoadQuicklist(File chosen) {
        if(chosen == null || !chosen.exists() || chosen.isDirectory() || !chosen.canRead()) {
            Logger.log(this, Severity.Error, "No file selected or unable to open file.");
            return;
        }
        Logger.log(this, Severity.Information, "Loading quicklist: " + chosen.getName());

        Properties quicklistContents = new Properties();
        try(FileInputStream streamChosen = new FileInputStream(chosen)) {
            quicklistContents.load(streamChosen);
        } catch (IOException ex) {
            Logger.log(this, Severity.Error, "Unable to parse quicklist: " + ex.getMessage());
            return;
        }

        quicklistContents.forEach((key, value) -> {
            PreliminaryImportItem itemNew = new PreliminaryImportItem(new File((String) key));
            try {
                itemNew.setType(ImportProcessors.processorForName((String)value));
            } catch (Exception e) {
                //Unable to set type
                Logger.log(this, Severity.Error, "Unable to process quicklist item: " + e.getMessage());
                return;
            }

            document.get().addPendingImport(itemNew);
            tblPending.getSelectionModel().select(itemNew);
        });
    }

    private void SaveQuicklist(Collection<PreliminaryImportItem> imports, File chosen) {
        if(imports == null || imports.isEmpty()) {
            Logger.log(this, Severity.Warning, "No files selected to save to quicklist.");
            return;
        }
        if(chosen == null || (chosen.exists() && chosen.isDirectory())) {
            Logger.log(this, Severity.Error, "The selected file cannot be created.");
            return;
        }

        Properties quicklistContents = new Properties();
        for(PreliminaryImportItem item : imports) {
            quicklistContents.put(new File(item.getPath()).getAbsolutePath(), item.getType().toString());
        }

        try(FileOutputStream out = new FileOutputStream(chosen)) {
            quicklistContents.store(out, "Version:" + Version.APPLICATION_VERSION);
        } catch (IOException ex) {
            Logger.log(this, Severity.Error, "An error occurred while writing the quicklist: " + ex.getMessage());
        }
    }

    public void addPreliminaryImportItemsForFiles(Collection<File> files) {
        if(files != null && !files.isEmpty()) {
            //Filter list to remove directories and files that don't exist.  There shouldn't be nulls in the list, but better to catch those too.
            files = files.stream().filter(file -> file != null && file.exists() && !file.isDirectory()).collect(Collectors.toList());

            ImportProcessors.ProcessorWrapper typeDefault = null;
            int remaining = files.size();
            // Find the import type for each item; if we can't figure it out quickly, prompt for a value and permit the user to apply the same type to all files of the same extension / to all files.
            for(File item : files) {
                remaining--;
                if(item == null || !item.exists()) {
                    continue;
                }

                PreliminaryImportItem itemNew = new PreliminaryImportItem(item);
                if(typeDefault != null) {
                    itemNew.setType(typeDefault);
                } else {
                    ImportProcessors.ProcessorWrapper typeNew = ImportProcessors.processorForPath(Paths.get(itemNew.getPath()));
                    if(typeNew != null) {
                        itemNew.setType(typeNew);
                    } else {
                        dialogSelectImportType.setCurrentFile(item);
                        dialogSelectImportType.setFilesRemaining(remaining);
                        dialogSelectImportType.showAndWait();

                        if(dialogSelectImportType.isAppliedToRemainingItems()) {
                            typeDefault = dialogSelectImportType.getImportType();
                        }
                        itemNew.setType(dialogSelectImportType.getImportType());
                    }
                }
                document.get().addPendingImport(itemNew);
                tblPending.getSelectionModel().select(itemNew);
            }
        }
    }
}
