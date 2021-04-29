package ui.custom.fx;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.HashMap;
import java.util.LinkedList;

public class ColorPalette extends GridPane {
    protected class Sample extends Rectangle {
        private final SimpleObjectProperty<Color> color;

        public Sample() {
            this(Color.WHITE);
        }
        public Sample(Color colorDefault) {
            super(24.0, 16.0);

            color = new SimpleObjectProperty<>(colorDefault);
            this.fillProperty().bind(color);
            this.setStrokeWidth(1.0);
            this.setStroke(Color.BLACK);
        }

        public ObjectProperty<Color> colorProperty() {
            return color;
        }
        public Color getColor() {
            return color.get();
        }
        public void setColor(Color color) {
            this.color.set(color);
        }
    }

    private final ObservableList<Color> samples;
    private final HashMap<Color, Sample> mapSamples;
    private final SimpleObjectProperty<Color> selectedColor;
    private int idxNext = 0;
    private final int cntCols;

    public ColorPalette() {
        this(1, 8);
    }
    public ColorPalette(int rows, int cols) {
        cntCols = cols;
        this.setPrefWidth(32.0 * cols);
        this.setPrefHeight(24.0 * rows);

        this.setPadding(new Insets(4.0, 4.0, 4.0, 4.0));
        this.setHgap(8.0);
        this.setVgap(8.0);

        selectedColor = new SimpleObjectProperty<>();

        samples = new ObservableListWrapper<>(new LinkedList<>());
        samples.addListener(this::Handle_SampleListChange);

        mapSamples = new HashMap<>();
    }

    private void Handle_SampleListChange(ListChangeListener.Change<? extends Color> change) {
        while(change.next()) {
            change.getRemoved().forEach(colorRemoved -> {
                this.getChildren().remove(mapSamples.get(colorRemoved));
            });
            change.getAddedSubList().forEach(colorAdded -> {
                if(!mapSamples.containsKey(colorAdded)) {
                    Sample sampleNew = new Sample(colorAdded);
                    AddSample(sampleNew);
                    mapSamples.put(colorAdded, sampleNew);
                }
            });
        }
    }

    private void AddSample(Sample sampleNew) {
        add(sampleNew, idxNext % cntCols, idxNext / cntCols);
        sampleNew.setOnMouseClicked(event -> {
            selectedColor.set(sampleNew.getColor());
        });
        idxNext++;
    }

    public ObjectProperty<Color> selectedColorProperty() {
        return selectedColor;
    }

    public ObservableList<Color> getColors() {
        return samples;
    }
}
