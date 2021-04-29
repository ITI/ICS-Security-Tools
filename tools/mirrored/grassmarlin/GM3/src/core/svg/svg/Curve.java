package core.svg.svg;

import core.svg.Svg;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

public class Curve extends Entity {
    private final javafx.scene.shape.CubicCurve source;

    public Curve(final javafx.scene.shape.CubicCurve source) {
        this.source = source;
    }

    @Override
    public String toSvg(final TransformStack transforms) {
        final Transform tr = transforms.get();
        Point2D[] arrPts = new Point2D[4];
        arrPts[0] = new Point2D(tr.getX() + source.getStartX(), tr.getY() + source.getStartY());
        arrPts[1] = new Point2D(tr.getX() + source.getControlX1(), tr.getY() + source.getControlY1());
        arrPts[2] = new Point2D(tr.getX() + source.getControlX2(), tr.getY() + source.getControlY2());
        arrPts[3] = new Point2D(tr.getX() + source.getEndX(), tr.getY() + source.getEndY());

        return String.format("<path d='M%s,%s C%s,%s %s,%s %s,%s' style='fill:none;stroke:%s;stroke-width:%s'/>",
                arrPts[0].getX(), arrPts[0].getY(),
                arrPts[1].getX(), arrPts[1].getY(),
                arrPts[2].getX(), arrPts[2].getY(),
                arrPts[3].getX(), arrPts[3].getY(),
                Svg.fromColor((Color) source.getStroke()),
                source.getStrokeWidth());
    }
}
