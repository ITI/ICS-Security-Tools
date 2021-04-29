package core.importmodule;

import core.fingerprint3.Fingerprint;
import core.importmodule.inputIterators.Bro2.BroFileIterator;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

/**
 * Class to import Bro2 Conn logs
 */
public class Bro2Import extends ImportItem {

    long lines;

    public Bro2Import(Path inPath, List<Fingerprint> fingerprints) {
        super(inPath, fingerprints);

    }

    @Override
    protected long getTotalUnits() {
        return this.lines;
    }

    @Override
    protected Iterator<?> getLogicalIterator() {
        Iterator<?> iterator = BroFileIterator.getBro2LogIterator(this, this.path);

        this.lines = ((BroFileIterator) iterator).getSize();

        return iterator;
    }

    @Override
    protected Iterator<?> getPhysicalIterator() {
        return null;
    }

    @Override
    public String getDisplaySize() {
        return lines + " Connections";
    }
}
