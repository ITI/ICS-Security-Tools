package core.protocol;

import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * Helper class to assist in the parsing of fixed-width formats.
 *
 * An easier method then a class containing two 'ints' for the offset and length of each field
 * a descriptor for each field can be given both values and be passed to the proper accessor method
 *
 */
public class BufferAccessor {

    private static char[] charset = "0123456789ABCDEF".toCharArray();

    private ByteBuffer buffer;

    public BufferAccessor(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public byte getByte(FieldDescriptor descriptor) {
        this.buffer.position(descriptor.findOffset(this.buffer));
        return this.buffer.get();
    }

    public short getShort(FieldDescriptor descriptor) {
        this.buffer.position(descriptor.findOffset(this.buffer));
        return this.buffer.getShort();
    }

    public int getInt(FieldDescriptor descriptor) {
        this.buffer.position(descriptor.findOffset(this.buffer));
        return this.buffer.getInt();
    }

    public long getLong(FieldDescriptor descriptor) {
        this.buffer.position(descriptor.findOffset(this.buffer));
        return this.buffer.getLong();
    }

    public byte[] getBytes(FieldDescriptor descriptor) {
        byte[] bytes = new byte[descriptor.findLength(this.buffer)];
        this.buffer.position(descriptor.findOffset(this.buffer));
        this.buffer.get(bytes);
        return bytes;
    }

    public byte[] getBytesReverse(FieldDescriptor descriptor) {
        byte[] bytes = getBytes(descriptor);
        ArrayUtils.reverse(bytes);
        return bytes;
    }

    public String getHex(FieldDescriptor descriptor) {
        byte[] bytes = getBytes(descriptor);
        String string;
        if( this.buffer.order().equals(ByteOrder.LITTLE_ENDIAN)) {
            string = getHexLittle(bytes, descriptor.findLength(this.buffer));
        } else {
            string = getHex(bytes, descriptor.findLength(this.buffer));
        }
        return string;
    }

    public String getHex(FieldDescriptor descriptor, int delimiterFrequency, String delimiter) {
        // Find {delimiterFrequency} characters followed by {delimiterFrequency} characters and replace the second
        //group with a delimiter followed by the matched second group.
        return getHex(descriptor).replaceAll("(?<=.{" + delimiterFrequency + "})(.{" + delimiterFrequency + "})", delimiter + "$1");
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    static String getHexLittle(final byte[] bytes, final int length) {
        char[] chars = new char[length * 2];
        for( int i = 0; i < length; i++ ) {
            int index = bytes[length-1-i] & 0xFF;
            chars[i*2] = charset[index >> 4];
            chars[i*2+1] = charset[index & 0x0F];
        }
        return new String(chars);
    }

    static String getHex(final byte[] bytes, final int length) {
        char[] chars = new char[length * 2];
        for( int i = 0; i < length; i++ ) {
            int index = bytes[i] & 0xFF;
            chars[i*2] = charset[index >> 4];
            chars[i*2+1] = charset[index & 0x0F];
        }
        return new String(chars);
    }

}
