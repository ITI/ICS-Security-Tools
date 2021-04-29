package ui.fingerprint;

import core.Configuration;
import core.document.fingerprint.FPDocument;
import core.fingerprint.FingerprintBuilder;
import core.fingerprint.FingerprintState;
import core.fingerprint3.Fingerprint;
import core.fingerprint3.ObjectFactory;
import core.logging.Logger;
import core.logging.Severity;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.converter.DefaultStringConverter;
import ui.EmbeddedIcons;
import ui.custom.fx.ValueAddedTextFieldTreeCell;
import ui.fingerprint.editorPanes.FilterEditPane;
import ui.fingerprint.editorPanes.FilterGroupEditPane;
import ui.fingerprint.editorPanes.FingerprintInfoPane;
import ui.fingerprint.editorPanes.PayloadEditorPane;
import ui.fingerprint.filters.Filter;
import ui.fingerprint.tree.FPItem;
import ui.fingerprint.tree.FilterGroupItem;
import ui.fingerprint.tree.FilterItem;
import ui.fingerprint.tree.PayloadItem;

import javax.xml.bind.JAXBElement;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class FingerPrintGui extends Application {

    private FPDocument document;

    private SplitPane content;

    private TreeView<String> tree;

    private TreeItem<String> rootItem;

    private ObjectFactory factory;

    private BooleanProperty selectedDirtyProperty;

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception{
        this.primaryStage = primaryStage;

        primaryStage.setTitle("GrassMarlin Fingerprint Editor");

        primaryStage.getIcons().add(EmbeddedIcons.Logo.getRawImage());
        primaryStage.getIcons().add(EmbeddedIcons.Logo_Large.getRawImage());
        primaryStage.setOnCloseRequest(this::checkDirtyOnClosing);

        this.document = FPDocument.getInstance();

        this.document.getFingerprints().addListener(this::handleFingerprintChange);

        this.factory = new ObjectFactory();

        this.selectedDirtyProperty = new SimpleBooleanProperty(false);

        this.rootItem = new TreeItem<>();

        this.rootItem.setExpanded(true);

        this.tree = new TreeView<>(rootItem);
        this.tree.setShowRoot(false);
        this.tree.setCellFactory(this::getCellWithMenu);
        this.tree.setOnKeyPressed(this::handleKeyPressed);
        this.tree.setEditable(true);
        this.tree.getSelectionModel().getSelectedItems().addListener(this::handleSelectionChange);

        if (!this.document.getFingerprints().isEmpty()) {
            List<FPItem> items = this.document.getFingerprints().stream()
            .map(fp -> {
                FPItem item = createTree(fp);
                return item;
            })
            .filter(item -> item != null)
            .collect(Collectors.toList());

            this.rootItem.getChildren().addAll(items);
        }



        BorderPane root = new BorderPane();

        root.setTop(new TopMenu(this));

        content = new SplitPane();
        content.setOrientation(Orientation.HORIZONTAL);
        content.setDividerPositions(0.25);

        content.getItems().add(this.tree);

        BorderPane conditionPane = new BorderPane();

        content.getItems().add(conditionPane);

        root.setCenter(content);

        Dimension screenDimensions = Toolkit.getDefaultToolkit().getScreenSize();
        primaryStage.setScene(new Scene(root, screenDimensions.getWidth() * 0.85, screenDimensions.getHeight() * 0.85));
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public FPDocument getDocument() {
        return this.document;
    }

    void newFingerprint() {
        String defaultName = "New Fingerprint";

        boolean exists;
        int count = 1;
        do {
            exists = false;
            for (FingerprintState fingerprint : this.document.getFingerprints()) {
                if (fingerprint.equals(defaultName, null)) {
                    defaultName = "New Fingerprint" + count++;
                    exists = true;
                    break;
                }
            }
        } while (exists);

        String defaultUser = System.getProperty("user.name", "User");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        String defaultDescription = "A new fingerprint created by " + defaultUser + " at " + timestamp;

        this.document.newFingerprint(defaultName, defaultUser, defaultDescription);
    }

    void newPayload(FPItem fingerprint) {
        String defaultName = "New Payload";

        boolean exists;
        int count = 1;

        do {
            exists = false;
            for (TreeItem<String> payload : fingerprint.getChildren()) {
                if (payload.getValue().equals(defaultName)) {
                    defaultName = "New Payload" + count++;
                    exists = true;
                    break;
                }
            }
        } while (exists);

        boolean added = this.document.newPayload(fingerprint.getName(), fingerprint.pathProperty().get(), defaultName);
        if (added) {
            Fingerprint.Payload payload = factory.createFingerprintPayload();
            payload.setFor(defaultName);
            PayloadItem pl = new PayloadItem(payload);
            fingerprint.getChildren().add(pl);
            this.tree.getSelectionModel().select(pl);
        }
    }

    void newFilterGroup(PayloadItem payload) {
        String defaultName = "New Group";

        boolean exists;
        int count = 1;

        do {
            exists = false;
            for (TreeItem<String> group : payload.getChildren()) {
                if (group.getValue().equals(defaultName)) {
                    defaultName = "New Group" + count++;
                    exists = true;
                    break;
                }
            }
        } while (exists);

        if (payload.getParent() instanceof FPItem) {
            FPItem fp = ((FPItem) payload.getParent());

            boolean added = this.document.newFilterGroup(fp.getName(), fp.pathProperty().get(), payload.getName(), defaultName);
            if (added) {
                FilterGroupItem fg = new FilterGroupItem(defaultName);
                payload.getChildren().add(fg);
                this.tree.getSelectionModel().select(fg);
            }
        }
    }

    private void handleSelectionChange(ListChangeListener.Change<? extends TreeItem<String>> change) {

        while (change.next()) {

            if (change.wasReplaced() || change.wasAdded()) {
                TreeItem<String> selected = change.getAddedSubList().get(0);

                Pane editPane;

                if (selected instanceof FPItem) {
                    FPItem fp = (FPItem) selected;
                    editPane = new FingerprintInfoPane(fp, this);
                } else if (selected instanceof PayloadItem) {
                    PayloadItem payload = (PayloadItem) selected;
                    editPane = PayloadEditorPane.getInstance(payload, this);
                } else if (selected instanceof FilterItem) {
                    FilterItem fi = (FilterItem) selected;
                    editPane = new FilterEditPane(fi, this);
                } else if (selected instanceof FilterGroupItem) {
                    FilterGroupItem fgi = ((FilterGroupItem) selected);
                    editPane = new FilterGroupEditPane(fgi, this);
                } else {
                    Pane empty = new Pane();
                    editPane = empty;
                }

                FPItem fp = this.getSelectedFPItem();
                if (fp != null) {
                    this.selectedDirtyProperty.bind(fp.dirtyProperty());
                }

                Node conditionPane = content.getItems().get(1);
                if (conditionPane instanceof BorderPane) {
                    ((BorderPane) conditionPane).setCenter(editPane);
                }
            } else if (change.wasRemoved()) {
                Node conditionPane = content.getItems().get(1);
                if (conditionPane instanceof  BorderPane) {
                    ((BorderPane) conditionPane).setCenter(new Pane());
                }

                this.selectedDirtyProperty.unbind();
                this.selectedDirtyProperty.setValue(false);
            }
        }
    }

    private void handleFingerprintChange(ListChangeListener.Change<? extends FingerprintState> change) {
        while(change.next()) {
            if (change.wasRemoved()) {
                processFingerprintRemoved(change.getRemoved());
            } else if (change.wasAdded()) {
                processFingerprintAdded(change.getAddedSubList());
            }
        }
    }

    private void handleKeyPressed(KeyEvent event) {
        TreeItem<String> selected = this.tree.getSelectionModel().getSelectedItem();
        if (null != selected && event.getCode() == KeyCode.DELETE) {
            if (selected instanceof FPItem) {
                boolean close = true;
                FPItem fp = ((FPItem) selected);
                if (fp.dirtyProperty().get()) {
                    Dialog<ButtonType> saveDialog = this.getSaveOnCloseDialog(fp.getName(), fp.pathProperty().get());
                    Optional<ButtonType> choice = saveDialog.showAndWait();
                    if (choice.isPresent()) {
                        switch(choice.get().getButtonData()) {
                            case YES:
                                close = this.saveFingerprintWODialog(null);
                                break;
                            case CANCEL_CLOSE:
                                close = false;
                                break;
                            case NO:
                                break;
                            default:
                                Alert unknownAlert = new Alert(Alert.AlertType.WARNING, "Invalid Selection");
                                unknownAlert.showAndWait();
                        }
                    }
                }
                int removeIndex = -1;
                for (FingerprintState fpState : this.document.getFingerprints()) {
                    if (fpState.equals(fp.getName(), fp.pathProperty().get())) {
                        removeIndex = this.document.getFingerprints().indexOf(fpState);
                        break;
                    }
                }
                if (close) {
                    if (removeIndex >= 0) {
                        this.document.getFingerprints().remove(removeIndex);
                        int numFingerprints = this.document.getFingerprints().size();
                        SelectionModel<TreeItem<String>> selectionModel = this.tree.getSelectionModel();
                        if (numFingerprints > 1) {
                            if (removeIndex == numFingerprints) {
                                // select previous fingerprint
                                while (!(selectionModel.getSelectedItem() instanceof FPItem)) {
                                    selectionModel.select(selectionModel.getSelectedIndex() - 1);
                                }
                            } else if (removeIndex == 0) {
                                selectionModel.selectFirst();
                            } else {
                                selectionModel.selectNext();
                            }
                        } else if (numFingerprints == 1) {
                            selectionModel.selectFirst();
                        } else {
                            selectionModel.clearSelection();
                        }
                    }
                }
            } else if (selected instanceof PayloadItem) {
                PayloadItem pl = ((PayloadItem) selected);
                FPItem fp = getFPItem(pl);
                if (this.document.delPayload(fp.getName(), fp.pathProperty().get(), pl.getName())) {
                    SelectionModel<TreeItem<String>> selectionModel = this.tree.getSelectionModel();
                    TreeItem<String> parent = selected.getParent();
                    selected.getParent().getChildren().remove(selected);
                    int numPayloads = parent.getChildren().size();
                    if (numPayloads == 0) {
                        //no payloads left
                        selectionModel.select(parent);
                    } else if (numPayloads == 1) {
                        //select the only payload
                        selectionModel.select(parent.getChildren().get(0));
                    } else {
                        //find the next payload or previous if no next
                        int startIndex = selectionModel.getSelectedIndex();
                        //Since this is a tree the next node will either be the next sibling of the
                        //deleted node or the next sibling of its parent or nothing
                        selectionModel.select(startIndex + 1);
                        if (!(selectionModel.getSelectedItem() instanceof PayloadItem)) {
                            while (!(selectionModel.getSelectedItem() instanceof PayloadItem)) {
                                selectionModel.select(startIndex--);
                            }
                        }
                    }
                }

            }  else if (selected instanceof FilterGroupItem) {
                FilterGroupItem group = ((FilterGroupItem) selected);
                FPItem fp = getFPItem(group);
                if (this.document.delFilterGroup(fp.getName(), fp.pathProperty().get(), getPayloadItem(group).getName(), group.getName())) {
                    SelectionModel<TreeItem<String>> selectionModel = this.tree.getSelectionModel();
                    TreeItem<String> parent = selected.getParent();
                    selected.getParent().getChildren().remove(selected);
                    int numGroups = parent.getChildren().size();
                    if (numGroups == 0) {
                        selectionModel.select(parent);
                    } else if (numGroups == 1) {
                        selectionModel.select(parent.getChildren().get(0));
                    } else {
                        int startIndex = selectionModel.getSelectedIndex();

                        selectionModel.select(startIndex + 1);
                        if (!(selectionModel.getSelectedItem() instanceof FilterGroupItem)) {
                            while (!(selectionModel.getSelectedItem() instanceof FilterGroupItem)) {
                                selectionModel.select(startIndex--);
                            }
                        }
                    }
                }
            } else if (selected instanceof FilterItem) {
                FilterItem filter = ((FilterItem) selected);
                SelectionModel<TreeItem<String>> selectionModel = this.tree.getSelectionModel();
                if (this.deleteFilter(filter)) {
                    TreeItem<String> parent = filter.getParent();
                    parent.getChildren().remove(filter);
                    int numFilters = parent.getChildren().size();
                    if (numFilters == 0) {
                        selectionModel.select(parent);
                    } else if (numFilters == 1) {
                        selectionModel.select(parent.getChildren().get(0));
                    } else {
                        int startIndex = selectionModel.getSelectedIndex();

                        selectionModel.select(startIndex + 1);
                        if (!(selectionModel.getSelectedItem() instanceof FilterItem)) {
                            while (!(selectionModel.getSelectedItem() instanceof FilterItem)) {
                                selectionModel.select(startIndex--);
                            }
                        }
                    }
                }
            }
        }
    }

    private void processFingerprintRemoved(List<? extends FingerprintState> removed) {
        List<Integer> indicesToRemove = new ArrayList<>();
        removed.forEach(fingerprint -> {
            this.rootItem.getChildren().forEach(child -> {
                if (fingerprint.getFingerprint().getHeader().getName().equals(child.getValue())) {
                    indicesToRemove.add(this.rootItem.getChildren().indexOf(child));
                }
            });
        });

        Collections.sort(indicesToRemove, Comparator.<Integer>reverseOrder());

        indicesToRemove.forEach(index -> {
            //This is dumb, but removing by index wasn't working for some reason
            TreeItem<String> toRemove = this.rootItem.getChildren().get(index);
            this.rootItem.getChildren().remove(toRemove);
        });
    }

    private void processFingerprintAdded(List<? extends FingerprintState> added) {
        TreeItem<String> firstNewItem = null;
        for (FingerprintState fingerprint : added) {
            FPItem item = createTree(fingerprint);
            if (item != null) {
                this.rootItem.getChildren().add(item);
                this.rootItem.getChildren().sort((ti1, ti2) -> ti1.getValue().compareTo(ti2.getValue()));
                if (null == firstNewItem) {
                    firstNewItem = item;
                }
            }
        }
        if (null != firstNewItem) {
            this.tree.getSelectionModel().select(firstNewItem);
        }
    }

    private FPItem createTree(FingerprintState fpState) {

        Map<String, PayloadItem> payloadMap = new HashMap<>();
        Fingerprint fp = fpState.getFingerprint();
        FPItem item = new FPItem(fpState);

        for (Fingerprint.Payload payload : fp.getPayload()) {
            PayloadItem payloadItem = new PayloadItem(payload);
            payloadMap.put(payload.getFor(), payloadItem);
            item.getChildren().add(payloadItem);
        }
        for (Fingerprint.Filter group : fp.getFilter()) {
            FilterGroupItem groupItem = new FilterGroupItem(group.getName());
            group.getAckAndMSSAndDsize().forEach(filter -> {
                FilterItem newFilter = new FilterItem(Filter.FilterType.valueOf(filter.getName().toString().replaceAll(" ", "").toUpperCase()), group.getAckAndMSSAndDsize().indexOf(filter), this, filter);
                groupItem.getChildren().add(newFilter);
            });
            PayloadItem payload = payloadMap.get(group.getFor());
            if (payload != null) {
                payload.getChildren().add(groupItem);
            } else {
                Logger.log(this, Severity.Warning, "Malformed Fingerprint: Filter group without payload");
                item = null;
            }
        }

        return item;
    }

    private TreeCell<String> getCellWithMenu(TreeView<String> view) {
        ValueAddedTextFieldTreeCell<String> newCell = new ValueAddedTextFieldTreeCell<String>(new DefaultStringConverter()){

            @Override
            public void updateItem(String item, boolean empty) {
                this.setBinding(null);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (!item.isEmpty()) {
                    TreeItem<String> treeItem = this.getTreeItem();
                    if (treeItem instanceof FPItem) {
                        FPItem fpItem = (FPItem) treeItem;
                        this.setBinding(Bindings.when(fpItem.pathProperty().isNotNull()).then(Bindings.concat("    ", fpItem.pathProperty())).otherwise(""));
                        // only need to do this if the name has actually changed
                        if (!fpItem.getName().equals(item)) {
                            boolean updated = FingerPrintGui.this.getDocument().updateFingerprintName(fpItem.getName(), item, fpItem.pathProperty().get());
                            if (!updated) {
                                this.cancelEdit();
                                this.getTreeItem().setValue(fpItem.getName());
                            } else {
                                fpItem.setName(item);
                            }
                        }
                    } else if (treeItem instanceof PayloadItem) {
                        PayloadItem payloadItem = (PayloadItem) treeItem;
                        // only need to do this if the name has actually changed
                        if (!payloadItem.getName().equals(item)) {
                            FPItem fp = getFPItem(payloadItem);
                            boolean updated = FingerPrintGui.this.getDocument().updatePayloadName
                                    (fp.getName(), fp.pathProperty().get(), payloadItem.getName(), item);
                            if (!updated) {
                                this.cancelEdit();
                                this.getTreeItem().setValue(payloadItem.getName());
                            } else {
                                payloadItem.setName(item);
                            }
                        }
                    } else if (treeItem instanceof FilterGroupItem) {
                        FilterGroupItem fgItem = (FilterGroupItem) treeItem;
                        // only need to do this if the name has actually changed
                        if (!fgItem.getName().equals(item)) {
                            FPItem fp = getFPItem(fgItem);
                            boolean updated = FingerPrintGui.this.getDocument().updateFilterGroupName
                                    (fp.getName(), fp.pathProperty().get(), getPayloadItem(fgItem).getName(), fgItem.getName(), item);
                            if (!updated) {
                                this.cancelEdit();
                                this.getTreeItem().setValue(fgItem.getName());
                            } else {
                                fgItem.setName(item);
                            }
                        }
                    }
                } else {
                    this.cancelEdit();
                }

                super.updateItem(item, empty);
            }
        };

        newCell.treeItemProperty().addListener((observable, oldValue, newValue) -> {
            if (null != newValue) {
                if (newValue instanceof FPItem) {
                    newCell.setContextMenu(getFingerprintCM((FPItem) newValue));
                } else if (newValue instanceof PayloadItem) {
                    newCell.setContextMenu(getPayloadCM((PayloadItem) newValue));
                } else if (newValue instanceof FilterGroupItem) {
                    newCell.setContextMenu(getGroupCM((FilterGroupItem) newValue));
                } else if (newValue instanceof FilterItem) {
                    newCell.setContextMenu(getFilterCM((FilterItem) newValue));
                    newCell.setEditable(false);
                }
            }
        });

        newCell.setConcatFill(Color.GRAY);
        newCell.setConcatOverrun(OverrunStyle.CENTER_ELLIPSIS);
        newCell.prefWidthProperty().bind(this.tree.widthProperty().subtract(10));


        return newCell;
    }

    private ContextMenu getFingerprintCM(FPItem source) {
        ContextMenu menu = new ContextMenu();
        menu.addEventFilter(MouseEvent.MOUSE_RELEASED, this::disableRightClick);

        MenuItem newItem = new MenuItem("New Payload");
        newItem.setOnAction(event -> {
            FingerPrintGui.this.newPayload(source);
            source.setExpanded(true);
        });

        MenuItem saveItem = new MenuItem("Save");
        saveItem.setOnAction(this::saveFingerprintWODialog);
        saveItem.disableProperty().bind(source.dirtyProperty().not());

        menu.getItems().addAll(saveItem, new SeparatorMenuItem(), newItem);

        return menu;
    }

    private ContextMenu getPayloadCM(PayloadItem source) {
        ContextMenu menu = new ContextMenu();
        menu.addEventFilter(MouseEvent.MOUSE_RELEASED, this::disableRightClick);

        MenuItem item = new MenuItem("New Filter Group");
        item.setOnAction(event -> {
            FingerPrintGui.this.newFilterGroup(source);
            source.setExpanded(true);
        });


        menu.getItems().add(item);

        return menu;
    }

    private ContextMenu getGroupCM(FilterGroupItem source) {
        ContextMenu menu = new ContextMenu();
        menu.addEventFilter(MouseEvent.MOUSE_RELEASED, this::disableRightClick);

        MenuItem item = new MenuItem("New Filter");
        item.setOnAction(event -> {
            FingerPrintGui.this.addFilter(source, true);
            source.setExpanded(true);
        });

        menu.getItems().add(item);

        return menu;
    }

    private ContextMenu getFilterCM(FilterItem source) {
        ContextMenu menu = new ContextMenu();
        menu.addEventFilter(MouseEvent.MOUSE_RELEASED, this::disableRightClick);

        MenuItem add = new MenuItem("Add");
        add.setOnAction(event -> {
            TreeItem<String> parent = source.getParent();
            FingerPrintGui.this.addFilter(parent, true);
        });


        menu.getItems().add(add);

        return menu;
    }

    private void disableRightClick(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY) {
            event.consume();
        }
    }

    public void addFilter(TreeItem<String> parent, boolean focus) {
        FPItem fp = getFPItem(parent);
        int index = this.document.addFilter(fp.getName(), fp.pathProperty().get(), getPayloadItem(parent).getName(),
                getGroupItem(parent).getName(), getDefaultFilterElement());

        if (index >= 0) {
            FilterItem filter = new FilterItem(Filter.FilterType.DSTPORT, index, this);

            parent.getChildren().add(filter);
            if (focus) {
                this.tree.getSelectionModel().select(filter);
                filter.setFocus();
            }
        }
    }

    private boolean deleteFilter(FilterItem filter) {
        boolean deleted = false;
        HashMap<Integer, Integer> newIndices = new HashMap<>();
        FPItem fp = getFPItem(filter);
        if (this.document.deleteFilter(newIndices, fp.getName(), fp.pathProperty().get(),
                getPayloadItem(filter).getName(), getGroupItem(filter).getName(), filter.getIndex())) {
            deleted = true;
            TreeItem<String> parent = filter.getParent();
            parent.getChildren().forEach(child -> {
                if (child instanceof FilterItem) {
                    Integer newIndex = newIndices.get(((FilterItem) child).getIndex());
                    if (newIndex != null) {
                        ((FilterItem) child).setIndex(newIndex);
                    }
                }
            });
        }

        return deleted;
    }

    public JAXBElement<Integer> getDefaultFilterElement() {
        return factory.createFingerprintFilterDstPort(80);
    }

    public FPItem getFPItem(TreeItem<String> item) {
        if (item instanceof FPItem) {
            return ((FPItem) item);
        } else if (item instanceof PayloadItem) {
            return getFPItem((PayloadItem) item);
        } else if (item instanceof FilterGroupItem) {
            return getFPItem((FilterGroupItem) item);
        } else if (item instanceof FilterItem) {
            return getFPItem((FilterItem) item);
        } else {
            throw new IllegalArgumentException("Unknown tree item type");
        }
    }

    public PayloadItem getPayloadItem(TreeItem<String> item) {
        if (item instanceof PayloadItem) {
            return ((PayloadItem) item);
        } else if (item instanceof FilterGroupItem) {
            return getPayloadItem((FilterGroupItem) item);
        } else if (item instanceof FilterItem) {
            return getPayloadItem((FilterItem) item);
        } else {
            throw new IllegalArgumentException("Unknown tree item type");
        }
    }

    public FilterGroupItem getGroupItem(TreeItem<String> item) {
        if (item instanceof FilterGroupItem) {
            return ((FilterGroupItem) item);
        } else if (item instanceof FilterItem) {
            return getGroupItem((FilterItem) item);
        } else {
            throw new IllegalArgumentException("Unknown tree item type");
        }
    }

    public FPItem getFPItem(PayloadItem item) {
        if (item.getParent() instanceof FPItem) {
            return ((FPItem) item.getParent());
        } else {
            return null;
        }
    }

    public FPItem getFPItem(FilterGroupItem item) {
        if (item.getParent().getParent() instanceof  FPItem) {
            return ((FPItem) item.getParent().getParent());
        } else {
            return null;
        }
    }

    public FPItem getFPItem(FilterItem item) {
        if (item.getParent().getParent().getParent() instanceof  FPItem) {
            return ((FPItem) item.getParent().getParent().getParent());
        } else {
            return null;
        }
    }

    public PayloadItem getPayloadItem(FilterGroupItem item) {
        if (item.getParent() instanceof PayloadItem) {
            return ((PayloadItem) item.getParent());
        } else {
            return null;
        }
    }

    public PayloadItem getPayloadItem(FilterItem item) {
        if (item.getParent().getParent() instanceof PayloadItem) {
            return ((PayloadItem) item.getParent().getParent());
        } else {
            return null;
        }
    }

    public FilterGroupItem getGroupItem(FilterItem item) {
        if (item.getParent() instanceof FilterGroupItem) {
            return ((FilterGroupItem) item.getParent());
        } else {
            return null;
        }
    }

    public static void selectAll(TextField field) {
        // currently required to make this work
        // apparently the select all gets overridden  if it's not run later
        Platform.runLater(() -> {
            if (field.isFocused()) {
                field.selectAll();
            }
        });
    }

    public void updatePayloadDescription(PayloadItem payload, String description) {
        FPItem fp = getFPItem(payload);
        this.document.updatePayloadDescription(fp.getName(), fp.pathProperty().get(), payload.getPayload().getFor(), description);
    }

    public void updateAlways(PayloadItem payload, Fingerprint.Payload.Always always) {
        FPItem fp = getFPItem(payload);
        this.document.updateAlways(fp.getName(), fp.pathProperty().get(), payload.getPayload().getFor(), always);
    }

    public void updateOperations(PayloadItem payload, List<Object> operationList) {
        FPItem fp = getFPItem(payload);
        this.document.updateOperations(fp.getName(), fp.pathProperty().get(), payload.getPayload().getFor(), operationList);
    }

    public FPItem getSelectedFPItem() {
        TreeItem<String> selected = this.tree.getSelectionModel().getSelectedItem();
        FPItem fpName = selected != null ? getFPItem(selected) : null;

        return fpName;
    }

    public BooleanProperty isSelectedDirtyProperty() {
        return this.selectedDirtyProperty;
    }

    public static Path getDefaultFingerprintDir() {
        return Paths.get(Configuration.getPreferenceString(Configuration.Fields.DIR_USER_FINGERPRINTS));
    }

    public static Path getSystemFingerprintDir() {
        return Paths.get(Configuration.getPreferenceString(Configuration.Fields.DIR_CORE_FINGERPRINTS));
    }

    public boolean saveFingerprintWDialog(ActionEvent event) {
        FPItem fp = this.getSelectedFPItem();

        boolean saved =  this.saveFingerprintWDialog(fp.getName(), fp.pathProperty().get());

        return saved;
    }


    public boolean saveFingerprintWDialog(String fpName, Path loadPath) {
        boolean saved;
        FileChooser chooser = new FileChooser();

        String fileName;
        Path initialDir;
        if (loadPath != null) {
            initialDir = loadPath.getParent();
            fileName = loadPath.getFileName().toString();
        } else {
            initialDir = getDefaultFingerprintDir();
            fileName = fpName;
        }
        if (initialDir != null) {
            chooser.setInitialDirectory(initialDir.toFile());
        }


        FileChooser.ExtensionFilter xmlFilter = new FileChooser.ExtensionFilter("Fingerprint", "*.xml");
        FileChooser.ExtensionFilter everything = new FileChooser.ExtensionFilter("All", "*.*");
        chooser.getExtensionFilters().addAll(xmlFilter, everything);
        chooser.setSelectedExtensionFilter(xmlFilter);
        chooser.setInitialFileName(fileName);

        File toSave = chooser.showSaveDialog(this.content.getScene().getWindow());

        while (toSave != null &&
                (toSave.getAbsolutePath().contains(FingerprintBuilder.WINDOWS_EXCLUSION_PATH) ||
                        toSave.getAbsolutePath().contains(FingerprintBuilder.LINUX_EXCLUSION_PATH))) {
            Alert badPathAlert = new Alert(Alert.AlertType.WARNING, "Can not save to the Default Fingerprints Directory");
            badPathAlert.showAndWait();
            toSave = chooser.showSaveDialog(this.content.getScene().getWindow());
        }

        if (toSave != null) {
            saved = this.saveFingerprint(fpName, loadPath, toSave.toPath());
        } else {
            saved = false;
        }

        return saved;
    }

    public boolean saveFingerprintWODialog(ActionEvent event) {

        FPItem fp = this.getSelectedFPItem();
        Path fpPath = fp.pathProperty().get();

        return this.saveFingerprintWODialog(fp.getName(), fpPath);
    }

    public boolean saveFingerprintWODialog(String fpName, Path loadPath) {
        boolean saved;
        if (!(loadPath == null || loadPath.toAbsolutePath().toString().contains(FingerprintBuilder.WINDOWS_EXCLUSION_PATH) || loadPath.toAbsolutePath().toString().contains(FingerprintBuilder.LINUX_EXCLUSION_PATH))) {
            saved = this.saveFingerprint(fpName, loadPath, loadPath);
        } else {
            saved = saveFingerprintWDialog(fpName, loadPath);
        }

        return saved;
    }

    private boolean saveFingerprint(String fpName, Path loadPath, Path toSave) {
        boolean saved = false;
        if (null != toSave) {
            try {
                this.document.save(fpName, loadPath, toSave);
                saved = true;
            } catch (Exception e) {
                if (e.getCause() != null && e.getCause().getMessage() != null) {
                    String[] errorArray = e.getCause().getMessage().split(":");
                    Alert error = new Alert(Alert.AlertType.WARNING, "Unable to save Fingerprint: " + errorArray[errorArray.length - 1]);
                    error.showAndWait();
                } else {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Unknown error saving Fingerprint!");
                    error.showAndWait();
                }
            }
        }
        return saved;
    }

    private void checkDirtyOnClosing(WindowEvent event) {
        mainLoop:
        for (FingerprintState state : this.document.getFingerprints()) {
            if (state.dirtyProperty().get()) {
                Optional<ButtonType> choice = this.getSaveOnCloseDialog(state.getFingerprint().getHeader().getName(),
                        state.pathProperty().get()).showAndWait();

                if (choice.isPresent()) {
                    switch (choice.get().getButtonData()) {
                        case YES:
                            boolean saved = this.saveFingerprintWODialog(state.getFingerprint().getHeader().getName(), state.pathProperty().get());
                            if (!saved) {
                                event.consume();
                                break mainLoop;
                            }
                            break;
                        case NO:
                            break;
                        case CANCEL_CLOSE:
                            event.consume();
                            break;
                        default:
                    }
                }
            }
        }

    }

    public Dialog<ButtonType> getSaveOnCloseDialog(String fpName, Path loadPath) {
        String atString = loadPath != null && !loadPath.toString().isEmpty() ? loadPath.toString() : "";
        Dialog<ButtonType> saveDialog = new Dialog<>();
        saveDialog.getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        saveDialog.setTitle("Closing");
        saveDialog.setHeaderText("Closing \"" + fpName + "\"\n\nWould you like to Save?");
        saveDialog.setContentText(atString);

        return saveDialog;
    }

    public void enableAll(ActionEvent event) {
        this.document.getFingerprints().forEach(state -> state.enabledProperty().setValue(true));
    }

    public void disableAll(ActionEvent event) {
        this.document.getFingerprints().forEach(state -> state.enabledProperty().setValue(false));
    }

    public void enableSelected(ActionEvent event) {
        FPItem selected = this.getSelectedFPItem();
        if (!this.document.setEnabled(selected.getName(), selected.pathProperty().get(), true)) {
            new Alert(Alert.AlertType.ERROR, "Unable to Find Fingerprint " + selected.getName());
        }
    }

    public void disableSelected(ActionEvent event) {
        FPItem selected = this.getSelectedFPItem();
        if (!this.document.setEnabled(selected.getName(), selected.pathProperty().get(), false)) {
            new Alert(Alert.AlertType.ERROR, "Unable to Find Fingerprint " + selected.getName());
        }
    }

    public void exit() {
        WindowEvent.fireEvent(this.primaryStage, new WindowEvent(this.primaryStage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
