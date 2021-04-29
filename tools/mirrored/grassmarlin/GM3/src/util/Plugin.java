package util;

import core.document.Session;
import core.importmodule.ImportItem;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import ui.TabController;

import java.util.Collection;
import java.util.List;

/**
 * Each Plugin must have a public zero-argument constructor in order to be created by the plugin interface.
 */
public interface Plugin {
    class ImportProcessorV1 {
        private final String name;
        private final Class<? extends ImportItem> processor;
        private final String[] extensions;

        public ImportProcessorV1(final String name, final Class<? extends ImportItem> processor, final String... extensions) {
            this.name = name;
            this.processor = processor;
            this.extensions = extensions;
        }

        public String getName() {
            return name;
        }
        public Class<? extends ImportItem> getProcessor() {
            return processor;
        }
        public String[] getExtensions() {
            return extensions;
        }
    }

    /**
     * Implement this interface if this plugin provides new import formats.
     */
    interface ImportProcessorsV1 extends Plugin{
        Collection<ImportProcessorV1> getImportProcessors();
    }

    /**
     * This interface allow the plugin to respond to events when a session is created, loaded, or cleared.
     *
     * THIS IS NOT SUPPORTED IN 3.2
     *
     * The 3.2 code has preliminary hooks for this interface, however it has not been tested thoroughly yet.  This
     * can be used, but there are no guarantees as to the correctness of the use of this interface.
     *
     * When a Session is loaded, a new Session is created first.
     * If there is an error loading a Session, the failure handler will construct a new Session again.
     *
     */
    interface SessionEventHooks extends Plugin {
        void sessionCreated(final Session session);
        void sessionLoaded(final Session session);
        void sessionCleared(final Session session);
    }

    /**
     * This interface allows a plugin to export data.
     *
     * THIS IS NOT SUPPORTED IN 3.2
     *
     * The getExportMenuItems method is called whenever the Export menu is displayed.
     */
    interface Export extends Plugin {
        List<MenuItem> getExportMenuItems(final TabController tabs, final Stage stage);
    }

    /**
     * @return The name of the plugin, as it will be displayed to the user.
     */
    String getName();

    /**
     * Each plugin (optionally) has a menu item to access its configuration interface.
     * @return Null if this plugin has no configuration interface, or a MenuItem which will launch the configuration.
     */
    MenuItem getMenuItem();
}
