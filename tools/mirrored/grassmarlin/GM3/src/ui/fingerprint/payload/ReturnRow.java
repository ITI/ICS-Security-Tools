package ui.fingerprint.payload;

import core.fingerprint3.DetailGroup;
import core.fingerprint3.Extract;
import core.fingerprint3.Return;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import ui.fingerprint.editorPanes.DetailsDialog;
import ui.fingerprint.editorPanes.ExtractDialog;
import ui.fingerprint.tree.PayloadItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


public class ReturnRow extends OpRow {

    private DetailsDialog details;
    private ExtractDialog extract;
    private String direction;
    private int confidence;


    public ReturnRow(DetailGroup details, List<Extract> extractList, String direction, int confidence) {
        super(PayloadItem.OpType.RETURN);

        Map<String, String> detailMap = new HashMap<>();
        if (details != null) {
            details.getDetail().forEach(detail -> detailMap.put(detail.getName(), detail.getValue()));

            this.details = new DetailsDialog(
                    details.getCategory() != null ? Category.valueOf(details.getCategory()) : null,
                    details.getRole() != null ? Role.valueOf(details.getRole()) : null,
                    detailMap);
        } else {
            this.details = new DetailsDialog();
        }
        if (extractList != null && !extractList.isEmpty()) {
            this.extract = new ExtractDialog(extractList);
        } else {
            this.extract = new ExtractDialog();
        }

        this.details.setOnShowing(this::centerDetails);
        this.extract.setOnShowing(this::centerExtract);

        this.direction = direction;
        this.confidence = confidence;
    }

    public ReturnRow() {
        this(null, null, null, 0);
    }

    @Override
    public HBox getInput() {
        HBox input = new HBox();

        Button detailsButton = new Button("Details...");
        detailsButton.setOnAction(event -> {
            Optional<DetailsDialog> tempDetails = this.details.showAndWait();
            tempDetails.ifPresent(details -> this.details = details);
            update();
        });
        Button extractButton = new Button("Extract...");
        extractButton.setOnAction(event -> {
            Optional<ExtractDialog> tempExtract = this.extract.showAndWait();
            tempExtract.ifPresent(extract -> this.extract = extract);
            update();
        });
        Label directionLabel = new Label("Direction:");
        ChoiceBox<String> directionBox = new ChoiceBox<>(FXCollections.observableArrayList("SOURCE", "DESTINATION"));
        directionBox.setValue(this.direction != null && !this.direction.isEmpty() ? this.direction : "SOURCE");
        this.direction = directionBox.getValue();
        directionBox.valueProperty().addListener(change -> {
            this.direction = directionBox.getValue();
            update();
        });

        Label confLabel = new Label("Confidence:");
        ChoiceBox<Integer> confBox = new ChoiceBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        confBox.setValue(this.confidence > 0 ? this.confidence : 5);
        this.confidence = confBox.getValue();
        confBox.valueProperty().addListener(change -> {
            this.confidence = confBox.getValue();
            update();
        });

        input.setAlignment(Pos.CENTER_LEFT);
        input.setSpacing(2);
        input.getChildren().addAll(detailsButton, extractButton, directionLabel, directionBox, confLabel, confBox);

        return input;
    }

    @Override
    public ObservableList<PayloadItem.OpType> getAvailableOps() {
        return null;
    }

    @Override
    public Object getOperation() {
        Return ret = factory.createReturn();
        ret.setDirection(this.direction);
        ret.setConfidence(this.confidence);
        DetailGroup detailGroup = factory.createDetailGroup();
        detailGroup.setCategory(this.details.getCategory());
        detailGroup.setRole(this.details.getRole());
        detailGroup.getDetail().addAll(
                this.details.getDetails().entrySet().stream()
                    .map(entry -> {
                        DetailGroup.Detail detail = factory.createDetailGroupDetail();
                        detail.setName(entry.getKey());
                        detail.setValue(entry.getValue());
                        return detail;
                    })
                    .collect(Collectors.toList())
        );
        
        ret.setDetails(detailGroup);
        ret.getExtract().addAll(this.extract.getExtractList());

        return ret;
    }

    private void centerDetails(DialogEvent event) {
        Platform.runLater(() -> this.center(this.details));
    }

    private void centerExtract(DialogEvent event) {
        Platform.runLater(() -> this.center(this.extract));
    }

    private void center(Dialog dialog) {
        if (parent != null) {
            Window window = parent.getChildrenBox().getScene().getWindow();
            double x = window.getX() + window.getWidth() / 2 - dialog.getWidth() / 2;
            double y = window.getY() + window.getHeight() / 2 - dialog.getHeight() / 2;
            dialog.setX(x);
            dialog.setY(y);
        }
    }
}
