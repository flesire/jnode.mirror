package org.jnode.fs.minix;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.jnode.driver.Device;
import org.jnode.fs.FSDirectory;
import org.jnode.fs.FSEntry;
import org.jnode.fs.FSFile;
import org.jnode.fs.FileSystem;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.FileSystemType;
import org.jnode.fs.spi.AbstractFileSystem;

public class MinixFileSystem extends AbstractFileSystem<MinixEntry> {

    private SuperBlock superBlock;
    //TODO INode bitmap
    //TODO Zones bitmap

    /**
     * Construct an AbstractFileSystem in specified readOnly mode
     *
     * @param device   device contains file system. This paramter is mandatory.
     * @param readOnly file system should be read-only.
     * @throws org.jnode.fs.FileSystemException
     *          device is null or device has no {@link org.jnode.driver.block.BlockDeviceAPI} defined.
     */
    public MinixFileSystem(Device device, boolean readOnly,
                           FileSystemType<? extends FileSystem<MinixEntry>> type)
        throws FileSystemException {
        super(device, readOnly, type);

    }

    @Override
    protected FSFile createFile(FSEntry entry) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected FSDirectory createDirectory(FSEntry entry) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected MinixEntry createRootEntry() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getTotalSpace() throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getFreeSpace() throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getUsableSpace() throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getVolumeName() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void read() throws FileSystemException {
        readSuperBlock();
    }

    //

    private void readSuperBlock() {
        ByteBuffer buffer = ByteBuffer.allocate(superBlock.getSize());
        superBlock = new SuperBlock(buffer.array());
    }
}
