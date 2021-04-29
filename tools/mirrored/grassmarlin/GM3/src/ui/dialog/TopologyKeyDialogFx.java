package ui.dialog;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import ui.EmbeddedIcons;

import java.util.ArrayList;

public class TopologyKeyDialogFx extends Dialog{
    protected static class TopologyKeyEntry {
        private final SimpleObjectProperty<EmbeddedIcons> icon;
        private final SimpleStringProperty field;
        private final SimpleStringProperty value;

        public TopologyKeyEntry() {
            icon = new SimpleObjectProperty<>(null);
            field = new SimpleStringProperty("");
            value = new SimpleStringProperty("");
        }
        public TopologyKeyEntry(EmbeddedIcons icon, String field, String value) {
            this.icon = new SimpleObjectProperty<>(icon);
            this.field = new SimpleStringProperty(field);
            this.value = new SimpleStringProperty(value);
        }

        public ObjectProperty<EmbeddedIcons> iconProperty() {
            return icon;
        }
        public StringProperty fieldProperty() {
            return field;
        }
        public StringProperty valueProperty() {
            return value;
        }
    }

    protected ObservableListWrapper<TopologyKeyEntry> legend;

    public TopologyKeyDialogFx() {
        legend = new ObservableListWrapper<>(new ArrayList<>());
        legend.addAll(
                new TopologyKeyEntry(EmbeddedIcons.Ics_Host, "Category", "PLC\nRTU\nMTU\nIED\nHMI\nICS_HOST"),
                new TopologyKeyEntry(EmbeddedIcons.Vista_Firewall, "Category", "FIREWALL"),
                new TopologyKeyEntry(EmbeddedIcons.Vista_Computer, "Category", "WORKSTATION"),
                new TopologyKeyEntry(EmbeddedIcons.Vista_QuestionMark, "Category", "OTHER\nUNKNOWN"),
                new TopologyKeyEntry(EmbeddedIcons.Port_Connected, "Category", "NETWORK_DEVICE\nPROTOCOL_CONVERTER"),

                new TopologyKeyEntry(EmbeddedIcons.Vista_QuestionMark, "Role", "UNKNOWN\nOTHER"),

                new TopologyKeyEntry(EmbeddedIcons.Vista_NetworkCenter, "IP, Network", "IP is the greatest value within the Network (broadcast)")
        );

        initComponents();
    }

    private void initComponents() {
        setTitle("Topology Key");
        Window stage = super.getDialogPane().getScene().getWindow();
        if(stage instanceof Stage) {
            ((Stage)stage).getIcons().add(EmbeddedIcons.Logo.getRawImage());
        }
        super.setResizable(true);

        BorderPane layout = new BorderPane();

        //Title
        Text textTitle = new Text("Logical Graph Icons");
        //TODO: Standardize on fonts and reference from Configuration, or some other centralized point.
        textTitle.setFont(Font.font(textTitle.getFont().getFamily(), FontWeight.NORMAL, 18.0));
        layout.setTop(textTitle);

        TableView<TopologyKeyEntry> tableContent = new TableView<>();
        TableColumn<TopologyKeyEntry, EmbeddedIcons> colIcon = new TableColumn<>("Icon");
        colIcon.setCellValueFactory(new PropertyValueFactory<>("icon"));
        colIcon.setCellFactory((column) -> new TableCell<TopologyKeyEntry, EmbeddedIcons>() {
            @Override
            public void updateItem(EmbeddedIcons icon, boolean empty) {
                if (icon == null || empty) {
                    setText("");
                    setStyle("");
                    setGraphic(null);
                } else {
                    setText("");
                    setStyle("");
                    setGraphic(icon.getImage(24.0));
                }
            }
        });

        TableColumn<TopologyKeyEntry, String> colField = new TableColumn<>("Field");
        colField.setCellValueFactory(new PropertyValueFactory<>("field"));

        TableColumn<TopologyKeyEntry, String> colValue = new TableColumn<>("Value(s)");
        colValue.setCellValueFactory(new PropertyValueFactory<>("value"));

        tableContent.getColumns().addAll(colIcon, colField, colValue);
        tableContent.setItems(legend);
        layout.setCenter(tableContent);

        getDialogPane().setContent(layout);
        getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
    }
}
