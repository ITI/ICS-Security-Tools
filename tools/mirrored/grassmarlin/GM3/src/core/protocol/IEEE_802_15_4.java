package core.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by BESTDOG on 12/9/2015.
 *
 * Based on packet-ieee802154.c by pdemil - wireshark.
 *
 * This protocol specifies the physical layer and media access controls for wireless personal area networsks. (PANs)
 *
 * Specifically low-rate PANs, hence "LoPAN" is keyed.
 *
 * There are physical classifications of devices in a LoPAN network,
 * 1. Full Function Devices (FFD) these are "smart" devices which can do general communication tasks and participate in
 * the network as a coordinator.
 * 2. Reduced Function Devices (RFD) these are "simple" devices that only operate with a lean communication stack
 * and can never be a coordinator. Note these may not always be leaf devices but will always communicate with 1 or more FFDs.
 *
 * There are many network configurations for LoPAN networks; personally I classify these as follows,
 * 1. Structured - This configuration consists of one Network coordinator and at least one or more Pan coordinator, this
 * us where all traffic is commanded through the network coordinator.
 *
 * These can always be classified as 'tree structured' without any cyclic communication following these descending rules,
 * Parent -> possible children
 * NC     -> PC, NPC, RFD
 * PC     -> PC, NPC, RFD
 * NPC    -> RFD
 *
 * 2. P2P Mesh - Point to point networks where there must be at least two or more Pan Coordinators and one or more other
 * device, possibly including a RFD.
 *
 * These can always be classified as 'mesh structured', following the same connectivity rules as the Structured network
 * but without a NC, and cyclic communication patterns may exist.
 *
 * <BLOCKQUOTE>
 * <pre>
 * 802.15.4 Additional Information
 *  802.15.4 will be referred to as 802x
 *  802x is a IEEE standard for wireless communication for low powered devices.
 *  The specification allows transmition over the 868-868.8MHz, 902-928MHz, and 2.4-2.4835GHz ISM bands.
 *
 *  Specification explains two network configurations,
 *  Point to Point, where each node talks through a network coordinator.
 *  Point to Multi-Point, where each node may talk to any other node or through a network coordinator.
 *
 *  ZigBee Note:
 *      ZigBee features, P2P, P2MP, and peer-to-peer
 *      peer-to-peer, not to be confused with P2P in this context, is a network configuration where there is no
 *      designated coordinator device to structure or pass through messages, all devices participate independently.
 * </pre>
 * </BLOCKQUOTE>
 *
 * For more information see RFC4944.
 */
public class IEEE_802_15_4 {

    private ByteBuffer buffer;
    private BufferAccessor accessor;
    private FCF fcf;
    private boolean HAS_PID;
    private boolean intraPan;

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.buffer.rewind();
        fcf = new FCF(this.buffer);
        HAS_PID = fcf.hasPanId();
        intraPan = fcf.isIntraPan();
        this.accessor = new BufferAccessor(this.buffer);
    }

    public FCF getFCF() {
        return fcf;
    }

    public int getSequence() {
        return this.accessor.getByte(FIELDS.SEQ_NUMBER);
    }

    public int getDestinationPanId() {
        return HAS_PID ? this.accessor.getShort(FIELDS.DEST_PAN) & 0x0000FFFF : 0;
    }

    public int getDestinationDeviceId() {
        return (HAS_PID ? this.accessor.getShort(FIELDS.DEST_DEV_ID_W_PID) : this.accessor.getShort(FIELDS.DEST_DEV_ID_NO_PID)) & 0x0000FFFF;
    }

    public int getSourceDeviceId() {
        return (HAS_PID ? this.accessor.getShort(FIELDS.SRC_DEV_ID_W_PID) : this.accessor.getShort(FIELDS.SRC_DEV_ID_NO_PID)) & 0x0000FFFF;
    }

    public boolean isIntraPan() {
        return this.intraPan;
    }

    @Override
    public String toString() {
        return String.format(
                "IEEE802.15.4 FCF: %s ",
                this.getFCF()
        );
    }

    private enum FIELDS implements FieldDescriptor {
        FCF(0, 2),
        SEQ_NUMBER(2, 1),
        DEST_PAN(3, 2),
        DEST_DEV_ID_W_PID(5, 2),
        DEST_DEV_ID_NO_PID(3, 2),
        SRC_DEV_ID_W_PID(7, 2),
        SRC_DEV_ID_NO_PID(5, 2)
        ;
        public final int offset;
        public final int length;

        FIELDS(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }

        public int findOffset(ByteBuffer buffer) {
            return offset;
        }

        public int findLength(ByteBuffer buffer) {
            return length;
        }

    }

    private static class FCF {
        static final int MASK_TYPE      = 0b0000_0000_0000_0011;
        static final int MASK_SEC_EN    = 0b0000_0000_0000_1000;
        static final int MASK_FRAME_PND = 0b0000_0000_0001_0000;
        static final int MASK_ACK_REQ   = 0b0000_0000_0010_0000;
        static final int MASK_INTRA_PAN = 0b0000_0000_0100_0000; /** AKA Pan ID COMPRESSION */
        static final int MASK_ADDR_DST  = 0b0000_1100_0000_0000;
        static final int MASK_VERSION   = 0b0011_0000_0000_0000;
        static final int MASK_ADDR_SRC  = 0b1100_0000_0000_0000;

        static final int IEEE802154_FCF_ADDR_SHORT = 2;
        static final int IEEE802154_FCF_ADDR_EXT = 3;

        static final String[] TYPES = {"Beacon", "Data", "ACK", "CMD", "Error"};
        static final int TYPE_BEACON = 0;
        static final int TYPE_DATA = 1;
        static final int TYPE_ACK = 2;
        static final int TYPE_CMD = 3;
        static final int TYPE_ERROR = 4;

        final byte[] bytes;
        final int val;

        public FCF(ByteBuffer buffer) {
            int t = buffer.getShort();
            byte[] b = new byte[2];

            b[0] = (byte) (t >> 8);
            b[1] = (byte) (t & 0xFF);

            t = b[0];
            t <<= 8;
            t |= b[1];
            t &= 0x0000FFFF;

            val = t;
            bytes = b;
        }

        public int getType() {
            return val & MASK_TYPE;
        }

        public boolean securityEnabled() {
            return (val & MASK_SEC_EN) == MASK_SEC_EN;
        }

        public boolean framePending() {
            return (val & MASK_FRAME_PND) == MASK_FRAME_PND;
        }

        public boolean acknowledgeRequest() {
            return (val & MASK_ACK_REQ) == MASK_ACK_REQ;
        }

        public boolean isIntraPan() {
            return (val & MASK_INTRA_PAN) == MASK_INTRA_PAN;
        }

        public boolean hasPanId() {
            int dst_addr_mode = this.getAddressingModeDestination();
            return dst_addr_mode == IEEE802154_FCF_ADDR_SHORT || dst_addr_mode == IEEE802154_FCF_ADDR_EXT;
        }

        public int getAddressingModeDestination() {
            return (val & MASK_ADDR_DST) >> 10;
        }

        public int getAddressingModeSource() {
            return (val & MASK_ADDR_SRC) >> 14;
        }

        public int getVersion() {
            return (val & MASK_VERSION) >> 12;
        }

        public String getTypeString() {
            int type = this.getType();
            String string;
            if( type < 0 || type >= TYPE_ERROR ) {
                string = TYPES[TYPE_ERROR];
            } else {
                string = TYPES[type];
            }
            return string;
        }

        public String hex() {
            return BufferAccessor.getHex(bytes, 2);
        }

        private String getAddressingMode(int addressingModeVal) {
            int bits = addressingModeVal * 8;
            return String.format("%d-bit", bits);
        }

        public String toString() {
            return String.format(
                "%s, Type: %s, Sec: %s, Pen: %b, Ack: %b, IntraPAN: %b, Dst: %s, Src: %s, Ver: %d",
                    hex(),
                    this.getTypeString(),
                    this.securityEnabled(),
                    this.framePending(),
                    this.acknowledgeRequest(),
                    this.isIntraPan(),
                    getAddressingMode(this.getAddressingModeDestination()),
                    getAddressingMode(this.getAddressingModeSource()),
                    this.getVersion()
            );
        }

    }

}
