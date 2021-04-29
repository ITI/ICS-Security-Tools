package core.protocol;

import java.nio.ByteBuffer;

/**
 * Interface to be implemented by something that can describe the offset and length of a field in a ByteBuffer
 */
public interface FieldDescriptor {

    int findOffset(ByteBuffer buffer);

    int findLength(ByteBuffer buffer);
}
