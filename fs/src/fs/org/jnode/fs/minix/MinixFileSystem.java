package org.jnode.fs.minix;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.jnode.driver.Device;
import org.jnode.fs.FSDirectory;
import org.jnode.fs.FSEntry;
import org.jnode.fs.FSFile;
import org.jnode.fs.FileSystem;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.FileSystemType;
import org.jnode.fs.spi.AbstractFileSystem;

import static org.jnode.fs.minix.MinixVersion.V2;
import static org.jnode.fs.minix.SuperBlock.BLOCK_SIZE;

public class MinixFileSystem extends AbstractFileSystem<MinixEntry> {

    /** Inode number for the root block */
    public static final int MINIX_ROOT_INODE_NUMBER = 1;

    private final Logger log = Logger.getLogger(getClass());

    protected SuperBlock superBlock;

    private MinixFileSystemFactory factory;

    /**
     * Construct an AbstractFileSystem in specified readOnly mode
     * 
     * @param device device contains file system. This parameter is mandatory.
     * @param readOnly file system should be read-only.
     * @throws org.jnode.fs.FileSystemException device is null or device has no
     *             {@link org.jnode.driver.block.BlockDeviceAPI} defined.
     */
    public MinixFileSystem(Device device, boolean readOnly,
            FileSystemType<? extends FileSystem<MinixEntry>> type) throws FileSystemException {
        super(device, readOnly, type);
        superBlock = new SuperBlock();
        factory = new MinixFileSystemFactory(this);
    }

    @Override
    protected FSFile createFile(FSEntry entry) throws IOException {
        return null;
    }

    @Override
    protected FSDirectory createDirectory(FSEntry entry) throws IOException {
        return new MinixDirectory((MinixEntry) entry);
    }

    @Override
    protected MinixEntry createRootEntry() throws IOException {
        INode rootINode = factory.getINode(MINIX_ROOT_INODE_NUMBER);
        MinixEntry entry = new MinixEntry(rootINode, "/", this, null);
        return entry;
    }

    @Override
    public long getTotalSpace() throws IOException {
        return superBlock.getDeviceSizeInBlocks() * BLOCK_SIZE;
    }

    @Override
    public long getFreeSpace() throws IOException {
        return -1;
    }

    @Override
    public long getUsableSpace() throws IOException {
        return superBlock.getDeviceSizeInBlocks() * BLOCK_SIZE - superBlock.getFirstDataZone() *
                BLOCK_SIZE;
    }

    @Override
    public String getVolumeName() throws IOException {
        return "";
    }

    public SuperBlock getSuperBlock() {
        return superBlock;
    }

    public MinixFileSystemFactory getFactory() {
        return factory;
    }

    public void read() throws FileSystemException {
        try {
            factory.readSuperBlock();
            factory.readBitmaps();
        } catch (IOException e) {
            throw new FileSystemException(e);
        }
    }

    public void create(MinixVersion version, int namelen) throws FileSystemException {

        int magic;
        if (version.equals(V2)) {
            if (namelen == 30) {
                magic = SuperBlock.MINIX2_SUPER_MAGIC2;
            } else {
                magic = SuperBlock.MINIX2_SUPER_MAGIC;
            }
        } else {
            if (namelen == 30) {
                magic = SuperBlock.MINIX_SUPER_MAGIC2;
            } else {
                magic = SuperBlock.MINIX_SUPER_MAGIC;
            }

        }

        log.info("Create a new minix file system " + version + " with name length set to " +
                namelen);

        try {
            long filesystemSize = this.getApi().getLength() / BLOCK_SIZE;
            superBlock.create(version, magic, filesystemSize, 0);
            log.info("New SuperBlock created.");
            if (log.isDebugEnabled()) {
                log.debug(superBlock);
            }
            factory.populateBitmaps(filesystemSize);
            this.getApi().write(1024, superBlock.toByteBuffer());
            factory.createRootBlock();
        } catch (IOException e) {
            throw new FileSystemException(e);
        }
    }

    //

}
