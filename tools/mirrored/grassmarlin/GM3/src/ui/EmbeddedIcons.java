package ui;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;

public enum EmbeddedIcons {
    // == Misc custom icons
    Port_Connected("/images/PhysicalPort.png", new Rectangle2D(0, 6, 48, 40)),
    Port_Disconnected("/images/PhysicalPort.png", new Rectangle2D(48, 6, 48, 40)),
    Port_Inverted_Connected("/images/PhysicalPort.png", new Rectangle2D(0, 50, 48, 40)),
    Port_Inverted_Disconnected("/images/PhysicalPort.png", new Rectangle2D(48, 50, 48, 40)),

    Logo_Large("/images/grassmarlin_circle_lg.png"),
    Logo("/images/grassmarlin_32.png"),
    Logo_Small("/images/grassmarlin.png"),

    Ics_Host("/images/ics_host.png"),

    // == Icons from VisualStudio 2010 Image Library
    Vista_Palette("/images/microsoft/1460_PaintPalette_48x48.png"),

    Vista_Flag_Blue("/images/microsoft/1532_Flag_system_Blue.png"),
    Vista_Flag_Green("/images/microsoft/1532_Flag_system_Green.png"),
    Vista_Flag_Purple("/images/microsoft/1532_Flag_system_Purple.png"),
    Vista_Flag_Red("/images/microsoft/1532_Flag_system_red.png"),
    Vista_Flag_Yellow("/images/microsoft/1532_Flag_system_yellow.png"),

    Vista_ZoomOut("/images/microsoft/2391_ZoomOut.png"),
    Vista_ZoomIn("/images/microsoft/2392_ZoomIn_48x48.png"),

    Vista_Enable("/images/microsoft/112_Tick_Green_64x64_72.png"),
    Vista_QuestionMark("/images/microsoft/1508_QuestionMarkRed.png"),

    Vista_Save("/images/microsoft/FloppyDisk.png"),
    Vista_Open("/images/microsoft/Folder_Open.png"),
    Vista_New("/images/microsoft/Generic_Document.png"),
    Vista_Import("/images/microsoft/077_AddFile_48x48_72.png"),
    Vista_Refresh("/images/microsoft/112_RefreshArrow_Green_48x48_72.png"),
    Vista_TextFile("/images/microsoft/Generic_Document.png"),
    Vista_Report("/images/microsoft/ActivityReports.png"),
    Vista_Filter("/images/microsoft/Filter.png"),

    Vista_Record("/images/microsoft/RecordHS.png"),
    Vista_Stop("/images/microsoft/StopHS.png"),

    Vista_Network("/images/microsoft/Network.png"),

    Vista_Lock("/images/microsoft/2608_GoldLock_48x48.png"),
    Vista_Settings("/images/microsoft/Settings.png"),
    Vista_Personalization("/images/microsoft/personalization.png"),

    Vista_CellPhone("/images/microsoft/cellphone.png"),
    Vista_Computer("/images/microsoft/Computer.png"),
    Vista_CPU("/images/microsoft/CPU.png"),
    Vista_NetworkCenter("/images/microsoft/Network_Center.png"),
    Vista_NetworkDrive("/images/microsoft/Network_Drive.png"),
    Vista_NetworkFax("/images/microsoft/Network_Fax.png"),
    Vista_NetworkFolder("/images/microsoft/Network_Folder.png"),
    Vista_NetworkInternet("/images/microsoft/Network_Internet.png"),
    Vista_NetworkMap("/images/microsoft/Network_Map.png"),
    Vista_Network_Printer("/images/microsoft/Network_Printer.png"),
    Vista_VPN("/images/microsoft/VPN.png"),
    Vista_Firewall("/images/microsoft/1496_wall_48x48.png"),

    // The annotations are composite images and the constituent components have to be parsed out.
    Vista_Blocked("/images/microsoft/Blocked.png", new Rectangle2D(11, 23, 25, 25)),
    Vista_Warning("/images/microsoft/Warning.png", new Rectangle2D(0, 13, 32, 32)),
    Vista_Information("/images/microsoft/Information.png", new Rectangle2D(1, 12, 32, 32)),
    Vista_CriticalError("/images/microsoft/CriticalError.png", new Rectangle2D(17, 26, 29, 29)),
    Vista_SeriousWarning("/images/microsoft/SeriousWarning.png", new Rectangle2D(17, 26, 16, 16)),
    Common_Cancel("/images/microsoft/Cancel.png", new Rectangle2D(38, 10, 19, 19))
    ;

    private final String path;
    private final Rectangle2D contentArea;

    //Concurrency is not needed as this should only be called from the UI thread.
    private final static HashMap<String, Image> cache = new HashMap<>();

    EmbeddedIcons(String pathRelative, Rectangle2D contentArea) {
        this.contentArea = contentArea;
        this.path = pathRelative.replace("|", File.separator);
    }

    /**
     * @param pathRelative The path to the image, relative to the icon directory.  The Pipe character should be used as a platform-agnostic separator character.
     */
    EmbeddedIcons(String pathRelative) {
        this.path = pathRelative.replace("|", File.separator);
        contentArea = null;
    }

    /**
     * Construct a new ImageView for the image, utilizing an image cache.
     * @return A unique ImageView for this image.
     */
    public ImageView getImage() {
        ImageView result = new ImageView(getRawImage());
        if(contentArea != null) {
            result.setViewport(contentArea);
        }
        return result;
    }

    /**
     * Construct a new ImageView for the image, scaled to the given height.
     * @param height The height to which the image should be scaled.  The aspect ratio will be preserved.
     * @return
     */
    public ImageView getImage(double height) {
        ImageView result = getImage();
        result.setPreserveRatio(true);
        result.setFitHeight(height);

        return result;
    }

    public Image getRawImage() {
        if(cache.containsKey(path)) {
            return cache.get(path);
        } else {
            InputStream streamImage = getClass().getResourceAsStream(path);
            Image image = new Image(streamImage);
            //Image image = new Image("file:" + Configuration.getPreferenceString(Configuration.Fields.DIR_IMAGES_ICON) + File.separator + path);
            cache.put(path, image);
            return image;
        }
    }

    public Rectangle2D getViewport() {
        return contentArea;
    }
}
