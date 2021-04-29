package core.document.serialization;


import core.document.Session;
import core.logging.Logger;
import core.logging.Severity;
import javafx.application.Platform;
import ui.GrassMarlinFx;
import ui.TabController;
import util.Launcher;
import util.Plugin;

import java.nio.file.Path;

public class LoadTask extends ProgressTask{

    private Path from;
    private Session session;
    private TabController tabController;
    private Runnable success;
    private Runnable failure;


    public LoadTask(Path from, Session session, TabController tabController, Runnable success, Runnable failure) {
        super("Loading", -1);

        this.from = from;
        this.session = session;
        this.tabController = tabController;
        this.success = success;
        this.failure = failure;
    }

    @Override
    protected void runTask() throws Exception {
            core.document.serialization.Grassmarlin.LoadState(from, session, tabController);
    }

    @Override
    protected void onSuccess() {
        if (success != null) {
            success.run();
        }
        Launcher.enumeratePlugins(Plugin.SessionEventHooks.class).forEach(plugin -> plugin.sessionLoaded(session));
    }

    @Override
    protected void onFailure() {
        Logger.log(this, Severity.Warning, "There was an error loading from " + this.from.getFileName().toString());
        failure.run();
    }

    @Override
    protected void onCancel() {
        // no other thread depends on monitors held by this thread and we need it to just stop immediately
        this.taskThread.stop();
        failure.run();
        Platform.runLater(this::close);
    }
}
