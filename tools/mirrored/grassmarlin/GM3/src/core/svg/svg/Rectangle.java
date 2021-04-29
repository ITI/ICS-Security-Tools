package core.svg.svg;

import core.svg.Svg;
import javafx.scene.paint.Color;

public class Rectangle extends Entity {
    private final javafx.scene.shape.Rectangle rectangle;

    public Rectangle(final javafx.scene.shape.Rectangle rectangle) {
        this.rectangle = rectangle;
    }

    @Override
    public String toSvg(final TransformStack transforms) {
        return String.format("<rect x='%s' y='%s' width='%s' height='%s' rx='%s' ry='%s' style='fill:%s;fill-opacity:%s;stroke:%s;stroke-width:%s' />",
                transforms.get().getX() + rectangle.getX(),
                transforms.get().getY() + rectangle.getY(),
                rectangle.getWidth(),
                rectangle.getHeight(),
                rectangle.getArcWidth(),
                rectangle.getArcHeight(),
                Svg.fromColor((Color) rectangle.getFill()),
                ((Color) rectangle.getFill()).getOpacity(),
                Svg.fromColor((Color) rectangle.getStroke()),
                rectangle.getStrokeWidth());
    }
}
