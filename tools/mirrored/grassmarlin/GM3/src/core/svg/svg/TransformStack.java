package core.svg.svg;

import javafx.geometry.Point2D;

import java.util.Stack;

public class TransformStack {
    protected final Stack<Transform> transforms;

    public TransformStack() {
        transforms = new Stack<>();
        transforms.push(new Transform(0.0, 0.0));
    }

    public int depth() {
        return transforms.size();
    }
    public Transform get() {
        return transforms.peek();
    }
    public Transform pop() {
        return transforms.pop();
    }
    public Transform push(double tX, double tY) {
        Transform top = transforms.peek();
        transforms.push(new Transform(top.getX() + tX, top.getY() + tY));
        return transforms.peek();
    }

    public Point2D apply(Point2D point) {
        return transforms.peek().apply(point);
    }
}
