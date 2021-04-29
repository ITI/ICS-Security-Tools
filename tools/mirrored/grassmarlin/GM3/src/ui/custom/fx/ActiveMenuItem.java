package ui.custom.fx;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import ui.EmbeddedIcons;

/**
 * Utility class to allow access to setOnAction method in the constructor.
 * Used when building menus to bind actions for menu items to lambdas.
 */
public class ActiveMenuItem extends MenuItem {
    private final static double ICON_SIZE = 16.0;

    public ActiveMenuItem(String title, EventHandler<ActionEvent> handler) {
        this(title, (Node) null, handler);
    }
    public ActiveMenuItem(String title, EmbeddedIcons graphic, EventHandler<ActionEvent> handler) {
        this(title, graphic == null ? null : graphic.getImage(ICON_SIZE), handler);
    }
    public ActiveMenuItem(String title, Node graphic, EventHandler<ActionEvent> handler) {
        super(title, graphic);
        super.setOnAction(handler);
    }

    public ActiveMenuItem(ObservableValue<String> title, EventHandler<ActionEvent> handler) {
        this(title, (Node)null, handler);
    }
    public ActiveMenuItem(ObservableValue<String> title, EmbeddedIcons graphic, EventHandler<ActionEvent> handler) {
        this(title, graphic == null ? null : graphic.getImage(ICON_SIZE), handler);
    }

    public ActiveMenuItem(ObservableValue<String> title, Node graphic, EventHandler<ActionEvent> handler) {
        this(title.getValue(), graphic, handler);
        super.textProperty().bind(title);
    }

    /**
     * Utility function to bind the disabled state to a boolean value.
     * @param controller A boolean property that is true when the menu item should be enabled.
     * @return self, to facilitate chaining to constructor when building menu.
     */
    public ActiveMenuItem bindEnabled(BooleanExpression controller) {
        this.disableProperty().bind(controller.not());
        return this;
    }

    public ActiveMenuItem setAccelerator(KeyCodeCombination.Modifier modifier, KeyCode key) {
        if(modifier == null) {
            this.setAccelerator(new KeyCodeCombination(key));
        } else {
            this.setAccelerator(new KeyCodeCombination(key, modifier));
        }
        return this;
    }
}
