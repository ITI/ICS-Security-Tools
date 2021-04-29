package ui.custom.fx;

import com.sun.javafx.collections.ObservableListWrapper;
import core.logging.Severity;
import core.svg.Svg;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import util.Wireshark;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

public class ScalableChartWrapper extends GridPane {
    protected class LegendEntry extends HBox {
        private final Chart.Series<ChartPacketBytesOverTime.FrameWrapper> series;

        public LegendEntry(Chart.Series<ChartPacketBytesOverTime.FrameWrapper> series) {
            this.series = series;

            initComponents();
        }

        private void initComponents() {
            Circle glyph = new Circle();
            glyph.setStroke(series.getColor());
            glyph.setFill(Color.TRANSPARENT);
            glyph.setStrokeWidth(2.0);
            glyph.setRadius(5.0);

            Label lblTitle = new Label(series.getName());

            this.getChildren().addAll(glyph, lblTitle);
        }

        public Chart.Series<ChartPacketBytesOverTime.FrameWrapper> getSeries() {
            return series;
        }
    }

    protected final ChartPacketBytesOverTime chart;
    protected final ObservableList<Rectangle2D> zoomHistory;
    protected final ScrollPane legendScroll;
    protected final VBox legend;
    protected final Label tooltip;

    protected final Rectangle polyHighlight;
    protected double xInitialHighlight;

    protected final ContextMenu menu;
    protected final Menu menuOpenInWireshark;
    protected final Menu menuSeries;

    public ScalableChartWrapper() {
        this.chart = new ChartPacketBytesOverTime();
        this.zoomHistory = new ObservableListWrapper<>(new LinkedList<>());
        this.legend = new VBox();
        this.legendScroll = new ScrollPane(legend);
        this.polyHighlight = new Rectangle();
        this.tooltip = new Label();
        //There is a single context menu.  When there is an event to show the context menu, populate it with the relevant content.
        //The legend options will always be available, other options are context-sensitive to the content of the chart and location of the mouse.
        //TODO: [508] Context menus should respond to the context menu button on a keyboard in addition to the relevant mouse events.
        // > Per-point (proximity-based):
        //   > Open in Wireshark

        this.menu = new ContextMenu();
        this.menuOpenInWireshark = new Menu("Open in _Wireshark");
        this.menuSeries = new Menu("_Series");

        this.setOnContextMenuRequested(event -> {
            event.consume();
            OnContextMenuShowing(new Point2D(event.getScreenX(), event.getScreenY()));
            menu.show(this, event.getScreenX(), event.getScreenY());
        });
        this.setOnMouseClicked(this::HandleClick_HideContextMenu);

        initComponents();
    }

    private void initComponents() {
        StackPane paneChart = new StackPane();

        //chart.widthProperty().bind(paneChart.widthProperty());
        //chart.heightProperty().bind(paneChart.heightProperty());

        polyHighlight.setVisible(false);
        polyHighlight.heightProperty().bind(chart.heightProperty());
        polyHighlight.setFill(Color.color(0.0, 0.0, 1.0, 0.5));

        tooltip.setVisible(false);
        tooltip.setBackground(new Background(new BackgroundFill(Color.BEIGE, null, null)));
        tooltip.setPadding(new Insets(2, 2, 2, 2));
        tooltip.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(1.0))));

        xInitialHighlight = -1.0;

        //We have to add a pane since the StackPane handles positioning, size, etc.
        Pane overlaysChart = new Pane();
        overlaysChart.getChildren().addAll(polyHighlight, tooltip);
        overlaysChart.prefWidthProperty().bind(paneChart.widthProperty());
        overlaysChart.prefHeightProperty().bind(paneChart.heightProperty());

        paneChart.getChildren().addAll(chart, overlaysChart);

        // Tooltip Handler
        overlaysChart.setOnMouseMoved(event -> {
            ChartPacketBytesOverTime.FrameWrapper point = chart.pointNearestLocation(new Point2D(event.getScreenX(), event.getScreenY()));
            if (point != null) {

                tooltip.setText(String.format("%s:%d -> %s:%d\n%d bytes\n%s",
                        point.getIpSrc(), point.getRecord().getSourcePort(),
                        point.getIpDest(), point.getRecord().getDestinationPort(),
                        point.getRecord().getBytes(),
                        Instant.ofEpochMilli(point.getTime()).atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_INSTANT)));
                tooltip.setVisible(true);
            } else {
                tooltip.setVisible(false);
            }
        });
        overlaysChart.setOnMouseExited(event -> {
            tooltip.setVisible(false);
        });
        // Handlers for zooming paneChart
        setOnMousePressed(event -> {
            menu.hide();
            if (event.getButton() == MouseButton.PRIMARY) {
                final double x = chart.screenToLocal(event.getScreenX(), event.getScreenY()).getX();
                xInitialHighlight = x;
                polyHighlight.setLayoutX(x);
                polyHighlight.setWidth(0);
                polyHighlight.setVisible(true);

                event.consume();
            } else if (event.isPopupTrigger()) {
                menu.show(this, event.getScreenX(), event.getScreenY());
                event.consume();
            }
        });
        setOnMouseDragged(event -> {
            //We want to constrain the X value to the chart area
            final double x = chart.screenToLocal(event.getScreenX(), event.getScreenY()).getX();

            //TODO: Clamp to the valid range rather than aborting.
            if(!chart.pointInsideViewport(chart.screenToLocal(event.getScreenX(), event.getScreenY()))) {
                return;
            }

            double min = Math.min(xInitialHighlight, x);
            double max = Math.max(xInitialHighlight, x);

            polyHighlight.setLayoutX(min);
            polyHighlight.setWidth(max - min);
        });
        setOnMouseReleased(event -> {
            if (polyHighlight.isVisible() && polyHighlight.getWidth() > 1.0) {
                event.consume();

                final double xMin = chart.viewportXForControlX(polyHighlight.getLayoutX());
                final double xMax = chart.viewportXForControlX(polyHighlight.getLayoutX() + polyHighlight.getWidth());

                Rectangle2D rectZoom = new Rectangle2D(xMin, 0.0, xMax - xMin, 1.05);

                zoomHistory.add(rectZoom);
                chart.zoom(rectZoom);
            }
            polyHighlight.setVisible(false);
        });


        this.add(legendScroll, 0, 0);
        this.add(paneChart, 1, 0);

        GridPane.setVgrow(paneChart, Priority.ALWAYS);
        GridPane.setHgrow(paneChart, Priority.ALWAYS);


        menu.getItems().addAll(
                new ActiveMenuItem("_Export to SVG...", (event) -> {
                    final FileChooser dlgExportTo = new FileChooser();
                    dlgExportTo.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("SVG Image Files (*.svg)", "*.svg"),
                            new FileChooser.ExtensionFilter("All Files", "*")
                    );
                    final File exportTo = dlgExportTo.showSaveDialog(ScalableChartWrapper.this.getScene().getWindow());
                    if(exportTo != null) {
                        try(BufferedWriter writer = new BufferedWriter(new FileWriter(exportTo))) {
                            writer.write(
                                    Svg.serialize(ScalableChartWrapper.this).replaceAll("(\\s+\\n)+", "\n")
                            );
                        } catch(IOException ex) {
                            core.logging.Logger.log(this, Severity.Error, "There was an error exporting the graph: " + ex.getMessage());
                        }
                    }
                }),
                new ActiveMenuItem("_Reset Zoom", (event) -> this.zoomReset()),
                new ActiveMenuItem("_Undo Zoom", (event) -> this.zoomPrevious())
                        .bindEnabled(new ListSizeBinding(zoomHistory).greaterThan(0)),
                new SeparatorMenuItem(),
                menuSeries,
                menuOpenInWireshark
        );
    }

    protected void OnContextMenuShowing(Point2D screen) {
        menuOpenInWireshark.getItems().clear();
        List<ChartPacketBytesOverTime.FrameWrapper> records = chart.pointsNearLocation(screen);

        if(records.isEmpty()) {
            menuOpenInWireshark.setDisable(true);
        } else {
            menuOpenInWireshark.setDisable(false);
            for(ChartPacketBytesOverTime.FrameWrapper wrapper : records) {
                menuOpenInWireshark.getItems().add(new ActiveMenuItem(
                        String.format("%s:%d (%s:%d -> %s:%d %d bytes@%s)",
                                wrapper.getSource().getPath().getFileName().toString(), wrapper.getRecord().getFrame(),
                                wrapper.getIpSrc(), wrapper.getRecord().getSourcePort(),
                                wrapper.getIpDest(), wrapper.getRecord().getDestinationPort(),
                                wrapper.getRecord().getBytes(),
                                Instant.ofEpochMilli(wrapper.getRecord().getTime()).atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_INSTANT)),
                        event -> {
                            Wireshark.OpenPcapFile(
                                    wrapper.getSource().getPath().toString(),
                                    wrapper.getRecord().getFrame());
                        }
                ));
            }
        }
    }

    private void HandleClick_HideContextMenu(MouseEvent event) {
        menu.hide();
    }

    public ChartPacketBytesOverTime getChart() {
        return chart;
    }

    public void clearSeries() {
        this.legend.getChildren().clear();
        this.chart.clearSeries();
        this.menuSeries.getItems().clear();
        this.menuSeries.setDisable(true);
    }
    public void addSeries(Chart.Series<ChartPacketBytesOverTime.FrameWrapper> series) {
        this.legend.getChildren().add(new LegendEntry(series));
        this.chart.addSeries(series);
        this.menuSeries.setDisable(false);

        Circle glyph = new Circle();
        glyph.setStroke(series.getColor());
        glyph.setFill(Color.TRANSPARENT);
        glyph.setStrokeWidth(2.0);
        glyph.setRadius(5.0);
        CheckMenuItem itemSeries = new CheckMenuItem(series.getName(), glyph);
        itemSeries.setSelected(true);
        itemSeries.setOnAction(event -> {
            if(itemSeries.isSelected()) {
                chart.addSeries(series);
            } else {
                chart.removeSeries(series);
            }
        });

        this.menuSeries.getItems().add(itemSeries);
    }
    public void suspendLayout(boolean suspend) {
        chart.suspendLayout(suspend);
    }

    public void zoomReset() {
        chart.zoom(Chart.DEFAULT_VIEWPORT);
        zoomHistory.clear();
    }
    public void zoomPrevious() {
        if(zoomHistory.size() > 0) {
            zoomHistory.remove(zoomHistory.size() - 1);
            if(zoomHistory.size() > 0) {
                chart.zoom(zoomHistory.get(zoomHistory.size() - 1));
            } else {
                zoomReset();
            }
        }
    }
}
