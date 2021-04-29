package ui.dialog;

import com.sun.javafx.collections.ObservableListWrapper;
import core.document.graph.LogicalEdge;
import core.document.graph.LogicalNode;
import core.document.graph.NetworkGraph;
import core.importmodule.ImportItem;
import javafx.beans.Observable;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import ui.EmbeddedIcons;
import ui.custom.fx.ActiveMenuItem;
import ui.custom.fx.Chart;
import ui.custom.fx.ChartPacketBytesOverTime;
import ui.custom.fx.ScalableChartWrapper;
import util.Cidr;
import util.Csv;
import util.Wireshark;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConnectionDetailsDialogFx extends Dialog {
    protected class DetailLine {
        private final boolean isSameDirection;
        private final ImportItem source;
        private final LogicalEdge.ConnectionDetails.FrameRecord details;
        private final LogicalEdge edge;

        public DetailLine(boolean isSameDirection, ImportItem item, LogicalEdge edge, LogicalEdge.ConnectionDetails.FrameRecord details) {
            this.isSameDirection = isSameDirection;
            this.source = item;
            this.edge = edge;
            this.details = details;
        }

        // These are called via reflection for the TableView (tbl) defined below.
        public String getPcapPath() {
            return source.getPath().toString();
        }
        public String getName() {
            return source.getPath().getFileName().toString();
        }
        public long getFrame() {
            return details.getFrame();
        }
        public int getBytes() {
            return details.getBytes();
        }
        public ZonedDateTime getTime() {
            return Instant.ofEpochMilli(details.getTime()).atZone(ZoneId.of("Z"));//.format(DateTimeFormatter.ISO_INSTANT);
        }
        public int getSourcePort() {
            if(isSameDirection) {
                return details.getSourcePort();
            } else {
                return details.getDestinationPort();
            }
        }
        public int getDestinationPort() {
            if(isSameDirection) {
                return details.getDestinationPort();
            } else {
                return details.getSourcePort();
            }
        }
        public String getProtocol() {
            return Wireshark.getProtocolName(details.getProtocol());
        }
        public Cidr getSourceIp() {
            if(isSameDirection) {
                return edge.getSource().getIp();
            } else {
                return edge.getDestination().getIp();
            }
        }
        public Cidr getDestinationIp() {
            if(isSameDirection) {
                return edge.getDestination().getIp();
            } else {
                return edge.getSource().getIp();
            }
        }
    }

    private final SimpleObjectProperty<LogicalNode> root;
    private ObservableListWrapper<DetailLine> content;
    private final NetworkGraph<LogicalNode, LogicalEdge> graph;

    private final ScalableChartWrapper chartControl;

    public ConnectionDetailsDialogFx(NetworkGraph<LogicalNode, LogicalEdge> graph) {
        root = new SimpleObjectProperty<>(null);
        root.addListener(this::Handle_RootNodeChanged);

        this.graph = graph;

        content = new ObservableListWrapper<>(new ArrayList<>(1024));

        chartControl = new ScalableChartWrapper();

        initComponents();
    }

    private void initComponents() {
        Window stage = super.getDialogPane().getScene().getWindow();
        if(stage instanceof Stage) {
            ((Stage)stage).getIcons().add(EmbeddedIcons.Logo.getRawImage());
        }
        super.setResizable(true);
        SplitPane pane = new SplitPane();
        pane.setOrientation(Orientation.VERTICAL);

        final VBox containerTable = new VBox();
        TableView<DetailLine> tbl = new TableView<>();

        TableColumn<DetailLine, ZonedDateTime> colTimestamp = new TableColumn<>("Timestamp");
        colTimestamp.setCellValueFactory(new PropertyValueFactory<>("time"));

        TableColumn<DetailLine, Cidr> colSourceIp = new TableColumn<>("Source IP");
        colSourceIp.setCellValueFactory(new PropertyValueFactory<>("sourceIp"));
        TableColumn<DetailLine, Integer> colSourcePort = new TableColumn<>("Source Port");
        colSourcePort.setCellValueFactory(new PropertyValueFactory<>("sourcePort"));
        TableColumn<DetailLine, Cidr> colDestIp = new TableColumn<>("Destination IP");
        colDestIp.setCellValueFactory(new PropertyValueFactory<>("destinationIp"));
        TableColumn<DetailLine, Integer> colDestPort = new TableColumn<>("Destination Port");
        colDestPort.setCellValueFactory(new PropertyValueFactory<>("destinationPort"));

        TableColumn<DetailLine, String> colSourceFile = new TableColumn<>("PCAP File");
        colSourceFile.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<DetailLine, Long> colFrame = new TableColumn<>("Frame");
        colFrame.setCellValueFactory(new PropertyValueFactory<>("frame"));
        TableColumn<DetailLine, String> colProtocol = new TableColumn<>("Protocol");
        colProtocol.setCellValueFactory(new PropertyValueFactory<>("protocol"));
        TableColumn<DetailLine, Integer> colSize = new TableColumn<>("Size");
        colSize.setCellValueFactory(new PropertyValueFactory<>("bytes"));

        //noinspection unchecked
        tbl.getColumns().addAll(
                colTimestamp,
                colSourceIp,
                colSourcePort,
                colDestIp,
                colDestPort,
                colSourceFile,
                colFrame,
                colProtocol,
                colSize
        );
        tbl.setItems(content);
        ContextMenu menuTbl = new ContextMenu();
        menuTbl.getItems().addAll(
                new ActiveMenuItem("Open in Wireshark", event -> {
                    DetailLine line = tbl.getSelectionModel().getSelectedItem();
                    if (line == null) {
                        return;
                    }
                    Wireshark.OpenPcapFile(line.getPcapPath(), line.getFrame());
                })
        );
        tbl.setContextMenu(menuTbl);

        final HBox toolbarTable = new HBox();
        final Pane spacerToolbar = new Pane();
        HBox.setHgrow(spacerToolbar, Priority.ALWAYS);

        final Button btnExportToCsv = new Button("Export CSV...", EmbeddedIcons.Vista_Save.getImage(16.0));
        btnExportToCsv.setOnAction(event -> {
            final FileChooser dlgSave = new FileChooser();
            dlgSave.setTitle("Export To...");
            dlgSave.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"),
                    new FileChooser.ExtensionFilter("Data Files (*.csv, *.xls, *.xlsx, *.xml, *.prn, *ods)", "*.csv", "*.xls", "*.xlsx", "*.xml", "*.prn", "*.ods"),
                    new FileChooser.ExtensionFilter("All Files", "*")
            );

            File result = dlgSave.showSaveDialog(this.getOwner());
            if(result != null) {
                Csv.ExportTableToFile(tbl, result);
            }
        });

        toolbarTable.getChildren().addAll(spacerToolbar, btnExportToCsv);
        containerTable.getChildren().addAll(toolbarTable, tbl);
        pane.getItems().addAll(containerTable, chartControl);
        this.getDialogPane().setContent(pane);
        this.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
    }

    public void setRootNode(LogicalNode root) {
        this.root.set(root);
        super.setTitle("Connections involving " + root.toString());
    }
    protected List<DetailLine> buildDetailList(LogicalEdge edge, LogicalEdge.ConnectionDetails detailsTo, LogicalEdge.ConnectionDetails detailsFrom) {
        //Pre-allocate for the full size requirement.
        final ArrayList<DetailLine> result = new ArrayList<>(detailsTo.getFrameCount() + detailsFrom.getFrameCount());

        for(ImportItem item : detailsTo.getFrameGroups()) {
            for(LogicalEdge.ConnectionDetails.FrameRecord record : detailsTo.getFrames(item)) {
                result.add(new DetailLine(detailsTo == edge.getDetailsToDestination(), item, edge, record));
            }
        }
        for(ImportItem item : detailsFrom.getFrameGroups()) {
            for(LogicalEdge.ConnectionDetails.FrameRecord record : detailsFrom.getFrames(item)) {
                result.add(new DetailLine(detailsFrom == edge.getDetailsToDestination(), item, edge, record));
            }
        }

        return result;
    }
    protected void Handle_RootNodeChanged(Observable o, LogicalNode rootOld, LogicalNode rootNew) {
        content.clear();
        chartControl.suspendLayout(true);
        try {
            chartControl.clearSeries();
            chartControl.zoomReset();

            if (rootNew == null) {
                return;
            }

            //Find all the edges connected to the root.
            //Each edge contains 2 sets of details, one set to the node, one set from.
            // Which set is which depends on which end of the edge is the node we care about.
            for (LogicalEdge edge : this.graph.getEdgesInvolving(rootNew)) {
                final LogicalNode nodeOther;
                //The edges are bidirectional and store directional data, so we need to check what endpoint the root is before assembling the data.
                if (edge.getSource().equals(rootNew)) {
                    nodeOther = edge.getDestination();
                    content.addAll(buildDetailList(edge, edge.getDetailsToSource(), edge.getDetailsToDestination()));
                } else {
                    nodeOther = edge.getSource();
                    content.addAll(buildDetailList(edge, edge.getDetailsToDestination(), edge.getDetailsToSource()));
                }

                Chart.Series<ChartPacketBytesOverTime.FrameWrapper> series = new Chart.Series<>();
                series.setName(nodeOther.getIp().toString());

                for (ImportItem item : edge.getDetailsToSource().getFrameGroups()) {
                    series.getData().addAll(edge.getDetailsToSource().getFrames(item).stream()
                            .map(frame -> new ChartPacketBytesOverTime.FrameWrapper(item, edge.getDestination().getIp(), edge.getSource().getIp(), frame))
                            .collect(Collectors.toList()));
                }
                for (ImportItem item : edge.getDetailsToDestination().getFrameGroups()) {
                    series.getData().addAll(edge.getDetailsToDestination().getFrames(item).stream()
                    .map(frame -> new ChartPacketBytesOverTime.FrameWrapper(item, edge.getSource().getIp(), edge.getDestination().getIp(), frame))
                    .collect(Collectors.toList()));
                }
                chartControl.addSeries(series);
            }
        } finally {
            chartControl.suspendLayout(false);
        }
    }
}
