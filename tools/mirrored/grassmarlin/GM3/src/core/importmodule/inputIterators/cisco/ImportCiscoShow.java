package core.importmodule.inputIterators.cisco;

import core.importmodule.ImportItem;
import core.logging.Logger;
import core.logging.Severity;
import util.FileUnits;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

public class ImportCiscoShow extends ImportItem {
    private long sizeFile;

    public ImportCiscoShow(Path inPath) {
        super(inPath, null);

        try {
            this.sizeFile = Files.size(inPath);
        } catch (IOException e) {
            Logger.log(this, Severity.Warning, "Unable to find the size of file " + inPath.getFileName());
            this.sizeFile = 0;
        }
    }

    @Override
    public long getTotalUnits() {
        return sizeFile;
    }

    @Override
    protected Iterator<?> getLogicalIterator() {
        return null;
    }

    @Override
    protected Iterator<?> getPhysicalIterator() {
        return CiscoFileIterator.getCiscoFileIterator(this, this.path);
    }

    @Override
    public String getDisplaySize() {
        return FileUnits.formatSize(this.sizeFile);
    }
}
