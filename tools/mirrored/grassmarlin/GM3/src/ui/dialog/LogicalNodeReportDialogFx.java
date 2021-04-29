package ui.dialog;

import core.document.graph.LogicalEdge;
import core.document.graph.LogicalNode;
import core.document.graph.NetworkGraph;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.transformation.FilteredList;
import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import ui.EmbeddedIcons;
import ui.custom.fx.ActiveMenuItem;
import util.Csv;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class LogicalNodeReportDialogFx extends Dialog {
    private final SimpleObjectProperty<NetworkGraph<LogicalNode, LogicalEdge>> network;
    private final HashMap<String, TableColumn<LogicalNode, String>> activeColumns;
    private final FilteredList<LogicalNode> nodesFiltered;
    private final FileChooser dlgSave;

    public LogicalNodeReportDialogFx(NetworkGraph<LogicalNode, LogicalEdge> network) {
        this.network = new SimpleObjectProperty<>(network);
        this.activeColumns = new HashMap<>();
        this.nodesFiltered = new FilteredList<>(network.getNodes());
        this.dlgSave = new FileChooser();

        initComponents();
    }

    private void initComponents() {
        setTitle("Logical Node Reports");
        Window stage = super.getDialogPane().getScene().getWindow();
        if(stage instanceof Stage) {
            ((Stage)stage).getIcons().add(EmbeddedIcons.Vista_Report.getRawImage());
        }
        this.setResizable(true);

        dlgSave.setTitle("Export To...");
        dlgSave.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"),
                new FileChooser.ExtensionFilter("Data Files (*.csv, *.xls, *.xlsx, *.xml, *.prn, *ods)", "*.csv", "*.xls", "*.xlsx", "*.xml", "*.prn", "*.ods"),
                new FileChooser.ExtensionFilter("All Files", "*")
        );

        TableView<LogicalNode> table = new TableView<>();
        //TODO: For some reason, the report columns are not sortable...  is it because it is a filtered list?
        table.setItems(nodesFiltered);
        TableColumn<LogicalNode, String> colTitle = new TableColumn<>("IP");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        table.getColumns().add(colTitle);

        BorderPane layoutWindow = new BorderPane();

        ToolBar toolbarWindow = new ToolBar();
        //Filter: Radio Group: All, Sources, Destinations, Both
        VBox rgFilter = new VBox();
        ToggleGroup groupFilter = new ToggleGroup();
        RadioButton optAll = new RadioButton("All");
        optAll.setSelected(true);
        RadioButton optSource = new RadioButton("Source");
        RadioButton optDestination = new RadioButton("Destination");
        RadioButton optBoth = new RadioButton("Source and Destination");
        groupFilter.getToggles().addAll(optAll, optSource, optDestination, optBoth);
        rgFilter.getChildren().addAll(optAll, optSource, optDestination, optBoth);

        optAll.setOnAction(event -> {
            nodesFiltered.setPredicate(logicalNode -> true);
        });
        //Frame counts make more sense, but bytesProperty evaluates faster.
        optSource.setOnAction(event -> {
            nodesFiltered.setPredicate(logicalNode ->
                            network.get().getEdgesInvolving(logicalNode).stream()
                                    .anyMatch(edge ->
                                            (edge.getSource().equals(logicalNode) && edge.getDetailsToDestination().bytesProperty().get() > 0)
                                            ||
                                            (edge.getDestination().equals(logicalNode) && edge.getDetailsToSource().bytesProperty().get() > 0)
                                    )
            );
        });
        optDestination.setOnAction(event -> {
            nodesFiltered.setPredicate(logicalNode ->
                            network.get().getEdgesInvolving(logicalNode).stream()
                                    .anyMatch(edge ->
                                            (edge.getSource().equals(logicalNode) && edge.getDetailsToSource().bytesProperty().get() > 0)
                                            ||
                                            (edge.getDestination().equals(logicalNode) && edge.getDetailsToDestination().bytesProperty().get() > 0)
                                    )
            );
        });
        optBoth.setOnAction(event -> {
            nodesFiltered.setPredicate(logicalNode ->
                            network.get().getEdgesInvolving(logicalNode).stream()
                                    .anyMatch(edge ->
                                                    (edge.getSource().equals(logicalNode) && edge.getDetailsToDestination().bytesProperty().get() > 0)
                                                    ||
                                                    (edge.getDestination().equals(logicalNode) && edge.getDetailsToSource().bytesProperty().get() > 0)
                                    )
                            &&
                            network.get().getEdgesInvolving(logicalNode).stream()
                                    .anyMatch(edge ->
                                                    (edge.getSource().equals(logicalNode) && edge.getDetailsToSource().bytesProperty().get() > 0)
                                                    ||
                                                    (edge.getDestination().equals(logicalNode) && edge.getDetailsToDestination().bytesProperty().get() > 0)
                                    )
            );
        });

        // Add column for field
        ComboBox<String> cbFields = new ComboBox<>();
        Button btnAddField = new Button("Add");
        cbFields.setOnShowing(this::Handle_ColumnSelectorShowing);
        cbFields.setOnAction(event1 -> {
            btnAddField.setDisable(cbFields.getSelectionModel().getSelectedItem() == null);
        });

        btnAddField.setOnAction(event -> {
            String key = cbFields.getSelectionModel().getSelectedItem();
            if(key == null || key.isEmpty() || activeColumns.containsKey(key)) {
                //Ignore it.
                return;
            }
            TableColumn<LogicalNode, String> colNew = new TableColumn<>(key);
            colNew.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getGroups().get(key)));

            colNew.setContextMenu(new ContextMenu());
            colNew.getContextMenu().getItems().add(new ActiveMenuItem("Remove Column", event1 -> {
                activeColumns.remove(key);
                table.getColumns().remove(colNew);
            }));
            activeColumns.put(key, colNew);
            table.getColumns().add(colNew);

            btnAddField.setDisable(true);
            cbFields.getSelectionModel().clearSelection();
        });
        //Export to CSV
        Button btnExportToCsv = new Button("Export CSV...", EmbeddedIcons.Vista_Save.getImage(16.0));
        btnExportToCsv.setOnAction(event -> {
            File result = dlgSave.showSaveDialog(this.getOwner());
            if(result != null) {
                Csv.ExportTableToFile(table, result);
            }
        });

        Pane paneToolbarSpacer = new Pane();
        HBox.setHgrow(paneToolbarSpacer, Priority.ALWAYS);
        toolbarWindow.getItems().addAll(
                rgFilter,
                paneToolbarSpacer,
                cbFields,
                btnAddField,
                btnExportToCsv
        );

        layoutWindow.setTop(toolbarWindow);
        layoutWindow.setCenter(table);

        this.getDialogPane().setContent(layoutWindow);
        this.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
    }

    protected void Handle_ColumnSelectorShowing(Event event) {
        List<String> allFields = findAllFieldNames();
        allFields.removeAll(activeColumns.keySet());
        ((ComboBox<String>)event.getTarget()).getItems().clear();
        ((ComboBox<String>)event.getTarget()).getItems().addAll(allFields);
    }

    protected List<String> findAllFieldNames() {
        NetworkGraph<LogicalNode, LogicalEdge> graph = network.get();
        if(graph == null) {
            return new LinkedList<>();
        } else {
            LinkedList<String> result = new LinkedList<>();
            for(LogicalNode node : graph.getNodes()) {
                Set<String> keys = node.getGroups().keySet();
                result.removeAll(keys);
                result.addAll(keys);
            }
            return result;
        }
    }
}
