package ui.dialog;

import core.document.graph.IEdge;
import core.document.graph.INode;
import core.document.graph.LogicalEdge;
import core.document.graph.NetworkGraph;
import javafx.beans.property.ReadOnlyStringWrapper;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class IntergroupConnectionReportDialogFx<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> extends Dialog {
    protected static class Connection {
        private final String source;
        private final String target;

        public Connection(String source, String target) {
            this.source = source;
            this.target = target;
        }

        public String getSource() {
            return source;
        }
        public String getTarget() {
            return target;
        }

        @Override
        public int hashCode() {
            //One can be null but not both.
            if(source == null) {
                return target.hashCode();
            }
            if(target == null) {
                return source.hashCode();
            }
            return source.hashCode() ^ target.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if(other instanceof Connection) {
                Connection conn = (Connection)other;

                return (target == null ? conn.target == null : target.equals(conn.target))
                        &&
                        (source == null ? conn.source == null : source.equals(conn.source));
            }
            return false;
        }
    }

    private final List<IEdge<? extends INode<?>>> edges;
    private final List<String> groups;
    private final FileChooser dlgSave;

    private final TableView<Connection> table;

    public IntergroupConnectionReportDialogFx(NetworkGraph<TNode, TEdge> network) {
        this.edges = new ArrayList<>(network.getEdges());
        this.groups = network.getNodes().stream().flatMap(node -> node.getGroups().keySet().stream()).distinct().collect(Collectors.toList());

        this.dlgSave = new FileChooser();

        table = new TableView<>();

        initComponents();
    }

    private static boolean areDifferent(Object o1, Object o2) {
        if(o1 == null) {
            return o2 != null;
        }
        if(o2 == null) {
            //o1 cannot be null
            return true;
        }
        //Neither are null, we can call .equals safely
        return !o1.equals(o2);
    }

    private void initComponents() {
        setTitle("Inter-Group Connection Report");
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

        ComboBox<String> cbGroupBy = new ComboBox<>();
        cbGroupBy.getItems().add("Group by...");
        cbGroupBy.getItems().addAll(this.groups);
        cbGroupBy.getSelectionModel().select(0);
        cbGroupBy.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            Set<Connection> connections = new HashSet<>();
            connections.addAll(
                    edges.stream()
                            .filter(edge -> areDifferent(edge.getSource().getGroups().get(newValue), edge.getDestination().getGroups().get(newValue)))
                            .filter(edge -> (!(edge instanceof LogicalEdge)) || ( ((LogicalEdge)edge).getDetailsToDestination().getFrameCount() > 0))
                            .map(edge -> new Connection(edge.getSource().getGroups().get(newValue), edge.getDestination().getGroups().get(newValue)))
                            .collect(Collectors.toList())
            );
            connections.addAll(
                    edges.stream()
                            .filter(edge -> areDifferent(edge.getSource().getGroups().get(newValue), edge.getDestination().getGroups().get(newValue)))
                            .filter(edge -> (!(edge instanceof LogicalEdge)) || (((LogicalEdge) edge).getDetailsToSource().getFrameCount() > 0))
                            .map(edge -> new Connection(edge.getDestination().getGroups().get(newValue), edge.getSource().getGroups().get(newValue)))
                            .collect(Collectors.toList())
            );

            table.getItems().clear();
            table.getItems().addAll(connections);
        });

        TableColumn<Connection, String> colSource = new TableColumn<>("Source");
        colSource.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getSource() == null ? "(undefined)" : cellData.getValue().getSource()));
        TableColumn<Connection, String> colDestination = new TableColumn<>("Target");
        colDestination.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getTarget() == null ? "(undefined)" : cellData.getValue().getTarget()));

        table.getColumns().addAll(
                colSource,
                colDestination
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
                cbGroupBy,
                paneToolbarSpacer,
                btnExportToCsv
        );

        layoutWindow.setTop(toolbarWindow);
        layoutWindow.setCenter(table);

        this.getDialogPane().setContent(layoutWindow);
        this.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
    }
}
