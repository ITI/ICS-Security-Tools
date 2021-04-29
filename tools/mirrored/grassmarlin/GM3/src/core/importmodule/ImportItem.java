/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.importmodule;

import core.document.Event;
import core.document.serialization.xml.XmlElement;
import core.fingerprint3.Fingerprint;
import core.logging.Logger;
import core.logging.Severity;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import ui.custom.fx.FxThresholdDoubleProperty;
import util.Launcher;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ImportItem {
    public AtomicLong edgeTime = new AtomicLong();
    public AtomicLong fpTime = new AtomicLong();
    public AtomicLong otherTime = new AtomicLong();

    public enum Status {
        Idle,
        Started,
        WaitingOnChildTasks,
        Complete,
        Failed
    }

    protected Map<Pipeline, Iterator<?>> iteratorMap;

    protected final List<Fingerprint> fingerprints;
    protected final Path path;

    private final SimpleObjectProperty<Status> status = new SimpleObjectProperty<>(Status.Idle);
    protected final FxThresholdDoubleProperty progress = new FxThresholdDoubleProperty(0.01, 0.0);

    public DoubleProperty progressProperty() {
        return progress;
    }

    /**
     * Every Import action involves performing a sequence of weighted actions.  This tracks how many units have been
     * processed.  Normally, 1 unit corresponds to 1 byte from the source file, but it might also represent records or
     * some other unit of similar granularity.
     */
    private final AtomicLong unitsProcessed = new AtomicLong();

    private final AtomicLong bytesSinceUpdate = new AtomicLong(0);
    private long bytesPerUpdate = 0;
    /**
     * Get the total number of units in this import.
     * @return By default, the number of units equals the number of bytes in the file.
     */
    protected abstract long getTotalUnits();

    public static class TImportItemUpdatedEventArgs {
        //TODO: Flesh out ImportItem event model.
    }

    public Event<TImportItemUpdatedEventArgs> OnImportItemModified = new Event<>();

    protected ImportItem(Path path, List<Fingerprint> fingerprints) {
        this.path = path;
        this.fingerprints = fingerprints;

        this.iteratorMap = new HashMap<>();

        unitsProcessed.set(0);

    }

    /**
     *
     * @return the iterator for the give pipeline
     */
    public Iterator<?> getIterator(Pipeline pipeline) {
        Iterator<?> iterator = this.iteratorMap.get(pipeline);
        if (iterator == null) {
            switch (pipeline) {
                case LOGICAL:
                    iterator = getLogicalIterator();
                    break;

                case PHYSICAL:
                    iterator = getPhysicalIterator();
                    break;
            }
            this.iteratorMap.put(pipeline, iterator);
        }
        return this.iteratorMap.get(pipeline);
    }

    protected abstract Iterator<?> getLogicalIterator();
    protected abstract Iterator<?> getPhysicalIterator();

    public String getDisplayName() {
        return getClass().getCanonicalName();
    }

    public Path getPath() {
        return this.path;
    }

    public Class<? extends ImportItem> getType() {
        return getClass();
    }

    public SimpleObjectProperty<Status> statusProperty() {
        return status;
    }
    public Status getStatus() {
        return status.get();
    }

    public List<Fingerprint> getFingerprints() {
        if(this.fingerprints == null) {
            return new LinkedList<>();
        } else {
            return Collections.unmodifiableList(this.fingerprints);
        }
    }

    /**
     * Update progress on a task without completing the task.
     * @param unitsProcessed
     */
    public void recordTaskProgress(int unitsProcessed) {
        long unitsTotal = this.unitsProcessed.addAndGet(unitsProcessed);
        long unitsSinceUpdate = bytesSinceUpdate.addAndGet(unitsProcessed);

        if(status.get() != Status.Complete) {
            //If the basic condition is met, then lock and check again, ensuring only a single thread performs the check and eliminating the lock contention unless a change has to be made.
            if(unitsSinceUpdate > bytesPerUpdate) {
                synchronized(bytesSinceUpdate) {
                    if(bytesSinceUpdate.get() > bytesPerUpdate) {
                        bytesSinceUpdate.addAndGet(-bytesPerUpdate);
                    }
                }
                //This is prone to race condition issues, but it is acceptable to call this multiple times, and preferable, to minimize the lock time.
                if(!progress.isBound()) {
                    progress.set((double) unitsTotal / (double) getTotalUnits());
                }
            }
        } else {
            //We're already marked as done; happens occasionally with multithreaded processing of task queue.
            return;
        }
    }

    public void recordTaskCompletion() {
        status.set(Status.Complete);
        AnnounceStatus();
        progress.setForceUpdate(1.0);

        /*
        System.out.println("Edge Time = " + edgeTime.get());
        System.out.println("FP Time = " + fpTime.get());
        System.out.println("Other Time = " + otherTime.get());
        */
    }

    /**
     * Return the current size formatted in human-readable units.
     * Despite the lack of apparent references, this is used via reflection in the Import Dialog.
     */
    public abstract String getDisplaySize();

    @Override
    public String toString() {
        return String.format("[%s: %s]", getDisplayName(), path.getFileName());
    }

    public void AnnounceStatus() {
        switch(getStatus()) {
            case Idle:
            case Started:
                Logger.log(this, Severity.Success, this.toString() + ": Import is running.");
                break;
            case Complete:
                Logger.log(this, Severity.Information, this.toString() + ": Import has completed.");
                break;
            case WaitingOnChildTasks:
                Logger.log(this, Severity.Warning, this.toString() + ": Import is waiting on child tasks to complete.");
                break;
            case Failed:
                Logger.log(this, Severity.Error, this.toString() + ": Import has failed.");
                break;
        }
    }

    public XmlElement toXml() {
        XmlElement xmlImport = new XmlElement("import");
        xmlImport.addAttribute("src").setValue(path.toAbsolutePath().toString());
        xmlImport.addAttribute("type").setValue(this.getClass().getName());

        final String namePlugin = Launcher.pluginFor(this.getClass());
        if(namePlugin != null) {
            xmlImport.addAttribute("plugin").setValue(namePlugin);
        }

        return xmlImport;
    }
}
