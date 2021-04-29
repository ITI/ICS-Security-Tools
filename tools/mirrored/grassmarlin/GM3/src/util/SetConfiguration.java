package util;

import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.io.File;

public class SetConfiguration {
    private static Map<String, String> properties = new HashMap<>();

    static {
        properties.put("GrassMarlin" + File.separator + "appData", ".");
    }

    public static void main(String[] args) {
        for(String arg : args) {
            if(arg.startsWith("-d")) {
                String[] tokens = arg.split("(-d|=|\")");
                if(tokens.length >= 3) {
                    if(properties.containsKey(tokens[1])) {
                        System.out.println("Defining [" + tokens[1] + "] := [" + tokens[2] + "]");
                        properties.put(tokens[1], tokens[2]);
                    } else {
                        System.out.println("Unknown Property: [" + tokens[1] + "] (" + tokens[2] + ")");
                    }
                }
            }
        }

        for(Entry<String, String> entry : properties.entrySet()) {
            System.out.println("Setting: [" + entry.getKey() + "] -> [" + entry.getValue() + "]");
            java.util.prefs.Preferences.systemRoot().put(entry.getKey(), entry.getValue());
        }
    }
}
