package ui.fingerprint.tree;


import core.fingerprint3.ObjectFactory;
import javafx.scene.control.TreeItem;
import javafx.scene.text.Text;
import ui.fingerprint.FingerPrintGui;
import ui.fingerprint.editorPanes.FilterRow;
import ui.fingerprint.filters.Filter;

import javax.xml.bind.JAXBElement;

public class FilterItem extends TreeItem<String>{

    private FingerPrintGui gui;
    private Filter.FilterType type;
    private FilterRow row;
    private int index;

    private ObjectFactory factory;

    public FilterItem(Filter.FilterType type, int index, FingerPrintGui gui, JAXBElement value) {
        super(type.getName());

        //This is for purely cosmetic reasons because of the dirty markings on FPItems
        this.setGraphic(new Text(" "));

        this.gui = gui;
        this.type = type;
        this.index = index;
        this.factory = new ObjectFactory();

        this.row = new FilterRow(this, value);
    }

    public FilterItem (Filter.FilterType type, int index, FingerPrintGui gui) {
        this(type, index, gui, null);
    }

    public Filter.FilterType getType() {
        return this.type;
    }

    public void setType(Filter.FilterType type) {
        this.type = type;
        this.setValue(type.getName());

        if (this.type.getPacketType() == Filter.PacketType.TCP) {
            // requires that we filter for TCP Packets
            boolean hasTcp = this.getParent().getChildren().stream()
                    // check for a Transport Protocol Filter with a value of 6(TCP)
                    .anyMatch(child -> child instanceof FilterItem
                            && ((FilterItem) child).getType() == Filter.FilterType.TRANSPORTPROTOCOL
                            && ((FilterItem) child).getElement().getValue().equals((short)6));

            if (!hasTcp) {
                //create TCP Filter
                FPItem fp = this.gui.getFPItem(this);
                JAXBElement<Short> element = factory.createFingerprintFilterTransportProtocol((short) 6);
                int index = this.gui.getDocument().addFilter(fp.getName(), fp.pathProperty().get(), this.gui.getPayloadItem(this).getName(),
                        this.gui.getGroupItem(this).getName(), element);
                FilterItem newItem = new FilterItem(Filter.FilterType.TRANSPORTPROTOCOL, index, this.gui, element);
                this.getParent().getChildren().addAll(newItem);
            }
        }
    }

    public FilterRow getRow() {
        return this.row;
    }

    public int getIndex() {
        return this.index;
    }
    public void setIndex(int index) {
        this.index = index;
    }

    public void updateValue(JAXBElement newValue) {
        FPItem fp = this.gui.getFPItem(this);
        this.gui.getDocument().updateFilter(fp.getName(), fp.pathProperty().get(),
                this.gui.getPayloadItem(this).getName(), this.gui.getGroupItem(this).getName(), newValue, index);
    }

    public JAXBElement getElement() {
        return this.row.getElement();
    }

    public void setRowIndex(int index) {
        this.row.setRow(index);
    }

    public void setFocus() {
        this.row.setFocus();
    }
}
