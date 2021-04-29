package ui.custom.fx;

import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import ui.EmbeddedIcons;

import java.util.Collection;

public class DynamicSubMenu extends Menu {
    private final MenuItem disabled = new MenuItem("No Options Available");

    @FunctionalInterface
    public interface IGetMenuItems {
        Collection<MenuItem> getItems();
    }

    private final IGetMenuItems fnGetItems;


    public DynamicSubMenu(String title, EmbeddedIcons graphic, IGetMenuItems fnGetMenuItems) {
        this(title, graphic.getImage(16.0), fnGetMenuItems);
    }
    public DynamicSubMenu(String title, Node graphic, IGetMenuItems fnGetMenuItems) {
        super(title, graphic);

        fnGetItems = fnGetMenuItems;
        // We need a single item to ensure the submenu can be rendered.
        getItems().add(disabled);

        setOnShowing(this::Handle_Showing);

        disabled.setDisable(true);
    }

    private void Handle_Showing(Event evt) {
        getItems().clear();
        Collection<MenuItem> items = fnGetItems.getItems();
        if(items == null || items.isEmpty()) {
            getItems().clear();
            getItems().add(disabled);
            //TODO: This is not the right place to hook setDisable; when the parent is shown, we need to evaluate the empty condition.
            //this.setDisable(true);
        } else {
            getItems().addAll(items);
        }
    }
}
