package ui.graphing.logical;

import core.document.graph.LogicalNode;
import core.knowledgebase.GeoIp;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.ImageView;
import ui.EmbeddedIcons;
import ui.custom.fx.ActiveMenuItem;
import ui.dialog.ConnectionDetailsDialogFx;
import ui.graphing.Cell;
import util.Cidr;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CellLogical extends Cell<LogicalNode> {
    protected final ImageView iconFlag;

    public CellLogical(LogicalNode node) {
        super(node);

        if(node.getCountry() != null) {
            iconFlag = GeoIp.getFlagIcon(node.getIp()).getView(16);
        } else {
            iconFlag = null;
        }

        node.networkProperty().addListener(this::Handle_DisplayInvalidation);
        node.countryProperty().addListener(this::Handle_DisplayInvalidation);

        Handle_DisplayInvalidation(null, null, null);
    }

    private <T> void Handle_DisplayInvalidation(ObservableValue<? extends T> node, T oldValue, T newValue) {
        if(!Platform.isFxApplicationThread()) {
            //A method reference here will prevent an overridden rebuildAnnotations from being used.
            Platform.runLater(() -> this.rebuildAnnotations());
        } else {
            rebuildAnnotations();
        }
    }


    protected final Set<EmbeddedIcons> iconsInternal = new LinkedHashSet<>();
    protected final Set<EmbeddedIcons> iconsExternal = new LinkedHashSet<>();

    protected void restoreDefaults() {
        boxInternalImages.getChildren().clear();
        boxExternalImages.getChildren().clear();
    }

    protected void rebuildAnnotations() {
        restoreDefaults();

        iconsInternal.clear();
        iconsExternal.clear();

        final Map<String, String> groups = node.getGroups();
        final String roles = groups.get(LogicalNode.GROUP_ROLE);
        if(roles != null) {
            for (String role : roles.split("\\n")) {
                switch (role) {
                    case "CLIENT":
                    case "SERVER":
                    case "MASTER":
                    case "SLAVE":
                    case "OPERATOR":
                    case "ENGINEER":
                        //No special handling for these.
                        break;
                    case "UNKNOWN":
                    case "OTHER":
                        iconsInternal.add(EmbeddedIcons.Vista_QuestionMark);
                    default:
                }
            }
        }
        //Check for broadcast address for this network.
        final Cidr ip = node.getIp();
        final Cidr net = node.networkProperty().get();
        if(net != null) {
            if (ip.getLastIp() == net.getLastIp()) {
                iconsInternal.add(EmbeddedIcons.Vista_NetworkCenter);
            }
        }

        final String categories = groups.get(LogicalNode.GROUP_CATEGORY);
        if(categories != null) {
            for (String category : categories.split("\\n")) {
                switch (category) {
                    case "PLC":
                    case "RTU":
                    case "MTU":
                    case "IED":
                    case "HMI":
                    case "ICS_HOST":
                        iconsExternal.add(EmbeddedIcons.Ics_Host);
                        break;
                    case "FIREWALL":
                        iconsExternal.add(EmbeddedIcons.Vista_Firewall);
                        break;
                    case "NETWORK_DEVICE":
                    case "PROTOCOL_CONVERTER":
                        iconsExternal.add(EmbeddedIcons.Port_Connected);
                        break;
                    case "WORKSTATION":
                        iconsExternal.add(EmbeddedIcons.Vista_Computer);
                        break;
                    case "UNKNOWN":
                    case "OTHER":
                    default:
                        iconsExternal.add(EmbeddedIcons.Vista_QuestionMark);
                        break;
                }
            }
        }

        boxExternalImages.getChildren().addAll(iconsExternal.stream().map(icon -> icon.getImage(16.0)).collect(Collectors.toList()));
        if(iconFlag != null) {
            boxExternalImages.getChildren().add(iconFlag);
        }
        boxInternalImages.getChildren().addAll(iconsInternal.stream().map(icon -> icon.getImage(16.0)).collect(Collectors.toList()));
    }
}
