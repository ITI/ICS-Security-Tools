package ui.custom.fx;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A line chart depicting various data points across multiple series.  Abstract to force consolidation of the logic behind implementations for various template types.
 * @param <T> The type of object which is contained within the graph.
 * @param <TX> The type for the X axis.
 * @param <TY> The type for the Y axis.
 */
public abstract class Chart<T, TX extends Comparable<TX>, TY extends Comparable<TY>> extends Canvas {
    public final static Rectangle2D DEFAULT_VIEWPORT = new Rectangle2D(-0.05, 0.0, 1.1, 1.05);
    private static final Color[] colors = new Color[] {Color.RED, Color.BLUE, Color.GREEN, Color.PURPLE, Color.ORANGE, Color.YELLOW, Color.LIGHTGREEN, Color.DARKCYAN, Color.DARKGREEN, Color.MAGENTA, Color.LIGHTBLUE, Color.DARKBLUE};
    private static int idx = 0;

    public static class Series<T> {
        private String name;
        private final List<T> data;
        private Color color;
        private final SimpleBooleanProperty visible;

        public Series() {
            this.name = null;
            this.data = new ArrayList<>();
            this.color = colors[Chart.idx++ % Chart.colors.length];
            this.visible = new SimpleBooleanProperty(true);
        }

        public List<T> getData() {
            return data;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
        public Color getColor() {
            return color;
        }
        public BooleanProperty visibleProperty() {
            return visible;
        }
    }
    public static class Range<T extends Comparable<T>> {
        private final T min;
        private final T max;

        public Range(T min, T max) {
            this.min = min;
            this.max = max;
        }

        public T getMin() {
            return min;
        }
        public T getMax() {
            return max;
        }

        public boolean contains(T val) {
            if(val == null) {
                return false;
            }

            if(min != null) {
                if(min.compareTo(val) > 0) {
                    return false;
                }
            }
            if(max != null) {
                if(max.compareTo(val) < 0) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return "<" + min + ", " + max + ">";
        }
    }
    @FunctionalInterface
    public interface Normalizer<T extends Comparable<T>> {
        double normalize(Range<T> bounds, T value);
    }
    @FunctionalInterface
    public interface GenerateTicks<T extends Comparable<T>> {
        List<T> generateTicks(Range<T> base, double low, double high);
    }
    protected static class Point<T, TX, TY> {
        private Point2D location;
        private TX x;
        private TY y;
        private T datum;
        private Color color;

        public Point(T datum, TX x, TY y, Point2D location, Color color) {
            this.location = location;
            this.x = x;
            this.y = y;
            this.datum = datum;
            this.color = color;
        }

        public T getDatum() {
            return datum;
        }
    }

    private final List<Series<T>> series;
    private final AtomicBoolean redrawPending = new AtomicBoolean(false);
    private final AtomicBoolean redrawSuspended = new AtomicBoolean(false);
    private final Function<T, TX> fnXValue;
    private final Function<T, TY> fnYValue;
    private final Normalizer<TX> fnNormalizerX;
    private final Normalizer<TY> fnNormalizerY;
    private final GenerateTicks<TX> fnTicksX;
    private final GenerateTicks<TY> fnTicksY;
    private final Function<TX, String> fnFormatX;
    private final Function<TY, String> fnFormatY;
    private Range<TX> rangeX;
    private Range<TY> rangeY;
    private double pxTickLength = 5.0;

    private Rectangle2D viewport;
    private final Text textForMeasuring = new Text();

    protected double pointRadius = 5.0;
    protected double interactRadius = 15.0;
    protected final List<Point<T, TX, TY>> renderedPoints = new ArrayList<>();

    protected Chart(Function<T, TX> fnX, Function<T, TY> fnY,
                 Normalizer<TX> fnNormalizerX, Normalizer<TY> fnNormalizerY,
                 GenerateTicks<TX> fnTicksX, GenerateTicks<TY> fnTicksY,
                 Function<TX, String> fnFormatX, Function<TY, String> fnFormatY) {
        this.series = new LinkedList<>();
        this.fnXValue = fnX;
        this.fnYValue = fnY;
        this.fnNormalizerX = fnNormalizerX;
        this.fnNormalizerY = fnNormalizerY;
        this.fnTicksX = fnTicksX;
        this.fnTicksY = fnTicksY;
        this.fnFormatX = fnFormatX;
        this.fnFormatY = fnFormatY;

        this.viewport = DEFAULT_VIEWPORT;
        this.textForMeasuring.setFont(Font.font("MONOSPACE", 12.0));

        setHeight(200.0);
        setWidth(200.0);
        widthProperty().addListener((observable, oldValue, newValue) -> redraw());
        heightProperty().addListener((observable, oldValue, newValue) -> redraw());
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double minHeight(double width) {
        return 0.0;
    }

    @Override
    public double maxHeight(double width) {
        return 10000.0;
    }

    @Override
    public double minWidth(double height) {
        return 0.0;
    }

    @Override
    public double maxWidth(double height) {
        return 10000.0;
    }

    @Override
    public double prefWidth(double height) {
        return 0.0;
    }

    @Override
    public double prefHeight(double width) {
        return 0.0;
    }

    @Override
    public void resize(double width, double height) {
        setWidth(width);
        setHeight(height);
    }

    public void clearSeries() {
        this.series.clear();
        redraw();
    }
    public void addSeries(Series<T> series) {
        this.series.add(series);
        redraw();
    }
    public void removeSeries(Series<T> series) {
        this.series.remove(series);
        redraw();
    }
    public void setXRange(Range<TX> range) {
        rangeX = range;
        redraw();
    }
    public void setYRange(Range<TY> range) {
        rangeY = range;
        redraw();
    }
    public void suspendLayout(boolean state) {
        if(state) {
            redrawSuspended.set(true);
        } else {
            if(redrawSuspended.getAndSet(false) && redrawPending.get()) {
                redraw();
            }
        }
    }
    public void zoom(Rectangle2D viewport) {
        this.viewport = viewport;
        redraw();
    }


    public double viewportXForControlX(final double pxX) {
        //Prevent tearing if used multithreaded.
        final Rectangle2D rect = rectChart;
        return (pxX - rect.getMinX()) / rect.getWidth() * viewport.getWidth() + viewport.getMinX();
    }
    public boolean pointInsideViewport(final Point2D pt) {
        return rectChart.contains(pt);
    }

    //Used in the paint method, stored for use in viewportXForControlX and pointInsideViewport methods.
    private Rectangle2D rectChart = new Rectangle2D(0, 0, 0, 0);

    protected double chartXFromDataX(Range<TX> axisX, TX x) {
        if(axisX.getMin().equals(axisX.getMax())) {
            return 0.5 * rectChart.getWidth() + rectChart.getMinX();
        } else {
            final double normalizedX = fnNormalizerX.normalize(axisX, x);
            return (normalizedX - viewport.getMinX()) / viewport.getWidth() * rectChart.getWidth() + rectChart.getMinX();
        }
    }
    protected double chartYFromDataY(Range<TY> axisY, TY y) {
        return (1 - (fnNormalizerY.normalize(axisY, y) - viewport.getMinY()) / viewport.getHeight()) * rectChart.getHeight() + rectChart.getMinY();
    }

    /**
     * Trigger a redraw in the Fx Application Thread.  If called from the app thread it will be called directly, otherwise it will be scheduled to run later.
     */
    protected final void redraw() {
        redrawPending.set(true);
        if(!Platform.isFxApplicationThread()) {
            Platform.runLater(this::redraw);
            return;
        }

        //If we haven't requested a redraw since the last draw, we can skip this redraw because nothing will have changed.
        if(!redrawSuspended.get()) {
            if (redrawPending.getAndSet(false)) {
                GraphicsContext gc = this.getGraphicsContext2D();
                gc.clearRect(0.0, 0.0, getWidth(), getHeight());
                paint(gc);
            }
        }
    }

    protected void paint(GraphicsContext gc) {
        // Get a sorted copy of the series data.  Once we have this we can release locks.
        Rectangle2D rectViewport = viewport;
        HashMap<Series, LinkedList<T>> data = new HashMap<>();
        for(Series<T> series : this.series) {
            LinkedList<T> seriesData = new LinkedList<>(series.getData());
            seriesData.sort((o1, o2) -> fnXValue.apply(o1).compareTo(fnXValue.apply(o2)));
            data.put(series, seriesData);
        }

        //Make sure we have data before continuing.
        if(data.size() == 0) {
            return;
        }
        if(data.values().stream().flatMap(LinkedList::stream).count() == 0) {
            return;
        }

        // Calculate the range on each axis.
        Range<TX> axisX;
        Range<TY> axisY;
        //TODO: Since we just sorted by X, we can optimize this a bit.
        if(rangeX != null) {
            axisX = new Range<>(
                    rangeX.min == null ? data.values().stream().flatMap(LinkedList::stream).map(fnXValue).min(TX::compareTo).get() : rangeX.min,
                    rangeX.max == null ? data.values().stream().flatMap(LinkedList::stream).map(fnXValue).max(TX::compareTo).get() : rangeX.max
            );
        } else {
            axisX = new Range<>(
                    data.values().stream().flatMap(LinkedList::stream).map(fnXValue).min(TX::compareTo).get(),
                    data.values().stream().flatMap(LinkedList::stream).map(fnXValue).max(TX::compareTo).get()
            );
        }
        if(rangeY != null) {
            axisY = new Range<>(
                    rangeY.min == null ? data.values().stream().flatMap(LinkedList::stream).map(fnYValue).min(TY::compareTo).get() : rangeY.min,
                    rangeY.max == null ? data.values().stream().flatMap(LinkedList::stream).map(fnYValue).max(TY::compareTo).get() : rangeY.max
            );
        } else {
            axisY = new Range<>(
                    data.values().stream().flatMap(LinkedList::stream).map(fnYValue).min(TY::compareTo).get(),
                    data.values().stream().flatMap(LinkedList::stream).map(fnYValue).max(TY::compareTo).get()
            );
        }
        final List<TX> ticksX = fnTicksX.generateTicks(axisX, viewport.getMinX(), viewport.getMaxX());
        //axisX = new Range<>(ticksX.get(0), ticksX.get(ticksX.size() - 1));
        //rangeXRendered = axisX;
        final List<TY> ticksY = fnTicksY.generateTicks(axisY, viewport.getMinY(), viewport.getMaxY());
        //axisY = new Range<>(ticksY.get(0), ticksY.get(ticksY.size() - 1));

        // Calculate the width of the widest Y-axis label and the height of the tallest X-axis label.
        double maxWidth = 0.0;
        double pxMinSpacingBetweenTicks = 0.0;
        for(TY tickY : ticksY) {
            textForMeasuring.setText(fnFormatY.apply(tickY));
            maxWidth = Math.max(maxWidth, textForMeasuring.getLayoutBounds().getWidth());
            pxMinSpacingBetweenTicks = Math.max(pxMinSpacingBetweenTicks, 2.0 * textForMeasuring.getLayoutBounds().getHeight());
        }
        double maxHeight = 0.0;
        for(TX tickX : ticksX) {
            //X-labels are displayed at a 30-degree incline.
            //The approximate width of the rotated text is 0.87*{width}
            //The distance from the top of the bounding to the origin from which text should be drawn is 0.5*{length} + 0.87*{height}
            textForMeasuring.setText(fnFormatX.apply(tickX));
            final Bounds boundsText = textForMeasuring.getLayoutBounds();
            maxHeight = Math.max(maxHeight, 0.5 * boundsText.getWidth() + 0.87 * boundsText.getHeight());
            //TODO: Also check maxWidth against the amount by which this would underflow the X=0 line
        }
        final Rectangle2D sizeAxisLabel = new Rectangle2D(0.0, 0.0, maxWidth, maxHeight);

        if(getWidth() <= sizeAxisLabel.getWidth() || getHeight() <= sizeAxisLabel.getHeight()) {
            return;
        }

        rectChart = new Rectangle2D(sizeAxisLabel.getWidth() + pxTickLength, 0.0, getWidth() - sizeAxisLabel.getWidth() - pxTickLength, getHeight() - sizeAxisLabel.getHeight() - pxTickLength);

        // Render series data, build tooltip cache
        renderedPoints.clear();
        for(Map.Entry<Series, LinkedList<T>> entry : data.entrySet()) {
            Point2D ptPrev = null;

            gc.setStroke(entry.getKey().getColor());
            //TODO: Make this customizable
            gc.setLineWidth(2.0);

            for(T value : entry.getValue()) {
                TX x = fnXValue.apply(value);
                TY y = fnYValue.apply(value);

                // Add rectViewport.getMinY() instead of subtracting because we're mirroring the Y coordinate around the X-axis.
                Point2D ptNew = new Point2D(
                        chartXFromDataX(axisX, x),
                        chartYFromDataY(axisY, y));
                Point<T, TX, TY> pt = new Point<>(
                        value,
                        x,
                        y,
                        ptNew,
                        entry.getKey().getColor()
                );
                renderedPoints.add(pt);

                if(ptPrev != null) {
                    gc.strokeLine(ptPrev.getX(), ptPrev.getY(), ptNew.getX(), ptNew.getY());
                }
                gc.strokeOval(ptNew.getX() - pointRadius, ptNew.getY() - pointRadius, pointRadius * 2, pointRadius * 2);
                ptPrev = ptNew;
            }
        }

        // Render axes (last so it overwrites any values near an axis)
        //Clear the axis area
        gc.clearRect(0.0, 0.0, sizeAxisLabel.getWidth() + pxTickLength, getHeight());
        gc.clearRect(0.0, getHeight() - sizeAxisLabel.getHeight() - pxTickLength, getWidth(), sizeAxisLabel.getHeight());
        //Draw the axes
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5);
        gc.strokeLine(rectChart.getMinX(), 0.0, rectChart.getMinX(), rectChart.getMaxY());
        gc.strokeLine(rectChart.getMinX(), rectChart.getMaxY(), rectChart.getMaxX(), rectChart.getMaxY());

        Font font = Font.font("MONOSPACE", 12.0);
        gc.setFont(font);
        //ticksX and ticksY are lists of the corresponding values; they need to be handed of to the corresponding fnNormalize and then scaled for display.

        double pxLast = -pxMinSpacingBetweenTicks;
        for(TX tickX : ticksX) {
            final double pxX = chartXFromDataX(axisX, tickX);
            if(pxLast + pxMinSpacingBetweenTicks > pxX) {
                continue;
            }
            pxLast = pxX;
            gc.strokeLine(pxX, rectChart.getMaxY(), pxX, rectChart.getMaxY() + pxTickLength);
            final String textLabel = fnFormatX.apply(tickX);
            textForMeasuring.setText(textLabel);
            final Bounds boundsText = textForMeasuring.getLayoutBounds();
            double offsetY = 0.5 * boundsText.getWidth() + 0.87 * boundsText.getHeight();
            double offsetX = -0.87 * boundsText.getWidth();

            gc.save();
            // Translate then rotate to rotate text around local origin rather than rotating around the canvas origin.
            // Rotating and drawing at an offset results in a rotation around the origin.
            gc.translate(pxX + offsetX, rectChart.getMaxY() + offsetY);
            gc.rotate(-30.0);
            gc.strokeText(textLabel, 0.0, 0.0);
            gc.restore();
        }
        for(TY tickY : ticksY) {
            final double pxY = chartYFromDataY(axisY, tickY);
            gc.strokeLine(rectChart.getMinX() - pxTickLength, pxY, rectChart.getMinX(), pxY);
            final String textLabel = fnFormatY.apply(tickY);
            textForMeasuring.setText(textLabel);
            gc.strokeText(fnFormatY.apply(tickY), 0.0, pxY + textForMeasuring.getLayoutBounds().getHeight());
        }
    }

    protected List<T> pointsNearLocation(final Point2D screenNear) {
        final Point2D ptNear = this.screenToLocal(screenNear);
        return renderedPoints.stream()
                .filter(point -> point.location.distance(ptNear) < interactRadius)
                .sorted((o1, o2) -> Double.compare(o1.location.distance(ptNear), o2.location.distance(ptNear)))
                .limit(50)  //Completely arbitrary number; drawing thousands of menu items causes performance issues, the 50 closest should be sufficient.
                .map(point -> point.getDatum())
                .collect(Collectors.toList());
    }

    protected T pointNearestLocation(final Point2D screenNear) {
        final Point2D ptNear = this.screenToLocal(screenNear);
        final Optional<T> result = renderedPoints.stream()
                .filter(point -> point.location.distance(ptNear) < interactRadius)
                .sorted((o1, o2) -> Double.compare(o1.location.distance(ptNear), o2.location.distance(ptNear)))
                .map(point -> point.getDatum())
                .findFirst();
        return result.isPresent() ? result.get() : null;
    }
}
