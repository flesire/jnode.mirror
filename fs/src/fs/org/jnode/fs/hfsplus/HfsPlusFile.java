package org.jnode.fs.hfsplus;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.jnode.fs.FSFileSlackSpace;
import org.jnode.fs.hfsplus.catalog.CatalogFile;
import org.jnode.fs.spi.AbstractFSFile;
import org.jnode.fs.spi.AbstractFileSystem;

public class HfsPlusFile extends AbstractFSFile implements FSFileSlackSpace {

    private CatalogFile file;

    /**
     * Constructor for a new AbstractFSFile
     * 
     * @param fs
     */
    public HfsPlusFile(AbstractFileSystem<?> fs, CatalogFile file) {
        super(fs);
        this.file = file;
    }

    @Override
    public long getLength() {
        return file.getDatas().getTotalSize();
    }

    @Override
    public void setLength(long length) throws IOException {
        // TODO
    }

    @Override
    public void read(long fileOffset, ByteBuffer dest) throws IOException {
        HfsPlusFileSystem fs = (HfsPlusFileSystem) getFileSystem();
        file.getDatas().read(fs, fileOffset, dest);
    }

    @Override
    public void write(long fileOffset, ByteBuffer src) throws IOException {
        HfsPlusFileSystem fs = (HfsPlusFileSystem) getFileSystem();
        // TODO
    }

    @Override
    public void flush() throws IOException {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    @Override
    public byte[] getSlackSpace() throws IOException {
        int blockSize = ((HfsPlusFileSystem) getFileSystem()).getVolumeHeader().getBlockSize();

        int slackSpaceSize = blockSize - (int) (getLength() % blockSize);

        if (slackSpaceSize == blockSize) {
            slackSpaceSize = 0;
        }

        byte[] slackSpace = new byte[slackSpaceSize];
        read(getLength(), ByteBuffer.wrap(slackSpace));

        return slackSpace;
    }
}
