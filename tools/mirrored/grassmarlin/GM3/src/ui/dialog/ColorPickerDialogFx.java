package ui.dialog;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.Window;
import ui.EmbeddedIcons;
import ui.custom.fx.ColorPalette;

public class ColorPickerDialogFx extends Dialog<ButtonType> {
    protected final SimpleObjectProperty<Color> color;

    public ColorPickerDialogFx() {
        color = new SimpleObjectProperty<>(Color.BLACK);
        color.addListener(Handler_ColorChanged);

        sliderRed = new Slider();
        sliderGreen = new Slider();
        sliderBlue = new Slider();

        sliderHue = new Slider();
        sliderSaturation = new Slider();
        sliderBrightness = new Slider();

        rectOriginal = new Rectangle();
        rectPreview = new Rectangle();

        paletteCustom = new ColorPalette(8, 2);
        paletteDefault = new ColorPalette(8, 8);

        initComponents();

        EnableListeners();
    }

    public ColorPickerDialogFx(Color color) {
        this();
        this.color.set(color);
    }

    protected void Handle_ColorChanged(ObservableValue<? extends Color> observable, Color oldValue, Color newValue) {
        //The "Original" value is bound to the color directly, but the preview needs to change, as do the sliders.
        //SetPreviewColor is the method which handles this for sliders changing, so we can use it here.
        SetPreviewColor(newValue, false, false);
    }

    protected final Slider sliderRed;
    protected final Slider sliderGreen;
    protected final Slider sliderBlue;

    protected final Slider sliderHue;
    protected final Slider sliderSaturation;
    protected final Slider sliderBrightness;

    protected final ColorPalette paletteDefault;
    protected final ColorPalette paletteCustom;

    protected final Rectangle rectOriginal;
    protected final Rectangle rectPreview;

    private boolean AllowEvents = true;

    private final InvalidationListener Handler_RgbChanged = this::Handle_RgbChanged;
    private final InvalidationListener Handler_HsbChanged = this::Handle_HsbChanged;
    private final ChangeListener<Color> Handler_ColorChanged = this::Handle_ColorChanged;

    protected void EnableListeners() {
        sliderRed.valueProperty().addListener(Handler_RgbChanged);
        sliderGreen.valueProperty().addListener(Handler_RgbChanged);
        sliderBlue.valueProperty().addListener(Handler_RgbChanged);

        sliderHue.valueProperty().addListener(Handler_HsbChanged);
        sliderSaturation.valueProperty().addListener(Handler_HsbChanged);
        sliderBrightness.valueProperty().addListener(Handler_HsbChanged);
    }

    protected void SetPreviewColor(Color color, boolean skipRgb, boolean skipHsb) {
        if(!AllowEvents) {
            return;
        }
        try {
            AllowEvents = false;

            rectPreview.setFill(color);

            sliderRed.setValue(color.getRed());
            sliderGreen.setValue(color.getGreen());
            sliderBlue.setValue(color.getBlue());

            sliderHue.setValue(color.getHue());
            sliderSaturation.setValue(color.getSaturation());
            sliderBrightness.setValue(color.getBrightness());
        } catch(Exception ex) {
            ex.printStackTrace();
        } finally {
            AllowEvents = true;
        }
    }
    //private void Handle_RgbChanged(Observable source) {
    private void Handle_RgbChanged(Observable source) {
        SetPreviewColor(Color.rgb(
                (int)(255.0 * sliderRed.getValue()),
                (int)(255.0 * sliderGreen.getValue()),
                (int)(255.0 * sliderBlue.getValue())
        ), true, false);
    }
    //private void Handle_HsbChanged(Observable source) {
    private void Handle_HsbChanged(Observable source) {
        SetPreviewColor(Color.hsb(
                (sliderHue.getValue()),
                (sliderSaturation.getValue()),
                (sliderBrightness.getValue())
        ), false, true);
    }

    private void initComponents() {
        setTitle("Select New Color...");
        Window stage = super.getDialogPane().getScene().getWindow();
        if(stage instanceof Stage) {
            ((Stage)stage).getIcons().add(EmbeddedIcons.Vista_Palette.getRawImage());
        }

        // RGB Sliders
        sliderRed.setMin(0.0);
        sliderRed.setMax(1.0);
        sliderGreen.setMin(0.0);
        sliderGreen.setMax(1.0);
        sliderBlue.setMin(0.0);
        sliderBlue.setMax(1.0);

        // HSV Sliders
        sliderHue.setMin(0.0);
        sliderHue.setMax(360.0);
        sliderSaturation.setMin(0.0);
        sliderSaturation.setMax(1.0);
        sliderBrightness.setMin(0.0);
        sliderBrightness.setMax(1.0);

        //Preview Rectangles
        rectOriginal.fillProperty().bind(color);
        rectOriginal.setWidth(40.0);
        rectOriginal.setHeight(60.0);
        rectPreview.widthProperty().bind(rectOriginal.widthProperty());
        rectPreview.heightProperty().bind(rectOriginal.heightProperty());

        //Layout:
        HBox groupsHoriz = new HBox();
        //TODO: Add graphical HSV control
        VBox groupPalettes = new VBox();
        groupPalettes.getChildren().addAll(
                paletteDefault,
                paletteCustom
        );
        paletteDefault.selectedColorProperty().addListener(Handler_ColorChanged);
        paletteCustom.selectedColorProperty().addListener(Handler_ColorChanged);
        paletteDefault.getColors().addAll(
                Color.rgb(255, 128, 128),
                Color.rgb(255, 255, 128),
                Color.rgb(128, 255, 128),
                Color.rgb(0, 255, 128),
                Color.rgb(128, 255, 255),
                Color.rgb(0, 128, 255),
                Color.rgb(255, 128, 192),
                Color.rgb(255, 128, 255),

                Color.rgb(255, 0, 0),
                Color.rgb(255, 255, 0),
                Color.rgb(128, 255, 0),
                Color.rgb(0, 255, 64),
                Color.rgb(0, 255, 255),
                Color.rgb(0, 128, 192),
                Color.rgb(128, 128, 192),
                Color.rgb(255, 0, 255),

                Color.rgb(128, 64, 64),
                Color.rgb(255, 128, 64),
                Color.rgb(0, 255, 0),
                Color.rgb(0, 128, 128),
                Color.rgb(0, 64, 128),
                Color.rgb(128, 128, 255),
                Color.rgb(128, 0, 64),
                Color.rgb(255, 0, 128),

                Color.rgb(128, 0, 0),
                Color.rgb(255, 128, 0),
                Color.rgb(0, 128, 0),
                Color.rgb(0, 128, 64),
                Color.rgb(0, 0, 255),
                Color.rgb(0, 0, 160),
                Color.rgb(128, 0, 128),
                Color.rgb(128, 0, 255),

                Color.rgb(64, 0, 0),
                Color.rgb(128, 64, 0),
                Color.rgb(0, 64, 0),
                Color.rgb(0, 64, 64),
                Color.rgb(0, 0, 128),
                Color.rgb(0, 0, 64),
                Color.rgb(64, 0, 64),
                Color.rgb(64, 0, 128),

                Color.rgb(0, 0, 0),
                Color.rgb(128, 128, 0),
                Color.rgb(128, 128, 64),
                Color.rgb(128, 128, 128),
                Color.rgb(64, 128, 128),
                Color.rgb(192, 192, 192),
                Color.rgb(64, 0, 32),
                Color.rgb(255, 255, 255)
        );
        //TODO: Define custom colors

        VBox groupsVert = new VBox();
        GridPane paneRgb = new GridPane();
        paneRgb.add(sliderRed, 1, 0);
        paneRgb.add(sliderGreen, 1, 1);
        paneRgb.add(sliderBlue, 1, 2);
        paneRgb.add(new Label("R"), 0, 0);
        paneRgb.add(new Label("G"), 0, 1);
        paneRgb.add(new Label("B"), 0, 2);
        GridPane paneHsb = new GridPane();
        paneHsb.add(sliderHue, 1, 0);
        paneHsb.add(sliderSaturation, 1, 1);
        paneHsb.add(sliderBrightness, 1, 2);
        paneHsb.add(new Label("H"), 0, 0);
        paneHsb.add(new Label("S"), 0, 1);
        paneHsb.add(new Label("V"), 0, 2);
        HBox boxPreview = new HBox();
        boxPreview.getChildren().addAll(
                rectOriginal,
                rectPreview
        );

        groupsVert.getChildren().addAll(
                paneRgb,
                paneHsb,
                boxPreview
        );

        groupsHoriz.getChildren().addAll(
                //TODO: Graphical HSV control
                groupPalettes,
                groupsVert
        );

        getDialogPane().setContent(groupsHoriz);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    }

    public ObjectProperty<Color> colorProperty() {
        return color;
    }
    public Color getColor() {
        return color.get();
    }
    public void setColor(Color color) {
        //If the new color is the same as the current color, the change event will not fire.
        if(color.equals(this.color.get())) {
            SetPreviewColor(color, false, false);
        } else {
            this.color.set(color);
        }
    }

    public Color getSelectedColor() {
        return (Color)rectPreview.getFill();
    }
}
