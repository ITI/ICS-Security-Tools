package ui;

import core.Configuration;
import core.PcapDeviceList;
import core.Preferences;
import core.Version;
import core.document.Session;
import core.document.fingerprint.FPDocument;
import core.document.graph.LogicalNode;
import core.document.serialization.LoadTask;
import core.document.serialization.ProgressTask;
import core.document.serialization.SaveTask;
import core.fingerprint3.Fingerprint;
import core.importmodule.ImportProcessors;
import core.importmodule.LivePCAPImport;
import core.logging.Logger;
import core.logging.Severity;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import ui.custom.fx.ActiveButton;
import ui.custom.fx.ActiveMenuItem;
import ui.custom.fx.DynamicSubMenu;
import ui.custom.fx.MemoryUsageTracker;
import ui.dialog.*;
import ui.dialog.importmanager.ImportDialog;
import ui.fingerprint.FingerPrintGui;
import ui.graphing.Graph;
import ui.graphing.graphs.*;
import util.Launcher;
import util.Plugin;

import java.awt.Toolkit;
import java.awt.Desktop;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GrassMarlinFx extends Application{
    // == Menu Items that need contents repopulated from other actions.
    protected Menu menuCollapseNetworks;
    protected Menu menuExpandNetworks;
    protected Menu menuUnhideNetworks;
    protected Menu menuUnhideHosts;

    // == Dialogs
    protected ui.dialog.importmanager.ImportDialog dlgImport;
    protected PreferencesDialogFx dlgPreferences;
    protected ui.dialog.AboutDialogFx dlgAbout;
    protected TopologyKeyDialogFx dlgTopologyKey;
    protected ui.dialog.FilterDialogFx dlgPcapFilters;      // This is the dialog to manage PCap filters
    protected ui.dialog.ViewFilterDialogFx dlgViewFilters;  // This is the dialog to manage filters applied to the physical view
    protected final ManageLogicalNetworksDialogFx dlgManageLogicalNetworks;

    protected FileChooser chooserSessionSerialization;

    // == Other UI
    private SimpleBooleanProperty isPcapRunning;
    protected ChoiceBox<PcapDeviceList.DeviceEntry> cbPcapDevices;
    protected TabPane paneTabs;
    // The tree views (or other navigation controls) for each tab will be added here.
    protected Pane paneContextualNavigation;
    //There will be several graphs, each one having a tab and a tree control.  These are managed by the TabController
    private final TabController tabController;

    // == Data Visualization
    // Note: Anything not in the session is regarded as transient state information.
    protected ObservableList<ViewFilterDialogFx.ViewFilter> filtersFingerprints;
    protected ObservableList<ViewFilterDialogFx.ViewFilter> filtersCategories;
    protected ObservableList<ViewFilterDialogFx.ViewFilter> filtersCountries;

    //Initially, assume PCap is not available
    private final static SimpleBooleanProperty pcapAvailable = new SimpleBooleanProperty(false);
    public static BooleanProperty pcapAvailableProperty() {
        return pcapAvailable;
    }

    private final static SimpleStringProperty selectedPcapFilter = new SimpleStringProperty(Configuration.getPreferenceString(Configuration.Fields.PCAP_FILTER_TITLE)) {
        {
            this.addListener((observable, valueOld, valueNew) -> {
                Configuration.setPreferenceString(Configuration.Fields.PCAP_FILTER_TITLE, valueNew);
            });
        }
    };
    public static StringProperty selectedPcapFilterProperty() {
        return selectedPcapFilter;
    }

    private final SimpleObjectProperty<Session> document;
    public ObjectProperty<Session> documentProperty() {
        return document;
    }
    protected final BorderPane fields;

    // ========================================================================

    public GrassMarlinFx() {
        super();

        document = new SimpleObjectProperty<>();
        tabController = new TabController();
        fields = new BorderPane();

        dlgManageLogicalNetworks = ManageLogicalNetworksDialogFx.getInstance();

        // Most of the initialization is handled in the start method.
    }

    /**
     * Launch the Grassmarlin main GUI as a JavaFX application.
     * @param allowPcap Whether or not to enable PCAP features.  Functionality that depends on PCap capabilities will be disabled when this is set to false.
     * @param args Arguments that are passed to the JavaFX launch command.
     */
    public static void launchFx(boolean allowPcap, String[] args) {
        pcapAvailable.set(allowPcap);
        Application.launch(args);
    }

    protected Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        initComponents(stage);

        if(!Preferences.SuppressUnchangedVersionNotes.get() || !(Configuration.getPreferenceString(Configuration.Fields.LAST_RUN_VERSION).equals(Version.APPLICATION_VERSION))) {
            Platform.runLater(() -> new ReleaseNotesDialogFx().showAndWait());
        }
        Configuration.setPreferenceString(Configuration.Fields.LAST_RUN_VERSION, Version.APPLICATION_VERSION);

        newDocument();

        stage.setOnCloseRequest(event -> {
            if (document.get().isDirty()) {
                event.consume();
                CheckSaveDocument(() -> {
                    document.get().dirtyProperty().setValue(false);
                    javafx.event.Event.fireEvent(stage, new WindowEvent((Window) event.getTarget(), event.getEventType()));
                });
            }
        });

        fields.setOnDragOver(event -> {
            if (event.getGestureSource() != this && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        fields.setOnDragDropped(event -> {
            Dragboard board = event.getDragboard();

            if (board.hasFiles()) {
                for (File file : board.getFiles()) {
                    Path path = Paths.get(file.toString());
                    ImportProcessors.ProcessorWrapper processor = ImportProcessors.processorForPath(path);
                    if (processor != null) {
                        document.get().ProcessImport(ImportProcessors.newItem(processor.getProcessor(), path, GrassMarlinFx.getRunningFingerprints()));
                    } else {
                        Logger.log(this, Severity.Error, "Unable to process dropped file [" + path.toString() + "].  Reason: Unknown Type");
                    }
                }
                event.setDropCompleted(true);
            } else {
                event.setDropCompleted(false);
            }
            event.consume();
        });

        stage.show();
    }

    protected void newDocument() {
        tabController.clear();
        Session docNew = new Session();

        final LogicalGraph graphLogical = new LogicalGraph(docNew.getLogicalGraph(), this::WatchLogicalConnections, docNew.getLogicalGraph().getCidrList());
        final PhysicalGraph graphPhysical = new PhysicalGraph(docNew.getPhysicalGraph(), docNew.getPhysicalTopologyMapper());
        final MeshGraph graphMesh = new MeshGraph(docNew.getMeshGraph());

        tabController.AddContent(graphLogical).setClosable(false);
        tabController.AddContent(graphPhysical).setClosable(false);
        tabController.AddContent(graphMesh).setClosable(false);

        stage.titleProperty().bind(
                new SimpleStringProperty(Version.APPLICATION_TITLE)
                        .concat(" [")
                        .concat(docNew.currentSessionNameProperty())
                        .concat("]")
                        .concat(
                                new When(docNew.dirtyProperty()).then("*").otherwise("")
                        ));

        Launcher.enumeratePlugins(Plugin.SessionEventHooks.class).forEach(plugin -> plugin.sessionCreated(docNew));

        document.set(docNew);
    }

    private void initComponents(Stage stage) {
        stage.setTitle(Version.APPLICATION_TITLE);
        stage.getIcons().add(EmbeddedIcons.Logo.getRawImage());
        stage.setOnCloseRequest(e -> Platform.exit());

        dlgImport = new ImportDialog(document, this);
        dlgPreferences = new PreferencesDialogFx();
        dlgAbout = new AboutDialogFx();
        dlgTopologyKey = new TopologyKeyDialogFx();
        dlgPcapFilters = new FilterDialogFx();
        dlgViewFilters = new ViewFilterDialogFx();

        chooserSessionSerialization = new FileChooser();
        //We can't set the title because it will change between Save, Save As, and Open
        chooserSessionSerialization.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("GrassMarlin 3.2 Session (*.gm3)", "*.gm3"),
                new FileChooser.ExtensionFilter("All Files", "*")
        );

        isPcapRunning = new SimpleBooleanProperty(false);

        //Setting size minimizes quirks related to percentage-based layouts in the SplitPanes after we maximize.
        fields.setPrefWidth(Toolkit.getDefaultToolkit().getScreenSize().getWidth() * 0.8);
        fields.setPrefHeight(Toolkit.getDefaultToolkit().getScreenSize().getHeight() * 0.8);
        stage.setMaximized(true);

        VBox header = new VBox();
        //<editor-fold defaultstate="collapsed" desc="Main Menu">
        // Menu (0,0)2x1
        MenuBar menu = new MenuBar();
        menu.getMenus().addAll(
                new Menu("_File") {
                    {
                        this.getItems().addAll(
                                new ActiveMenuItem("_New Session", EmbeddedIcons.Vista_New, event -> {
                                    CheckSaveDocument(GrassMarlinFx.this::newDocument);
                                    //TODO: Memory appears to not be freed properly when creating a new session.
                                }).setAccelerator(KeyCodeCombination.CONTROL_DOWN, KeyCode.N),
                                new ActiveMenuItem("_Open Session...", EmbeddedIcons.Vista_Open, event -> {
                                    CheckSaveDocument(() -> {
                                        LoadDocument(null);
                                    });
                                }).setAccelerator(KeyCodeCombination.CONTROL_DOWN, KeyCode.O),
                                new ActiveMenuItem("_Save Session", EmbeddedIcons.Vista_Save, event -> {
                                    SaveDocument(document.get().getSavePath(), null);
                                }).setAccelerator(KeyCodeCombination.CONTROL_DOWN, KeyCode.S),
                                new ActiveMenuItem("Save Session _As...", event -> {
                                    SaveDocument(null, null);
                                }),
                                new SeparatorMenuItem(),
                                new ActiveMenuItem("_Clear Topology", EmbeddedIcons.Vista_Refresh, (event) -> {
                                    document.get().clearTopology();
                                    tabController.clearTopology();
                                }).setAccelerator(KeyCodeCombination.CONTROL_DOWN, KeyCode.X),
                                new SeparatorMenuItem(),
                                new ActiveMenuItem("_Import Files...", EmbeddedIcons.Vista_Import, GrassMarlinFx.this::Handle_ShowImportDialog).setAccelerator(KeyCodeCombination.CONTROL_DOWN, KeyCode.I),
                                new DynamicSubMenu("_Export", (Node)null, () -> {
                                    return Launcher.enumeratePlugins(Plugin.Export.class).stream()
                                            .flatMap(plugin -> plugin.getExportMenuItems(tabController, stage).stream())
                                            .collect(Collectors.toList());
                                }),
                                new SeparatorMenuItem(),
                                new ActiveMenuItem("E_xit", (action) -> {
                                    Event.fireEvent(stage, new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
                                })
                        );
                    }
                },
                new Menu("_View") {
                    {
                        this.getItems().addAll(
                                new ActiveMenuItem("Current _Log File", EmbeddedIcons.Vista_TextFile, (action) -> {
                                    try {
                                        String viewerLog = Configuration.getPreferenceString(Configuration.Fields.TEXT_EDITOR_EXEC);
                                        String pathLog = Launcher.getLogFilePath();
                                        Runtime.getRuntime().exec(new String[] {
                                                viewerLog,
                                                pathLog
                                        });
                                        Logger.log(this, Severity.Success, "Displaying log file (" + pathLog + ") using " + viewerLog);
                                    } catch(IOException ex) {
                                        Logger.log(this, Severity.Error, "Unable to display log file: " + ex.getMessage());
                                    }
                                }),
                                new SeparatorMenuItem(),
                                new ActiveMenuItem("Logical _Nodes Report", EmbeddedIcons.Vista_Report, (event) -> {
                                    new LogicalNodeReportDialogFx(document.get().getLogicalGraph()).show();
                                }),
                                new ActiveMenuItem("Logical _Connections Report", EmbeddedIcons.Vista_Report, (event) -> {
                                    new LogicalEdgeReportDialogFx(document.get().getLogicalGraph()).show();
                                }),
                                new ActiveMenuItem("Inter-_Group Connections Report", EmbeddedIcons.Vista_Report, event -> {
                                    new IntergroupConnectionReportDialogFx<>(document.get().getLogicalGraph()).show();
                                }),
                                new SeparatorMenuItem(),
                                new ActiveMenuItem("New _Filtered Logical View", event -> {
                                    CreateFilterView();
                                })
                        );
                    }
                },
                new Menu("_Packet Capture") {
                    {
                        this.getItems().addAll(
                                new ActiveMenuItem("Pcap Filter _Manager", EmbeddedIcons.Vista_Filter, (event) -> {
                                    dlgPcapFilters.LoadData();
                                    Optional<ButtonType> result = dlgPcapFilters.showAndWait();
                                    if(result.isPresent() && result.get() == ButtonType.OK) {
                                        dlgPcapFilters.SaveData();
                                    }
                                }).bindEnabled(pcapAvailable),
                                new ActiveMenuItem("_Start Live Capture", EmbeddedIcons.Vista_Record, (event) -> {
                                    StartLiveCapture();
                                }).bindEnabled(pcapAvailable.and(isPcapRunning.not())).setAccelerator(KeyCodeCombination.CONTROL_DOWN, KeyCode.R),
                                new ActiveMenuItem("_Halt Live Capture", EmbeddedIcons.Vista_Stop, (event) -> {
                                    StopLiveCapture();
                                }).bindEnabled(pcapAvailable.and(isPcapRunning)),
                                new SeparatorMenuItem(),
                                new ActiveMenuItem("Open Capture _Folder", EmbeddedIcons.Vista_Open, (event) -> {
                                    String pathCapture = Configuration.getPreferenceString(Configuration.Fields.DIR_LIVE_CAPTURE);
                                    try {
                                        Desktop.getDesktop().open(new File(pathCapture));
                                    } catch (IOException ex) {
                                        Logger.log(Desktop.class, Severity.Error, "Unable to open live capture folder (" + pathCapture + "): " + ex.getMessage());
                                    }
                                }),
                                new SeparatorMenuItem(),
                                new ActiveMenuItem("Manage _Networks", event -> {
                                    dlgManageLogicalNetworks.itemsProperty().set(document.get().getLogicalGraph().getCidrList());
                                    dlgManageLogicalNetworks.showAndWait();
                                })
                        );
                    }
                },
                new Menu("_Tools") {
                    {
                        this.getItems().addAll(
                                Launcher.getPluginMenuItem(),
                                new ActiveMenuItem("_Fingerprint Manager", event -> {
                                    FingerPrintGui editorGui = new FingerPrintGui();
                                    Stage editorWindow = new Stage();
                                    try {
                                        editorGui.start(editorWindow);
                                        editorWindow.show();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Logger.log(this, Severity.Error, "Error opening Fingerprint Editor");
                                    }
                                }),
                                new SeparatorMenuItem(),
                                new ActiveMenuItem("_Preferences", EmbeddedIcons.Vista_Personalization, (event) -> {
                                    dlgPreferences.reloadValues();

                                    dlgPreferences.showAndWait().ifPresent(response -> {
                                        if(!response.equals(ButtonType.CANCEL)) {
                                            final Set<Map.Entry<Configuration.Fields, String>> updates = dlgPreferences.getUpdatedValues().entrySet();
                                            if(!updates.isEmpty()) {
                                                Logger.log(dlgPreferences, Severity.Success, "Updating Preferences...");
                                                for (Map.Entry<Configuration.Fields, String> entry : updates) {
                                                    Configuration.setPreferenceString(entry.getKey(), entry.getValue());
                                                }
                                                //This will update the bound values by reloading the fields that were just saved.
                                                Preferences.refreshValues();
                                            }
                                        }
                                    });
                                })
                        );
                    }
                },
                new Menu("_Help") {
                    {
                        this.getItems().addAll(
                                new ActiveMenuItem("User _Guide", (event) -> DisplayUserGuide()),
                                new ActiveMenuItem("Topology _Key", (event) -> dlgTopologyKey.showAndWait()),
                                new ActiveMenuItem("_About", (event) -> dlgAbout.showAndWait()).setAccelerator(null, KeyCode.F1),
                                new ActiveMenuItem("Release _Notes", event -> new ReleaseNotesDialogFx().showAndWait())
                        );
                    }
                }
        );
        //</editor-fold>
        header.getChildren().add(menu);

        cbPcapDevices = new ChoiceBox<>();
        if(pcapAvailable.get()) {
            cbPcapDevices.setItems(PcapDeviceList.get());
            if (cbPcapDevices.getItems().size() == 0) {
                //If there are no pcap devices, disable pcap.
                pcapAvailable.set(false);
            } else {
                cbPcapDevices.getSelectionModel().select(0);
            }
        } else {
            cbPcapDevices.setDisable(true);
        }

        Pane paneToolbarSpacer = new Pane();
        HBox.setHgrow(paneToolbarSpacer, Priority.ALWAYS);
        ActiveButton btnStartPcap = new ActiveButton("Start", EmbeddedIcons.Vista_Record, (event) -> StartLiveCapture() );
        btnStartPcap.disableProperty().bind(pcapAvailable.not().or(isPcapRunning));
        ActiveButton btnStopPcap = new ActiveButton("Stop", EmbeddedIcons.Vista_Stop, (event) -> StopLiveCapture() );
        btnStopPcap.disableProperty().bind(pcapAvailable.not().or(isPcapRunning.not()));
        ToolBar toolbar = new ToolBar(
                new ActiveButton("", EmbeddedIcons.Vista_Import, this::Handle_ShowImportDialog),
                new ActiveButton("", EmbeddedIcons.Vista_Open, (event) -> {
                    CheckSaveDocument(() -> {
                        LoadDocument(null);
                    });
                }),
                new ActiveButton("", EmbeddedIcons.Vista_Save, (event) -> {
                    SaveDocument(document.get().getSavePath(), null);
                }),
                paneToolbarSpacer,
                cbPcapDevices,
                btnStartPcap,
                btnStopPcap
        );
        header.getChildren().add(toolbar);
        fields.setTop(header);

        // Navigation View (Upper portion of left side of main window)
        paneContextualNavigation = new Pane();
        // Event Log (Lower portion of left side of main window)
        ui.custom.fx.LogViewer logViewer = new ui.custom.fx.LogViewer();


        SplitPane paneLeft = new SplitPane();
        paneLeft.setOrientation(Orientation.VERTICAL);
        paneLeft.getItems().addAll(
                paneContextualNavigation,
                logViewer
        );
        paneLeft.setDividerPositions(0.7);
        //paneLeft will be added to paneMain below.

        // Tab Views (Right side of main window)
        paneTabs = new TabPane();

        //tabController will manage the content on paneTabs and paneContextualNavigation
        tabController.Init(paneContextualNavigation, paneTabs);

        SplitPane paneMain = new SplitPane();
        paneMain.setOrientation(Orientation.HORIZONTAL);
        paneMain.getItems().addAll(
                paneLeft,
                paneTabs
        );
        paneMain.setDividerPositions(0.2);
        fields.setCenter(paneMain);

        // Status bar (bottom)
        ToolBar statusbar = new ToolBar(
                new MemoryUsageTracker(20)
        );
        fields.setBottom(statusbar);

        stage.setScene(new Scene(fields));
    }

    private LivePCAPImport pcapCurrent = null;
    public void StartLiveCapture() {
        try {
            //Get Device
            PcapDeviceList.DeviceEntry entry = cbPcapDevices.getSelectionModel().getSelectedItem();
            if (entry == null) {
                return;
            }

            if (pcapCurrent != null) {
                Logger.log(this, Severity.Error, "Cannot start a new Live PCAP session while performing a Live PCAP session.");
                return;
            }

            isPcapRunning.set(true);
            final String nameDumpFile = Configuration.getPreferenceString(Configuration.Fields.DIR_LIVE_CAPTURE) + File.separator + Instant.now().atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_INSTANT).replaceAll("\\D", "_") + ".pcap";
            pcapCurrent = new LivePCAPImport(Paths.get(nameDumpFile), entry.getDevice(), GrassMarlinFx.getRunningFingerprints(), () -> document.get().getLogicalGraph().refresh());
            document.get().ProcessImport(pcapCurrent);
        } catch(Exception ex) {
            Logger.log(this, Severity.Error, "Unable to start live pcap: " + ex.getMessage());
            isPcapRunning.set(false);
            StopLiveCapture();
        }
    }

    public void StopLiveCapture() {
        if(pcapCurrent != null) {
            pcapCurrent.stop();
            isPcapRunning.set(false);
            pcapCurrent = null;
            document.get().getLogicalGraph().refresh();
            document.get().getMeshGraph().refresh();
        }
    }

    public void CreateFilterView() {
        for(Graph graphTemp : tabController.getGraphs()) {
            if(graphTemp instanceof LogicalGraph) {
                final LogicalGraph graphRoot = (LogicalGraph)graphTemp;
                LogicalFilterGraph graph = new LogicalFilterGraph(graphRoot);

                Tab tabFilter = tabController.AddContent(graph, "Filtered View");
                tabController.ShowTab(tabFilter);

                return;
            }
        }
    }

    public void WatchLogicalConnections(LogicalNode nodeRoot, int degrees) {
        LogicalWatchGraph graph = new LogicalWatchGraph(document.get().getLogicalGraph(), nodeRoot, degrees);
        Tab tabWatch = tabController.AddContent(graph);

        tabController.ShowTab(tabWatch);

        Platform.runLater(() -> graph.getVisualizationView().zoomToFit());
    }

    public void DisplayUserGuide() {
        // Attempt to locate the user guide in the MISC directory.
        File fileMisc = new File(Configuration.getPreferenceString(Configuration.Fields.PATH_USERGUIDE));
        try {
            if(!fileMisc.exists()) {
                Logger.log(this, Severity.Error, "Unable to locate User Guide.");
                return;
            }

            String path = fileMisc.getAbsolutePath();
            String exec = Configuration.getPreferenceString(Configuration.Fields.PDF_VIEWER_EXEC);
            if (exec != null && !exec.isEmpty()) {
                // If the PDF Viewer path has been set, then use it, assuming that a single parameter for the PDF to open is accepted.
                try {
                    Runtime.getRuntime().exec(new String[]{exec, path});
                } catch (IOException ex) {
                    Logger.log(this, Severity.Error, "Error opening User guide: " + ex.getMessage());
                }
            } else {
                //PDF Viewer isn't set, so try desktop execute
                Logger.log(this, Severity.Warning, "PDF Viewer needs to be set in Preferences.  Attempting fallback.");
                try {
                    Desktop.getDesktop().open(fileMisc);
                } catch (IOException ex) {
                    Logger.log(Desktop.class, Severity.Error, "Unable to open User Guide (" + fileMisc.getPath() + "): " + ex.getMessage());
                }
            }
        } catch(NullPointerException ex) {
            Logger.log(this, Severity.Error, "Unable to locate User Guide.");
        }
    }

    /**
     * If the current session has unsaved changes (isDirty) then ask the user if they want to save the changes.
     * If the user elects to save, if a filename must be specified, then prompt as per "Save As...", otherwise Save
     * If the user declines to save, then return.
     * If the user cancels at any point, abort the save and return false.
     */
    public void CheckSaveDocument(Runnable onSuccess) {
        if(document.get().isDirty()) {
            //If the session is clean, there is no reason to prompt.
            if(document.get().getSavePath() != null) {
                Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to save changes to " + document.get().getSavePath().getFileName() + "?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL).showAndWait();
                if(!result.isPresent() || result.get().equals(ButtonType.CANCEL)) {
                    //Cancel
                } else if (result.get().equals(ButtonType.NO)) {
                    onSuccess.run();
                } else if (result.get().equals(ButtonType.YES)) {
                    SaveDocument(document.get().getSavePath(), onSuccess);
                }
            } else {
                Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to save changes to New Session?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL).showAndWait();
                if(!result.isPresent() || result.get().equals(ButtonType.CANCEL)) {
                } else if (result.get().equals(ButtonType.NO)) {
                    onSuccess.run();
                } else if (result.get().equals(ButtonType.YES)) {
                    SaveDocument(null, onSuccess);
                }

            }
        } else {
            //If the session isn't dirty, there is nothing to save.
            onSuccess.run();
        }
    }

    public void LoadDocument(Runnable onSuccess) {
        Launcher.CreateUserDirectories();

        chooserSessionSerialization.setTitle("Open Session...");
        File file = chooserSessionSerialization.showOpenDialog(this.stage);
        if(file == null) {
            return;
        }
        Path from = file.toPath();
        if(from == null || ! Files.exists(from)) {
            return;
        }

        tabController.clear();
        newDocument();

        ProgressTask task = new LoadTask(from, document.get(), tabController, onSuccess, this::newDocument);

        task.start();
    }

    public void SaveDocument(Path as, Runnable onSuccess) {
        if(as == null) {
            //Get path or cancel
            chooserSessionSerialization.setTitle("Save As...");
            Launcher.CreateUserDirectories();
            File result = chooserSessionSerialization.showSaveDialog(this.stage);
            if(result == null) {
                return;
            }
            as = result.toPath();
            document.get().setSavePath(as);
        }

        ProgressTask task = new SaveTask(document.get(), tabController, as, onSuccess);

        task.start();
    }

    public static List<Fingerprint> getRunningFingerprints() {
        return Collections.unmodifiableList(FPDocument.getInstance().getEnabledFingerprints());
    }

    private void Handle_ShowImportDialog(Object event) {
        if(dlgImport.isShowing()) {
            dlgImport.close();
        }
        dlgImport.show();
    }
}
