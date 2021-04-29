package iadgov.sessioneventtest;

import core.logging.Severity;
import javafx.scene.control.MenuItem;

import java.lang.Override;

/**
 * This plugin adds hooks that produce messages in response to session events.
 * It exists to aid in the testing of the SessionEventHooks interface.
 */
public class Plugin implements util.Plugin, util.Plugin.SessionEventHooks {
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
        return "Session Event Hooks";
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
    public void sessionCreated(core.document.Session session) {
        core.logging.Logger.log(this, Severity.Success, "New Session: " + session);
    }
    @Override
    public void sessionLoaded(core.document.Session session) {
        core.logging.Logger.log(this, Severity.Success, "Loaded Session: " + session);
    }
    @Override
    public void sessionCleared(core.document.Session session) {
        core.logging.Logger.log(this, Severity.Success, "Cleared Session: " + session);
    }
}
