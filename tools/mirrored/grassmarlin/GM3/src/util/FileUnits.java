package util;

import org.apache.commons.io.FileUtils;

/**
 * Class to support formatting of file sizes.
 * Chances are nothing over GB will be needed, but the @link#org.apache.commons.io.FileUtils class includes constants to exabytes, so we will match that.
 */
public class FileUnits {
    protected FileUnits(long sizeUnit, String units) {
        //Threshold is set to 1.2 times the size of the unit to keep a higher precision unit when close to the threshold.
        this.threshold = (long)(sizeUnit * 1.2);
        this.sizeUnit = sizeUnit;
        this.units = " " + units;
    }

    protected static final FileUnits[] arrUnits = new FileUnits[] {
            new FileUnits(FileUtils.ONE_EB, "EB"),
            new FileUnits(FileUtils.ONE_PB, "PB"),
            new FileUnits(FileUtils.ONE_TB, "TB"),
            new FileUnits(FileUtils.ONE_GB, "GB"),
            new FileUnits(FileUtils.ONE_MB, "MB"),
            new FileUnits(FileUtils.ONE_KB, "KB"),
    };

    protected long threshold;
    protected long sizeUnit;
    protected String units;

    public static String formatSize(long size) {
        for(FileUnits unit : arrUnits) {
            if(size > unit.threshold) {
                return unit.formatSizeInternal(size);
            }
        }
        return size + " B";
    }

    protected String formatSizeInternal(long size) {
        long tenTimesPrintableSize = 10 * size / sizeUnit;
        return (tenTimesPrintableSize / 10) + "." + (tenTimesPrintableSize % 10) + units;
    }
}