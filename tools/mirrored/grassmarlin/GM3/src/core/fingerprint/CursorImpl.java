package core.fingerprint;

public class CursorImpl {
    private int main;
    private int start;
    private int end;

    public CursorImpl() {
        start = main = end = 0;
    }

    public int getMain() {
        return main;
    }

    public void setMain(int main) {
        this.main = main;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public void forward(int bytes) {
        this.main += bytes;
    }

    public void reset() {
        start = main = end = 0;
    }
}
