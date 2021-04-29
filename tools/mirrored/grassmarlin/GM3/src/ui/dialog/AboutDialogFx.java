package ui.dialog;

import core.Version;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import ui.EmbeddedIcons;

public class AboutDialogFx extends Dialog{
    private final Font fontFieldHeader;
    private final Font fontFieldValue;

    public AboutDialogFx() {
        fontFieldHeader = Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, Font.getDefault().getSize());
        fontFieldValue = Font.getDefault();

        initComponents();
    }

    private void initComponents() {
        setTitle("About GRASSMARLIN");

        GridPane layout = new GridPane();

        // Logo:
        ImageView imgLogo = EmbeddedIcons.Logo_Large.getImage();
        layout.add(imgLogo, 0, 0, 1, 5);

        //Title
        Text textTitle = new Text();
        textTitle.setText("GRASSMARLIN");
        textTitle.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 14.0));
        layout.add(textTitle, 1, 0, 2, 1);

        //Subtitle
        Text textSubtitle = new Text();
        textSubtitle.setText("SCADA and ICS analysis tool");
        layout.add(textSubtitle, 1, 1, 2, 1);

        //TODO: Version/Vendor should have some right padding for the headers.
        //Version
        Text headerVersion = new Text("Version");
        headerVersion.setFont(fontFieldHeader);
        layout.add(headerVersion, 1, 2);
        Text valueVersion = new Text(Version.APPLICATION_VERSION);
        valueVersion.setFont(fontFieldValue);
        layout.add(valueVersion, 2, 2);

        //Vendor
        Text headerVendor = new Text("Vendor");
        headerVendor.setFont(fontFieldHeader);
        layout.add(headerVendor, 1, 3);
        Text valueVendor = new Text("Department of Defense");
        valueVendor.setFont(fontFieldValue);
        layout.add(valueVendor, 2, 3);

        this.getDialogPane().setContent(layout);

        this.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }
}
