package core.svg.svg;

import java.util.Collection;

public class Container extends Entity {

    private final Collection<Entity> components;
    private double tX = 0.0;
    private double tY = 0.0;

    public Container(final Collection<Entity> components) {
        this.components = components;
    }

    public void setOffsetX(final double value) {
        tX = value;
    }
    public void setOffsetY(final double value) {
        tY = value;
    }

    @Override
    public String toSvg(final TransformStack transforms) {
        final StringBuilder result = new StringBuilder();

        //DEBUG:  Add an indent and line break so visual parsing is easier.
        String indent = "\n";
        for(int idx = transforms.depth(); idx >= 0; idx--) {
            indent += "  ";
        }

        if(tX != 0.0 || tY != 0.0) {
            transforms.push(tX, tY);
        }
        for(Entity child : components) {
            result.append(indent);
            result.append(child.toSvg(transforms));
        }
        if(tX != 0.0 || tY != 0.0) {
            transforms.pop();
        }

        return result.toString();
    }
}
