package util;

import javafx.application.Platform;

import java.util.Timer;
import java.util.TimerTask;

public class RateLimitedTask {
    private static final Timer timer = new Timer("Scheduler-RateLimitedTask", true);

    private long lastExecution;
    private final long interval;
    private final Runnable fnTask;
    private boolean scheduled;

    public RateLimitedTask(long interval, Runnable fnTask) {
        this.interval = interval;
        this.fnTask = fnTask;
    }

    private class ChildTask extends TimerTask {
        @Override
        public void run() {
            try {
                RateLimitedTask.this.fnTask.run();
                scheduled = false;
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Cause the task to execute, or be scheduled for execution, depending on the last execution time.
     * Multiple pending executions cannot be queued; multiple calls to trigger during the debounce period will result in a single queued execution.
     * @return true if the task has already executed, false if it is queued for execution.
     */
    public synchronized boolean trigger() {
        long now = System.currentTimeMillis();

        //If we are calling this from the FX thread, then run now; it is assumed that, when that use case happens, the task must run now in the current thread.
        if(now >= lastExecution + interval || Platform.isFxApplicationThread()) {
            lastExecution = now;
            fnTask.run();
            return true;
        } else if(lastExecution < now && !scheduled) {
            // Keep track of if the scheduled task has run, if it hasn't don't schedule again
            timer.schedule(new ChildTask(), lastExecution + interval - now);
            lastExecution += interval;
            return false;
        }
        return false;
    }
}
