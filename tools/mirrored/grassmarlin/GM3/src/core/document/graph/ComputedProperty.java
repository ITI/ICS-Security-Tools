package core.document.graph;

public class ComputedProperty {
    private final String value;
    private final int confidence;

    public ComputedProperty(String value, int confidence) {
        this.value = value;
        this.confidence = confidence;
    }

    public String getValue() {
        return value;
    }

    public int getConfidence() {
        return confidence;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof ComputedProperty) {
            ComputedProperty o = (ComputedProperty)other;
            return value.equals(o.value) && confidence == o.confidence;
        } else {
            return false;
        }
    }
}
