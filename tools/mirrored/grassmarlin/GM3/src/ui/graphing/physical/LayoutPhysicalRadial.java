package ui.graphing.physical;

import core.document.graph.*;
import ui.graphing.Cell;
import ui.graphing.CellGroup;
import ui.graphing.Edge;
import ui.graphing.Visualization;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class LayoutPhysicalRadial implements ui.graphing.Layout<PhysicalNode, PhysicalEdge> {
    private final FactoryDeviceGroups deviceFactory;

    public LayoutPhysicalRadial(FactoryDeviceGroups deviceFactory) {
        this.deviceFactory = deviceFactory;
    }

    protected List<String> getDeviceGroups(Visualization<PhysicalNode, PhysicalEdge> visualization) {
        return visualization.getAllCellsForLayout().stream()
                .map(value -> value.getNode().getGroups().get(PhysicalNode.GROUP_OWNER))
                .filter(owner -> owner != null)
                .distinct()
                .collect(Collectors.toList());
    }

    private static class GroupArc {
        private final GroupSwitch device;
        private final Cell<PhysicalNode> cloudDevice;
        private final Cell<PhysicalNode> cloudLeaves;

        private final String name;
        private final List<GroupComputer> workstations;

        public GroupArc(GroupSwitch device) {
            this.device = device;
            this.cloudDevice = null;
            this.cloudLeaves = null;

            name = device.nameProperty().get();

            workstations = new LinkedList<>();
        }

        public GroupArc(String name, Cell<PhysicalNode> device, Cell<PhysicalNode> leaves) {
            this.device = null;
            this.cloudDevice = device;
            this.cloudLeaves = leaves;

            this.name = name;

            workstations = new LinkedList<>();
        }

        public String getName() {
            return name;
        }
        public List<GroupComputer> getWorkstations() {
            return workstations;
        }

        public void positionElements(double radiansCenter, double radiansArc, double radiusBase) {
            if(device != null) {
                device.offsetXProperty().set(Math.cos(radiansCenter) * radiusBase);
                device.offsetYProperty().set(Math.sin(radiansCenter) * radiusBase);
            } else {
                cloudDevice.layoutXProperty().set(Math.cos(radiansCenter) * radiusBase * 0.9);
                cloudDevice.layoutYProperty().set(Math.sin(radiansCenter) * radiusBase * 0.9);

                cloudLeaves.layoutXProperty().set(Math.cos(radiansCenter) * radiusBase * 1.0);
                cloudLeaves.layoutYProperty().set(Math.sin(radiansCenter) * radiusBase * 1.0);
            }

            //TODO: Fix the math here.  It was written on a Friday.  Variable names probably need a reassessment as well.
            // Arrange workstations
            final double radiansInitial = radiansCenter - (radiansArc / 2.0);
            final double radiansPerWorkstation = radiansArc / (double)workstations.size();
            double radiansCurrentWorkstation = 0.0;
            double radiusCurrentWorkstation = 0.0;
            //Calculate the bounding diameter of a workstation
            double radiusOffsetPerWorkstation = 100.0;
            for(GroupComputer workstation : workstations) {
                radiusOffsetPerWorkstation = Math.max(radiusOffsetPerWorkstation, workstation.getLayoutBounds().getWidth());
            }
            // calculate the approximate arc-width of a workstation; assume wider than taller, compute as circle, base on 1.25*radiusBase radius
            double arcWorkstation = radiusOffsetPerWorkstation / (1.25 * radiusBase);
            double radReset = arcWorkstation;


            for(GroupComputer workstation : workstations) {
                workstation.offsetXProperty().set(Math.cos(radiansInitial + radiansCurrentWorkstation) * (radiusBase * 1.25 + radiusCurrentWorkstation));
                workstation.offsetYProperty().set(Math.sin(radiansInitial + radiansCurrentWorkstation) * (radiusBase * 1.25 + radiusCurrentWorkstation));

                radiansCurrentWorkstation += radiansPerWorkstation;
                radiusCurrentWorkstation += radiusOffsetPerWorkstation;
                if(radiansCurrentWorkstation > radReset) {
                    radReset = radiansCurrentWorkstation + arcWorkstation;
                    radiusCurrentWorkstation = 0.0;
                }
            }
        }
    }

    public void layoutAll(Visualization<PhysicalNode, PhysicalEdge> visualization) {
        List<GroupArc> lstGroups = new LinkedList<>();
        List<GroupComputer> lstWorkstations = new LinkedList<>();
        double diameterSwitch = 0.0;

        int cntProblems = 0;
        boolean hasProblematicSwitch = false;

        for(String nameGroup : getDeviceGroups(visualization)) {
            System.out.println("Evaluating group: " + nameGroup);
            CellGroup<PhysicalNode, ? extends IEdge<PhysicalNode>> visualGroup = deviceFactory.getGroup(PhysicalNode.GROUP_OWNER, nameGroup);

            if(visualGroup instanceof GroupSwitch) {
                System.out.println(" > Group is a switch");
                lstGroups.add(new GroupArc((GroupSwitch) visualGroup));
                diameterSwitch = Math.max(diameterSwitch, visualGroup.getLayoutBounds().getWidth());
            } else if(visualGroup instanceof GroupComputer) {
                System.out.println(" > Group is a Computer");
                lstWorkstations.add((GroupComputer) visualGroup);
            } else if(visualGroup instanceof GroupCloud) {
                System.out.println(" > Group is a Cloud");
                List<Cell<PhysicalNode>> members = (visualGroup).getMembers();
                if(members.size() != 2) {
                    System.out.println(" > PROBLEM: Cloud does not have 2 members.");
                    cntProblems++;
                    continue;
                }
                if(members.get(0).getNode() instanceof PhysicalCloud && members.get(1).getNode() instanceof PhysicalCloud) {
                    if(((PhysicalCloud)members.get(0).getNode()).getSubtype().equals("Switches")) {
                        lstGroups.add(new GroupArc(visualGroup.nameProperty().get(), members.get(0), members.get(1)));
                    } else {
                        lstGroups.add(new GroupArc(visualGroup.nameProperty().get(), members.get(1), members.get(0)));
                    }
                } else {
                    System.out.println(" > PROBLEM: Neither member of a cloud is a PhysicalCloud");
                    cntProblems++;
                }
            } else if(visualGroup instanceof LayoutManagedGroup) {
                //No idea what this is, but it is a distinct case from the else below, although closely related.
                ((LayoutManagedGroup) visualGroup).offsetXProperty().set(0);
                ((LayoutManagedGroup) visualGroup).offsetYProperty().set(0);

                System.out.println(" > PROBLEM: Group is an unknown LayoutManagedGroup");
                cntProblems++;
            } else {
                // No idea what this is; it probably shouldn't exist.
                for(Cell<?> cell : visualGroup.getMembers()) {
                    cell.setLayoutX(0);
                    cell.setLayoutY(0);
                }

                System.out.println(" > PROBLEM: Group is of unknown type");
                cntProblems++;
            }
        }

        for(GroupComputer workstation : lstWorkstations) {
            System.out.println("Evaluating WORKSTATION " + workstation.nameProperty().get());
            if(workstation.getMembers().isEmpty()) {
                System.out.println(" > PROBLEM: Workstation is empty.");
                cntProblems++;
                continue;
            }
            Cell<PhysicalNode> cell = workstation.getMembers().get(0);
            if(cell.getEdges().isEmpty()) {
                System.out.println(" > PROBLEM: Workstation has no edges.");
                cntProblems++;
                continue;
            }
            if(cell.getEdges().size() != 1) {
                System.out.println(" > PROBLEM: Workstation has more than 1 edge.");
                cntProblems++;
            }
            Edge<PhysicalNode> edge = cell.getEdges().get(0);
            final String owner;
            if(edge.getSource().getNode() instanceof PhysicalPort) {
                owner = edge.getSource().getNode().getGroups().get(PhysicalNode.GROUP_OWNER);
            } else {
                owner = edge.getTarget().getNode().getGroups().get(PhysicalNode.GROUP_OWNER);
            }
            if(owner == null) {
                System.out.println(" > PROBLEM: owner is null");
                cntProblems++;
            } else {
                for(GroupArc arc : lstGroups) {
                    System.out.println("   + Comparing " + arc.getName() + " to " + owner);
                    if(arc.getName().equals(owner)) {
                        arc.workstations.add(workstation);
                        System.out.println(" > Workstation is assigned to arc " + arc.getName());
                        break;
                    }
                }
            }
        }

        //The error handling is reasonably robust (e.g. this won't crash when problems are encountered), but output for debugging purposes.
        /* The layout tends to be called twice, and the first run (add nodes) will have problems (no edges).
        if(cntProblems != 0) {
            if(hasProblematicSwitch) {
                Logger.log(this, Severity.Warning, "Physical Layout has detected " + cntProblems + " problems (1 or more are switches)");
            } else {
                Logger.log(this, Severity.Warning, "Physical Layout has detected " + cntProblems + " problems (0 are switches)");
            }
        }*/

        //Arrange switches / clouds in a circle.
        //Conventional wisdom suggests you should keep your switches in line, and this is linear on a polar coordinate system.
        final double anglePerNode = Math.PI * 2.0 / (double)lstGroups.size();
        //If I did my math right (I didn't, but if not this should still be close enough) the radius is calculated such
        // that the chord from the center of a switch to the center of the switch 2 places away will be tangent to the
        // edge of the bounding circle around the switch between them.  This is then extended by the radius of the
        // bounding circle so that no segment between the two switches can intersect the middle switch.
        //This is just a back-of-the-napkin calculation; having a whiteboard or even a pen would make these sorts of
        // things easier to work out.  Cilantro lime salsa does not make for a good writing implement.  Good lunch, but
        // bad writing implement.
        final double radius = diameterSwitch * Math.cos(Math.PI / 6.0) / Math.sin(anglePerNode / 2.0) + (diameterSwitch / 2.0);

        for(int idx = 0; idx < lstGroups.size(); idx++) {
            lstGroups.get(idx).positionElements((double)idx*anglePerNode, anglePerNode, radius);
        }
    }
    public void layoutSingle(Visualization<PhysicalNode, PhysicalEdge> visualization, Cell<PhysicalNode> cell, PhysicalNode node) {
        String nameGroup = node.getGroups().get(visualization.getCurrentGroupBy());
        CellGroup<PhysicalNode, PhysicalEdge> uiGroup = visualization.groupFor(nameGroup);
        if(uiGroup != null) {
            //If there is a single member in the group then it is a new group and we have to layout the groups.
            //If the group is not a LayoutManagedGroup then we have to layout everything.
            //Otherwise the layout will be handled by the group itself.
            if(uiGroup.getMembers().size() == 1 || !(uiGroup instanceof LayoutManagedGroup)) {
                layoutAll(visualization);
            }
        }
    }
}
