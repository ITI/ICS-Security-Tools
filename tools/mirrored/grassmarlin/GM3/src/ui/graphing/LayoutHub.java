package ui.graphing;

import core.document.graph.IEdge;
import core.document.graph.INode;
import javafx.geometry.Point2D;

import java.util.List;

public class LayoutHub<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> implements Layout<TNode, TEdge> {
    @FunctionalInterface
    public interface IOriginFactory {
        Point2D getOrigin();
    }

    protected IOriginFactory origin;

    protected double radiusGroups = 450.0;
    protected double radiusCells = 200.0;

    public LayoutHub() {
        origin = null;
    }
    public LayoutHub(double radiusGroups, double radiusCells) {
        this();
    }
    public LayoutHub(double radiusGroups, double radiusCells, IOriginFactory factory) {
        this(radiusGroups, radiusCells);
        this.origin = factory;
    }

    @Override
    public void layoutAll(Visualization<TNode, TEdge> visualization) {
        //If there is a single group (treating null as a group) then layout a single circle in the center.
        //Otherwise lay out circles around the points of a regular N-gon (coincidentally equidistant around a circle) where the circles contain the elements of each identified group.
        List<String> groups = visualization.getAllGroupsForLayout();
        Point2D ptCenter = new Point2D(0.0, 0.0);
        if(origin != null) {
            ptCenter = origin.getOrigin();
        }
        if(groups.size() == 0) {
            // Do nothing
        } else if(groups.size() == 1) {
            //Use origin as center for all nodes.
            LayoutAround(ptCenter, radiusCells, 0.0, visualization.getAllCellsForLayout());
        } else {
            double radIncrement = 2.0 * Math.PI / groups.size();
            groups.sort((o1, o2) -> {
                //We make the assumption that both cannot be null.
                if(o1 == null) {
                    return -1;
                }
                if(o2 == null) {
                    return 1;
                }
                return o1.compareTo(o2);
            });

            double radiusMin = radiusGroups;
            // 1.2 to give a little padding between groups to allow whitespace and compensate for the size of the labels.
            // 2.0 * radiusCells / radIncrement gives the radius necessary to have an arc length equal to twice the radius of cell circle.
            // This isn't an exact calculation, but the error is fairly low and the overhead trivial.  Since we're not
            // checking the size of the labels (which are often 0 because of when the layout is run) a little error is fine.
            double radiusCalculated = 1.2 * 2.0 * radiusCells / radIncrement;

            final double radius = radiusCalculated > radiusMin ? radiusCalculated : radiusMin;

            for(int idx = 0; idx < groups.size(); idx++) {
                String nameGroup = groups.get(idx);
                List<Cell<TNode>> cells = visualization.getAllCellsForLayout(nameGroup);

                LayoutAround(new Point2D(
                        ptCenter.getX() + radius * Math.cos(radIncrement * idx),
                        ptCenter.getY() + radius * Math.sin(radIncrement * idx)
                ), radiusCells, (radIncrement * idx) + (Math.PI / 2.0), cells);
            }
        }
    }

    @Override
    public void layoutSingle(Visualization<TNode, TEdge> visualization, Cell<TNode> cell, TNode node) {
        //Punt to layoutAll; at the least, everything in this group needs to be shifted.
        layoutAll(visualization);
    }

    public void LayoutAround(Point2D center, double radius, double angleInitial, List<Cell<TNode>> cells) {
        LayoutAround(center, radius, angleInitial, cells, true);
    }
    public void LayoutAround(Point2D center, double radius, double angleInitial, List<Cell<TNode>> cells, boolean CenterSingleCell) {
        double radIncrement = 2.0 * Math.PI / cells.size();
        cells.sort((o1, o2) -> o1.toString().compareTo(o2.toString()));

        if(CenterSingleCell && cells.size() == 1) {
            cells.get(0).setLayoutX(center.getX() - cells.get(0).getWidth() / 2.0);
            cells.get(0).setLayoutY(center.getY() - cells.get(0).getHeight() / 2.0);
        } else {
            for (int idx = 0; idx < cells.size(); idx++) {
                Cell cell = cells.get(idx);

                cell.setLayoutX(center.getX() + radius * Math.cos(angleInitial + radIncrement * idx) - cell.getWidth() / 2.0);
                cell.setLayoutY(center.getY() + radius * Math.sin(angleInitial + radIncrement * idx) - cell.getHeight() / 2.0);
            }
        }
    }

}
