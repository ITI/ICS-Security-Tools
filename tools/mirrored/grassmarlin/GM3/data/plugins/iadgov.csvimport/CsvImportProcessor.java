package iadgov.csvimport;

import core.fingerprint3.Fingerprint;
import core.importmodule.ImportItem;
import util.FileUnits;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

/**
 * An ImportItem is constructed for each file being imported.
 * The ImportItem will be created via reflection using the (Path, List) constructor.
 * At this point the reporting of size is slightly wrong for Host-based iterators; the workflow around that is being reworked.
 * getLogicalIterator and getPhysicalIterator return iterators that will provide the elements from the file.  A null iterator indicates that that type of data is not provided.
 */
public class CsvImportProcessor extends ImportItem {
    private final long size;

    public CsvImportProcessor(Path path, List<Fingerprint> fingerprints) {
        super(path, fingerprints);

        long _size;
        try {
            _size = Files.size(path);
        } catch(IOException ex) {
            _size = -1;
        }
        size = _size;

    }

    @Override
    protected long getTotalUnits() {
        return size;
    }

    @Override
    protected Iterator<?> getLogicalIterator() {
        CsvFileIterator iterator = new CsvFileIterator(path);
        // Calling parseFile spawns a thread that handles the actual parsing.
        // for simple files, it is possible to perform the parsing here and return an iterator to a list of the resulting data objects.
        // It is, however, generally regarded as a bad idea to do that.
        iterator.parseFile();

        return iterator;
    }

    @Override
    protected Iterator<?> getPhysicalIterator() {
        return null;
    }

    @Override
    public String getDisplaySize() {
        if(size == -1) {
            try {
                return FileUnits.formatSize(Files.size(path));
            } catch(IOException ex) {
                return FileUnits.formatSize(0);
            }
        } else {
            return FileUnits.formatSize(this.size);
        }
    }
}
