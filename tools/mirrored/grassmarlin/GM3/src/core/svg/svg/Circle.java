package core.svg.svg;

import core.svg.Svg;
import javafx.scene.paint.Color;

public class Circle  extends Entity {
    private final javafx.scene.shape.Circle circle;

    public Circle(javafx.scene.shape.Circle source) {
        this.circle = source;
    }

    @Override
    public String toSvg(final TransformStack transforms) {
        return String.format("<circle cx='%s' cy='%s' r='%s' style='fill:%s;fill-opacity:%s;stroke:%s;stroke-width:%s' />",
                transforms.get().getX() + circle.getCenterX() + circle.getLayoutX(),
                transforms.get().getY() + circle.getCenterY() + circle.getLayoutY(),
                circle.getRadius(),
                Svg.fromColor((Color) circle.getFill()),
                ((Color) circle.getFill()).getOpacity(),
                Svg.fromColor((Color) circle.getStroke()),
                circle.getStrokeWidth());
    }
}
