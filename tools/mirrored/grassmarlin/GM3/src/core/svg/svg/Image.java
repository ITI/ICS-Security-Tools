package core.svg.svg;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import sun.awt.image.IntegerComponentRaster;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Base64;

public class Image extends Entity {
    private final javafx.scene.image.ImageView image;

    public Image(final javafx.scene.image.ImageView image) {
        this.image = image;
    }

    @Override
    public String toSvg(final TransformStack transforms) {
        StringBuilder result = new StringBuilder();

        result.append("<image x='").append(transforms.get().getX() + image.getX());
        result.append("' y='").append(transforms.get().getY() + image.getY());
        result.append("' width='").append(image.prefWidth(0.0));
        result.append("' height='").append(image.prefHeight(0.0));
        result.append("' xlink:href='data:image/png;base64,");

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            PixelReader pr = image.getImage().getPixelReader();

            Rectangle2D bounds = image.getViewport();
            if(bounds == null) {
                bounds = new Rectangle2D(0, 0, image.getImage().getWidth(), image.getImage().getHeight());
            }
            int iw = (int) bounds.getWidth();
            int ih = (int) bounds.getHeight();

            BufferedImage img = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
            IntegerComponentRaster icr = (IntegerComponentRaster) img.getRaster();
            int offset = icr.getDataOffset(0);
            int scan = icr.getScanlineStride();
            int data[] = icr.getDataStorage();
            WritablePixelFormat<IntBuffer> pf = PixelFormat.getIntArgbInstance();
            pr.getPixels((int)bounds.getMinX(), (int)bounds.getMinY(), iw, ih, pf, data, offset, scan);

            ImageIO.write(img, "png", stream);
        } catch(IOException ex) {
            //Ignore
        }
        result.append(Base64.getEncoder().encodeToString(stream.toByteArray()));

        result.append("' />");

        return result.toString();
    }
}
