package core.importmodule;

import core.document.PhysicalDevice;
import core.document.Session;
import core.document.graph.*;
import core.document.serialization.xml.XmlElement;
import core.exec.IEEE802154Data;
import core.fingerprint.PacketData;
import core.fingerprint3.Fingerprint;
import ui.GrassMarlinFx;
import util.Cidr;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.ZipOutputStream;

/**
 * We use this as an opportunity to add the session to a task.
 *
 * The session should be the access to the end point storage and the
 * fingerprinting framework.
 */
public class TaskDispatcher {
    private final ExecutorService wexec;
    private final ExecutorService lexec;
    private final BlockingQueue<Runnable> logicalQueue;
    private final Session session;
    private final CopyOnWriteArrayList<Iterator<?>> logicalIterators;
    private final CopyOnWriteArrayList<Iterator<PhysicalDevice>> physicalIterators;
    private final CopyOnWriteArrayList<Iterator<?>> meshIterators;
    private final LogicalEdgeFactory factoryLogicalEdges;
    private final MeshEdgeFactory factoryMeshEdges;
    private final Map<Iterator<?>, ImportItem> sourceFromIter;
    private final ThreadLocal<LogicalProcessor> proc;
    private final ThreadLocal<MeshProcessor> meshProc;

    private boolean shutdown;
    private boolean running;

    /**
     * @param session Reference to the session provided to all Task object
     * that will run through this TaskDispatcher.
     */
    public TaskDispatcher(final int cntThreads, final Session session) {
        wexec = Executors.newCachedThreadPool();
        logicalQueue = new ArrayBlockingQueue<>(1000);
        lexec = new ThreadPoolExecutor(1, cntThreads, 30, TimeUnit.SECONDS, logicalQueue, new ThreadPoolExecutor.AbortPolicy());
        this.session = session;
        this.factoryLogicalEdges = new LogicalEdgeFactory(session);
        this.factoryMeshEdges = new MeshEdgeFactory();

        this.sourceFromIter = new HashMap<>();

        this.logicalIterators = new CopyOnWriteArrayList<>();
        this.physicalIterators = new CopyOnWriteArrayList<>();
        this.meshIterators = new CopyOnWriteArrayList<>();
        shutdown = false;
        running = false;

        proc = new ThreadLocalLogicalProcessor(session, GrassMarlinFx::getRunningFingerprints, factoryLogicalEdges);
        meshProc = new ThreadLocalMeshProcessor(session, factoryMeshEdges);
    }

    public boolean isRunning() {
        return this.running;
    }

    public boolean isShutdown() {
        return this.shutdown;
    }

    @SuppressWarnings("unchecked")
    public void accept(final ImportItem item) {
        Iterator<?> iterLogical = item.getIterator(Pipeline.LOGICAL);
        if(iterLogical != null) {
            logicalIterators.add(iterLogical);
            sourceFromIter.put(iterLogical, item);
        }
        Iterator<?> iterPhysical = item.getIterator(Pipeline.PHYSICAL);
        if(iterPhysical != null) {
            physicalIterators.add((Iterator<PhysicalDevice>)iterPhysical);
        }
    }

    public void run() {
        wexec.execute(this::startLogical);
        wexec.execute(this::startPhysical);
        running = true;
    }

    public void shutdown() {
        this.shutdown = true;
    }

    //<editor-fold defaultstate="collapsed" desc="Logical Graph">
    private void startLogical() {
        final List<Iterator> doneList = new LinkedList<>();

        final AtomicInteger pendingLogicalTasks = new AtomicInteger(0);
        boolean needsRefresh = false;

        boolean run;
        PacketData packetData;
        IEEE802154Data meshData;
        boolean allNulls;
        while(!shutdown) {
            allNulls = true;
            if (logicalIterators.isEmpty() && meshIterators.isEmpty()) {
                if(needsRefresh && pendingLogicalTasks.get() == 0) {
                    this.session.getLogicalGraph().refresh();
                    this.session.getMeshGraph().refresh();
                    needsRefresh = false;
                }

                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // don't care
                }
            }
            for(Iterator<?> iterator : logicalIterators) {
                if (iterator.hasNext()) {
                    Object next = iterator.next();
                    if (next instanceof PacketData) {
                        packetData = (PacketData)next;

                        if (packetData != null) {
                            run = false;
                            allNulls = false;
                            final ProcThread proc = new ProcThread(packetData, pendingLogicalTasks);
                            needsRefresh = true;
                            while (!run) {
                                try {
                                    lexec.execute(proc);
                                    run = true;
                                } catch (RejectedExecutionException e) {
                                    run = false;
                                    try {
                                        Thread.sleep(1);
                                    } catch (InterruptedException ie) {
                                        // don't care
                                    }
                                }
                            }
                        }
                    } else if (next instanceof IEEE802154Data) {
                        meshData = (IEEE802154Data)next;

                        if (meshData != null) {
                            run = false;
                            allNulls = false;
                            final MeshProcThread proc = new MeshProcThread(meshData, pendingLogicalTasks);
                            needsRefresh = true;
                            while (!run) {
                                try {
                                    lexec.execute(proc);
                                    run = true;
                                } catch (RejectedExecutionException e) {
                                    run = false;
                                    try {
                                        Thread.sleep(1);
                                    } catch (InterruptedException ie) {
                                        // don't care
                                    }
                                }
                            }
                        }
                    } else if (next instanceof LogicalProcessor.Host) {
                        //The Host code is a bit of a hack.  It was never expected that we would build the LogicalGraph
                        // from anything other than packet data/metadata, but to provide the example CSV plugin
                        // implementation, we felt that it was better to offer a simple data format rather than one as
                        // complex as the PacketData class.
                        //Also, it was easier to add this than to add proper PacketData support to the CSV parser, and
                        // as this was added in the week before the 3.2 release, I didn't feel like taking the more
                        // ambitious approach.
                        LogicalProcessor.Host host = (LogicalProcessor.Host)next;
                        if(host != null) {
                            run = false;
                            allNulls = false;
                            final HostThread proc = new HostThread(host);
                            needsRefresh = true;
                            while(!run) {
                                try {
                                    lexec.execute(proc);
                                    run = true;
                                } catch(RejectedExecutionException e) {
                                    run = false;
                                    try {
                                        Thread.sleep(1);
                                    } catch(InterruptedException ie) {
                                        // Ignore
                                    }
                                }
                            }
                        }
                    }
                } else {
                    run = false;
                    while(!run) {
                        try {
                            lexec.execute(() -> {
                                sourceFromIter.get(iterator).recordTaskCompletion();
                            });
                            doneList.add(iterator);
                            run = true;
                        } catch (RejectedExecutionException re) {
                            run = false;
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException ie) {
                                // don't care
                            }
                        }
                    }
                }
            }

            this.logicalIterators.removeAll(doneList);
            doneList.clear();


            if (allNulls) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private class ProcThread implements Runnable {
        private final PacketData data;
        private final AtomicInteger counter;

        public ProcThread(final PacketData data, final AtomicInteger counter) {
            this.data = data;
            this.counter = counter;
            counter.incrementAndGet();
        }

        @Override
        public void run() {
            try {
                proc.get().process(data);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                counter.decrementAndGet();
            }
        }
    }

    private class HostThread implements Runnable {
        private final LogicalProcessor.Host data;

        public HostThread(final LogicalProcessor.Host host) {
            this.data = host;
        }

        @Override
        public void run() {
            try {
                proc.get().process(data);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private class MeshProcThread implements Runnable {

        private final IEEE802154Data data;
        private final AtomicInteger counter;

        public MeshProcThread(final IEEE802154Data data, final AtomicInteger counter) {
            this.data = data;
            this.counter = counter;
            counter.incrementAndGet();
        }

        @Override
        public void run() {
            try {
                meshProc.get().process(data);
            } catch(Exception ex) {
                ex.printStackTrace();
            } finally {
                counter.decrementAndGet();
            }
        }
    }

    private static class ThreadLocalLogicalProcessor extends ThreadLocal<LogicalProcessor>{
        private final Session session;
        private final Supplier<List<Fingerprint>> fingerprints;
        private final Function<PacketData, IEdge<LogicalNode>> graphFunction;
        private final Consumer<PacketData>[] plugins;

        public ThreadLocalLogicalProcessor(Session session, Supplier<List<Fingerprint>> fingerprints,
                                           Function<PacketData, IEdge<LogicalNode>> graphFunction, Consumer<PacketData>... plugins) {
            super();

            this.session = session;
            this.fingerprints = fingerprints;
            this.graphFunction = graphFunction;
            this.plugins = plugins;
        }

        @Override
        protected LogicalProcessor initialValue() {
            return new LogicalProcessor(session, fingerprints, graphFunction, plugins);
        }
    }

    private static class ThreadLocalMeshProcessor extends ThreadLocal<MeshProcessor> {

        private final Session session;
        private final Function<IEEE802154Data, IEdge<MeshNode>> graphFunction;

        public ThreadLocalMeshProcessor(Session session, Function<IEEE802154Data, IEdge<MeshNode>> graphFunction) {
            this.session = session;
            this.graphFunction = graphFunction;
        }

        @Override
        protected MeshProcessor initialValue() {
            return new MeshProcessor(session, graphFunction);
        }
    }

    private class MeshEdgeFactory implements Function<IEEE802154Data, IEdge<MeshNode>> {

        @Override
        public IEdge<MeshNode> apply(IEEE802154Data data) {
            int sourcePan = data.isIntraPan() ? data.getTargetPan() : -1;
            MeshEdge edgeNew = new MeshEdge(
                    new MeshNode(data.getsDevice(), sourcePan),    //Default PAN ID; not necessarily right?
                    new MeshNode(data.getTDevice(), data.getTargetPan())
            );

            return TaskDispatcher.this.session.getMeshGraph().addEdge(edgeNew);
        }
    }

    private static class LogicalEdgeFactory implements Function<PacketData, IEdge<LogicalNode>> {

        /**
         * This is a horrible hack of a class aimed at reducing the memory churn associated with detecting edges.
         * We only need to find the edge entry in the HashMap with this, so we just need to track the IPs (for .equals) and the hash code.
         * From the perspective of a HashMap, it must be indistinguishable from a LogicalEdge (effectively AbstractBidirectionalEdge).
         * This class relies on the fact that a HashMap lookup will take an object (o) and evaluate o.equals(k) against every key value (k) in the HashMap, rather than evaluating k.equals(o).
         */
        private static class LogicalEdgePlaceholder implements IEdge<LogicalNode>{
            private Cidr source;
            private Cidr target;

            public LogicalEdgePlaceholder() {
                this.source = null;
                this.target = null;
            }

            public void setEndpoints(Cidr source, Cidr target) {
                this.source = source;
                this.target = target;
            }

            // == IEdge -- To meet the requirements for findEdge, not actually necessary

            public LogicalNode getSource() {
                return null;
            }
            public LogicalNode getDestination() {
                return null;
            }
            public void setSource(LogicalNode source) { }
            public void setDestination(LogicalNode destination) { }

            @Override
            public XmlElement toXml(List<LogicalNode> nodes, List<ImportItem> items,  ZipOutputStream zos) {
                return null;
            }

            // == Object

            @Override
            public int hashCode() {
                return source.hashCode() ^ target.hashCode();
            }

            @Override
            public boolean equals(Object other) {
                if(other instanceof LogicalEdge) {
                    LogicalEdge edge = (LogicalEdge)other;
                    return edge.getSource().getIp().equals(source) && edge.getDestination().getIp().equals(target);
                } else if(other instanceof LogicalEdgePlaceholder) {
                    return source.equals(((LogicalEdgePlaceholder)other).source) && target.equals(((LogicalEdgePlaceholder)other).target);
                }
                return false;
            }
        }

        private final Session session;
        private final ThreadLocal<LogicalEdgePlaceholder> edgePlaceholder;

        public LogicalEdgeFactory(final Session session) {
            this.session = session;
            this.edgePlaceholder = new ThreadLocal<LogicalEdgePlaceholder>() {
                @Override
                protected LogicalEdgePlaceholder initialValue() {
                    return new LogicalEdgePlaceholder();
                }
            };
        }

        public IEdge<LogicalNode> apply(PacketData packet) {
            final LogicalEdgePlaceholder edge = edgePlaceholder.get();
            edge.setEndpoints(packet.getSourceIp(), packet.getDestIp());
            LogicalEdge edgeExisting = session.getLogicalGraph().findMatchingEdge(edge);

            if (edgeExisting != null) {
                edgeExisting.AddPacket(edgeExisting.getSource().getIp().equals(packet.getSourceIp()), packet.getSourcePort(), packet.getDestPort(), packet.getTransportProtocol(), packet.getTime(), packet.getSource(), packet.getdSize(), packet.getFrame());
            } else {
                final LogicalNode nodeSource = new LogicalNode(packet.getSourceIp(), packet.getSourceMac(), session.getLogicalGraph().getCidrList());
                final LogicalNode nodeDestination = new LogicalNode(packet.getDestIp(), packet.getDestMac(), session.getLogicalGraph().getCidrList());
                edgeExisting = new LogicalEdge(nodeSource, nodeDestination);
                //addEdge will return either the edge passed to it or the edge representing the same connection, if one already exists.
                //Since it might not be the same ordering (edges are bidirectional), the direction needs to be checked for the first parameter.
                edgeExisting = session.getLogicalGraph().addEdge(edgeExisting);
                edgeExisting.AddPacket(edgeExisting.getSource().equals(nodeSource), packet.getSourcePort(), packet.getDestPort(), packet.getTransportProtocol(), packet.getTime(), packet.getSource(), packet.getdSize(), packet.getFrame());
            }
            return edgeExisting;
        }
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Physical Graph">
    private void startPhysical() {
        final List<Iterator<PhysicalDevice>> doneList = new LinkedList<>();
        final List<PhysicalDevice> devicesPending = new LinkedList<>();
        while(!shutdown) {
            //We can sleep every iteration since the number of devices is relatively low; the overhead is minimal.
            try {
                Thread.sleep(1);
            } catch(InterruptedException ex) {
                //Ignore interruption; this is just to keep the thread from hogging CPU time.
            }

            if(physicalIterators.isEmpty()) {
                if(!devicesPending.isEmpty()) {
                    session.getPhysicalTopologyMapper().getDevices().addAll(devicesPending);
                    devicesPending.clear();
                }
                continue;
            }

            for(Iterator<PhysicalDevice> iterator : physicalIterators) {
                if(iterator.hasNext()) {
                    PhysicalDevice dev = iterator.next();
                    if(dev != null) {
                        devicesPending.add(dev);
                    }
                } else {
                    doneList.add(iterator);
                }
            }

            this.physicalIterators.removeAll(doneList);
            doneList.clear();
        }
    }
    //</editor-fold>
}
