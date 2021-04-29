package util;

public class StringParser {
    private final String text;
    private int index;

    public StringParser(String text) {
        this.text = text;
        this.index = 0;
    }

    public int peek() {
        if(index >= text.length()) {
            return -1;
        } else {
            return text.charAt(index);
        }
    }

    public int read() {
        if(index >= text.length()) {
            return -1;
        } else {
            return text.charAt(index++);
        }
    }

    public String remaining() {
        return text.substring(index);
    }

    public void skip(int cntChars) {
        index += cntChars;
    }
}
