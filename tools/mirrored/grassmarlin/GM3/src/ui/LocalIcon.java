package ui;

import core.Configuration;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.util.HashMap;

/**
 * A LocalIcon is an image file located on disk in a user-modifiable directory.  These are the icons that
 * can be mapped through fingerprints.  An instance of a LocalIcon contains the image data and creates
 * ImageView instances.
 *
 * LocalIcons are created on-demand for a given path and cached for the lifetime of the application.
 *
 * Generally the files referenced can be replaced by the user, however code will reference specific paths for
 * certain defaults.
 */
public class LocalIcon {
    private static HashMap<String, LocalIcon> cache = new HashMap<>();
    protected static final String PATH_BASE = Configuration.getPreferenceString(Configuration.Fields.DIR_IMAGES_ICON) + File.separator;

    /**
     * Create a LocalIcon for the given path, or return an existing LocalIcon.
     * @param path The relative path within the images directory where the file can be found.  Pipe characters in the
     *             path will be replaced with the system's File.separator character.
     * @return A LocalIcon object containing the requested resource.
     */
    public static LocalIcon forPath(String path) {
        LocalIcon result = cache.get(path);
        if(result == null) {
            File src = new File(path.replace("|", File.separator));
            if(!src.exists()) {
                return null;
            }
            result = new LocalIcon(src);
            cache.put(path, result);
        }
        return result;
    }

    private Image image;

    protected LocalIcon(File src) {
        image = new Image("file:" + src.getAbsolutePath());
    }

    public ImageView getView(double size) {
        ImageView result = getView();
        result.setPreserveRatio(true);
        result.setFitHeight(size);
        return result;
    }

    public ImageView getView() {
        return new ImageView(image);
    }

    public Image getRawImage() {
        return image;
    }
}
