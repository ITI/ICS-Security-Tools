package core;

import core.logging.Logger;
import core.logging.Severity;
import org.apache.commons.lang3.SystemUtils;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration is responsible for serialization of application-level variables.
 */
public class Configuration {
    public static final String INSTALL_PROPERTY_KEY = "dir.install";
    public static final String APPLICATION_NAME = "GRASSMARLIN3";

    @FunctionalInterface
    public interface IDefaultValue {
        String getDefault();
    }

    public enum Fields {
        // == SUPPORTING APPLICATIONS =========================================
        WIRESHARK_EXEC("path.wireshark", () -> {
            String path;
            if(SystemUtils.IS_OS_WINDOWS) {
                //Ideally we could pull the path from the registry, but doing that from Java is needlessly complicated.
                path = "C:\\Program Files\\Wireshark\\Wireshark.exe";
            } else {
                path = null;
                //Testing suggests there is no reliable solution for all supported configurations at this time.
            }
            if(path != null && new File(path).exists()) {
                return path;
            } else {
                return null;
            }
        }),
        TEXT_EDITOR_EXEC("path.texteditor", () -> {
            String path;
            if(SystemUtils.IS_OS_WINDOWS) {
                //Again, pulling the application associated with .txt files would be preferable, but registry access from Java is both complicated and bad form.
                path = "C:\\Windows\\System32\\Notepad.exe";
            } else {
                //This could probably be done, but we will not weigh in on the emacs/vi/vim/butterfly debate, so null is the safest bet.
                path = null;
            }
            if(path != null && new File(path).exists()) {
                return path;
            } else {
                return null;
            }
        }),
        PDF_VIEWER_EXEC("path.pdfviewer"),   //If a PDF viewer isn't set we'll try a shell execute.  Plus there is no way to guess this for any platform--windows uses version-specific paths.

        // == PATHS ===========================================================
        //Installed Application Content (written once on install).
        DIR_INSTALL("dir", () -> "."),
        DIR_IMAGES_ICON("dir.images.icon", () -> "images"),
        PATH_MANUFACTURER_DB("path.wireshark.manuf", () -> {
            if(SystemUtils.IS_OS_WINDOWS) {
                if (Configuration.getPreferenceString(Configuration.Fields.WIRESHARK_EXEC) != null) {
                    return Paths.get(Paths.get(Configuration.getPreferenceString(Configuration.Fields.WIRESHARK_EXEC)).getParent().toString(), "manuf").toString();
                } else {
                    return null;
                }
            } else {
                return "/usr/share/wireshark/manuf";
            }
        }),
        PATH_USERGUIDE("path.reference.userguide", () -> "data" + File.separator + "reference" + File.separator + Version.FILENAME_USER_GUIDE),
        DIR_CORE_FINGERPRINTS("dir.fingerprints.core", () -> "data" + File.separator + "fingerprint"),

        //Application Data (written by application as it is running, not necessarily user-specific)
        DIR_LIVE_CAPTURE("dir.misc.livecaptures", () -> getAppDataDirectory() + File.separator + "livecaptures"),
        DIR_LOGS("dir.logs", () -> getAppDataDirectory() + File.separator + "logs"),
        DIR_USER_FINGERPRINTS("dir.fingerprints.core", () -> getAppDataDirectory() + File.separator + "fingerprints"),

        //User Data (user-specific settings and files)
        DIR_QUICKLIST("dir.quicklist", () -> Paths.get(getUserDataDirectory(), "quicklist").toString()),
        DIR_SESSIONS("dir.sessions", () -> getUserDataDirectory()),

        // == PcapReader.java FLAGS ===========================================
        PCAP_FLAG_MODE("flags.pcap.mode", () -> "1"),
        PCAP_FLAG_SNAPLEN("flags.pcap.snaplen", () -> "0xFFFF"),
        PCAP_FLAG_TIMEOUT("flags.pcap.timeout", () -> "30000"), //30 Seconds
        PCAP_FLAG_CAPTURE_LIMIT("flags.pcap.capture_limit", () -> "-1"), //No limit

        // == PcapReader.java OTHER ===========================================
        PATH_PCAP_FILTERS("path.data.kb", () -> getAppDataDirectory() + File.separator + "PcapFilters.txt"),
        PCAP_FILTER_STRING("pcap.filter.string", () -> " "),
        PCAP_FILTER_TITLE("pcap.filter.title", () -> "ALLOW ALL TRAFFIC"),

        UI_VIEW_UPDATE_DELAY("ui.viewupdatedelay", () -> "1500"),

        // == Display Preferences =============================================
        COLOR_NODE_NEW("colors.nodes.new", () -> "0000FF"),
        COLOR_NODE_MODIFIED("colors.nodes.modified", () -> "00FF00"),
        COLOR_NODE_BACKGROUND("colors.visualization.nodes.background", () -> "000000"),
        COLOR_NODE_HIGHLIGHT("colors.visualization.nodes.background.highlighted", () -> "FFFF00"),
        COLOR_NODE_TEXT("colors.visualization.nodes.text", () -> "FFFFFF"),

        // == Other Preferences ===============================================
        LOGICAL_CREATE_DYNAMIC_SUBNETS("logical.use_dynamic_subnets", () -> "true"),
        LOGICAL_DYNAMIC_SUBNET_BITS("logical.dynamic_subnet_size", () -> "24"),

        LAST_RUN_VERSION("last_version", () -> ""),
        SUPPRESS_UNCHANGED_VERSION_NOTES("suppress_version_notes", () -> "true"),

        FINGERPRINT_SAVEAS_LEAVES_OLD("fingerprints.preserveold", () -> "true")
        ;

        final String propertyKey;
        final IDefaultValue fnGetDefault;

        Fields(String propertyKey) {
            this(propertyKey, null);
        }
        Fields(String propertyKey, IDefaultValue fnGetDefault) {
            this.propertyKey = propertyKey;
            this.fnGetDefault = fnGetDefault;
        }
    }

    private static String getAppDataDirectory() {
        return Paths.get(java.util.prefs.Preferences.systemRoot().get("GrassMarlin" + File.separator + "appData", "")).toAbsolutePath().toString();
    }

    private static String getUserDataDirectory() {
        Path result = Paths.get(FileSystemView.getFileSystemView().getDefaultDirectory().getPath(), "GrassMarlin");

        try {
            Files.createDirectories(result);
        } catch(IOException ex) {
            Logger.log(Configuration.class, Severity.Error, "Unable to create user Data Directory: " + ex.getMessage());
        }
        return result.toString();
    }

    public static String getPreferenceString(Fields preference) {
        String result = java.util.prefs.Preferences.userRoot().get("GrassMarlin" + File.separator + preference.propertyKey, null);
        if(result == null) {
            if(preference.fnGetDefault != null) {
                result = preference.fnGetDefault.getDefault();
            }
        }


        return result;
    }

    public static void setPreferenceString(Fields preference, String value) {
        if(value == null) {
            java.util.prefs.Preferences.userRoot().remove("GrassMarlin" + File.separator + preference.propertyKey);
        } else {
            java.util.prefs.Preferences.userRoot().put("GrassMarlin" + File.separator + preference.propertyKey, value);
        }
    }

    public static long getPreferenceLong(Fields preference) {
        String value = getPreferenceString(preference);
        if(value == null) {
            return 0;
        } else {
            if(value.startsWith("0x")) {    // Hex
                return Long.parseLong(value.substring(2), 16);
            } else if(value.startsWith("0")) {  // Octal
                return Long.parseLong(value, 8);
            } else {    // Default to decimal
                return Long.parseLong(value);
            }
        }
    }

    public static void setPreferenceLong(Fields preference, long value) {
        setPreferenceString(preference, Long.toString(value));
    }

    public static Boolean getPreferenceBoolean(Fields preference) {
        String value = getPreferenceString(preference);
        if(value == null) {
            return null;
        } else {
            return !(value.equalsIgnoreCase("false") || value.equals("0"));
        }
    }

    public static void setPreferenceBoolean(Fields preference, boolean value) {
        setPreferenceString(preference, Boolean.toString(value));
    }
}
