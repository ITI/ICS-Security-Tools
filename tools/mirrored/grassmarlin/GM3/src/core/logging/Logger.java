/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.logging;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Logger {
    private final ObservableList<Message> history;

    private static final Logger globalEmitter;

    public static class Message {

        public final Class<?> source;
        public final Severity severity;
        public final String message;
        public final Long tsCreated;

        public Message(final Object source, final Severity severity, final String message) {
            if (source instanceof Class) {
                this.source = (Class) source;
            } else {
                this.source = source.getClass();
            }
            this.severity = severity;
            this.message = message;
            this.tsCreated = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return message;
        }

        // == Accessor functions for use in JavaFxLogViewer  ==================
        public String getMessage() {
            return message;
        }
        public String getTimestamp() {
            return Instant.ofEpochMilli(tsCreated).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_TIME);
        }
    }

    static {
        globalEmitter = new Logger();
    }

    public Logger() {
        history = new ObservableListWrapper<>(new ArrayList<>()) ;
    }

    public static void log(final Object i, final Severity a, final String o) {
        final Message msg = new Message(i, a, o);

        try {
            //Even (especially) if we are in the FX Thread use a deferred add.
            Platform.runLater(() -> globalEmitter.history.add(msg));
        } catch(IllegalStateException ex) {
            //If the JavaFX Platform is not yet initialized, we get an IllegalStateException.
            //If that happens then we will just add in-thread as we're still working through the initialization and the Fx Thread doesn't exist yet / won't exist until after we exit here.
            globalEmitter.history.add(msg);
        }
    }

    public static ObservableList<Message> getMessageHistory() {
        return globalEmitter.history;
    }
}
