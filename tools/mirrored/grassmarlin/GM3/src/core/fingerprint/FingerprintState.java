package core.fingerprint;


import core.fingerprint3.Fingerprint;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.nio.file.Path;

public class FingerprintState {
    private BooleanProperty dirty;
    private BooleanProperty enabled;
    private ObjectProperty<Path> path;
    private final Fingerprint fingerprint;

    public FingerprintState(Fingerprint fp, Path savePath) {
        this.dirty = new SimpleBooleanProperty(false);
        this.enabled = new SimpleBooleanProperty(false);
        this.path = new SimpleObjectProperty<>(savePath);
        this.fingerprint = fp;
    }

    public FingerprintState(Fingerprint fp) {
        this(fp, null);
    }


    public BooleanProperty dirtyProperty() {
        return this.dirty;
    }

    public BooleanProperty enabledProperty() { return this.enabled; }

    public ObjectProperty<Path> pathProperty() {
        return this.path;
    }

    public Fingerprint getFingerprint() {
        return this.fingerprint;
    }


    @Override
    public int hashCode() {
        //paths should be unique
        return this.path.get().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof FingerprintState) {
            Path p = this.path.get();
            String name = this.fingerprint.getHeader().getName();

            Path otherP = ((FingerprintState) other).path.get();
            String otherName = ((FingerprintState) other).fingerprint.getHeader().getName();

            //if both have paths than the path must match
            //if neither have paths than the name must match
            //otherwise not equal
            if (p != null && otherP != null) {
                return (p.equals(otherP));
            } else if (p == null && otherP == null) {
                return name.equals(otherName);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean equals(String name, Path path) {
        boolean equals;
        if (path != null && this.path.get() != null) {
            equals = this.path.get().equals(path);
        } else if (name != null) {
            equals = this.fingerprint.getHeader().getName().equals(name);
        } else {
            equals = false;
        }

        return equals;
    }
}
