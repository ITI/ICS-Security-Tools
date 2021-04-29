package core.svg.svg;

import core.svg.Svg;
import javafx.scene.paint.Color;

public class Text extends Entity {
    private final javafx.scene.text.Text text;

    public Text(final javafx.scene.text.Text text) {
        this.text = text;
    }

    @Override
    public String toSvg(TransformStack transforms) {
        return String.format("<text x='%s' y='%s' style='fill:%s;font-family:%s' font-size='%s'>%s</text>",
                transforms.get().getX() + text.getLayoutX() + 2.0,
                transforms.get().getY() + text.getLayoutY(),
                Svg.fromColor((Color) text.getFill()),
                Svg.XmlString(text.getFont().getFamily()),
                (int)text.getFont().getSize(),
                text.getText());
    }
}
