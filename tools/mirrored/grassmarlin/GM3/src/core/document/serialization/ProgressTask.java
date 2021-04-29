package core.document.serialization;


import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ui.EmbeddedIcons;


public abstract class ProgressTask extends Dialog<ButtonType> {

    private ProgressBar pb;
    private EventHandler<? super MouseEvent> cancelPressed;
    protected Thread taskThread;

    protected ProgressTask(String title, double initialProgress) {
        super();

        VBox content = new VBox();
        HBox labelBox = new HBox();
        Label label = new Label(title);
        labelBox.getChildren().addAll(label);
        this.setTitle(title);
        ((Stage)this.getDialogPane().getScene().getWindow()).getIcons().addAll(EmbeddedIcons.Logo_Small.getRawImage());
        pb = new ProgressBar(initialProgress);
        HBox.setHgrow(pb, Priority.ALWAYS);
        this.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);
        this.cancelPressed = this.getDialogPane().lookupButton(ButtonType.CANCEL).getOnMousePressed();
        this.getDialogPane().lookupButton(ButtonType.CANCEL).setOnMousePressed(this::handleCancel);
        content.getChildren().addAll(labelBox, pb);

        this.getDialogPane().setContent(content);
    }

    protected abstract void runTask() throws Exception;

    protected abstract void onSuccess();

    protected abstract void onFailure();

    protected abstract void onCancel();

    private void handleSuccess() {
        this.onSuccess();
    }

    private void handleFailure() {
        this.onFailure();
    }

    private void handleCancel(MouseEvent event) {
        this.onCancel();
        if (cancelPressed != null) {
            this.cancelPressed.handle(event);
        }
    }

    private void handleClose() {
        this.close();
    }

    public void updateProgress(double progress) {
        this.pb.setProgress(progress);
    }

    public Thread start() {
        this.show();
        Thread t = new Thread(() -> {
            try {
                runTask();
                if (Platform.isFxApplicationThread()) {
                    this.handleSuccess();
                } else {
                    Platform.runLater(this::handleSuccess);
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (Platform.isFxApplicationThread()) {
                    this.handleFailure();
                } else {
                    Platform.runLater(this::handleFailure);
                }
            } finally {
                if (Platform.isFxApplicationThread()) {
                    this.handleClose();
                } else {
                    Platform.runLater(this::handleClose);
                }
            }
        });

        t.start();

        this.taskThread = t;

        return t;
    }
}
