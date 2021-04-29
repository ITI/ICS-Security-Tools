package ui.dialog;

import core.Preferences;
import core.logging.Logger;
import core.logging.Severity;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import ui.EmbeddedIcons;
import util.Cidr;

import java.util.ArrayList;
import java.util.List;

public class ManageLogicalNetworksDialogFx extends Dialog {
    private static ManageLogicalNetworksDialogFx instance = null;

    private final ListView<Cidr> viewCidrs;
    private final TextField txtNewCidr;
    private final Button btnAddCidr;

    private ManageLogicalNetworksDialogFx() {
        viewCidrs = new ListView<>();

        txtNewCidr = new TextField("");
        btnAddCidr = new Button("Add CIDR");

        initComponents();
    }

    public static ManageLogicalNetworksDialogFx getInstance() {
        if (instance == null) {
            instance = new ManageLogicalNetworksDialogFx();
        }

        return instance;
    }

    private void initComponents() {
        setTitle("Logical Networks");
        Window stage = super.getDialogPane().getScene().getWindow();
        if(stage instanceof Stage) {
            ((Stage)stage).getIcons().add(EmbeddedIcons.Logo.getRawImage());
        }
        this.setResizable(true);

        Label lblTitle = new Label("CIDRs");
        viewCidrs.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                event.consume();
                List<Cidr> cidrsToDelete = new ArrayList<>(viewCidrs.getSelectionModel().getSelectedItems());
                viewCidrs.getItems().removeAll(cidrsToDelete);
            }
        });

        txtNewCidr.setOnKeyPressed(event1 -> {
            if(event1.getCode() == KeyCode.ENTER) {
                btnAddCidr.fire();
                event1.consume();
            }
        });
        btnAddCidr.setOnAction(event -> {
            try {
                viewCidrs.getItems().add(new Cidr(txtNewCidr.getText()));
                txtNewCidr.setText("");
            } catch(Exception ex) {
                Logger.log(this, Severity.Error, "Unable to add CIDR: " + ex.getMessage());
            }
        });
        Label lblWarning = new Label("Note:  CIDRs are not permitted to overlap.\nAn existing CIDR must be removed before an overlapping CIDR can be added.");
        CheckBox ckCreateSubnetsOnDemand = new CheckBox();
        ckCreateSubnetsOnDemand.textProperty().bind(new ReadOnlyStringWrapper("Create /").concat(Preferences.CreatedSubnetSize).concat(" subnets on demand."));
        ckCreateSubnetsOnDemand.selectedProperty().bindBidirectional(Preferences.CreateSubnetsOnDemand);


        GridPane paneContent = new GridPane();
        paneContent.add(lblTitle, 0, 0, 2, 1);
        paneContent.add(viewCidrs, 0, 1, 2, 1);
        paneContent.add(txtNewCidr, 0, 2);
        paneContent.add(btnAddCidr, 1, 2);
        paneContent.add(lblWarning, 0, 3, 2, 1);
        paneContent.add(ckCreateSubnetsOnDemand, 0, 4, 2, 1);

        getDialogPane().getButtonTypes().add(ButtonType.FINISH);
        super.getDialogPane().setContent(paneContent);
    }

    public ObjectProperty<ObservableList<Cidr>> itemsProperty() {
        return viewCidrs.itemsProperty();
    }
}
