package ui.fingerprint.tree;

import javafx.scene.control.TreeItem;
import javafx.scene.text.Text;

public class FilterGroupItem extends TreeItem<String> {

    String name;

    public FilterGroupItem(String groupName) {
        super(groupName);
        this.name = groupName;

        //This is for purely cosmetic reasons because of the dirty markings on FPItems
        this.setGraphic(new Text(" "));
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
