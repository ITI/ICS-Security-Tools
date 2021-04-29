package ui.fingerprint.filters;


import javafx.beans.property.ObjectProperty;
import javafx.scene.layout.HBox;

import javax.xml.bind.JAXBElement;

/**
 *
 * @param <T> The Type of the JAXBElement created by this Filter
 */
public interface Filter<T> {

    default HBox getInput(){
        return new HBox();
    }

    FilterType getType();

    ObjectProperty<JAXBElement<T>> elementProperty();

    enum PacketType {
        TCP,
        UDP,
        ANY,
        OTHER
    }

    enum FilterType {
        ACK("Ack", "Accepts TCP packets with the ACK flag set. \n" +
                "This will force the TransportProtocol to only check TCP packets.",
                Filter.PacketType.TCP, AckFilter.class),
        MSS("MSS", "Accepts a MSS value for TCP packets with the MSS optional flag set. \n" +
                "This will force the TransportProtocol to only check TCP packets. ",
                Filter.PacketType.TCP, MssFilter.class),
        DSIZE("Dsize", "Accepts a packet with a payload size equal to a number of bytess. \n" +
                "This does not include the size of a packet header. ",
                Filter.PacketType.ANY, DsizeFilter.class),
        DSIZEWITHIN("DsizeWithin", "Accepts a packet with a payload size within a range of bytes(s). \n" +
                "This does not include the size of a packet header. Dsize must contain one to two inequalities. ",
                Filter.PacketType.ANY, DsizeWithinFilter.class),
        DSTPORT("DstPort", "Accepts the destination port of a TCP or UDP packet. ",
                Filter.PacketType.ANY, DestPortFilter.class),
        SRCPORT("SrcPort", "Accepts the source port of a TCP or UDP packet. ",
                Filter.PacketType.ANY, SourcePortFilter.class),
        ETHERTYPE("Ethertype", "Accepts the Ethertype of an ethernet frame. [2048] = IPv4",
                Filter.PacketType.OTHER, EthertypeFilter.class),
        FLAGS("Flags", "This Filter will check for the presence of TCP flags. \n" +
                "This will force the TransportProtocol to only check TCP packets.",
                Filter.PacketType.TCP, FlagsFilter.class),
        SEQ("Seq", "Accepts TCP packets which contain a SEQ field equal to the indicated value. \n" +
                "This will force the TransportProtocol to only check TCP packets. ",
                Filter.PacketType.TCP, SeqFilter.class),
        TRANSPORTPROTOCOL("TransportProtocol", "Accepts the protocol number of a packet by the assigned Internet Protocol Numbers. \n" +
                "GM only supports IPv4, UDP and TCP protocols. It is not suggested to use values other than TCP(6) and UDP(17). ",
                Filter.PacketType.OTHER, TransportProtoFilter.class),
        TTL("TTL", "Accepts TCP packets which contain a TTL(Time To Live) field equal to a value",
                Filter.PacketType.ANY, TtlFilter.class),
        TTLWITHIN("TTLWithin", "Accepts TCP packets which contain a TTL(Time To Live) field within a range of value(s)",
                Filter.PacketType.ANY, TtlWithinFilter.class),
        WINDOW("Window", "Accepts a TCP packet which contains a Window Size field equal to the indicated value. \n" +
                "This will force the TransportProtocol to only check TCP packets.",
                Filter.PacketType.TCP, WindowFilter.class);

        private String name;
        private String tooltip;
        private Filter.PacketType packetType;
        Class<? extends Filter> implementingClass;

        FilterType(String name, String tooltip, Filter.PacketType packetType, Class<? extends Filter> implementingClass) {
            this.name = name;
            this.tooltip = tooltip;
            this.packetType = packetType;
            this.implementingClass = implementingClass;
        }

        public String getName() {
            return this.name;
        }

        public String getTooltip() {
            return this.tooltip;
        }

        public Filter.PacketType getPacketType() {
            return this.packetType;
        }

        public Class<? extends Filter> getImplementingClass() {
            return this.implementingClass;
        }

    }
}
