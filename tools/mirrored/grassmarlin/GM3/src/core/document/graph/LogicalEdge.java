package core.document.graph;

import core.document.serialization.xml.XmlElement;
import core.importmodule.ImportItem;
import javafx.beans.binding.When;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import ui.custom.fx.FxLongProperty;
import ui.custom.fx.FxObservableSet;
import ui.custom.fx.FxStringFromCollectionBinding;
import ui.custom.fx.FxStringProperty;
import util.Wireshark;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

public class LogicalEdge extends AbstractBidirectionalEdge<LogicalNode> {
    public static class ConnectionDetails {
        public static class FrameRecord {
            private final long frame;
            private final long time;
            private final int bytes;
            private final int protocol;
            private final int portSource;
            private final int portDestination;

            public FrameRecord(int portSource, int portDestination, int protocol, long frame, long time, int cntBytes) {
                this.portSource = portSource;
                this.portDestination = portDestination;
                this.protocol = protocol;
                this.frame = frame;
                this.time = time;
                this.bytes = cntBytes;
            }

            public long getFrame() {
                return frame;
            }
            public long getTime() {
                return time;
            }
            public int getBytes() {
                return bytes;
            }
            public int getProtocol() {
                return protocol;
            }
            public int getSourcePort() {
                return portSource;
            }
            public int getDestinationPort() {
                return portDestination;
            }

            public XmlElement toXml() {
                XmlElement xmlResult = new XmlElement("frame");
                xmlResult.addAttribute("time").setValue(Long.toString(time));
                xmlResult.addAttribute("bytes").setValue(Integer.toString(bytes));
                xmlResult.addAttribute("frame").setValue(Long.toString(frame));
                xmlResult.addAttribute("protocol").setValue(Integer.toString(protocol));
                xmlResult.addAttribute("srcPort").setValue(Integer.toString(portSource));
                xmlResult.addAttribute("dstPort").setValue(Integer.toString(portDestination));

                return xmlResult;
            }
        }
        private final SimpleLongProperty cntBytes;
        private final Map<ImportItem, List<FrameRecord>> Frames;
        private final FxObservableSet<Integer> Protocols;

        public ConnectionDetails() {
            cntBytes = new FxLongProperty(0);
            Frames = new HashMap<>();
            Protocols = new FxObservableSet<>();
        }

        public synchronized void AddPacket(ImportItem source, int portSource, int portDestination, long cntBytes, long idxFrame, int proto, long time) {
            this.cntBytes.set(this.cntBytes.get() + cntBytes);
            //The frames are partitioned by the ImportItem that was the source of the frame information.
            List<FrameRecord> frames = Frames.get(source);
            if(frames == null) {
                //Initial buffer for 1024 frames; completely arbitrary number, there is no science behind why it was chosen.
                frames = new ArrayList<>(1024);
                Frames.put(source, frames);
            }
            frames.add(new FrameRecord(portSource, portDestination, proto, idxFrame, time, (int) cntBytes)); //A single frame should not come anywhere near the size of an int, let alone a long.
            Protocols.add(proto);
        }

        public long getBytes() {
            return cntBytes.get();
        }
        public LongProperty bytesProperty() {
            return cntBytes;
        }
        public int getFrameCount() {
            int cntFrames = 0;
            for(List<?> frames : Frames.values()) {
                cntFrames += frames.size();
            }
            return cntFrames;
        }
        public Collection<ImportItem> getFrameGroups() {
            return Frames.keySet();
        }
        public List<FrameRecord> getFrames(ImportItem set) {
            return Frames.get(set);
        }
    }

    public LogicalEdge(LogicalNode source, LogicalNode destination) {
        super(source, destination);

        detailsDestinationToSource = new ConnectionDetails();
        detailsSourceToDestination = new ConnectionDetails();

        detailsEdge = new FxStringProperty();
        if(source == null || destination == null) {
            detailsEdge.setValue("Source or Destination was null");
        } else {
            detailsEdge.bind(
                    new When(detailsSourceToDestination.bytesProperty().greaterThan(0))
                            .then(detailsSourceToDestination.bytesProperty().asString().concat(" bytes -> " + destination.toString() + " ").concat(new FxStringFromCollectionBinding<>(detailsSourceToDestination.Protocols, Collectors.joining(", ", "(", ")\n"), Wireshark::getProtocolName)))
                            .otherwise("")
                            .concat(new When(detailsDestinationToSource.bytesProperty().greaterThan(0))
                                            .then(detailsDestinationToSource.bytesProperty().asString().concat(" bytes -> " + source.toString() + " ").concat(new FxStringFromCollectionBinding<>(detailsDestinationToSource.Protocols, Collectors.joining(", ", "(", ")\n"), Wireshark::getProtocolName)))
                                            .otherwise("")
                            ));
        }
    }

    protected final FxStringProperty detailsEdge;
    protected final ConnectionDetails detailsSourceToDestination;
    protected final ConnectionDetails detailsDestinationToSource;

    public void AddPacket(boolean sameDirection, int portSource, int portDest, int proto, long time, ImportItem source, long cntBytes, long idxFrame) {
        if(sameDirection) {
            detailsSourceToDestination.AddPacket(source, portSource, portDest, cntBytes, idxFrame, proto, time);
        } else {
            detailsDestinationToSource.AddPacket(source, portDest, portSource, cntBytes, idxFrame, proto, time);
        }
    }

    public ConnectionDetails getDetailsToSource() {
        return detailsDestinationToSource;
    }
    public ConnectionDetails getDetailsToDestination() {
        return detailsSourceToDestination;
    }

    public FxStringProperty edgeDetailsProperty() {
        return detailsEdge;
    }

    @Override
    public XmlElement toXml(List<LogicalNode> nodes, List<ImportItem> items, ZipOutputStream zos) throws IOException {
        XmlElement xmlEdge = super.toXml(nodes, items, zos);

        zos.write(xmlEdge.openTag().getBytes(StandardCharsets.UTF_8));
        zos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));

        //Details to Destination
        XmlElement xmlDetailsTo = new XmlElement("details").appendedTo(xmlEdge);
        xmlDetailsTo.addAttribute("direction").setValue("destination");
        xmlDetailsTo.addAttribute("bytes").setValue(Long.toString(getDetailsToDestination().getBytes()));

        zos.write(xmlDetailsTo.openTag().getBytes(StandardCharsets.UTF_8));
        zos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));

        for(ImportItem item : getDetailsToDestination().getFrameGroups()) {
            XmlElement xmlSource = new XmlElement("source");
            xmlSource.addAttribute("ref").setValue(Integer.toString(items.indexOf(item)));

            zos.write(xmlSource.openTag().getBytes(StandardCharsets.UTF_8));

            for(LogicalEdge.ConnectionDetails.FrameRecord frame : getDetailsToDestination().getFrames(item)) {
                zos.write(frame.toXml().toString().getBytes(StandardCharsets.UTF_8));
                zos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
            }

            zos.write(xmlSource.closeTag().getBytes(StandardCharsets.UTF_8));
            zos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
        }

        zos.write(xmlDetailsTo.closeTag().getBytes(StandardCharsets.UTF_8));
        zos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));

        //Details to Source
        XmlElement xmlDetailsFrom = new XmlElement("details").appendedTo(xmlEdge);
        xmlDetailsFrom.addAttribute("direction").setValue("source");
        xmlDetailsFrom.addAttribute("bytes").setValue(Long.toString(getDetailsToSource().getBytes()));

        zos.write(xmlDetailsFrom.openTag().getBytes(StandardCharsets.UTF_8));
        zos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));

        for(ImportItem item : getDetailsToSource().getFrameGroups()) {
            XmlElement xmlSource = new XmlElement("source").appendedTo(xmlDetailsFrom);
            xmlSource.addAttribute("ref").setValue(Integer.toString(items.indexOf(item)));

            zos.write(xmlSource.openTag().getBytes(StandardCharsets.UTF_8));

            for(LogicalEdge.ConnectionDetails.FrameRecord frame : getDetailsToDestination().getFrames(item)) {
                zos.write(frame.toXml().toString().getBytes(StandardCharsets.UTF_8));
                zos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
            }

            zos.write(xmlSource.closeTag().getBytes(StandardCharsets.UTF_8));
            zos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
        }

        zos.write(xmlDetailsFrom.closeTag().getBytes(StandardCharsets.UTF_8));
        zos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));

        zos.write(xmlEdge.closeTag().getBytes(StandardCharsets.UTF_8));
        zos.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));

        return null;
    }
}
