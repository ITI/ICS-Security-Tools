package ui.custom.fx;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import ui.EmbeddedIcons;

public class ActiveButton extends Button {
    public ActiveButton(String text, EventHandler<ActionEvent> handler) {
        super(text);
        setOnAction(handler);
    }
    public ActiveButton(String text, Node graphic, EventHandler<ActionEvent> handler) {
        super(text, graphic);
        setOnAction(handler);
    }
    public ActiveButton(String text, EmbeddedIcons graphic, EventHandler<ActionEvent> handler) {
        this(text, graphic.getImage(16.0), handler);
    }
}
