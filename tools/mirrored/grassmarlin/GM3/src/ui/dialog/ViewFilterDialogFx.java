package ui.dialog;

import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import ui.EmbeddedIcons;

public class ViewFilterDialogFx extends Dialog {
    public static class ViewFilter {
        protected final SimpleBooleanProperty active;
        protected final SimpleStringProperty value;

        public ViewFilter(String value) {
            active = new SimpleBooleanProperty(true);
            this.value = new SimpleStringProperty(value);
        }

        public BooleanProperty activeProperty() {
            return active;
        }
        public StringProperty valueProperty() {
            return value;
        }
    }

    private final SimpleStringProperty nameFilters;
    private final SimpleObjectProperty<ObservableList<ViewFilter>> listFilters;

    private TableView<ViewFilter> table;

    public ViewFilterDialogFx() {
        nameFilters = new SimpleStringProperty("");
        listFilters = new SimpleObjectProperty<>(null);

        initComponents();
    }

    private void initComponents() {
        setTitle("View Filters");
        Window stage = super.getDialogPane().getScene().getWindow();
        if(stage instanceof Stage) {
            ((Stage)stage).getIcons().add(EmbeddedIcons.Vista_Filter.getRawImage());
        }
        VBox layout = new VBox();

        Label textTitle = new Label();
        textTitle.textProperty().bind(nameFilters.concat(" Filters"));
        layout.getChildren().add(textTitle);

        table = new TableView<>();
        TableColumn<ViewFilter, Boolean> colActive = new TableColumn<>("Active");
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        colActive.setCellFactory((column) -> new TableCell<ViewFilter, Boolean>() {
            @Override
            public void updateItem(Boolean isActive, boolean empty) {
                setText("");
                setStyle("-fx-alignment:center");

                //TODO: [508] Changing active filters should also be available through a context menu.
                this.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && getTableRow().getItem() != null) {
                        BooleanProperty active = ((ViewFilter) getTableRow().getItem()).activeProperty();
                        active.set(!active.get());
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

        TableColumn<ViewFilter, String> colValue = new TableColumn<>("Value");
        colValue.setCellValueFactory(new PropertyValueFactory<>("value"));

        table.getColumns().addAll(colActive, colValue);
        layout.getChildren().add(table);

        super.getDialogPane().setContent(layout);
        super.getDialogPane().getButtonTypes().addAll(ButtonType.OK);
    }



    // == Accessors ===========================================================

    public StringProperty nameFiltersProperty() {
        return nameFilters;
    }
    public ObjectProperty<ObservableList<ViewFilter>> listFiltersProperty() {
        return listFilters;
    }
}
