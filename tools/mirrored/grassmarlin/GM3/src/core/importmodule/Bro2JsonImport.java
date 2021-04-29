package core.importmodule;

import core.fingerprint.PacketData;
import core.fingerprint3.Fingerprint;
import core.importmodule.inputIterators.Bro2.Bro2JsonIterator;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

/**
 * Class representing an import of a Bro2 Json file
 */
public class Bro2JsonImport extends ImportItem{

    long lines;

    public Bro2JsonImport(Path inPath, List<Fingerprint> fingerprints) {
        super(inPath, fingerprints);

        Iterator<PacketData> iterator = Bro2JsonIterator.getBro2JsonIterator(this, inPath);

        this.lines = ((Bro2JsonIterator)iterator).getSize();

        this.iteratorMap.put(Pipeline.LOGICAL, iterator);
    }

    @Override
    protected long getTotalUnits() {
        return this.lines;
    }

    @Override
    protected Iterator<?> getLogicalIterator() {
        Iterator<?> iterator = Bro2JsonIterator.getBro2JsonIterator(this, this.path);

        this.lines = ((Bro2JsonIterator) iterator).getSize();

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
