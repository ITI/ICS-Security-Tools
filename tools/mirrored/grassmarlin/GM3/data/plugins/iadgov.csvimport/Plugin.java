package iadgov.csvimport;

import javafx.scene.control.MenuItem;

import java.util.ArrayList;
import java.util.Collection;


/**
 * A plugin will be loaded by GrassMarlin if:
 *  1) The plugin is contained in a jar file located in the plugins directory
 *  2) The name of the file is in the format "package.jar" where "package" corresponds
 *     to a package within the jar containing a class named Plugin which implements the
 *     util.Plugin interface.
 *  3) The Plugin class contains a 0-argument constructor.
 *
 * When a plugin is loaded, an instance of the Plugin class is created.  The util.Plugin
 *  interface provides only the most basic of functions--the name and a menu item that
 *  will be integrated into the main UI.  Additional capabilities are inferred by other
 *  interfaces implemented by the plugin class.
 * Presently, the only interface which provides for additional behavior is the
 *  util.Pligin.ImportProcessors class, which allows a plugin to provide import methods
 *  for new data formats.
 */
public class Plugin implements util.Plugin, util.Plugin.ImportProcessorsV1 {
    /**
     * Constructor required as part of the util.Plugin interface to GrassMarlin.
     */
    public Plugin() {
        // No content
    }

    /**
     * Part of the util.Plugin interface.
     * @return The pretty-print version of the Plugin's name.
     */
    @Override
    public String getName() {
        return "CSV Import";
    }

    /**
     * Part of the util.Plugin interface.
     * @return A MenuItem which will be added to the Tools->Plugin menu.  If null is returned, a disabled item bearing the name of the plugin will be provided automatically.
     */
    @Override
    public MenuItem getMenuItem() {
        return null;
    }

    /**
     * Part of the util.Plugin.ImportProcessors interface.
     * @return A collection of ImportProcessors to be added to GrassMarlin.  An ImportProcessor is a tuple containing a title, a class, and a list of file extensions.
     */
    @Override
    public Collection<ImportProcessorV1> getImportProcessors() {
        ArrayList<ImportProcessorV1> result = new ArrayList<>();

        // The title will be used in quicklists and saved sessions.
        //  Duplicating the title of another processor will cause unspecified problems.
        // The provided class must extend ImportItem.
        // The extensions are optional.  Any file which ends with a given extension string
        //  will be automatically associated with this processor.
        result.add(new ImportProcessorV1("CSV", CsvImportProcessor.class, ".csv"));

        return result;
    }
}
