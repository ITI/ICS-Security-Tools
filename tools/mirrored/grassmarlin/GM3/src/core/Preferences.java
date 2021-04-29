package core;

import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;

/**
 * The preferences are user-specified Properties which are used and bound throughout the UI.  If it doesn't need to be bound as a property, it shouldn't be here.
 * The preferences are serialized through the Configuration object.
 */
public class Preferences {
    public static final ObjectProperty<Color> VisualizationNodeBackgroundColor;
    public static final ObjectProperty<Color> VisualizationNodeSelectedBackgroundColor;

    public static final ObjectProperty<Color> VisualizationNodeTextColor;
    public static final ObjectProperty<Color> NodeNewColor;
    public static final ObjectProperty<Color> NodeModifiedColor;

    public static final BooleanProperty CreateSubnetsOnDemand;
    public static final LongProperty CreatedSubnetSize;

    public static final BooleanProperty SuppressUnchangedVersionNotes;

    public static final BooleanProperty FingerprintSaveAsLeavesOld;

    protected static class BoundLongProperty extends SimpleLongProperty {
        private final Configuration.Fields field;

        public BoundLongProperty(Configuration.Fields field) {
            super(Configuration.getPreferenceLong(field));

            this.field = field;
            addListener(this::Handle_Change);
        }

        private void Handle_Change(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            Configuration.setPreferenceLong(field, newValue.longValue());
        }

        private void refresh() {
            super.set(Configuration.getPreferenceLong(field));
        }
    }

    protected static class BoundBooleanProperty extends SimpleBooleanProperty {
        private final Configuration.Fields field;

        public BoundBooleanProperty(Configuration.Fields field) {
            super(Configuration.getPreferenceBoolean(field));

            this.field = field;
            addListener(this::Handle_Change);
        }

        private void Handle_Change(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            Configuration.setPreferenceBoolean(field, newValue);
        }

        private void refresh() {
            super.set(Configuration.getPreferenceBoolean(field));
        }
    }
    /**
     * Class that binds to the initial value from Configuration and sets the configuration value on change.
     */
    protected static class BoundColorProperty extends SimpleObjectProperty<Color> {
        private final Configuration.Fields field;

        public BoundColorProperty(Configuration.Fields field) {
            super(Color.web(Configuration.getPreferenceString(field)));

            this.field = field;
            addListener(this::Handle_Change);
        }

        private void Handle_Change(ObservableValue<? extends Color> observable, Color oldValue, Color newValue) {
            //The toString format for Color is 0xRRGGBBAA, we just want RRGGBB
            Configuration.setPreferenceString(field, newValue.toString().substring(2, 8));
        }

        private void refresh() {
            super.set(Color.web(Configuration.getPreferenceString(field)));
        }
    }

    static {
        VisualizationNodeBackgroundColor = new BoundColorProperty(Configuration.Fields.COLOR_NODE_BACKGROUND);
        VisualizationNodeSelectedBackgroundColor = new BoundColorProperty(Configuration.Fields.COLOR_NODE_HIGHLIGHT);
        VisualizationNodeTextColor = new BoundColorProperty(Configuration.Fields.COLOR_NODE_TEXT);
        NodeNewColor = new BoundColorProperty(Configuration.Fields.COLOR_NODE_NEW);
        NodeModifiedColor = new BoundColorProperty(Configuration.Fields.COLOR_NODE_MODIFIED);

        CreateSubnetsOnDemand = new BoundBooleanProperty(Configuration.Fields.LOGICAL_CREATE_DYNAMIC_SUBNETS);
        CreatedSubnetSize = new BoundLongProperty(Configuration.Fields.LOGICAL_DYNAMIC_SUBNET_BITS);

        SuppressUnchangedVersionNotes = new BoundBooleanProperty(Configuration.Fields.SUPPRESS_UNCHANGED_VERSION_NOTES);

        FingerprintSaveAsLeavesOld = new BoundBooleanProperty(Configuration.Fields.FINGERPRINT_SAVEAS_LEAVES_OLD);
    }

    public static void refreshValues() {
        ((BoundColorProperty)VisualizationNodeBackgroundColor).refresh();
        ((BoundColorProperty)VisualizationNodeSelectedBackgroundColor).refresh();

        ((BoundColorProperty)VisualizationNodeTextColor).refresh();
        ((BoundColorProperty)NodeNewColor).refresh();
        ((BoundColorProperty)NodeModifiedColor).refresh();

        ((BoundBooleanProperty)CreateSubnetsOnDemand).refresh();
        ((BoundLongProperty)CreatedSubnetSize).refresh();

        ((BoundBooleanProperty)SuppressUnchangedVersionNotes).refresh();

        ((BoundBooleanProperty)FingerprintSaveAsLeavesOld).refresh();
    }
}
