package ui.fingerprint.tree;

import core.fingerprint3.Fingerprint;
import javafx.scene.control.TreeItem;
import javafx.scene.text.Text;


public class PayloadItem extends TreeItem<String>{

    private Fingerprint.Payload payload;

    public enum OpType {
        ALWAYS,
        RETURN,
        MATCH,
        BYTE_TEST,
        IS_DATA_AT,
        BYTE_JUMP,
        ANCHOR
    }

    public PayloadItem(Fingerprint.Payload payload) {
        super(payload.getFor());
        this.payload = payload;
        //This is for purely cosmetic reasons because of the dirty markings on FPItems
        this.setGraphic(new Text(" "));
    }

    public String getName() {
        return this.payload.getFor();
    }

    public void setName(String name) {
        this.payload.setFor(name);
    }

    public String getDescription() {
        return this.payload.getDescription();
    }

    public Fingerprint.Payload getPayload () { return this.payload; }

}