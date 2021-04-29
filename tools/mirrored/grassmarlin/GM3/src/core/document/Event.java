/*
 *  Copyright (C) 2016
 *  This file is part of GRASSMARLIN.
 */
package core.document;

import javafx.application.Platform;

import java.util.concurrent.CopyOnWriteArraySet;

public class Event<TArgs> {
    public interface EventListener<TArgs> {
        void HandleEvent(Event<TArgs> source, TArgs arguments);
    }

    private final CopyOnWriteArraySet<EventListener<TArgs>> handlers;
    private final boolean runInFxThread;

    public Event(boolean runInFxThread) {
        this.runInFxThread = runInFxThread;
        handlers = new CopyOnWriteArraySet<>();
    }

    public Event() {
        this(true);
    }

    public synchronized void addHandler(EventListener<TArgs> handler) {
        if(handler == null)
            return;
        handlers.add(handler);
    }

    public synchronized void removeHandler(EventListener<TArgs> handler) {
        handlers.remove(handler);
    }

    public void call(TArgs args) {
        for(EventListener<TArgs> handler : handlers) {
            if(runInFxThread && !Platform.isFxApplicationThread()) {
                Platform.runLater(() -> handler.HandleEvent(this, args));
            } else {
                handler.HandleEvent(this, args);
            }
        }
    }

    public synchronized void clearHandlers() {
        handlers.clear();
    }
}
