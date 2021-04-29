package core.document.graph;

import core.Configuration;
import core.document.CidrList;
import core.document.ImportList;
import core.document.serialization.xml.XmlElement;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import ui.dialog.ManageLogicalNetworksDialogFx;
import util.Cidr;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

public class LogicalGraph extends NetworkGraph<LogicalNode, LogicalEdge> {

    private final ObservableList<Cidr> cidrsLogical;

    protected LogicalGraph(ObservableList<Cidr> cidrs) {
        this.cidrsLogical = cidrs;
        cidrsLogical.addListener(this::Handle_NetworksChanged);
        ManageLogicalNetworksDialogFx.getInstance().itemsProperty().setValue(this.cidrsLogical);
    }

    public LogicalGraph() {
        this(new CidrList());
    }

    private void Handle_NetworksChanged(ListChangeListener.Change<? extends Cidr> c) {
        //Find all removed groups and invalidate any node with an IP in the given range.
        //If any groups were added then invalidate any cached null values.
        final List<LogicalNode> nodesToEvaluate = new LinkedList<>();

        while(c.next()) {
            if(!c.getRemoved().isEmpty()) {
                for (LogicalNode node : nodesObservable) {
                    for (Cidr cidrRemoved : c.getRemoved()) {
                        Cidr ip = node.getIp();
                        if (cidrRemoved.contains(ip)) {
                            node.networkProperty().clear();
                            nodesToEvaluate.add(node);
                            break;
                        }
                    }
                }
            }
            if(!c.getAddedSubList().isEmpty()) {
                for(LogicalNode node : nodesObservable) {
                    if(node.networkProperty().get() == null) {
                        node.networkProperty().clear();
                        nodesToEvaluate.add(node);
                    }
                }
            }
        }

        for(LogicalNode node : nodesToEvaluate) {
            node.networkProperty().get();
        }

        Platform.runLater(() -> OnGroupingInvalidated.call(new UpdateGraphArgs(this)));
    }

    @Override
    protected void Process_commitUI() {
        synchronized(lock) {
            //Find all new nodes and identify which, if any, networks need to be created.
            if (Configuration.getPreferenceBoolean(Configuration.Fields.LOGICAL_CREATE_DYNAMIC_SUBNETS)) {
                final List<LogicalNode> nodesNew = new LinkedList<>(nodes.keySet());
                nodesNew.removeAll(nodesObservable);

                createSubnets(nodesNew);
            }

            //Commit the nodes/edges now.  We will look at the committed entities to determine what networks need to be built
            super.Process_commitUI();
        }
    }

    protected void createSubnets(final List<LogicalNode> nodesNew) {
        final short bits = (short) Configuration.getPreferenceLong(Configuration.Fields.LOGICAL_DYNAMIC_SUBNET_BITS);
        final Set<Cidr> cidrsToAdd = new HashSet<>();

        for (LogicalNode node : nodesNew) {
            //If the network has already been loaded then we don't need to check to see if we need to create one.
            //This is mostly relevant when loading; since it is explicitly set, we don't want to auto-calculate a new
            // Cidr if it was explicitly set to null.
            if(!node.networkProperty().isLoaded()) {
                cidrsToAdd.add(new Cidr(node.getIp().getFirstIp(), bits));
            }
        }

        cidrsToAdd.removeIf(cidr -> cidrsLogical.stream().filter(existing -> existing.contains(cidr)).findAny().isPresent());
        cidrsLogical.addAll(cidrsToAdd);
    }

    @Override
    public LogicalEdge addEdge(LogicalEdge edgeNew) {
        addNode(edgeNew.getSource());
        addNode(edgeNew.getDestination());

        return super.addEdge(edgeNew);
    }

    @Override
    public void clearTopology() {
        super.clearTopology();

        cidrsLogical.clear();
    }

    public ObservableList<Cidr> getCidrList() {
        return cidrsLogical;
    }

    @Override
    protected String getEntryName() {
        return "logical.xml";
    }

    @Override
    public void toXml(ImportList imports, ZipOutputStream zos) throws IOException {
        super.toXmlStart(zos);
        super.toXmlContents(imports, zos);
        zos.write("<cidrs>".getBytes(StandardCharsets.UTF_8));
        zos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
        for(Cidr cidr : cidrsLogical) {
            XmlElement xmlCidr = new XmlElement("cidr");
            xmlCidr.addAttribute("t").setValue(cidr.toString());
            zos.write(xmlCidr.toString().getBytes(StandardCharsets.UTF_8));
            zos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
        }
        zos.write("</cidrs>".getBytes(StandardCharsets.UTF_8));
        zos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));

        super.toXmlEnd(zos);
    }
}
