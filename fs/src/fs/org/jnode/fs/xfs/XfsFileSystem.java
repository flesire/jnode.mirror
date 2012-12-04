package org.jnode.fs.xfs;

import java.io.IOException;

import org.jnode.driver.Device;
import org.jnode.fs.FSDirectory;
import org.jnode.fs.FSEntry;
import org.jnode.fs.FSFile;
import org.jnode.fs.FileSystem;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.FileSystemType;
import org.jnode.fs.ext2.BlockSize;
import org.jnode.fs.spi.AbstractFileSystem;

public class XfsFileSystem extends AbstractFileSystem<FSEntry> {

    public XfsFileSystem(Device device, boolean readOnly,
            FileSystemType<? extends FileSystem<FSEntry>> type) throws FileSystemException {
        super(device, readOnly, type);
    }

    public void read() throws FileSystemException {

    }

    public void write(BlockSize blockSize) throws FileSystemException {

    }

    @Override
    public long getTotalSpace() throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getFreeSpace() throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getUsableSpace() throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getVolumeName() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected FSFile createFile(FSEntry entry) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected FSDirectory createDirectory(FSEntry entry) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected FSEntry createRootEntry() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

}
