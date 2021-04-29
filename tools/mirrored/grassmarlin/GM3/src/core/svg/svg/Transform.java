package core.svg.svg;

import javafx.geometry.Point2D;

public final class Transform {
    private final double tX;
    private final double tY;

    public Transform(double x, double y) {
        tX = x;
        tY = y;
    }

    public Point2D apply(Point2D point) {
        return point.add(tX, tY);
    }

    public double getX() {
        return tX;
    }
    public double getY() {
        return tY;
    }
}
