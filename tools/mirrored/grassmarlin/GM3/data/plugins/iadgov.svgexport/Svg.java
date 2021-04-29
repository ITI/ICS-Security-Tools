package iadgov.svgexport;

import iadgov.svgexport.svg.*;
import core.document.serialization.xml.Escaping;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import ui.graphing.ZoomableScrollPane;

import java.util.LinkedList;

public class Svg {
    public static String fillFromColor(Color color) {
        if(color.getOpacity() == 1.0) {
            return "fill:rgb(" + (int)(255.0 * color.getRed()) + ","  + (int)(255.0 * color.getGreen()) + "," + (int)(255.0 * color.getBlue()) + ")";
        } else {
            return "fill:rgb(" + (int)(255.0 * color.getRed()) + ","  + (int)(255.0 * color.getGreen()) + "," + (int)(255.0 * color.getBlue()) + ");fill-opacity:" + color.getOpacity();
        }
    }
    public static String XmlString(String in) {
        return Escaping.XmlString(in);
    }
    public static String fromColor(Color color) {
        if(color == null || color.getOpacity() == 0.0) {
            return "none";
        } else {
            return "rgb(" + (int) (255.0 * color.getRed()) + "," + (int) (255.0 * color.getGreen()) + "," + (int) (255.0 * color.getBlue()) + ")";
        }
    }

    public static String serialize(ZoomableScrollPane object) {
        StringBuilder result = new StringBuilder();

        Bounds bounds = object.getChildren().get(0).getLayoutBounds();
        TransformStack transforms = new TransformStack();
        transforms.push(bounds.getMinX() * -1.0, bounds.getMinY() * -1.0);

        result.append("<svg xmlns='http://www.w3.org/2000/svg' version='1.0' xmlns:xlink='http://www.w3.org/1999/xlink' width='")
                .append(bounds.getWidth())
                .append("' height='")
                .append(bounds.getHeight())
                .append("'>");

        result.append(fromNode(object).toSvg(transforms));
        result.append("\n</svg>");

        return result.toString();
    }

    public static Entity fromNode(Node object) {
        if(object == null || !object.isVisible() || object.getOpacity() < 1.0) {
            return null;
        } else if(object instanceof javafx.scene.shape.Polygon) {
            return new Polygon((javafx.scene.shape.Polygon)object);
        } else if(object instanceof javafx.scene.shape.Line) {
            return new Line((javafx.scene.shape.Line)object);
        } else if(object instanceof javafx.scene.shape.CubicCurve) {
            return new Curve((javafx.scene.shape.CubicCurve)object);
        } else if(object instanceof javafx.scene.Parent) {
            javafx.scene.Parent parent = (javafx.scene.Parent)object;

            final LinkedList<Entity> children = new LinkedList<>();
            for(Node child : parent.getChildrenUnmodifiable()) {
                final Entity entity = fromNode(child);
                if(entity != null) {
                    children.add(entity);
                }
            }

            if(children.isEmpty()) {
                return null;
            } else {
                Container result = new Container(children);

                if(parent instanceof javafx.scene.layout.Region) {
                    //Region adds padding.
                    javafx.scene.layout.Region region = (javafx.scene.layout.Region)parent;

                    result.setOffsetX(parent.getLayoutX() + region.getPadding().getLeft() - parent.getTranslateX());
                    result.setOffsetY(parent.getLayoutY() + region.getPadding().getTop() - parent.getTranslateY());
                } else {
                    result.setOffsetX(parent.getLayoutX() - parent.getTranslateX());
                    result.setOffsetY(parent.getLayoutY() - parent.getTranslateY());
                }

                return result;
            }
        } else if(object instanceof javafx.scene.shape.Rectangle) {
            return new Rectangle( (javafx.scene.shape.Rectangle)object );
        } else if(object instanceof javafx.scene.image.ImageView) {
            return new Image((javafx.scene.image.ImageView) object);
        } else if(object instanceof javafx.scene.text.Text) {
            //A Label is a Parent (covered above) which contains a Text
            return new Text( (javafx.scene.text.Text)object );
        } else {
            System.out.println("Unable to convert Node to SVG: (" + object.getClass() + ")" + object);
            return null;
        }
    }
}
