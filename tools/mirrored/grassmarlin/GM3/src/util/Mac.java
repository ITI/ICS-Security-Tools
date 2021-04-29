package util;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Mac {
    private final int[] value;

    public static final Mac NULL_MAC = new Mac((String)null);

    private Mac(boolean hasValue) {
        value = hasValue ? new int[6] : null;
    }

    public Mac(byte[] bytes) {
        this(bytes != null);

        if(bytes != null) {
            for (int idx = 0; idx < 6; idx++) {
                value[idx] = bytes[idx] < 0 ? 256 + (int) bytes[idx] : bytes[idx];
            }
        }
    }
    public Mac(Byte[] bytes) {
        this(bytes != null);

        if(bytes != null) {
            for (int idx = 0; idx < 6; idx++) {
                value[idx] = bytes[idx] < 0 ? 256 + (int) bytes[idx] : bytes[idx];
            }
        }
    }
    public Mac(String formatted) {
        this(formatted != null);

        if(formatted != null) {
            String[] tokens = formatted.replaceAll("[^a-fA-F0-9]", "").replaceAll("..", "$0:").split(":");

            for (int idx = 0; idx < 6; idx++) {
                value[idx] = Integer.parseInt(tokens[idx], 16);
            }
        }
    }

    public int[] getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        if(value == null) {
            return 0;
        } else {
            int result = 0;
            for(int idx = 0; idx < 6; idx++) {
                result <<= 5;
                result ^= value[idx];
            }
            return result;
        }
    }

    @Override
    public String toString() {
        return Arrays.stream(value).boxed().map(i -> (i < 16 ? "0" : "") + Integer.toHexString(i)).collect(Collectors.joining(":"));
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof Mac) {
            if(value == null) {
                return ((Mac)other).value == null;
            } else {
                if(((Mac)other).value == null) {
                    return false;
                }
            }

            for(int idx = 0; idx < 6; idx++) {
                if(value[idx] != ((Mac)other).value[idx]) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
