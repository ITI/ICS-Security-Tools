package ui.custom.fx;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import org.apache.commons.io.FileUtils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A JavaFX UI component that displays the currently-used and total-available memory, as a number and a percentage bar.
 * The values displayed are averages based on the past 20 data samples.  Samples are generally taken once per second.
 */
public class MemoryUsageTracker extends StackPane {
    private final ReadOnlyIntegerWrapper filterDepth;
    //The state isn't relevant to other application components, so this isn't publicly exposed.
    protected final DoubleProperty pctMemoryUsage;
    protected final LongProperty mbMemoryUsed;
    protected final LongProperty mbMemoryTotal;

    protected static final String UNITS = " MB";
    protected static final long UNIT_VALUE = FileUtils.ONE_MB;

    private double[] pctReadings;
    private int nextReading = 0;
    private boolean wrappedReadings = false;

    private AtomicBoolean reading;

    /**
     * @param cntHistory The number of historical memory-usage readings to track.
     */
    public MemoryUsageTracker(int cntHistory) {
        filterDepth = new ReadOnlyIntegerWrapper(cntHistory);
        pctMemoryUsage = new SimpleDoubleProperty(0.0);
        mbMemoryUsed = new SimpleLongProperty(0);
        mbMemoryTotal = new SimpleLongProperty(0);
        pctReadings = new double[cntHistory];
        reading = new AtomicBoolean(false);

        initComponents();

        //Take the first reading to set the memory reading values
        TakeReading();

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!reading.get()) {
                    reading.set(true);
                    Platform.runLater(MemoryUsageTracker.this::TakeReading);
                    reading.set(false);
                }
            }
        }, 1000, 1000);
    }

    private void initComponents() {
        ProgressBar barMemory = new ProgressBar();
        barMemory.setPrefWidth(200.0);
        barMemory.progressProperty().bind(pctMemoryUsage);

        Label lblMemory = new Label();
        lblMemory.textProperty().bind(mbMemoryUsed.asString().concat(UNITS).concat(" / ").concat(mbMemoryTotal).concat(UNITS));

        StackPane layout = new StackPane();
        layout.getChildren().addAll(
                barMemory,
                lblMemory
        );
        this.getChildren().add(layout);
    }

    public synchronized void TakeReading() {
        long memTotal = Runtime.getRuntime().totalMemory();
        long memUsed = memTotal - Runtime.getRuntime().freeMemory();

        // There is a potential divide-by-zero error here.  If that happens, divide by zero is the lesser problem.
        double pctReading = (double)memUsed / (double)memTotal;
        pctReadings[nextReading] = pctReading;

        // Move to the next reading slot.  If the next slot is 0, then we have wrapped.
        nextReading = (nextReading + 1) % pctReadings.length;
        wrappedReadings |= (nextReading == 0);

        // If we have wrapped, then use every element in the array.  If not, then use the index
        int limit = wrappedReadings ? pctReadings.length : nextReading;
        double avgUsage = 0.0;
        for(int idx = 0; idx < limit; idx++) {
            avgUsage += pctReadings[idx];
        }
        avgUsage /= limit;

        pctMemoryUsage.set(avgUsage);
        mbMemoryTotal.set(memTotal / UNIT_VALUE);
        mbMemoryUsed.set(memUsed / UNIT_VALUE);

    }

    // == Accessors
    public IntegerProperty depthProperty() {
        return filterDepth;
    }
    public int getDepth() {
        return filterDepth.get();
    }
}
