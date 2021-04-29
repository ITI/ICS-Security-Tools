package ui.dialog;

import core.document.graph.LogicalEdge;
import core.document.graph.LogicalNode;
import core.document.graph.NetworkGraph;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import ui.EmbeddedIcons;
import util.Csv;

import java.io.File;

public class LogicalEdgeReportDialogFx extends Dialog {
    private final FilteredList<LogicalEdge> edgesFiltered;
    private final FileChooser dlgSave;

    public LogicalEdgeReportDialogFx(NetworkGraph<LogicalNode, LogicalEdge> network) {
        this.edgesFiltered = new FilteredList<>(network.getEdges());
        this.dlgSave = new FileChooser();

        initComponents();
    }

    private void initComponents() {
        setTitle("Logical Connection Report");
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

        TableView<LogicalEdge> table = new TableView<>();
        table.setItems(edgesFiltered);
        TableColumn<LogicalEdge, String> colSource = new TableColumn<>("Source");
        colSource.setCellValueFactory(cellData -> cellData.getValue().getSource().titleProperty());
        TableColumn<LogicalEdge, String> colDestination = new TableColumn<>("Destination");
        colDestination.setCellValueFactory(cellData -> cellData.getValue().getDestination().titleProperty());

        TableColumn<LogicalEdge, String> colBytesSent = new TableColumn<>("Bytes Sent");
        colBytesSent.setCellValueFactory(cellData -> cellData.getValue().getDetailsToDestination().bytesProperty().asString());

        TableColumn<LogicalEdge, String> colBytesRecv = new TableColumn<>("Bytes Received");
        colBytesRecv.setCellValueFactory(cellData -> cellData.getValue().getDetailsToSource().bytesProperty().asString());

        table.getColumns().addAll(
                colSource,
                colDestination,
                colBytesSent,
                colBytesRecv
        );

        BorderPane layoutWindow = new BorderPane();

        ToolBar toolbarWindow = new ToolBar();

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
                paneToolbarSpacer,
                btnExportToCsv
        );

        layoutWindow.setTop(toolbarWindow);
        layoutWindow.setCenter(table);

        this.getDialogPane().setContent(layoutWindow);
        this.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
    }
}
