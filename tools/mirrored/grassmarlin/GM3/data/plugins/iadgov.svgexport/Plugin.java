package iadgov.svgexport;

import core.logging.Severity;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.lang.Override;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

/**
 * This plugin allows a graph to be exported as a SVG image.
 */
public class Plugin implements util.Plugin, util.Plugin.Export {
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
        return "SVG Export";
    }

    /**
     * Part of the util.Plugin interface.
     * @return A MenuItem which will be added to the Tools->Plugin menu.  If null is returned, a disabled item bearing the name of the plugin will be provided automatically.
     */
    @Override
    public MenuItem getMenuItem() {
        return null;
    }

    @Override
    public List<MenuItem> getExportMenuItems(ui.TabController tabs, javafx.stage.Stage stage) {
        List<MenuItem> result = new ArrayList<>(1);
        result.add(new ui.custom.fx.DynamicSubMenu("_SVG", (Node)null, () ->
            tabs.getGraphs().stream().map(graph ->
            new ui.custom.fx.ActiveMenuItem(graph.titleProperty().get(), event -> {
                FileChooser dlgExportTo = new FileChooser();
                dlgExportTo.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("SVG Image Files (*.svg)", "*.svg"),
                        new FileChooser.ExtensionFilter("All Files", "*")
                );
                //TODO: Default path for export
                File exportTo = dlgExportTo.showSaveDialog(stage);
                if(exportTo != null) {
                    try(BufferedWriter writer = new BufferedWriter(new FileWriter(exportTo))) {
                        writer.write(
                                Svg.serialize(graph.getVisualizationView()).replaceAll("(\\s+\\n)+", "\n")
                        );
                    } catch(IOException ex) {
                        core.logging.Logger.log(this, Severity.Error, "There was an error exporting the graph: " + ex.getMessage());
                    }
                }
            })
            ).collect(Collectors.toList())
        ));
        return result;
    }
}
