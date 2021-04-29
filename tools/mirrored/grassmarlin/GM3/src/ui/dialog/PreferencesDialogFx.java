package ui.dialog;

import core.Configuration;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.*;
import ui.EmbeddedIcons;
import ui.custom.fx.ColorPreview;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PreferencesDialogFx extends Dialog<ButtonType> {
    private final class TitleLabel extends Label {
        public TitleLabel(String text) {
            super(text);

            this.setFont(Font.font(getFont().getFamily(), FontWeight.BOLD, 16.0));
            this.setPadding(new Insets(4.0, 0.0, 0.0, 0.0));
        }
    }
    private final SimpleStringProperty pathWireshark;
    private final SimpleStringProperty pathTextEditor;
    private final SimpleStringProperty pathPdfViewer;

    private final SimpleObjectProperty<Color> colorNewNode;
    private final SimpleObjectProperty<Color> colorModifiedNode;
    private final SimpleObjectProperty<Color> colorNodeText;
    private final SimpleObjectProperty<Color> colorNodeBackground;
    private final SimpleObjectProperty<Color> colorNodeHighlight;

    private final SimpleBooleanProperty networksCreateOnDemand;
    private final SimpleObjectProperty<Integer> networksSubnetSize;

    private final ButtonType saveButton;

    private final ColorPickerDialogFx colorPicker;
    private final FileChooser chooser;

    public PreferencesDialogFx() {
        pathWireshark = new SimpleStringProperty();
        pathTextEditor = new SimpleStringProperty();
        pathPdfViewer = new SimpleStringProperty();

        colorNewNode = new SimpleObjectProperty<>();
        colorModifiedNode = new SimpleObjectProperty<>();
        colorNodeText = new SimpleObjectProperty<>();
        colorNodeBackground = new SimpleObjectProperty<>();
        colorNodeHighlight = new SimpleObjectProperty<>();

        networksCreateOnDemand = new SimpleBooleanProperty();
        networksSubnetSize = new SimpleObjectProperty<>(24);    //We can't let this be null when bound to the spinner.

        saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        colorPicker = new ColorPickerDialogFx();
        chooser = new FileChooser();

        initComponents();
        reloadValues();
    }

    protected void browseForFile(StringProperty property) {
        browseForFile(property, null);
    }
    protected void browseForFile(StringProperty property, String prompt) {
        if(prompt == null) {
            chooser.setTitle("Open");
        } else {
            chooser.setTitle(prompt);
        }

        File f = chooser.showOpenDialog(this.getOwner());
        if (f != null && !f.getAbsolutePath().isEmpty()) {
            property.set(f.getAbsolutePath());
        }
    }

    private void initComponents() {
        final Window stage = super.getDialogPane().getScene().getWindow();
        if(stage instanceof Stage) {
            ((Stage)stage).getIcons().add(EmbeddedIcons.Vista_Personalization.getRawImage());
        }

        this.setTitle("Preferences");
        this.initModality(Modality.APPLICATION_MODAL);
        this.initStyle(StageStyle.UTILITY);

        final GridPane layout = new GridPane();
        int idxRow = -1;

        layout.add(new TitleLabel("External Applications"), 0, ++idxRow, 3, 1);
        layout.add(new Label("Wireshark"), 0, ++idxRow);
        final TextField tbWireshark = new TextField();
        tbWireshark.textProperty().bindBidirectional(pathWireshark);
        layout.add(tbWireshark, 1, idxRow);
        final Button btnWireshark = new Button("Browse...");
        btnWireshark.setOnAction(event -> browseForFile(pathWireshark));
        layout.add(btnWireshark, 2, idxRow);

        layout.add(new Label("Text Viewer"), 0, ++idxRow);
        final TextField tbTextViewer = new TextField();
        tbTextViewer.textProperty().bindBidirectional(pathTextEditor);
        layout.add(tbTextViewer, 1, idxRow);
        final Button btnTextViewer = new Button("Browse...");
        btnTextViewer.setOnAction(event -> browseForFile(pathTextEditor));
        layout.add(btnTextViewer, 2, idxRow);

        layout.add(new Label("PDF Viewer"), 0, ++idxRow);
        final TextField tbPdfViewer = new TextField();
        tbPdfViewer.textProperty().bindBidirectional(pathPdfViewer);
        layout.add(tbPdfViewer, 1, idxRow);
        final Button btnPdfViewer = new Button("Browse...");
        btnPdfViewer.setOnAction(event -> browseForFile(pathPdfViewer));
        layout.add(btnPdfViewer, 2, idxRow);

        layout.add(new TitleLabel("Visualization"), 0, ++idxRow, 3, 1);
        layout.add(new Label("New Node Highlight"), 0, ++idxRow);
        final ColorPreview previewNewNode = new ColorPreview(colorNewNode);
        previewNewNode.setOnMouseClicked(event -> {
            colorPicker.setColor(colorNewNode.get());
            final Optional<ButtonType> result = colorPicker.showAndWait();
            if (result.isPresent() && result.get().equals(ButtonType.OK)) {
                colorNewNode.set(colorPicker.getSelectedColor());
            }
        });
        layout.add(previewNewNode, 2, idxRow);

        layout.add(new Label("Modified Node Highlight"), 0, ++idxRow);
        final ColorPreview previewModifiedNode = new ColorPreview(colorModifiedNode);
        previewModifiedNode.setOnMouseClicked(event -> {
            colorPicker.setColor(colorModifiedNode.get());
            final Optional<ButtonType> result = colorPicker.showAndWait();
            if (result.isPresent() && result.get().equals(ButtonType.OK)) {
                colorModifiedNode.set(colorPicker.getSelectedColor());
            }
        });
        layout.add(previewModifiedNode, 2, idxRow);

        layout.add(new Label("Node Text"), 0, ++idxRow);
        final ColorPreview previewNodeText = new ColorPreview(colorNodeText);
        previewNodeText.setOnMouseClicked(event -> {
            colorPicker.setColor(colorNodeText.get());
            final Optional<ButtonType> result = colorPicker.showAndWait();
            if (result.isPresent() && result.get().equals(ButtonType.OK)) {
                colorNodeText.set(colorPicker.getSelectedColor());
            }
        });
        layout.add(previewNodeText, 2, idxRow);

        layout.add(new Label("Node Background"), 0, ++idxRow);
        final ColorPreview previewNodeBkg = new ColorPreview(colorNodeBackground);
        previewNodeBkg.setOnMouseClicked(event -> {
            colorPicker.setColor(colorNodeBackground.get());
            final Optional<ButtonType> result = colorPicker.showAndWait();
            if (result.isPresent() && result.get().equals(ButtonType.OK)) {
                colorNodeBackground.set(colorPicker.getSelectedColor());
            }
        });
        layout.add(previewNodeBkg, 2, idxRow);

        layout.add(new Label("Node Background (Selected)"), 0, ++idxRow);
        final ColorPreview previewNodeBkgSelected = new ColorPreview(colorNodeHighlight);
        previewNodeBkgSelected.setOnMouseClicked(event -> {
            colorPicker.setColor(colorNodeHighlight.get());
            final Optional<ButtonType> result = colorPicker.showAndWait();
            if (result.isPresent() && result.get().equals(ButtonType.OK)) {
                colorNodeHighlight.set(colorPicker.getSelectedColor());
            }
        });
        layout.add(previewNodeBkgSelected, 2, idxRow);


        layout.add(new TitleLabel("Logical Networks"), 0, ++idxRow, 3, 1);
        layout.add(new Label("Create CIDRs automatically"), 0, ++idxRow);
        final CheckBox ckCreateCidrs = new CheckBox();
        ckCreateCidrs.selectedProperty().bindBidirectional(networksCreateOnDemand);
        layout.add(ckCreateCidrs, 2, idxRow);

        layout.add(new Label("Created CIDR size"), 0, ++idxRow);
        final Spinner<Integer> spCidrSize = new Spinner<>();
        spCidrSize.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 32));
        spCidrSize.getValueFactory().valueProperty().bindBidirectional(networksSubnetSize);
        layout.add(spCidrSize, 2, idxRow);


        this.getDialogPane().setContent(layout);
        this.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, saveButton);
    }

    public void reloadValues() {
        String ws = Configuration.getPreferenceString(Configuration.Fields.WIRESHARK_EXEC);
        String te = Configuration.getPreferenceString(Configuration.Fields.TEXT_EDITOR_EXEC);
        String pdf = Configuration.getPreferenceString(Configuration.Fields.PDF_VIEWER_EXEC);
        pathWireshark.set(ws == null ? "" : ws);
        pathTextEditor.set(te == null ? "" : te);
        pathPdfViewer.set(pdf == null ? "" : pdf);

        colorNewNode.set(Color.web(Configuration.getPreferenceString(Configuration.Fields.COLOR_NODE_NEW)));
        colorModifiedNode.set(Color.web(Configuration.getPreferenceString(Configuration.Fields.COLOR_NODE_MODIFIED)));
        colorNodeText.set(Color.web(Configuration.getPreferenceString(Configuration.Fields.COLOR_NODE_TEXT)));
        colorNodeBackground.set(Color.web(Configuration.getPreferenceString(Configuration.Fields.COLOR_NODE_BACKGROUND)));
        colorNodeHighlight.set(Color.web(Configuration.getPreferenceString(Configuration.Fields.COLOR_NODE_HIGHLIGHT)));

        networksCreateOnDemand.set(Configuration.getPreferenceBoolean(Configuration.Fields.LOGICAL_CREATE_DYNAMIC_SUBNETS));
        networksSubnetSize.set((int) Configuration.getPreferenceLong(Configuration.Fields.LOGICAL_DYNAMIC_SUBNET_BITS));

        //TODO: Disable save and only enable if anything has changed.
        //this.getDialogPane().lookupButton(saveButton).setDisable(true);
    }

    public Map<Configuration.Fields, String> getUpdatedValues() {
        HashMap<Configuration.Fields, String> updatedValues = new HashMap<>();

        if(Configuration.getPreferenceString(Configuration.Fields.WIRESHARK_EXEC) == null || !pathWireshark.get().equals(Configuration.getPreferenceString(Configuration.Fields.WIRESHARK_EXEC))) {
            updatedValues.put(Configuration.Fields.WIRESHARK_EXEC, pathWireshark.get().equals("") ? null : pathWireshark.get());
        }
        if(Configuration.getPreferenceString(Configuration.Fields.TEXT_EDITOR_EXEC) == null|| !pathTextEditor.get().equals(Configuration.getPreferenceString(Configuration.Fields.TEXT_EDITOR_EXEC))) {
            updatedValues.put(Configuration.Fields.TEXT_EDITOR_EXEC, pathTextEditor.get().equals("") ? null : pathTextEditor.get());
        }
        if(Configuration.getPreferenceString(Configuration.Fields.PDF_VIEWER_EXEC) == null || !pathPdfViewer.get().equals(Configuration.getPreferenceString(Configuration.Fields.PDF_VIEWER_EXEC))) {
            updatedValues.put(Configuration.Fields.PDF_VIEWER_EXEC, pathPdfViewer.get().equals("") ? null : pathWireshark.get());
        }

        // Colors give AARRGGBB, we want only RGB
        if(!colorNewNode.get().equals(Color.web(Configuration.getPreferenceString(Configuration.Fields.COLOR_NODE_NEW)))) {
            updatedValues.put(Configuration.Fields.COLOR_NODE_NEW, colorNewNode.get().toString().substring(2, 8));
        }
        if(!colorModifiedNode.get().equals(Color.web(Configuration.getPreferenceString(Configuration.Fields.COLOR_NODE_MODIFIED)))) {
            updatedValues.put(Configuration.Fields.COLOR_NODE_MODIFIED, colorModifiedNode.get().toString().substring(2, 8));
        }
        if(!colorNodeText.get().equals(Color.web(Configuration.getPreferenceString(Configuration.Fields.COLOR_NODE_TEXT)))) {
            updatedValues.put(Configuration.Fields.COLOR_NODE_TEXT, colorNodeText.get().toString().substring(2, 8));
        }
        if(!colorNodeBackground.get().equals(Color.web(Configuration.getPreferenceString(Configuration.Fields.COLOR_NODE_BACKGROUND)))) {
            updatedValues.put(Configuration.Fields.COLOR_NODE_BACKGROUND, colorNodeBackground.get().toString().substring(2, 8));
        }
        if(!colorNodeHighlight.get().equals(Color.web(Configuration.getPreferenceString(Configuration.Fields.COLOR_NODE_HIGHLIGHT)))) {
            updatedValues.put(Configuration.Fields.COLOR_NODE_HIGHLIGHT, colorNodeHighlight.get().toString().substring(2, 8));
        }

        if(networksCreateOnDemand.get() != Configuration.getPreferenceBoolean(Configuration.Fields.LOGICAL_CREATE_DYNAMIC_SUBNETS)) {
            updatedValues.put(Configuration.Fields.LOGICAL_CREATE_DYNAMIC_SUBNETS, Boolean.toString(networksCreateOnDemand.get()));
        }
        if(networksSubnetSize.get() != Configuration.getPreferenceLong(Configuration.Fields.LOGICAL_DYNAMIC_SUBNET_BITS)) {
            updatedValues.put(Configuration.Fields.LOGICAL_DYNAMIC_SUBNET_BITS, Long.toString(networksSubnetSize.get()));
        }


        return updatedValues;
    }
}
