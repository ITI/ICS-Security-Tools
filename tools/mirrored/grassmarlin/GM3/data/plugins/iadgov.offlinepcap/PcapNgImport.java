package iadgov.offlinepcap;

import core.fingerprint3.Fingerprint;
import core.logging.Logger;
import core.logging.Severity;
import util.FileUnits;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

public class PcapNgImport extends core.importmodule.ImportItem {
    private long size;

    public PcapNgImport(final Path inPath, final List<Fingerprint> fingerprints) throws IllegalStateException, IOException {
        super(inPath, fingerprints);

        if(!validateFileformat()) {
            throw new IOException("The file does not conform to the PcapNg specification.");
        }
        try {
            this.size = Files.size(inPath);
        } catch (IOException e) {
            this.size = -1;
        }
    }

    private boolean validateFileformat() {
        try(SeekableByteChannel reader = Files.newByteChannel(path)) {
            ByteBuffer buffer = ByteBuffer.allocate(12);
            reader.read(buffer);
            int bom = buffer.getInt(8);
            if(bom == 0x1A2B3C4D || bom == 0x4D3C2B1A) {
                return true;
            }

            //Check for Pcap
            bom = buffer.getInt(0);
            if(bom == 0xA1B2C3D4 || bom == 0xD4C3B2A1) {
                Logger.log(PCAPImport.class, Severity.Warning, "The file appears to be a Pcap file, not a PcapNg file (" + path.getFileName() + ")");
            }
            return false;
        } catch(IOException ex) {
            return false;
        }
    }

    @Override
    protected long getTotalUnits() {
        return this.size;
    }

    @Override
    protected Iterator<?> getLogicalIterator() {
        return PcapNgFileParser.getPcapFileIterator(this, this.path);
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
