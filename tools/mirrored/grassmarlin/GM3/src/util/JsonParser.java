package util;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonParser {
    private enum typeJson {
        string,
        object,
        objectClose,
        objectValueDelimiter,
        array,
        arrayClose,
        number,
        bool,
        nil,
        comma,
        whitespace
    }

    private static typeJson detectType(int charNext) throws IOException {
        switch(charNext) {
            case -1:
                throw new IOException("Unexpected end-of-stream while determining type.");
            case '"':
                return typeJson.string;
            case '{':
                return typeJson.object;
            case '}':
                return typeJson.objectClose;
            case ':':
                return typeJson.objectValueDelimiter;
            case '[':
                return typeJson.array;
            case ']':
                return typeJson.arrayClose;
            case ',':
                return typeJson.comma;
            case '-':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return typeJson.number;
            case 't':
            case 'f':
                return typeJson.bool;
            case 'n':
                return typeJson.nil;
            case ' ':
            case '\r':
            case '\n':
            case '\t':
            case '\b':
                return typeJson.whitespace;
            default:
                throw new IOException("Unknown type starting with '" + (char)charNext + "'");
        }
    }

    private static typeJson findNextToken(StringParser reader) throws IOException {
        typeJson detected;
        while((detected = detectType(reader.peek())) == typeJson.whitespace) {
            reader.skip(1);
        }
        return detected;
    }
    public static Object readUnknown(StringParser reader) throws IOException {
        switch(findNextToken(reader)) {
            case nil:
                //Skip the null, since there is no method to read the value, removing it.
                reader.skip(4);
                return null;
            case object:
                return readObject(reader);
            case array:
                return readArray(reader);
            case bool:
                return readBoolean(reader);
            case number:
                return readNumber(reader);
            case string:
                return readString(reader);
            default:
                throw new IOException("Unknown token: " + (char)reader.peek());
        }
    }
    public static HashMap<String, Object> readObject(StringParser reader) throws IOException {
        //Leading '{'
        reader.read();
        HashMap<String, Object> result = new HashMap<>();

        for(int charNext = reader.peek();;charNext = reader.peek()) {
            switch(detectType(charNext)) {
                case string:
                    String key = readString(reader);
                    if(detectType(reader.read()) != typeJson.objectValueDelimiter) {
                        throw new IOException("Expected ':'");
                    }
                    Object value = readUnknown(reader);
                    result.put(key, value);
                    break;
                case whitespace:
                case comma:
                    reader.read();
                    break;
                case objectClose:
                    reader.read();
                    return result;
                default:
                    throw new IOException("Expected '}' or '\"'");
            }
        }

    }
    public static List<Object> readArray(StringParser reader) throws IOException {
        //Leading '['
        reader.read();
        List<Object> result = new LinkedList<>();

        for(int charNext = reader.peek();;charNext = reader.peek()) {
            switch(findNextToken(reader)) {
                case comma:
                    result.add(null);
                    break;
                case nil:
                    //Skip the null, since there is no method to read the value, removing it.
                    reader.skip(4);
                    result.add(null);
                    break;
                case object:
                    result.add(readObject(reader));
                    break;
                case array:
                    result.add(readArray(reader));
                    break;
                case bool:
                    result.add(readBoolean(reader));
                    break;
                case number:
                    result.add(readNumber(reader));
                    break;
                case string:
                    result.add(readString(reader));
                case arrayClose:
                    //We'll re-detect this and pull it off the input stream later.
                    break;
                default:
                    throw new IOException("Unknown token while reading array: " + (char)charNext);
            }
            //Strip leading whitespace

            //after reading a value, the next token must be either a comma(skip and continue with next iteration) or a close bracket.
            switch(findNextToken(reader)) {
                case arrayClose:
                    reader.skip(1);
                    return result;
                case comma:
                    reader.skip(1);
                    break;
                default:
                    throw new IOException("Unknown token while reading array: " + (char)charNext);
            }
        }
    }
    private static Pattern reNumber = Pattern.compile("^-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+\\-]?[0-9]+)?");
    public static String readNumber(StringParser reader) throws IOException {
        Matcher matcherNumeric = reNumber.matcher(reader.remaining());
        //Since we only match the start of the string, use find instead of matches (which matches the entire string).
        if(matcherNumeric.find()) {
            String value = matcherNumeric.group(0);
            reader.skip(value.length());
            return value;
        } else {
            throw new IOException("Unexpected end-of-stream found; expected Number.");
        }
    }
    public static boolean readBoolean(StringParser reader) throws IOException {
        int charFirst = reader.read();
        if(charFirst == 't') {
            if(reader.read() == 'r' && reader.read() == 'u' && reader.read() == 'e') {
                return true;
            }
        } else if(charFirst == 'f') {
            if(reader.read() == 'a' && reader.read() == 'l' && reader.read() == 's' && reader.read() == 'e') {
                return false;
            }
        }
        throw new IOException("Expected 'true' or 'false'");
    }
    public static String readString(StringParser reader) throws IOException {
        if(reader.read() != '"') {
            // We could throw an exception, but how did we get here if this is false?
            return "";
        }

        StringBuilder result = new StringBuilder();
        int charIn = -1;

        while((charIn = reader.read()) != '"') {
            if(charIn == -1) {
                //End of stream reached
                throw new IOException("Unexpected end-of-stream; expected \"");
            }
            if(charIn == '\\') {
                int charEscaped = reader.read();
                switch(charEscaped) {
                    case -1:
                        throw new IOException("Unexpected end-of-stream; expected \"");
                    case '"':
                        result.append('\"');
                        break;
                    case '\\':
                        result.append('\\');
                        break;
                    case '/':
                        result.append('/');
                        break;
                    case 'b':
                        // Include the escape for a backspace character rather than the actual backspace.
                        result.append("\\b");
                        break;
                    case 'f':
                        result.append('\f');
                        break;
                    case 'n':
                        result.append('\n');
                        break;
                    case 'r':
                        result.append('\r');
                        break;
                    case 't':
                        result.append('\t');
                        break;
                    case 'u':
                        // Rather than processing unicode, just include the escape code.
                        result.append("\\u");
                    default:
                        //Technically invalid, but include any escaped character as a literal.
                        result.append((char)charEscaped);
                }
            } else {
                result.append((char)charIn);
            }
        }

        return result.toString();
    }
}
