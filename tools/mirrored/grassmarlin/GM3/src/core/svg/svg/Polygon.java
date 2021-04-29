package core.svg.svg;

import core.svg.Svg;
import javafx.scene.paint.Color;

public class Polygon extends Entity {
    private final javafx.scene.shape.Polygon polygon;

    public Polygon(final javafx.scene.shape.Polygon polygon) {
        this.polygon = polygon;
    }

    @Override
    public String toSvg(final TransformStack transforms) {
        if(!polygon.isVisible() || polygon.getPoints().size() < 6) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        result.append("<polygon points='");
        for(int idxPoint = 0; idxPoint < polygon.getPoints().size(); idxPoint += 2) {
            result.append(polygon.getPoints().get(idxPoint) + transforms.get().getX()).append(",").append(polygon.getPoints().get(idxPoint + 1) + transforms.get().getY()).append(" ");
        }
        result.append(String.format("' style='fill:%s;fill-opacity:%s;stroke:%s;stroke-width:%s' />",
                Svg.fromColor((Color) polygon.getFill()),
                ((Color) polygon.getFill()).getOpacity(),
                Svg.fromColor((Color) polygon.getStroke()),
                polygon.getStrokeWidth()));

        return result.toString();
    }
}
