package ui.graphing.physical;

import core.document.graph.PhysicalEdge;
import core.document.graph.PhysicalNode;
import core.document.serialization.xml.XmlElement;
import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.paint.Color;
import ui.EmbeddedIcons;
import ui.custom.fx.ActiveMenuItem;
import ui.graphing.Cell;
import ui.graphing.Graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GroupSwitch extends LayoutManagedGroup<PhysicalNode, PhysicalEdge> {
    private static final double IMAGE_ASPECT_RATIO = 48.0 / 40.0;
    protected static double IMAGE_WIDTH = 24.0;
    protected static double IMAGE_HEIGHT = IMAGE_WIDTH / IMAGE_ASPECT_RATIO;

    public enum EGroupLayout {
        SingleRow("Single Row", (offset, total) -> new Point2D(offset * IMAGE_WIDTH, -IMAGE_HEIGHT)),
        SingleRowInverted("Single Row (Inverted)", (offset, total) -> new Point2D(offset * IMAGE_WIDTH, 0.0)),
        SingleRowCentered("Single Row (Centered)", (offset, total) -> new Point2D(offset * IMAGE_WIDTH, -0.5 * IMAGE_HEIGHT)),

        DoubleRowHorizontal("Two Rows (Horizontal)", (offset, total) -> {
            int cntPerRow = (total + 1) / 2;    //If odd, place the odd element in the first row.
            return new Point2D(
                    (offset % cntPerRow) * IMAGE_WIDTH,
                    ((offset / cntPerRow) - 1) * IMAGE_HEIGHT
            );
        }),
        DoubleRowAlternating("Two Rows (Alternating)", (offset, total) -> new Point2D((offset / 2) * IMAGE_WIDTH, ((offset % 2) - 1) * IMAGE_HEIGHT)),
        DoubleRowHorizontalBlocks("Two Rows (Horizontal, Groups of 4)", (offset, total) -> {
            int cntBlocks = (total + 3 ) / 4;
            int cntBlocksPerRow = (cntBlocks + 1) / 2;

            int idxBlock = offset / 4;
            int xBlock = idxBlock % cntBlocksPerRow;
            int yBlock = idxBlock / cntBlocksPerRow - 1;

            int xPort = offset % (4 * cntBlocksPerRow);

            return new Point2D(
                    xPort * IMAGE_WIDTH + xBlock * (IMAGE_WIDTH / 4.0),
                    yBlock * IMAGE_HEIGHT
            );
        })
        ;

        @FunctionalInterface
        public interface LayoutFunction {
            Point2D calculate(int offset, int total);
        }

        private final LayoutFunction fnLayout;
        private final String displayName;

        EGroupLayout(String displayName, LayoutFunction fnLayout) {
            this.fnLayout = fnLayout;
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
        public Point2D getPosition(int index, int total) {
            return fnLayout.calculate(index, total);
        }
    }

    private static class PortGroup {
        private final SimpleDoubleProperty offsetX;
        private final SimpleDoubleProperty offsetY;
        private final SimpleDoubleProperty width;
        private final SimpleObjectProperty<EGroupLayout> layout;
        private final SimpleStringProperty name;
        private final ObservableList<Cell<PhysicalNode>> ports;

        public static PortGroup of(String name, ObservableList<Cell<PhysicalNode>> contents) {
            return new PortGroup(
                    name,
                    new FilteredList<>(contents, cellPort ->
                            (cellPort instanceof CellPort)
                            &&
                            ((CellPort)cellPort).getPort().groupProperty().get().equals(name)
                    )
            );
        }

        private PortGroup(String name, ObservableList<Cell<PhysicalNode>> ports) {
            offsetX = new SimpleDoubleProperty(0.0);
            offsetY = new SimpleDoubleProperty(0.0);
            width = new SimpleDoubleProperty(0);
            layout = new SimpleObjectProperty<>(EGroupLayout.DoubleRowHorizontalBlocks);
            layout.addListener((observable, oldValue, newValue) -> RepositionElements(null));
            this.name = new SimpleStringProperty(name);
            this.ports = ports;

            this.ports.addListener(this::RepositionElements);
            RepositionElements(null);
        }

        protected void RepositionElements(ListChangeListener.Change<? extends Node> change) {
            EGroupLayout layout = this.layout.get();

            if (ports.size() > 0) {
                ArrayList<Cell> portsToDisplay = new ArrayList<>(ports);
                portsToDisplay.sort((o1, o2) -> {
                    if (o1 instanceof CellPort && o2 instanceof CellPort) {
                        return Integer.compare(((CellPort) o1).getPort().indexProperty().get(), ((CellPort) o2).getPort().indexProperty().get());
                    } else {
                        return o1.getNode().titleProperty().get().compareTo(o2.getNode().titleProperty().get());
                    }
                });

                int cntTotal = portsToDisplay.size();
                int idxChild = 0;
                double widthNew = 0;
                for (Node child : portsToDisplay) {
                    Point2D position = layout.getPosition(idxChild++, cntTotal);
                    child.layoutXProperty().bind(offsetX.add(position.getX()));
                    child.layoutYProperty().bind(offsetY.add(position.getY()));

                    if (child instanceof CellPort) {
                        //Anything where the Y position is >= 0 should be inverted.
                        ((CellPort) child).invertedProperty().set(position.getY() >= 0.0);
                    }

                    widthNew = Double.max(widthNew, position.getX() + IMAGE_WIDTH);
                }

                width.set(widthNew);
            }
        }

        public DoubleProperty offsetXProperty() {
            return offsetX;
        }
        public DoubleProperty offsetYProperty() {
            return offsetY;
        }
        public DoubleProperty widthProperty() {
            return width;
        }
        public ObjectProperty<EGroupLayout> layoutProperty() {
            return layout;
        }
        public StringProperty nameProperty() {
            return name;
        }
    }

    private final HashMap<String, PortGroup> portGroups;
    public GroupSwitch(String name, Graph<PhysicalNode, PhysicalEdge> owner) {
        super(name, EmbeddedIcons.Vista_Network, owner);

        super.fillColor.set(Color.TAN);

        portGroups = new HashMap<>();

        miChangeColor.setText("Change Device Color");
        miCollapseGroup.setText("Collapse Device");

        Menu miPortGroups = new Menu("Port Groupings");
        miPortGroups.getItems().add(new SeparatorMenuItem());
        miPortGroups.setOnShowing(event -> {
            miPortGroups.getItems().clear();
            for(Map.Entry<String, PortGroup> entry : portGroups.entrySet()) {
                Menu miCurrentGroup = new Menu(entry.getKey());

                for(EGroupLayout layout : EGroupLayout.values()) {
                    miCurrentGroup.getItems().add(new ActiveMenuItem(layout.getDisplayName(), evt -> {
                        entry.getValue().layoutProperty().set(layout);
                        RebuildPortGroups();
                    }));
                }

                miPortGroups.getItems().add(miCurrentGroup);
            }

            if(miPortGroups.getItems().isEmpty()) {
                miPortGroups.getItems().add(new MenuItem("No Groups Defined"));
            }
        });
        contextItems.add(miPortGroups);
    }

    public void setPortGroupLayout(String name, EGroupLayout layout) {
        PortGroup group = portGroups.get(name);
        group.layoutProperty().setValue(layout);
    }

    protected void RebuildPortGroups() {
        String[] keys = portGroups.keySet().toArray(new String[portGroups.size()]);
        Arrays.sort(keys);
        double offsetGroup = 0;
        for(String key : keys) {
            PortGroup group = portGroups.get(key);
            group.offsetXProperty().bind(this.offsetXProperty().add(offsetGroup));
            group.offsetYProperty().bind(this.offsetYProperty());

            group.RepositionElements(null);
            offsetGroup += group.widthProperty().get();
            offsetGroup += IMAGE_WIDTH;
        }
    }

    @Override
    protected void memberAdded(final Cell<PhysicalNode> member) {
        if (member instanceof CellPort) {
            CellPort portNew = (CellPort) member;

            String nameGroup = portNew.getPort().groupProperty().get();

            if (!portGroups.containsKey(nameGroup)) {
                portGroups.put(nameGroup, PortGroup.of(nameGroup, getMembers()));
            }
        }

        super.memberAdded(member);
    }

    @Override
    protected void membersChanged() {
        RebuildPortGroups();

        super.membersChanged();
    }

    @Override
    public XmlElement toXml() {
        XmlElement xmlGroup = super.toXml();

        xmlGroup.addAttribute("type").setValue("switch");

        for(Map.Entry<String, PortGroup> entry : portGroups.entrySet()) {
            EGroupLayout layout = entry.getValue().layoutProperty().get();

            XmlElement xmlPortGroup = new XmlElement("portGroup").appendedTo(xmlGroup);
            xmlPortGroup.addAttribute("name").setValue(entry.getKey());
            xmlPortGroup.addAttribute("layout").setValue(layout.toString());
        }

        return xmlGroup;
    }
}
