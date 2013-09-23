package org.jnode.fs.minix;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.jnode.fs.spi.AbstractFSFile;

public class MinixFile extends AbstractFSFile {

    private MinixEntry entry;

    /**
     * Constructor for a new AbstractFSFile
     * 
     * @param fs
     */
    public MinixFile(MinixEntry entry) {
        super((MinixFileSystem) entry.getFileSystem());
        this.entry = entry;

    }

    @Override
    public long getLength() {
        return entry.getiNode().getSize();
    }

    @Override
    public void setLength(long length) throws IOException {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    @Override
    public void read(long fileOffset, ByteBuffer dest) throws IOException {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    @Override
    public void write(long fileOffset, ByteBuffer src) throws IOException {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    @Override
    public void flush() throws IOException {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }
}
