package ui.dialog;

import com.sun.javafx.collections.ObservableListWrapper;
import core.Configuration;
import core.logging.Logger;
import core.logging.Severity;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import ui.EmbeddedIcons;
import ui.GrassMarlinFx;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class FilterDialogFx extends Dialog<ButtonType> {
    protected static class PcapFilter {
        private final SimpleBooleanProperty active;
        private final SimpleStringProperty name;
        private final SimpleStringProperty filter;

        public PcapFilter() {
            active = new SimpleBooleanProperty(false);
            name = new SimpleStringProperty("");
            filter = new SimpleStringProperty("");

            BindProperties();
        }

        public PcapFilter(String text) {
            active = new SimpleBooleanProperty(false);
            name = new SimpleStringProperty(text.toUpperCase());
            filter = new SimpleStringProperty(text);

            BindProperties();
        }

        public PcapFilter(String name, String filter) {
            active = new SimpleBooleanProperty(false);
            this.name = new SimpleStringProperty(name);
            this.filter = new SimpleStringProperty(filter);

            BindProperties();
        }

        /**
         * Bind active and add a handler to name to update the active if the active is renamed.
         */
        private void BindProperties() {
            active.bind(this.name.isEqualTo(GrassMarlinFx.selectedPcapFilterProperty()));
            name.addListener((observable, oldValue, newValue) -> {
                if(oldValue.equals(GrassMarlinFx.selectedPcapFilterProperty().get())) {
                    GrassMarlinFx.selectedPcapFilterProperty().set(newValue);
                }
            });
            filterProperty().addListener((observable, oldValue, newValue) -> {
                if(activeProperty().get()) {
                    if(!oldValue.equals(newValue)) {
                        Configuration.setPreferenceString(Configuration.Fields.PCAP_FILTER_STRING, newValue );
                    }
                }
            });
        }

        // Despite the apparent lack of references, these are accessed via reflection by the table view.
        public BooleanProperty activeProperty() {
            return active;
        }
        public StringProperty nameProperty() {
            return name;
        }
        public StringProperty filterProperty() {
            return filter;
        }
    }

    private ObservableListWrapper<PcapFilter> filters;

    public FilterDialogFx() {
        filters = new ObservableListWrapper<>(new ArrayList<>(3));

        initComponents();
    }

    private void initComponents() {
        setTitle("PCAP Filter Manager");
        Window stage = super.getDialogPane().getScene().getWindow();
        if(stage instanceof Stage) {
            ((Stage)stage).getIcons().add(EmbeddedIcons.Vista_Filter.getRawImage());
        }
        super.setResizable(true);

        VBox layout = new VBox();

        // New Filter
        //TODO: The layout for this can be nicer than two controls in a HBox.
        HBox layoutNewItem = new HBox();
        TextField textNew = new TextField("");
        Button btnNew = new Button("Add");
        textNew.textProperty().addListener((observable, oldValue, newValue) -> {
            btnNew.setDisable(!isValidBpfString(newValue));
        });
        //Enter in text box should fire the 'Add' button, not close the form.
        textNew.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if(event.getCode() == KeyCode.ENTER) {
                btnNew.fire();
                event.consume();
            }
        });
        btnNew.setOnAction(event -> {
            filters.add(new PcapFilter(textNew.getText()));
            textNew.setText("");
        });
        layoutNewItem.getChildren().addAll(textNew, btnNew);
        layout.getChildren().add(layoutNewItem);

        TableView<PcapFilter> tableFilters = new TableView<>();
        TableColumn<PcapFilter, Boolean> colActive = new TableColumn<>("Active");
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        colActive.setCellFactory((column) -> new TableCell<PcapFilter, Boolean>() {
            @Override
            public void updateItem(Boolean isActive, boolean empty) {
                setText("");
                setStyle("-fx-alignment:center");

                this.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && getTableRow().getItem() != null) {
                        // When we set the selected filter in GrassMarlinFx, the change will be committed to the configuration object (and therefore to disk) directly.
                        GrassMarlinFx.selectedPcapFilterProperty().set(((PcapFilter) getTableRow().getItem()).nameProperty().get());
                        Configuration.setPreferenceString(Configuration.Fields.PCAP_FILTER_STRING, ((PcapFilter)getTableRow().getItem()).filterProperty().get() );
                        //It doesn't like to update the previously-active row without a call to refresh.
                        getTableView().refresh();
                    }
                });

                if (isActive == null || empty) {
                    //Leave blank
                } else {
                    if (isActive) {
                        setGraphic(EmbeddedIcons.Vista_Enable.getImage(16.0));
                    }
                }
            }
        });
        TableColumn<PcapFilter, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setCellFactory(TextFieldTableCell.forTableColumn());
        colName.setEditable(true);
        TableColumn<PcapFilter, String> colFilter = new TableColumn<>("Filter");
        colFilter.setCellValueFactory(new PropertyValueFactory<>("filter"));
        colFilter.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<String>() {
            @Override
            public String toString(String object) {
                return object;
            }

            @Override
            public String fromString(String string) throws IllegalArgumentException {
                if (isValidBpfString(string)) {
                    return string;
                }
                // The only way I have found to prevent an edit from being accepted is to throw an exception in the converter.
                // It seems that once the commit event fires, it can't be stopped.
                throw new IllegalArgumentException("Not a valid BPF string: " + string);
            }
        }));

        colName.setEditable(true);

        tableFilters.setEditable(true);
        tableFilters.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.DELETE) {
                // Use a new list to store a cached copy of the selected items.
                filters.removeAll(new ArrayList<>(tableFilters.getSelectionModel().getSelectedItems()));
                tableFilters.getSelectionModel().clearSelection();
            }
        });

        tableFilters.getColumns().addAll(colActive, colName, colFilter);
        tableFilters.setItems(filters);

        layout.getChildren().addAll(tableFilters);
        super.getDialogPane().setContent(layout);

        //TODO: Add support for cancel; this will require a more in-depth transient state to be tracked
        //super.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE, ButtonType.OK);
        super.getDialogPane().getButtonTypes().addAll(ButtonType.OK);
    }

    protected static Path getSavedDataPath() {
        // Actively evaluate this every time it is called in case the preferences have changed.
        return Paths.get(Configuration.getPreferenceString(Configuration.Fields.PATH_PCAP_FILTERS));
    }

    public void LoadData() {
        Path pathData = getSavedDataPath();
        if(Files.exists(pathData)) {
            try {
                filters.clear();
                filters.addAll(
                        Files.lines(pathData).map(text -> {
                            if(text == null || text.isEmpty()) {
                                return null;
                            }
                            String[] tokens = text.split("//");
                            if(tokens.length != 2) {
                                //No delimiter found--assume it is just a label.  The delimiter is also probably present, so we still use the results of the split() call.
                                return new PcapFilter(new String(Base64.getDecoder().decode(tokens[0])), "");
                            } else {
                                return new PcapFilter(new String(Base64.getDecoder().decode(tokens[0])), new String(Base64.getDecoder().decode(tokens[1])));
                            }
                        }).filter(item -> item != null).collect(Collectors.toList()));
            } catch (IOException ex) {
                Logger.log(this, Severity.Error, "Unable to load filters: " + ex.getMessage());
            }
        } else {
            //If the file doesn't exist there is nothing to load.
        }
    }

    public void SaveData() {
        try(BufferedWriter writer = Files.newBufferedWriter(getSavedDataPath())) {
            for(PcapFilter filter : filters) {
                writer.write(Base64.getEncoder().encodeToString(filter.nameProperty().get().getBytes()));
                writer.write("//");
                writer.write(Base64.getEncoder().encodeToString(filter.filterProperty().get().getBytes()));
                writer.write("\r\n");
            }
        } catch(IOException ex) {
            Logger.log(this, Severity.Error, "There was an error saving the filters: " + ex.getMessage());
        }
    }

    // == Filter validation ===================================================

    public static boolean isValidBpfString(String filter) {
        if(filter == null) {
            return false;
        }
        if(filter.isEmpty()) {
            // The old check was to ask with a single space as the string; but we know that is valid.
            return true;
        }

        PcapBpfProgram test = null;
        try {
            test = new PcapBpfProgram();
            /* check the manual, device type of "1" should be safe to use for all expressions */
            Pcap pcap = Pcap.openDead(1, Pcap.DEFAULT_SNAPLEN);
            /* 1 = always 1 + read manual, -128 is 255.255.255.0 hashed */
            int res = pcap.compile(test, filter, 1, -128);

            /* anything but -1 means success */
            return res != -1;
        } catch (Error ex) {
            //Ignore the error and say it isn't valid.
            return false;
        } finally {
            try {
                Pcap.freecode(test);
            } catch (Exception | Error ex) {
                //Ignore the error since this is just a test
            }
        }
    }

    /**
     * Checks high level BPF code.
     *
     * @param filterString String containing a BPF expression.
     * @param errCB A callback which returns the messages of the errors
     * generated here, no exception are thrown or logged this method is meant to
     * fail.
     * @return True if the expression is valid, else false
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public static boolean testFilter(String filterString, Consumer<String> errCB) {

        if( filterString.isEmpty() ) {
            filterString = " ";
        }
        try {
            try {
                PcapBpfProgram test = new PcapBpfProgram();
                /* check the manual, device type of "1" should be safe to use for all expressions */
                Pcap pcap = Pcap.openDead(1, Pcap.DEFAULT_SNAPLEN);
                /* 1 = always 1 + read manual, -128 is 255.255.255.0 hashed */
                int res = pcap.compile(test, filterString, 1, -128);

                try {
                    Pcap.freecode(test);
                } catch (Exception | Error ex) {
                    //Ignore the error since this is just a test
                }

                if (errCB != null && !pcap.getErr().isEmpty()) {
                    errCB.accept(pcap.getErr());
                }
                /* anything but -1 means success */
                return res != -1;
            } catch (Error e) {
                if (errCB != null) {
                    errCB.accept(e.getLocalizedMessage());
                }
            }
        } catch (Exception ex) {
            if (errCB != null) {
                errCB.accept(ex.getLocalizedMessage());
            }
        }
        return false;
    }
}
