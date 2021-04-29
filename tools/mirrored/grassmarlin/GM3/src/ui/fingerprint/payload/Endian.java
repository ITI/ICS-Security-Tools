package ui.fingerprint.payload;

public enum Endian {
    BIG,
    LITTLE;

    public static Endian getDefault() {
        return BIG;
    }
}
