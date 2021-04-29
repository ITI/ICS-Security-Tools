package ui.dialog;

import com.sun.javafx.collections.ObservableListWrapper;
import core.document.graph.INode;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.stage.Window;
import ui.EmbeddedIcons;

import java.util.Map;
import java.util.stream.Collectors;

public class NodeDetailsDialogFx<TNode extends INode<TNode>> extends Dialog {
    protected static class Field {
        private final SimpleStringProperty name;
        private final SimpleStringProperty value;

        public Field(Map.Entry<String, String> entry) {
            name = new SimpleStringProperty(entry.getKey());
            value = new SimpleStringProperty(entry.getValue());
        }

        public StringProperty nameProperty() {
            return name;
        }
        public StringProperty valueProperty() {
            return value;
        }
    }

    private final TableView<Field> table;
    private final SimpleStringProperty titleBase;

    public NodeDetailsDialogFx() {
        table = new TableView<>();
        titleBase = new SimpleStringProperty("Node Details: ");

        initComponents();
    }

    private void initComponents() {
        titleProperty().bind(titleBase);
        Window stage = super.getDialogPane().getScene().getWindow();
        if(stage instanceof Stage) {
            ((Stage)stage).getIcons().add(EmbeddedIcons.Vista_Report.getRawImage());
        }
        this.setResizable(true);

        TableColumn<Field, String> colField = new TableColumn<>("Field");
        colField.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Field, String> colValue = new TableColumn<>("Value");
        colValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        table.getColumns().addAll(
                colField,
                colValue
        );

        getDialogPane().setContent(table);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }

    public void setNode(TNode nodeNew) {
        titleProperty().bind(titleBase.concat(nodeNew.titleProperty()));
        table.setItems(new ObservableListWrapper<>(nodeNew.getGroups().entrySet().stream()
                .map(entry -> new Field(entry))
                .collect(Collectors.toList())));
    }
}
