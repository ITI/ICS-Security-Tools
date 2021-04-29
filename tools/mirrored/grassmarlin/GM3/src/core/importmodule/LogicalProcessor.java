package core.importmodule;

import core.document.Session;
import core.document.graph.ComputedProperty;
import core.document.graph.IEdge;
import core.document.graph.LogicalNode;
import core.fingerprint.FProcessor;
import core.fingerprint.PacketData;
import core.fingerprint3.Fingerprint;
import util.Cidr;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Class representing the processing pipeline for Logical data
 */
public class LogicalProcessor {
    public static class Host {
        private final Cidr cidr;
        private final Map<String, String> properties;
        private final Object source;

        public Host(final Cidr cidr, final Map<String, String> properties, Object source) {
            this.cidr = cidr;
            this.properties = properties;
            this.source = source;
        }

        public Cidr getCidr() {
            return cidr;
        }
        public Map<String, String> getProperties() {
            return properties;
        }
        public Object getSource() {
            return source;
        }
    }

    private PacketData data;
    private final Supplier<List<Fingerprint>> fingerprints;
    private final Function<PacketData, IEdge<LogicalNode>> graph;
    private final List<Consumer<PacketData>> plugins;
    private final FProcessor processor;
    private final Session session;

    public LogicalProcessor(Session session, Supplier<List<Fingerprint>> fingerprints,
                            Function<PacketData, IEdge<LogicalNode>> graphFunction, Consumer<PacketData>... plugins) {
        this.session = session;
        this.fingerprints = fingerprints;
        this.graph = graphFunction;
        this.plugins = Arrays.asList(plugins);
        this.processor = new FProcessor(this.fingerprints.get());
    }

    // Process a packet
    public void process(PacketData data) {
        this.data = data;
        this.run();
    }

    //Process an endpoint
    public void process(Host host) {
        LogicalNode nodeNew = new LogicalNode(host.getCidr(), null, session.getLogicalGraph().getCidrList());
        // This will return the node in the graph if there is a conflict.
        nodeNew = session.getLogicalGraph().addNode(nodeNew);
        //Set the annotations after adding to the graph to ensure they are set on the correct node.
        for(Map.Entry<String, String> entry : host.getProperties().entrySet()) {
            nodeNew.addAnnotation(host.getSource() == null ? null : host.getSource().toString(), entry.getKey(), new ComputedProperty(entry.getValue(), 5));
        }
    }

    public void run() {
        long start = System.currentTimeMillis();
        IEdge<LogicalNode> edge = graph.apply(this.data);
        LogicalNode edgeSource = edge.getSource();
        LogicalNode edgeDestination = edge.getDestination();
        long end = System.currentTimeMillis();

        data.getSource().edgeTime.addAndGet(end - start);

        LogicalNode dataSource = edgeSource.getIp().equals(data.getSourceIp()) ? edgeSource : edgeDestination;
        LogicalNode dataDestination = edgeDestination.getIp().equals(data.getDestIp()) ? edgeDestination : edgeSource;

        this.data.setSourceNode(dataSource);
        this.data.setDestNode(dataDestination);

        start = System.currentTimeMillis();
        this.processor.process(data);
        end = System.currentTimeMillis();

        data.getSource().fpTime.addAndGet(end - start);

        plugins.forEach(con -> con.accept(data));

        start = System.currentTimeMillis();
        this.data.getSource().recordTaskProgress(data.getCompletionUnits());
        end = System.currentTimeMillis();

        data.getSource().otherTime.addAndGet(end - start);
    }
}
