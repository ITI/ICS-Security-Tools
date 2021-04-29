package core.document.serialization.xml;

public abstract class Escaping {
    public static String XmlString(final String in) {
        return in.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
