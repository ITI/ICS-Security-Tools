package ui.custom.fx;

import javafx.beans.property.ObjectProperty;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ColorPreview extends Rectangle {

    public ColorPreview(ObjectProperty<Color> fillColor) {
        super(14.0, 14.0);

        setStrokeWidth(1.0);
        setStroke(Color.BLACK);
        fillProperty().bind(fillColor);
    }
}
