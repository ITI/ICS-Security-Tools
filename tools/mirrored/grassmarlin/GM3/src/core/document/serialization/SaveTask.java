package core.document.serialization;


import core.document.Session;
import core.logging.Logger;
import core.logging.Severity;
import javafx.application.Platform;
import ui.TabController;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipOutputStream;

public class SaveTask extends ProgressTask {

    private Session session;
    private TabController tabs;
    private Path savePath;
    private Runnable success;


    ZipOutputStream zos;

    public SaveTask(Session session, TabController tabs, Path savePath, Runnable onSuccess) {
        super("Saving", -1);

        this.session = session;
        this.tabs = tabs;
        this.savePath = savePath;

        this.success = onSuccess;
    }

    @Override
    protected void runTask() throws Exception{
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(savePath)))) {
            this.zos = zos;
            core.document.serialization.Grassmarlin.SaveState(zos, session, tabs);
            Platform.runLater(() -> session.dirtyProperty().set(false));
        }
    }

    @Override
    protected void onSuccess() {
        if(success != null) {
            success.run();
        }
    }

    @Override
    protected void onFailure() {
        Logger.log(this, Severity.Warning, "Saving to " + savePath.getFileName().toString() + " failed");
    }

    @Override
    protected void onCancel() {
        try {
            // no other thread depends on monitors held by this thread and we need it to just stop immediately
            this.taskThread.stop();
            try {
                // want to make sure that thread is actually dead
                this.taskThread.join();
            } catch (InterruptedException ie) {}
            try {
                zos.close();
            } catch (ClosedChannelException cce) {
                // this is what we were trying to do anyway
            }
            Files.deleteIfExists(savePath);
            Platform.runLater(this::close);
        } catch (IOException ioe) {
            Logger.log(this, Severity.Error, "Error stopping save task");
        }
    }
}
