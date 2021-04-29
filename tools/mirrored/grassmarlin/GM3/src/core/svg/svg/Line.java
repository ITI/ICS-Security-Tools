package core.svg.svg;

import core.svg.Svg;
import javafx.scene.paint.Color;

public class Line extends Entity {
    private final javafx.scene.shape.Line source;

    public Line(final javafx.scene.shape.Line source) {
        this.source = source;
    }

    @Override
    public String toSvg(final TransformStack transforms) {
        final Transform tr = transforms.get();
        return "<line x1='" + (tr.getX() + source.getStartX()) + "' y1='" + (tr.getY() + source.getStartY()) + "' x2='" + (tr.getX() + source.getEndX()) + "' y2='" + (tr.getY() + source.getEndY()) + "' style='stroke:" + Svg.fromColor((Color)source.getStroke()) + ";stroke-width:" + source.getStrokeWidth() + "'/>";
    }
}
