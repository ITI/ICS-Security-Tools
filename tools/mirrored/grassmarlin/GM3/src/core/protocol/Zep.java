package core.protocol;

import org.jnetpcap.protocol.tcpip.Udp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * Created by BESTDOG on 12/9/2015.
 * <p>
 * ZigBee Encapsulation Protocol. The accessor are meant to be "at will"
 * which means the actual fields of the V1, v2-data, and v2-ACK packets
 * will be determined on request. This isn't very efficient but
 * our model relies on not recording all data - so if speed is an issue this class should be cut up into more granular
 * entities.
 *
 *
 *
 * More on ZigBee,
 * ZigBee Devices may be classified in three categories,
 * 1. Coordinator, a device full communication functionality. These devices may form a network root device (to bridge networks),
 * All networks MUST have one coordinator. It acts ass the trust center for the network.
 *
 * 2. Router, running as high as the application layer, is an intermediate bridge between other devices participating in the
 * Pan.
 *
 * 3. End Devices, may function below the application layer, need only communicate on the layer required to talk to
 * its parent device, either a Router or Coordinator.
 */
public class Zep {

    final static int ZEP_PORTS = 17754;
    final static String PREAMBLE = "EX";
    final static int DATA_LENGTH_MASK = 0x7F;

    final static int MODE_CRC = 1;
    final static int TYPE_ACK = 2;
    final static int TYPE_DATA = 1;
    final static int LENGTH_v1 = 16;
    final static int LENGTH_v2 = 32;
    int version;
    boolean v1;
    boolean data;
    private ByteBuffer buffer;
    private BufferAccessor accessor;

    public Zep() {

    }

    public static boolean isZEPProtocol(Udp udp) {
        return udp.source() == ZEP_PORTS && udp.destination() == ZEP_PORTS;
    }

    public static boolean isZEPProtocol(int srcPort, int dstPort) { return srcPort == ZEP_PORTS && dstPort == ZEP_PORTS; }

    public boolean hasProtocol(Udp udp) {
        boolean match = Zep.isZEPProtocol(udp);
        if (match) {
            this.buffer = ByteBuffer.allocate(udp.getPayloadLength());
            udp.transferPayloadTo(this.buffer);
            this.buffer.order(ByteOrder.LITTLE_ENDIAN);
            this.accessor = new BufferAccessor(this.buffer);
            this.version = this.accessor.getByte(FIELDS.VERSION);
            this.v1 = this.version == 1;
            if (!v1) {
                data = this.getType() != TYPE_ACK; // all packets are data unless they are ACK packets.
            } else {
                data = false;
            }
        }
        return match;
    }

    public void fromArray(byte[] buffer) {
        this.buffer = ByteBuffer.wrap(buffer);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.accessor = new BufferAccessor(this.buffer);
        this.version = this.accessor.getByte(FIELDS.VERSION);
        this.v1 = this.version == 1;
        if (!v1) {
            data = this.getType() != TYPE_ACK;
        } else {
            data = false;
        }
    }

    public int getChannelID() {
        return v1 ? this.accessor.getByte(FIELDS.CHANNEL_ID_v1) : this.accessor.getByte(FIELDS.CHANNEL_ID_v2_d);
    }

    public int getType() {
        return v1 ? 0 : this.accessor.getByte(FIELDS.TYPE_v2);
    }

    public String getDestinationDeviceID() {
        byte addressFormat = this.accessor.getByte(FIELDS.ADDRESS_FORMAT);

        if(lookupAddressFormats.containsKey(addressFormat)) {
            return lookupAddressFormats.get(addressFormat).getDestinationAddress(this.accessor);
        } else {
            return null;
        }    }

    public String getSourceDeviceID() {
        byte addressFormat = this.accessor.getByte(FIELDS.ADDRESS_FORMAT);

        if(lookupAddressFormats.containsKey(addressFormat)) {
            return lookupAddressFormats.get(addressFormat).getSourceAddress(this.accessor);
        } else {
            return null;
        }
    }

    public int getMode() {
        return v1 ? this.accessor.getByte(FIELDS.CRC_LQI_MODE_v1) : this.accessor.getByte(FIELDS.CRC_LQI_MODE_v2_d);
    }

    public int getLqiValue() {
        return v1 ? this.accessor.getByte(FIELDS.LQI_VAL_v1) : this.accessor.getByte(FIELDS.LQI_VAL_v2_d);
    }

    public byte[] getReserved() {
        return v1 ? this.accessor.getBytes(FIELDS.RESERVED_v1) : this.accessor.getBytes(FIELDS.RESERVED_v2_d);
    }

    public int getLength() {
        return v1 ? this.accessor.getByte(FIELDS.LENGTH_v1) : this.accessor.getByte(FIELDS.LENGTH_v2_d);
    }

    public int getSeq() {
        return data ? this.accessor.getInt(FIELDS.SEQ_NUMBER_v2_d) : this.accessor.getInt(FIELDS.SEQ_NUMBER_v2_a);
    }

    public long getNTPTimeStamp() {
        return v1 ? 0 : data ? this.accessor.getLong(FIELDS.NTP_TIMESTAMP_v2_d) : 0;
    }

    public int getVersion() {
        return this.version;
    }

    public boolean isProtocol(Udp udp) {
        return Zep.isZEPProtocol(udp);
    }

    public String getPreamble() {
        byte[] bytes = new byte[FIELDS.PREAMBLE.findLength(this.buffer)];
        this.buffer.rewind();
        this.buffer.get(bytes);
        return new String(bytes);
    }

    private String format(long s) {
        /** requires static method org.apache.commons.net.ntp.TimeStamp#toString(long ntpTime) from Apache Commons-NET */
        return "No Format";
    }

    /**
     * @return ByteBuffer of the bytes located in the payload of this protocol.
     */
    public ByteBuffer getNextBuffer() {
        int length = v1 ? LENGTH_v1 : LENGTH_v2;
        this.buffer.rewind();
        ByteBuffer temp = this.buffer;
        ByteBuffer dest = ByteBuffer.allocate(temp.remaining());
        temp.position(length);
        dest.put(temp);
        return dest;
    }

    @Override
    public String toString() {
        String string;
        boolean crc = this.getMode() == MODE_CRC;
        if (v1) {
            string = String.format(
                    "ZEP Version: %d, Channel ID: %d, Device ID: %s, Mode: %s, Length: %d",
                    this.getVersion(),
                    this.getChannelID(),
                    this.getDestinationDeviceID(),
                    crc ? "CRC" : "LQI",
                    this.getLength()
            );
        } else {
            if (data) {
                string = String.format(
                        "ZEP Version: %d, Type: %s, Channel ID: %d, Device ID: %s, Mode: %s, TS: %s, Seq: %d, Length: %d",
                        this.getVersion(),
                        "data",
                        this.getChannelID(),
                        this.getDestinationDeviceID(),
                        crc ? "CRC" : "LQI",
                        format(this.getNTPTimeStamp()),
                        this.getSeq(),
                        this.getLength()
                );
            } else {
                string = String.format(
                        "ZEP Version: %d, Type: %s, Seq: %d",
                        this.getVersion(),
                        "ack",
                        this.getSeq()
                );
            }
        }
        return string;
    }

    private static Map<Byte, AddressFormats> lookupAddressFormats = new HashMap<Byte, AddressFormats>() {{
        put((byte)-36, AddressFormats.LONG_DEST_LONG_SOURCE);
        put((byte)-52, AddressFormats.LONG_DEST_LONG_SOURCE);
        put((byte)-40, AddressFormats.SHORT_DEST_LONG_SOURCE);
        put((byte)-56, AddressFormats.SHORT_DEST_LONG_SOURCE);
        put((byte)-100, AddressFormats.LONG_DEST_SHORT_SOURCE);
        put((byte)-116, AddressFormats.LONG_DEST_SHORT_SOURCE);
        put((byte)-104, AddressFormats.SHORT_DEST_SHORT_SOURCE);
        put((byte)-120, AddressFormats.SHORT_DEST_SHORT_SOURCE);
    }};

    private enum AddressFormats {
        SHORT_DEST_LONG_SOURCE(FIELDS.DESTINATION_DEVICE_NAME_C8, FIELDS.SOURCE_DEVICE_NAME_C8),
        LONG_DEST_SHORT_SOURCE(FIELDS.DESTINATION_DEVICE_NAME_8C, FIELDS.SOURCE_DEVICE_NAME_8C),
        SHORT_DEST_SHORT_SOURCE(FIELDS.DESTINATION_DEVICE_NAME_88, FIELDS.SOURCE_DEVICE_NAME_88),
        LONG_DEST_LONG_SOURCE(FIELDS.DESTINATION_DEVICE_NAME_CC, FIELDS.SOURCE_DEVICE_NAME_CC);


        private FieldDescriptor locSource;
        private FieldDescriptor locDestination;

        AddressFormats(FieldDescriptor locDestination, FieldDescriptor locSource) {
            this.locSource = locSource;
            this.locDestination = locDestination;
        }

        public String getSourceAddress(BufferAccessor accessor) {
            if(locSource.findLength(accessor.getBuffer()) == 2) {
                return accessor.getHex(locSource);
            } else {
                return accessor.getHex(locSource, 2, ":");
            }
        }

        public String getDestinationAddress(BufferAccessor accessor) {
            if(locDestination.findLength(accessor.getBuffer()) == 2) {
                return accessor.getHex(locDestination);
            } else {
                return accessor.getHex(locDestination, 2, ":");
            }
        }
    }

    private enum FIELDS implements FieldDescriptor {
        PREAMBLE(0, 2),
        VERSION(2, 1),
        /**
         * v1 fields
         */
        CHANNEL_ID_v1(3, 1),
        DEVICE_ID_v1(4, 2),
        CRC_LQI_MODE_v1(6, 1),
        LQI_VAL_v1(7, 1),
        RESERVED_v1(8, 7),
        LENGTH_v1(15, 1),
        /**
         * v2 fields
         */
        TYPE_v2(3, 1),
        /**
         * v2 data fields
         */
        CHANNEL_ID_v2_d(4, 1),
        DEVICE_ID_v2_d(5, 2),

        DESTINATION_DEVICE_NAME_8C(37, 8),
        SOURCE_DEVICE_NAME_8C(45, 2),
        DESTINATION_DEVICE_NAME_C8(37, 2),
        SOURCE_DEVICE_NAME_C8(39, 8),
        DESTINATION_DEVICE_NAME_88(37, 2),
        SOURCE_DEVICE_NAME_88(39, 2),
        DESTINATION_DEVICE_NAME_CC(37, 8),
        SOURCE_DEVICE_NAME_CC(45, 8),

        CRC_LQI_MODE_v2_d(7, 1),
        LQI_VAL_v2_d(8, 1),
        NTP_TIMESTAMP_v2_d(9, 8),
        SEQ_NUMBER_v2_d(17, 4),
        RESERVED_v2_d(21, 10),
        LENGTH_v2_d(31, 1),
        ADDRESS_FORMAT(33, 1),
        /**
         * v2 ack fields
         */
        SEQ_NUMBER_v2_a(5, 4);

        private ToIntFunction<ByteBuffer> calcOffset;
        private ToIntFunction<ByteBuffer> calcLength;

        FIELDS(int offset, int length) {
            this(buffer -> {
                return offset;
            }, buffer -> {
                return length;
            });
        }

        FIELDS (ToIntFunction<ByteBuffer> calcOffset, ToIntFunction<ByteBuffer> calcLength) {
            this.calcOffset = calcOffset;
            this.calcLength = calcLength;
        }

        public int findOffset(ByteBuffer buffer) {
            return this.calcOffset.applyAsInt(buffer);
        }

        public int findLength(ByteBuffer buffer) {
            return this.calcLength.applyAsInt(buffer);
        }
    }

}
