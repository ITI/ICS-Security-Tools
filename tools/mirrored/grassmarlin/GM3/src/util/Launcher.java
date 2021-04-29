package util;

import core.Configuration;
import core.Version;
import core.document.fingerprint.FPDocument;
import core.importmodule.ImportProcessors;
import core.knowledgebase.GeoIp;
import core.knowledgebase.Manufacturer;
import core.logging.Logger;
import core.logging.Severity;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.jnetpcap.Pcap;
import ui.GrassMarlinFx;
import ui.fingerprint.FingerPrintGui;

import javax.xml.bind.JAXBException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * The Launcher class handles the basic initialization of the application.
 * Presently, there is no way to decouple the functionality from the interface.  Ideally, this decoupling will
 * happen and it will be possible (although not necessarily supported) to have a script interface.
 *
 * The decoupling probably won't be done before 3.4 and the script interface will be 3.5 or later, if it is ever done.
 * Whether or not the script interface is done is more a matter of policy than effort; it isn't particularly complicated
 * but it isn't heavily requested, either.
 */
public class Launcher {
    private static final Menu menuPlugins;

    static {
        menuPlugins = new Menu("_Plugins");
    }

    public static void main(String[] args) {
        InitializeLogging();
        RecordLogMessage(Version.APPLICATION_TITLE + "-r" + Version.APPLICATION_REVISION);

        GeoIp.Initialize("data/cidr_to_geo_id.csv", "data/geo_id_to_name.csv");
        try {
            Manufacturer.parseSourceFile(Paths.get(Configuration.getPreferenceString(Configuration.Fields.PATH_MANUFACTURER_DB)));
        } catch(Exception ex) {
            Logger.log(Launcher.class, Severity.Warning, "Unable to load manufacturer database from Wireshark; MAC lookup will be disabled. (" + ex.getMessage() + ")");
        }

        CreateAppDirectories();
        ReportConfigurationSettings();

        boolean allowPlugins = true;
        boolean allowPcap = true;

        for(String arg : args) {
            switch(arg) {
                case "-nopcap":
                    allowPcap = false;
                    break;
                case "-noplugins":
                    allowPlugins = false;
                    break;
                default:
                    //TODO: Attempt to evaluate this as a path to a saved session and open it.
                    break;
            }
        }

        if(allowPcap) {
            try {
                // If jnetpcap can't be found then loadLibrary will result in an exception
                System.loadLibrary("jnetpcap");
                Pcap.libVersion();
            } catch (Error | Exception var3) {
                //We won't be able to do offline pcap either, but there are no hooks (yet) to prevent this.
                Logger.log(Launcher.class, Severity.Warning, "Unable to initialize JNetPCap; packet capture functionality will be disabled.");
                allowPcap = false;
            }
        }

        String wiresharkPath = Configuration.getPreferenceString(Configuration.Fields.WIRESHARK_EXEC);
        if(wiresharkPath == null || !Files.exists(Paths.get(wiresharkPath))) {
            Logger.log(args, Severity.Warning, "Cannot locate Wireshark; please verify the value in the Tools -> Preferences menu.");
        }


        final FPDocument fpDoc = FPDocument.getInstance();
        final Path userFingerprintPath = FingerPrintGui.getDefaultFingerprintDir();
        final Path systemFingerprintPath = FingerPrintGui.getSystemFingerprintDir();

        try {
            Files.list(userFingerprintPath).forEach(path -> {
                try {
                    fpDoc.load(path);
                } catch (final JAXBException je) {
                    Logger.log(Launcher.class, Severity.Warning, "Unable to load Fingerprint at " + path);
                }
            });
        } catch (final IOException ioe) {
            Logger.log(Launcher.class, Severity.Warning, "Unable to load user Fingerprints");
        }

        try {
            Files.list(systemFingerprintPath).forEach(path -> {
                try {
                    fpDoc.load(path);
                } catch (final JAXBException je) {
                    Logger.log(Launcher.class, Severity.Warning, "Unable to load Fingerprint at " + path);
                }
            });
        } catch (final IOException ioe) {
            Logger.log(Launcher.class, Severity.Warning, "Unable to load system Fingerprints");
        }

        if(allowPlugins) {
            LoadPlugins();
        } else {
            //To prevent future loading of plugins.
            pluginsLoaded = true;
        }

        enumeratePlugins(Plugin.ImportProcessorsV1.class).forEach(plugin -> {
            MenuItem menuPlugin = plugin.getMenuItem();
            if(menuPlugin == null) {
                menuPlugin = new MenuItem(plugin.getName());
                menuPlugin.setDisable(true);
            }
            menuPlugins.getItems().add(menuPlugin);

            for(final Plugin.ImportProcessorV1 processor : plugin.getImportProcessors()) {
                ImportProcessors.registerProcessor(processor.getProcessor(), processor.getName(), processor.getExtensions());
            }
        });

        if(menuPlugins.getItems().isEmpty()) {
            final MenuItem itemEmpty = new MenuItem("No Plugins Loaded");
            itemEmpty.setDisable(true);
            menuPlugins.getItems().add(itemEmpty);
        }

        GrassMarlinFx.launchFx(allowPcap, args);

        TerminateLogging();
        //JavaFx leaves some lingering threads, this forces an application exit.
        System.exit(0);
    }

    public static MenuItem getPluginMenuItem() {
        return menuPlugins;
    }

    public static void ReportConfigurationSettings() {
        RecordLogMessage("Configuration:");
        for(final Configuration.Fields field : Configuration.Fields.values()) {
            RecordLogMessage(String.format("  [%s]: '%s'",
                    field.toString(),
                    Configuration.getPreferenceString(field)));
        }
    }

    /**
     * Creates the directories for user data.
     */
    public static void CreateUserDirectories() {
        try {
            Files.createDirectories(Paths.get(Configuration.getPreferenceString(Configuration.Fields.DIR_QUICKLIST)));
            Files.createDirectories(Paths.get(Configuration.getPreferenceString(Configuration.Fields.DIR_SESSIONS)));
        } catch(final IOException ex) {
            Logger.log(Configuration.class, Severity.Error, "Unable to create user directories: " + ex.getMessage());
        }
    }

    /**
     * Create the directories for application data.
     */
    public static void CreateAppDirectories() {
        try {
            // On windows, this typically fails since the user cannot create directories within AppData
            Files.createDirectories(Paths.get(Configuration.getPreferenceString(Configuration.Fields.DIR_LIVE_CAPTURE)));
            Files.createDirectories(Paths.get(Configuration.getPreferenceString(Configuration.Fields.DIR_LOGS)));
            Files.createDirectories(Paths.get(Configuration.getPreferenceString(Configuration.Fields.DIR_USER_FINGERPRINTS)));
        } catch(final IOException ex) {
            Logger.log(Configuration.class, Severity.Error, "Unable to create application directories: " + ex.getMessage());
        }
    }

    private static Path pathLogFile = null;
    private static BufferedWriter writerLogFile = null;
    public static String getLogFilePath() {
        return pathLogFile.toAbsolutePath().toString();
    }
    public static void InitializeLogging() {
        // Use the current timestamp with non-numeric characters replaced by underscores.
        String nameFile = Instant.now().atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_INSTANT).replaceAll("\\D+", "_") + ".txt";
        pathLogFile = Paths.get(Configuration.getPreferenceString(Configuration.Fields.DIR_LOGS), nameFile);

        try {
            writerLogFile = Files.newBufferedWriter(pathLogFile);
            //Write a line to the file to ensure that isn't generating errors, either.
            RecordLogMessage(new Logger.Message(Launcher.class, Severity.Information, "Logging subsystem initialized."));

            //Start listening for alerts.
            Logger.getMessageHistory().addListener(Launcher::Handle_writeLogMessageToDisk);
        } catch (IOException ex) {
            Logger.log(Launcher.class, Severity.Error, "This session cannot be logged to disk: " + ex.getMessage());
        }
    }
    private static void Handle_writeLogMessageToDisk(final ListChangeListener.Change<? extends Logger.Message> c) {
        while(c.next()) {
            for(final Logger.Message msg : c.getAddedSubList()) {
                Launcher.RecordLogMessage(msg);
            }
        }
    }
    public static void TerminateLogging() {
        if(writerLogFile != null) {
            try {
                writerLogFile.flush();
                writerLogFile.close();
            } catch (final Exception ex) {
                System.err.println("Error shutting down logging subsystem: " + ex.getMessage());
            } finally {
                writerLogFile = null;
            }
        }
    }
    public static void RecordLogMessage(final String message) {
        RecordLogMessage(message, true);
    }
    public static void RecordLogMessage(final String message, final boolean echoToConsole) {
        if(writerLogFile != null) {
            String line = String.format("[%s] DEBUG - %s\r\n", Instant.now().atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_INSTANT), message);
            try {
                writerLogFile.write(line);
                writerLogFile.flush();
            } catch(final IOException ex) {
                System.err.println("Unable to write message message to disk:\n" + line);
            }
            if(echoToConsole) {
                System.out.println(message);
            }
        }
    }
    public static void RecordLogMessage(final Logger.Message message) {
        if(writerLogFile != null) {
            String line = String.format("[%s] %s - %s\r\n", Instant.ofEpochMilli(message.tsCreated).atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_INSTANT), message.severity, message.message);
            try {
                writerLogFile.write(line);
                writerLogFile.flush();
            } catch(final IOException ex) {
                System.err.println("Unable to write message message to disk:\n" + line);
            }
        }
    }

    private static boolean pluginsLoaded = false;
    private static final List<Plugin> plugins = new LinkedList<>();
    private static final Map<String, ClassLoader> loaderForPlugin = new HashMap<>();
    public static ClassLoader loaderFor(final String namePlugin) {
        if(namePlugin == null || namePlugin.equals("")) {
            return Launcher.class.getClassLoader();
        } else {
            return loaderForPlugin.get(namePlugin);
        }
    }
    public static String pluginFor(final Class<?> clazz) {
        for(Map.Entry<String, ClassLoader> entry : loaderForPlugin.entrySet()) {
            if(entry.getValue().equals(clazz.getClassLoader())) {
                return entry.getKey();
            }
        }
        return null;
    }
    public static synchronized void LoadPlugins() {
        // Loading already-loaded classes is bad.
        if(pluginsLoaded) {
            return;
        }
        pluginsLoaded = true;

        try {
            for (Path pathPlugin : Files.newDirectoryStream(Paths.get("plugins"))) {
                if(pathPlugin.toString().endsWith(".jar")) {
                    String nameFile = pathPlugin.getFileName().toString().replace(".jar", "");
                    ClassLoader loader = new URLClassLoader(new URL[] { pathPlugin.toUri().toURL() } );
                    loaderForPlugin.put(nameFile, loader);
                    try {
                        Class<?> clazz = loader.loadClass(nameFile + ".Plugin");
                        if(util.Plugin.class.isAssignableFrom(clazz)) {
                            plugins.add((util.Plugin)clazz.getConstructor().newInstance());
                        }
                    } catch(ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                        Logger.log(Launcher.class, Severity.Error, "Plugin '" + nameFile + "' could not be loaded: " + ex.getMessage());
                        continue;
                    }
                    Logger.log(Launcher.class, Severity.Information, "Loaded plugin '" + nameFile + "'");
                }
            }
        } catch(IOException ex) {
            Logger.log(Launcher.class, Severity.Error, "There was an error enumerating plugins: " + ex.getMessage());
        }
    }

    public static <T> List<T> enumeratePlugins(final Class<T> clazz) {
        final List<T> result = new LinkedList<>();
        for(final Plugin plugin : plugins) {
            if(clazz.isAssignableFrom(plugin.getClass())) {
                result.add(clazz.cast(plugin));
            }
        }
        return result;
    }
}
