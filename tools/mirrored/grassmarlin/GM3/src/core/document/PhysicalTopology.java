package core.document;

import com.sun.javafx.collections.ObservableListWrapper;
import core.document.graph.*;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import util.Mac;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

/**
 * This class builds the Physical network graph by building associations between a list of PhysicalDevices.
 */
public class PhysicalTopology {
    /**
     * This is used to differentiate workstations from clouds.
     */
    public static final String TOKEN_WORKSTATION = "\\\\";

    private final ListChangeListener deviceListener;

    public static class DirectConnection {
        private final Mac mac1;
        private final Mac mac2;

        public DirectConnection(Mac mac1, Mac mac2) {
            this.mac1 = mac1;
            this.mac2 = mac2;
        }

        public Mac start() {
            return mac1;
        }
        public Mac end() {
            return mac2;
        }

        @Override
        public int hashCode() {
            return mac1.hashCode() ^ mac2.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if(other == null) {
                return false;
            }
            if(other instanceof DirectConnection) {
                DirectConnection rhs = (DirectConnection)other;

                //Reference equality is safe because the connections are made between a fixed set of objects.
                return (mac1 == rhs.mac1 && mac2 == rhs.mac2) || (mac1 == rhs.mac2 || mac2 == rhs.mac1);
            } else {
                return false;
            }
        }
    }
    public static class Cloud extends HashSet<Mac> {
        public Cloud(final Mac mac) {
            this.add(mac);
        }
        public Cloud(final Mac mac, Collection<Mac> macsInitial) {
            this.add(mac);
            this.addAll(macsInitial);
        }

        public boolean containsAny(final Collection<Mac> other) {
            return this.stream().filter(other::contains).findAny().isPresent();
        }

        public static List<Cloud> mergeClouds(List<Cloud> cloudsExisting, Cloud cloudNew) {
            List<Cloud> cloudsResult = new LinkedList<>();
            cloudsResult.add(cloudNew);
            for(Cloud cloud : cloudsExisting) {
                if(cloudNew.containsAny(cloud)) {
                    cloudNew.addAll(cloud);
                } else {
                    cloudsResult.add(cloud);
                }
            }

            return cloudsResult;
        }
    }

    protected final ObservableListWrapper<PhysicalDevice> devices;
    protected final NetworkGraph<PhysicalNode, PhysicalEdge> graphPhysical;
    protected final AtomicBoolean pendingUpdate;

    public PhysicalTopology(NetworkGraph<PhysicalNode, PhysicalEdge> graphPhysical) {
        pendingUpdate = new AtomicBoolean(false);
        this.graphPhysical = graphPhysical;

        devices = new ObservableListWrapper<>(new CopyOnWriteArrayList<>());
        this.deviceListener = this::Handle_DeviceListModified;
        devices.addListener(this.deviceListener);
    }

    public void startLoading() {
        devices.removeListener(this.deviceListener);
    }

    public void endLoading() {
        devices.addListener(this.deviceListener);
    }

    public ObservableList<PhysicalDevice> getDevices() {
        return devices;
    }

    private void Handle_DeviceListModified(ListChangeListener.Change<? extends PhysicalDevice> change) {
        //This list should only be changed from the owning session's task dispatcher.
        //changes should be performed in bulk, so after the calculation we can notify the UI of the changes.

        RebuildTopology();
    }

    private void RebuildTopology() {
        //Take a snapshot of the device list so that, if it is modified while we're processing this, we don't end up
        //using different lists at different points in this method.
        //This shouldn't happen, but a redesign to the threading could easily introduce this sort of error.
        final ArrayList<PhysicalDevice> lstDevices = new ArrayList<>(devices);

        // == IDENTIFY WHICH MACS BELONG TO SWITCHES AND WHICH BELONG TO WORKSTATIONS ==
        //We're going to manage MACs as strings instead of Byte[] because .equals() is reference equality on arrays and it is more cumbersome to do this without relying on .equals()
        // Build a list of MAC Addresses which belong to the devices (which we will assume are all switches).
        final List<Mac> macSwitchPorts = lstDevices.stream()
                .flatMap(device -> device.getPorts().stream())
                .map(port -> port.macProperty().get())
                .filter(mac -> mac != null && mac.hashCode() != 0)
                .distinct()
                .collect(Collectors.toList());
        // Build a list of all other MAC Addresses (which we will assume are all workstations).
        final List<Mac> macWorkstations = lstDevices.stream()
                .flatMap(device -> device.getPorts().stream())
                .flatMap(port -> port.getEndpoints().stream())
                .map(endpoint -> endpoint.macProperty().get())
                .filter(mac -> !macSwitchPorts.contains(mac))
                .distinct()
                .collect(Collectors.toList());

        // == PRE-PROCESS PORT MAC LISTS ==
        // For every Port, if it is a trunk port, remove all workstations, otherwise remove all switch ports
        // Every remaining set of endpoints is added to a cloud.
        final HashMap<PhysicalDevice, Set<Mac>> workstationsByDevice = new HashMap<>();
        final HashMap<Mac, PhysicalDevice.Port> lookupPorts = new HashMap<>();
        List<Cloud> clouds = new LinkedList<>();
        for(PhysicalDevice device : lstDevices) {
            Set<Mac> macs = new HashSet<>();
            workstationsByDevice.put(device, macs);

            for(PhysicalDevice.Port port : device.getPorts()) {
                //Skip ports which aren't connected to anything.
                if(port.getEndpoints().isEmpty()) {
                    //If the port connects to nothing we skip it for now.
                } else {
                    //We're connected to something...
                    //Cache the port lookup
                    Mac macPort = port.macProperty().get();
                    lookupPorts.put(macPort, port);

                    //Get all the non-null MACs connected to this port.
                    List<Mac> connectedMacs = port.getEndpoints().stream()
                            .map(endpoint -> endpoint.macProperty().get())
                            .filter(mac -> !mac.equals(Mac.NULL_MAC))
                            .distinct() //MAC on multiple VLans will appear multiple times
                            .collect(Collectors.toList());

                    if(port.trunkProperty().get()) {
                        connectedMacs.removeAll(macWorkstations);
                    } else {
                        connectedMacs.removeAll(macSwitchPorts);
                    }

                    //If we removed everything then there is unknown infrastructure; so add everything as a cloud.
                    // If a trunk port connects only to workstations then, logically, the workstations are connected to a switch which connects to this port.
                    // If a non-trunk port connects only to switches then, logically, there is some other infrastructure which links them together.
                    if(connectedMacs.size() == 0) {
                        //This is the optimistic approach--we assume that whatever connects to this will report that fact and we will merge the clouds.
                        //If this doesn't happen, we have to either revisit this and start estimating unknowns or we can just flag the port as being wonky.
                        clouds = Cloud.mergeClouds(clouds, new Cloud(macPort));
                    } else {
                        //All remaining MACs that connect to this port are part of a single cloud.
                        //build a cloud of those ports and integrate into the set of clouds.
                        clouds = Cloud.mergeClouds(clouds, new Cloud(macPort, connectedMacs));
                    }
                }
            }
        }

        // == PRE-PROCESS CLOUDS ==
        //Find all workstations not associated with a port and add them to the relevant port clouds.
        final List<Cloud> cloudsSearchable = clouds;
        List<Mac> unmatchedWorkstations = macWorkstations.stream()
                .filter(mac -> !cloudsSearchable.stream()
                        .anyMatch(cloud -> cloud.contains(mac)))
                .collect(Collectors.toList());
        for(Mac mac : unmatchedWorkstations) {
            if (macWorkstations.contains(mac)) {
                // Find the cloud containing a trunk port connected to this mac and add it.
                for(PhysicalDevice.Port port : lookupPorts.values()) {
                    //TODO: Filter to only trunk ports?
                    if(port.getEndpoints().stream().anyMatch(endpoint -> endpoint.macProperty().get().equals(mac))) {
                        // Find the cloud containing the port.
                        for(Cloud cloud : clouds) {
                            if(cloud.contains(port.macProperty().get())) {
                                //Removal is probably unnecessary, but it shouldn't hurt, either.
                                //Re-merge the cloud containing the old cloud fields and the previously unassociated workstation
                                clouds.remove(cloud);
                                clouds = Cloud.mergeClouds(clouds, new Cloud(mac, cloud));
                                //Each port will belong to only one cloud, so we can stop searching clouds.  We still have to continue searching ports, however.
                                break;
                            }
                        }
                    }
                }
            }
        }

        // == PROCESS CLOUDS ==
        // Any cloud containing two MACs is a direct connection.
        // Any cloud containing more than two MACs is a cloud
        // Any cloud containing a single MAC is in an undefinable state
        //TODO: Ensure cloud names don't conflict with switch names.  If there are any bugs that result from this missing "feature" you should really rethink your naming convention for switches.
        List<DirectConnection> connections = new LinkedList<>();
        List<Cloud> cloudsFiltered = new LinkedList<>();
        for(Cloud cloud : clouds) {
            if(cloud.size() == 2) {
                Mac[] macs = cloud.toArray(new Mac[2]);
                connections.add(new DirectConnection(macs[0], macs[1]));
            } else {
                cloudsFiltered.add(cloud);
            }
        }

        // == Final Graph Construction ==
        //Lookups for when we need to connect edges.
        HashMap<Mac, PhysicalNode> nodeFromMac = new HashMap<>();    //This works on ports and NICs
        //Devices are not nodes, they are groups; we will implicitly create them as we add the nodes.

        //Add the workstations
        for(Mac macWorkstation : macWorkstations) {
            PhysicalNode nodeNew = new PhysicalNic(macWorkstation);
            nodeFromMac.put(macWorkstation, nodeNew);
        }
        //Add the ports
        for(PhysicalDevice device : lstDevices) {
            for(PhysicalDevice.Port port : device.getPorts()) {
                Mac macPort = port.macProperty().get();
                PhysicalPort nodeNew = new PhysicalPort(device, port);
                //Identify the connected MACs.
                nodeNew.connectedTo().addAll(
                        port.getEndpoints().stream()
                                .filter(endpoint -> endpoint.macProperty().get() != null)
                                .map(endpoint -> endpoint.macProperty().get())
                                .distinct()
                                .collect(Collectors.toList())
                );
                nodeFromMac.put(macPort, nodeNew);
            }
        }

        // Copy VLANs to all connected nodes.
        // For each cloud copy the VLAN settings from any Ports to all other (non-port) nodes in the cloud.
        // Port nodes to which it is connected have a separate field for tracking VLAN traffic they can receive.  If the two lists differ then we flag an inconsistency.
        for(Cloud cloud : clouds) {
            Set<Integer> vlans = new HashSet<>();
            for(Mac mac : cloud) {
                PhysicalNode node = nodeFromMac.get(mac);
                if(node instanceof PhysicalPort) {
                    vlans.addAll(node.getVLans());
                }
            }

            for(Mac mac : cloud) {
                PhysicalNode node = nodeFromMac.get(mac);
                if(!(node instanceof PhysicalPort)) {
                    node.getVLans().addAll(vlans);
                } else {
                    ((PhysicalPort)node).getConnectedVlans().addAll(vlans);
                }
            }
        }


        final List<PhysicalNode> nodes = new ArrayList<>(nodeFromMac.size());
        nodes.addAll(nodeFromMac.values());
        final List<PhysicalEdge> edges = new ArrayList<>(connections.size());

        //All nodes are accounted for, so add the direct edges...
        for(DirectConnection direct : connections) {
            edges.add(new PhysicalEdge(nodeFromMac.get(direct.start()), nodeFromMac.get(direct.end())));
        }
        // then build and link the clouds
        int idxCloud = 0;
        for(Cloud cloud : cloudsFiltered) {
            if(cloud.size() == 1) {
                Mac mac = cloud.iterator().next();
                PhysicalNode node = nodeFromMac.get(mac);
                if(node instanceof PhysicalPort) {  //This should always be true, but it doesn't hurt to validate before casting.
                    ((PhysicalPort)node).unknownConnectionProperty().set(true);
                }
            } else {
                int idxCurrent = ++idxCloud;

                final PhysicalCloud cloudWorkstations;
                final PhysicalCloud cloudSwitches;
                if (cloud.containsAny(macWorkstations)) {
                    cloudWorkstations = new PhysicalCloud(idxCurrent, "Workstations");
                    edges.addAll(cloud.stream()
                                    .filter(mac -> macWorkstations.contains(mac))
                                    .map(mac -> new PhysicalEdge(cloudWorkstations, nodeFromMac.get(mac)))
                                    .collect(Collectors.toList())
                    );
                    nodes.add(cloudWorkstations);
                } else {
                    cloudWorkstations = null;
                }
                if (cloud.containsAny(macSwitchPorts)) {
                    cloudSwitches = new PhysicalCloud(idxCurrent, "Switches");
                    edges.addAll(cloud.stream()
                                    .filter(mac -> !macWorkstations.contains(mac))
                                    .map(mac -> new PhysicalEdge(cloudSwitches, nodeFromMac.get(mac)))
                                    .collect(Collectors.toList())
                    );
                    nodes.add(cloudSwitches);
                } else {
                    cloudSwitches = null;

                }
                //Connect clouds.
                if (cloudSwitches != null && cloudWorkstations != null) {
                    edges.add(new PhysicalEdge(cloudWorkstations, cloudSwitches));
                }
            }
        }

        //No more pending updates; update the UI
        if(Platform.isFxApplicationThread()) {
            graphPhysical.clearTopology();
            graphPhysical.addNodes(nodes);
            graphPhysical.addEdges(edges);
            graphPhysical.refresh();
        } else {
            Platform.runLater(() -> {
                // It is far easier to destroy and rebuild the graph than it is to change it since the structure will vary
                // wildly when previously-unknown infrastructure becomes known.
                graphPhysical.clearTopology();

                //Add the nodes first since bulk nodes don't check for duplicates, but edges will.
                graphPhysical.addNodes(nodes);
                graphPhysical.addEdges(edges);
                graphPhysical.refresh();
            });
        }
    }

    public void toXml(ZipOutputStream zos) throws IOException {
        zos.write("<physical_topology>".getBytes(StandardCharsets.UTF_8));
        for(PhysicalDevice device : devices) {
            device.toXml(zos);
        }
        zos.write("</physical_topology>".getBytes(StandardCharsets.UTF_8));
    }
}
