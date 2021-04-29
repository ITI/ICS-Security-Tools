package ui.graphing.logical;

import core.document.graph.IEdge;
import core.document.graph.INode;
import javafx.geometry.Point2D;
import ui.graphing.Cell;
import ui.graphing.Edge;
import ui.graphing.Layout;
import ui.graphing.Visualization;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Runs a physics simulation to place groups.  With the load-test dataset this executes in approximately 500ms.
 * Groups repel each other with a fixed force based on the attractive force of linked groups at a given equilibrium distance.
 * All groups are pulled towards the center to prevent excessive gaps from forming.
 * Since this is n-squared with respect to the number of groups, an iterative approach is taken where the set of groups
 * is broken down by tiers of size and fewer iterations are run of the later passes.
 *
 * The resulting layout is still not ideal; many edges cross, clusters of related nodes are often intertwined with
 * disjoint sets of nodes, etc.
 * Internally, two proposed solutions have been at the forefront of the debate; one involves a simulation in which
 * linked nodes orbit each other, with forces acting on individual elements as well as groups.  This would provide a
 * better layout for the cells within groups, but preliminary simulations result in the cells of the largest group being
 * pulled well outside the desired bounds, often into the middle of distant groups.  Reorganizing such a graph is more
 * cumbersome than updating the existing graph, rendering the proposed LayoutForceAwakenedGroups impractical, but still
 * promising.  The other proposed option is, in the opinion of the majority of developers, rather contrived and
 * unnecessary, offering no practical gain, so there will be no further mention of LayoutMidichlorianDirectedGroups.
 * @param <TNode> Node type; generally unnecessary except to preserve generic typing of Cell and visualization.
 * @param <TEdge> Edge type; generally unnecessary except to preserve generic typing of Cell and visualization.
 */
public class LayoutForceDirectedGroups<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> implements Layout<TNode, TEdge> {
    private static final int MAX_ITERATIONS = 250;

    private static class Group<TNode extends INode<TNode>, TEdge extends IEdge<TNode>> {

        private final String name;
        private final double cntMembers;    //This is a double since we will use it in the force calculations and this saves typecast overhead every iteration.

        private Point2D location;
        private Point2D force;

        public Group(final String name, final int cntMembers) {
            this.name = name;

            this.cntMembers = (double)cntMembers;
            this.location = Point2D.ZERO;
            this.force = Point2D.ZERO;

            associations = new ArrayList<>();

            if(cntMembers <= 2.5) {
                //Single Cell or 2 cells, one above the other
                radius = (CELL_WIDTH + CELL_MARGIN) / 2.0;
            } else if(cntMembers <= 4.5) {
                //2 cells wide
                radius =  (CELL_WIDTH + CELL_MARGIN);
            } else {
                radius = Math.ceil(cntMembers / 2.0) * (CELL_HEIGHT + CELL_MARGIN);
            }

        }

        public Point2D getLocation() {
            return location;
        }

        public String getName() {
            return name;
        }

        public void setLocation(Point2D location) {
            this.location = location;
            this.force = Point2D.ZERO;
        }

        public void setAssociations(Collection<String> connections) {
            associations.addAll(connections);
        }

        private final ArrayList<String> associations;

        //Estimated size of nodes; we can't rely on node size being correct.
        private static final double CELL_HEIGHT = 18.0;
        private static final double CELL_WIDTH = 80.0;
        private static final double CELL_MARGIN = 4.0;

        private final double radius;

        public double calculateRadius() {
            return radius;
        }

        // == Calculating midpoint of this group relative to other groups

        public void applyForces(double multiplier) {
            this.location = this.location.add(force.multiply(multiplier));
        }

        private static final double EQUILIBRIUM_DISTANCE = 3.0;
        private static final double GRAVITY_STRENGTH = 0.1;
        private static final double OVERLAP_REPULSION_RATE = 1.1;
        private static final double COEFFICIENT_REPULSION = 15000.0;

        /**
         * Forces are applied in the following manner:
         *   - Every group is pulled towards the origin with a strength of 1.0
         *   - Overlapping groups repel each other by 55% of the amount of overlap
         *   - All other groups repel each other (always) and attract each other (only if connected)
         *     - The equilibrium for these forces is set to 3 times the sum of the radius of both groups.
         *     - Attractive force is linear with respect to distance whereas repulsive is proportional to the inverse square of the distance
         * @param groups
         */
        public double calculateForces(Collection<Group<TNode, TEdge>> groups) {
            force = Point2D.ZERO;
            for(Group<TNode, TEdge> group : groups) {
                if(group == this) {
                    // Apply gravity as a force towards the origin.
                    force = force.add(location.normalize().multiply(-GRAVITY_STRENGTH));
                } else if(group.location.equals(location)) {
                    //Groups occupy the same spot; ignore (for now)
                } else if(group.location.subtract(location).magnitude() < (calculateRadius() + group.calculateRadius())) {
                    // Groups overlap, so they repel
                    force = force.add(location.subtract(group.location)
                            .normalize()
                            .multiply(
                                    (calculateRadius() + group.calculateRadius() - group.location.subtract(location).magnitude()) * OVERLAP_REPULSION_RATE));
                } else {
                    double magnitude = 0.0;
                    double distance = location.subtract(group.location).magnitude();

                    if(associations.contains(group.getName())) {
                        // Groups are connected, so apply an attractive (negative) force
                        //Calculate the force at the equilibrium distance multiplier
                        double distanceEquilibrium = (calculateRadius() + group.calculateRadius()) * EQUILIBRIUM_DISTANCE;
                        double forceEquilibrium = (group.cntMembers * cntMembers) / (distanceEquilibrium * distanceEquilibrium);

                        magnitude -= (forceEquilibrium);

                    }
                    // Regardless of connectivity, apply a repulsive (positive) force
                    magnitude += (group.cntMembers * cntMembers) / (distance * distance);

                    force = force.add(location.subtract(group.location).normalize().multiply(COEFFICIENT_REPULSION * magnitude));
                }
            }
            return force.magnitude();
        }

        // == Arranging nodes within this group

        //TODO: DISTORTION_COEFFICIENT should be calculated more precisely; this is a crude estimate to spread out the nodes better than a uniform distribution produces.
        private static final double DISTORTION_COEFFICIENT = 0.08;
        private static final double SOCKET_RATIO = 1.0;

        public List<Double> calculateItemVectors() {
            //Result is the angle (in radians) of each connection.
            final List<Double> result = new LinkedList<>();
            //Uniformly distribute the vectors throughout [0, 2pi)
            double offset = 2.0 * Math.PI / Math.ceil(cntMembers * SOCKET_RATIO);
            for(int idx = 0; idx * offset < Math.PI * 2.0; idx++) {
                result.add((double)idx * offset);
            }
            //Distort the values so that values near 0 and 1pi are closer to those points and values near 0.5pi and 1.5pi are further
            // The magnitude of distortion will be of the same sign of cos(x) and of magnitude sin(x).
            for(int idx = 0; idx < cntMembers; idx++) {
                result.set(idx,
                        result.get(idx) +
                                (
                                        (Math.cos(result.get(idx)) > 0.0 ? -1.0 : 1.0) *
                                                Math.sin(result.get(idx)) *
                                                DISTORTION_COEFFICIENT
                                )
                );
            }
            return result;
        }
    }

    public LayoutForceDirectedGroups() {
        super();
    }

    /**
     * Must be called from the UI Thread
     * @param visualization
     */
    public void layoutAll(Visualization<TNode, TEdge> visualization) {
        try {
            final String groupBy = visualization.getCurrentGroupBy();
            // Identify all the subnets that are subject to layout
            final List<String> groups = visualization.getAllGroupsForLayout();

            //Start with all the cells...
            HashMap<String, Set<String>> connectionMap = new HashMap<>();
            for (String group : groups) {
                connectionMap.put(group, new HashSet<>());
            }

            HashSet<Cell<TNode>> cells = new HashSet<>(visualization.getAllCellsForLayout());
            for (Edge<TNode> edge2 : cells.stream()
                    //Extract the edges which connect members of the layoutable subset of nodes
                    .flatMap(cell -> cell.getEdges()
                            .stream()
                                    //Reduce to only those edges where both endpoints are part of the layout
                            .filter(edge -> cells.contains(edge.getSource()) && cells.contains(edge.getTarget())))
                            //Remove edges where both endpoints belong to the same group
                    .filter(edge ->
                            edge.getSource().getNode().getGroups().get(groupBy) == null
                                    ? edge.getTarget().getNode().getGroups().get(groupBy) != null
                                    : !edge.getSource().getNode().getGroups().get(groupBy).equals(edge.getTarget().getNode().getGroups().get(groupBy)))
                            //reduce to distinct connections; we expect every edge to be doubled since they are/can be bidirectional
                    .distinct()
                    .collect(Collectors.toList())) {
                String groupSource = edge2.getSource().getNode().getGroups().get(groupBy);
                String groupTarget = edge2.getTarget().getNode().getGroups().get(groupBy);

                connectionMap.get(groupSource).add(groupTarget);
                connectionMap.get(groupTarget).add(groupSource);
            }

            final HashMap<String, Integer> groupSizes = new HashMap<>();
            final HashMap<String, List<Cell<TNode>>> groupContents = new HashMap<>();
            for(String group : groups) {
                List<Cell<TNode>> contents = visualization.getAllCellsForLayout(group);
                groupSizes.put(group, contents.size());
                groupContents.put(group, contents);
            }

            //Build the Group objects and pre-calculate the initial radius that will be used for each simulation step
            final HashMap<String, Group<TNode, TEdge>> lookupGroups = new HashMap<>();
            double halfCircumfrence = 0.0;
            for (String name : groups) {
                final Group<TNode, TEdge> group = new Group<>(name, groupSizes.get(name));
                lookupGroups.put(name, group);
                group.setAssociations(connectionMap.get(name));

                halfCircumfrence += group.calculateRadius();
            }

            //Calculate how large of a circle is needed (roughly) to place the groups in a non-overlapping manner.
            //Then, position them around the circle and calculate
            double radius = halfCircumfrence / Math.PI;
            double radiansPerUnitSize = Math.PI / halfCircumfrence;
            //This will be used to normalize the movement speed
            double rateAverage = radius / MAX_ITERATIONS;
            if (rateAverage < 1.0) {
                rateAverage = 1.0;
            }

            // Run the simulation in stages.
            //  The basic simulation is N^2 complexity (every object generates a force on every other object)
            //  This partitions the nodes into groups and generates forces between the current partition and all previously-processed partitions.
            //  The worst case is still N^2, the best case is closer to half that.
            //  Performance aside, this produces a better-looking result.
            final List<String> namesAwaitingLayout = new ArrayList<>(groups);
            final List<String> namesProcessed = new ArrayList<>();
            final Collection<Group<TNode, TEdge>> groupsProcessed = new ArrayList<>();

            int maxIterations = MAX_ITERATIONS;

            while(!namesAwaitingLayout.isEmpty()) {
                int cntLargest = 0;
                for(String group : namesAwaitingLayout) {
                    int cntCurrent = groupSizes.get(group);
                    if(cntCurrent > cntLargest) {
                        cntLargest = cntCurrent;
                    }
                }

                final int cntThreshold = cntLargest / 4;    // 4 chosen for completely arbitrary reasons.
                final List<String> groupsThisIteration = namesAwaitingLayout.stream().filter(group -> groupSizes.get(group) >= cntThreshold).limit(Math.max(1, groups.size() / 2)).collect(Collectors.toList());
                namesAwaitingLayout.removeAll(groupsThisIteration);
                namesProcessed.addAll(groupsThisIteration);

                // Order the groups by the number of connections.
                //  Break ties by favoring the larger number of members.
                //  Break remaining ties with alpha-sort.
                groupsThisIteration.sort((o2, o1) -> {
                    if (o1 == null) {
                        return o2 == null ? 0 : -1;
                    }
                    if (o2 == null) {
                        return 1;
                    }

                    int connections1 = connectionMap.get(o1).size();
                    int connections2 = connectionMap.get(o2).size();
                    if (connections1 != connections2) {
                        return (connections1 - connections2);
                    }
                    int members1 = groupSizes.get(o1);
                    int members2 = groupSizes.get(o2);
                    if (members1 != members2) {
                        return members1 - members2;
                    }
                    return o1.compareTo(o2);
                });

                // Identify the group objects to be processed this iteration.
                final Collection<Group<TNode, TEdge>> groupsSimulated = lookupGroups.values().stream().filter(group -> groupsThisIteration.contains(group.getName())).collect(Collectors.toList());
                groupsProcessed.addAll(groupsSimulated);

                // Initial placement - Fixed radius uniformly around a circle... sort of.
                //  If the group is connected only to a single group, and that group has already been processed, then we will place it around that group.
                double angle = 0.0;
                final Map<Group<TNode, TEdge>, List<Group<TNode, TEdge>>> soloGroups = new HashMap<>();
                for (Group<TNode, TEdge> group : groupsSimulated) {
                    double radiansForItem = radiansPerUnitSize * group.calculateRadius();
                    angle += radiansForItem;

                    //If this group only connects to groups that have been processed, then start it at the midpoint of the groups.
                    // Otherwise, it will be placed around the exterior and be pulled in.
                    final Set<String> namesConnected = connectionMap.get(group.getName());
                    final List<Group<TNode, TEdge>> groupsConnected = new ArrayList<>(namesConnected.size());
                    for(String name : namesConnected) {
                        groupsConnected.add(lookupGroups.get(name));
                    }
                    if(groupsProcessed.containsAll(groupsConnected)) {
                        double x = 0;
                        double y = 0;
                        int cntLinks = 0;

                        for(Group<TNode, TEdge> groupAssociated : groupsConnected) {
                            x += groupAssociated.getLocation().getX();
                            y += groupAssociated.getLocation().getY();
                            if(!groupsSimulated.contains(groupAssociated)) {
                                cntLinks++;
                            }
                        }
                        if(groupsConnected.size() > 0) {
                            x /= groupsConnected.size();
                            y /= groupsConnected.size();
                        }

                        group.setLocation(new Point2D(x, y));

                        if(cntLinks == 1) {
                            final Group<TNode, TEdge> groupOther = groupsConnected.stream().filter(g -> !groupsSimulated.contains(g)).findAny().get();
                            if(!soloGroups.containsKey(groupOther)) {
                                soloGroups.put(groupOther, new LinkedList<>());
                            }
                            soloGroups.get(groupOther).add(group);
                        }
                    } else {
                        group.setLocation(
                                new Point2D(
                                        radius * Math.cos(angle),
                                        radius * Math.sin(angle)));
                    }
                    angle += radiansForItem;
                }

                //Anything that was marked as being a group linked to a single existing node will be placed near it.
                for(Map.Entry<Group<TNode, TEdge>, List<Group<TNode, TEdge>>> entry : soloGroups.entrySet()) {
                    final Group<TNode, TEdge> parent = entry.getKey();
                    double radiusChildren = 0.0;
                    for(Group<TNode, TEdge> child : entry.getValue()) {
                        radius = Math.max(radiusChildren, child.calculateRadius());
                    }
                    radiusChildren += parent.calculateRadius();
                    radiusChildren *= 1.1;

                    double angleChild = 0.0;
                    final double anglePerChild = Math.PI * 2.0 / entry.getValue().size();
                    for(Group<TNode, TEdge> child : entry.getValue()) {
                        child.setLocation(new Point2D(
                                parent.getLocation().getX() + Math.cos(angleChild) * radiusChildren,
                                parent.getLocation().getY() + Math.sin(angleChild) * radiusChildren
                        ));
                        angleChild += anglePerChild;
                    }
                }

                for (int iteration = 0; iteration < maxIterations; iteration++) {
                    double moveRate = 2.0 * rateAverage * (double) (maxIterations - iteration) / (double) maxIterations;

                    double maxDistance = 0.0;
                    for (Group<TNode, TEdge> group : groupsSimulated) {
                        maxDistance = Math.max(maxDistance, group.calculateForces(groupsProcessed));
                    }

                    if (maxDistance > 0.0) {
                        final double multiplier = moveRate / maxDistance;
                        for (Group<TNode, TEdge> group : groupsSimulated) {
                            group.applyForces(multiplier);
                        }
                    } else {
                        break;
                    }
                }
                maxIterations = maxIterations / 2 + (MAX_ITERATIONS / 10);
            }

            //Group centers are identified; render groups
            for (Group<TNode, TEdge> group : groupsProcessed) {
                LayoutGroup(group, groupContents.get(group.getName()), lookupGroups, groupBy);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void layoutSingle(Visualization<TNode, TEdge> visualization, Cell<TNode> cell, TNode node) {
        if(node.getGroups().get(visualization.getCurrentGroupBy()) != null) {
            layoutAll(visualization);
        }
    }

    public void LayoutGroup(Group<TNode, TEdge> group, List<Cell<TNode>> cells, HashMap<String, Group<TNode, TEdge>> groups, String groupBy) {
        if(cells.isEmpty()) {
            return;
        }
        if(cells.size() == 1) {
            cells.get(0).setLayoutX(group.getLocation().getX());
            cells.get(0).setLayoutY(group.getLocation().getY());
            return;
        }

        double angularShift = 0.0;
        if(cells.size() == 2) {
            angularShift = Math.PI / 2.0;
        }

        List<Double> angles = group.calculateItemVectors();
        cells.sort((o1, o2) -> o2.getEdges().size() - o1.getEdges().size());

        for(Cell<TNode> cell : cells) {
            Point2D ptOther = Point2D.ZERO;
            for(Edge<TNode> edge : cell.getEdges()) {
                String nameOther;
                if(edge.getSource().equals(cell)) {
                    nameOther = edge.getTarget().getNode().getGroups().get(groupBy);
                } else {
                    nameOther = edge.getSource().getNode().getGroups().get(groupBy);
                }

                Group<TNode, TEdge> groupOther = groups.get(nameOther);
                if(groupOther != null) {
                    ptOther = ptOther.add(
                            groupOther.getLocation().getX() - group.getLocation().getX(),
                            groupOther.getLocation().getY() - group.getLocation().getY()
                    );
                }
            }

            // ptOther is the sum of the vectors to other points.
            // Normalize it, then find the member of angles which most closely matches this angle.
            // Use it for the cell and remove from the list.
            ptOther = ptOther.normalize();
            double error = Double.MAX_VALUE;
            Double angle = 0.0;
            for(Double val : angles) {
                double dX = Math.cos(val) - ptOther.getX();
                double dY = Math.sin(val) - ptOther.getY();

                double e = dX * dX + dY * dY;
                if(e < error) {
                    error = e;
                    angle = val;
                }
            }
            angles.remove(angle);

            cell.setLayoutX(group.getLocation().getX() + Math.cos(angle + angularShift) * group.calculateRadius());
            cell.setLayoutY(group.getLocation().getY() + Math.sin(angle + angularShift) * group.calculateRadius());
        }
    }
}