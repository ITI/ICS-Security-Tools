package core.knowledgebase;

import core.logging.Logger;
import core.logging.Severity;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for accessing data stored in the reference flat files
 */
public class Reference {
    private final Path referencePath = Paths.get("data", "reference");
    private final Path bacnetPath = referencePath.resolve("BACnetVendors.htm");
    private final Path enipDevicePath = referencePath.resolve("enipDevice.csv");
    private final Path enipVendorsPath = referencePath.resolve("enipVendors.csv");

    private final Map<Integer, String> bacnetMap;
    private final Map<Integer, String> enipDeviceMap;
    private final Map<Integer, String> enipVendorMap;
    private final Map<Integer, String> ouiMap;

    private static Reference instance;

    private Reference() {
        bacnetMap = new HashMap<>();
        enipDeviceMap = new HashMap<>();
        enipVendorMap = new HashMap<>();
        ouiMap = new HashMap<>();

        // this.initialize must be called, but only after the constructor completes.
    }

    // Because initialize can log, we must only call initialize after the constructor has completed.
    private void initialize() {
        try (BufferedReader reader = Files.newBufferedReader(bacnetPath, StandardCharsets.ISO_8859_1)) {
            String line;
            int column = 0;
            int id = -1;
            String vendor = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("<tr>")) {
                    column = 0;
                    id = -1;
                    vendor = null;
                } else if (line.startsWith("<td>" )) {
                    if (column == 0) {
                        try {
                            id = Integer.parseInt(line.replaceAll("[</td>]", ""));
                        } catch (NumberFormatException e) {
                            // id just won't get set which we'll deal with later
                        }
                    } else if (column == 1) {
                        vendor = line.replaceAll("[</td>]", "");
                    }
                    column++;
                } else if (line.startsWith("</tr>")) {
                    if (id >= 0 && vendor != null) {
                        bacnetMap.put(id, vendor);
                    }
                }
            }
        } catch (IOException ioe) {
            Logger.log(this, Severity.Error, "Unable to Load BACNet reference: " + ioe.getMessage());
        }

        try (BufferedReader reader = Files.newBufferedReader(enipDevicePath, StandardCharsets.ISO_8859_1)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                int id = -1;
                try {
                    id = Integer.parseInt(columns[0].trim());
                } catch (NumberFormatException e) {
                    // id just won't be set which we'll deal with later
                }
                if (id >= 0) {
                    enipDeviceMap.put(id, columns[1].trim());
                }
            }
        } catch (IOException ioe) {
            Logger.log(this, Severity.Error, "Unable to Load ENIP Device reference: " + ioe.getMessage());
        }

        try (BufferedReader reader = Files.newBufferedReader(enipVendorsPath, StandardCharsets.ISO_8859_1)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                int id = -1;
                try {
                    id = Integer.parseInt(columns[0].trim());
                } catch (NumberFormatException e) {
                    // id just won't be set which we'll deal with later
                }
                if (id >= 0) {
                    enipVendorMap.put(id, columns[1].trim());
                }
            }
        } catch (IOException ioe) {
            Logger.log(this, Severity.Error, "Unable to Load ENIP Vendor reference: " + ioe.getMessage());
        }
    }

    public static Reference getInstance() {
        if (instance == null) {
            instance = new Reference();
            instance.initialize();
        }

        return instance;
    }


    public String getBacnetVendor(int id) {
        return this.bacnetMap.get(id);
    }

    public String getEnipDevice(int id) {
        return this.enipDeviceMap.get(id);
    }

    public String getEnipVendor(int id) {
        return this.enipVendorMap.get(id);
    }
}
