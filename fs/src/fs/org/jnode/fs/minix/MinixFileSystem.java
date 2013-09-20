package org.jnode.fs.minix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.apache.log4j.Logger;
import org.jnode.driver.Device;
import org.jnode.fs.FSDirectory;
import org.jnode.fs.FSEntry;
import org.jnode.fs.FSFile;
import org.jnode.fs.FileSystem;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.FileSystemType;
import org.jnode.fs.minix.inode.INode;
import org.jnode.fs.minix.inode.INodeFactory;
import org.jnode.fs.spi.AbstractFileSystem;

import static org.jnode.fs.minix.MinixVersion.V2;
import static org.jnode.fs.minix.SuperBlock.BLOCK_SIZE;
import static org.jnode.fs.minix.inode.INode.S_IFDIR;

public class MinixFileSystem extends AbstractFileSystem<MinixEntry> {

    /** Inode number for the root block */
    public static final int MINIX_ROOT_INODE_NUMBER = 1;

    private final Logger log = Logger.getLogger(getClass());

    private SuperBlock superBlock;

    private byte[] inodeBitmap;

    private byte[] zoneBitmap;

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
        INode rootINode = INodeFactory.getINode(this, MINIX_ROOT_INODE_NUMBER);
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

    public void read() throws FileSystemException {
        try {
            readSuperBlock();
            readBitmaps();
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
            populateBitmaps(filesystemSize);
            this.getApi().write(1024, superBlock.toByteBuffer());
            createRootBlock();
        } catch (IOException e) {
            throw new FileSystemException(e);
        }
    }

    public void createRootBlock() throws IOException, FileSystemException {
        MinixBitmap.mark(inodeBitmap, MINIX_ROOT_INODE_NUMBER);
        long rootBlock =
                MinixBitmap.findFreeBlock(zoneBitmap, superBlock.getZMapBlocks()) +
                        superBlock.getFirstDataZone() - 1;
        INode inode = new INode(MINIX_ROOT_INODE_NUMBER, S_IFDIR, 64, 2);
        inode.setZone(0, rootBlock);
        // FIXME no correct (WIP)
        int rootInodeOffset = 2 + superBlock.getImapBlocks() + superBlock.getZMapBlocks();
        this.getApi().write(rootInodeOffset * BLOCK_SIZE, inode.toByteBuffer());
        MinixBitmap.mark(zoneBitmap, (int) rootBlock);
        // Create root block
        ByteBuffer buffer = ByteBuffer.allocate(64);
        MinixDirectoryEntry dir = new MinixDirectoryEntry();
        dir.setInodeNumber(MINIX_ROOT_INODE_NUMBER);
        dir.setName(".");
        buffer.put(dir.getData());
        dir = new MinixDirectoryEntry();
        dir.setInodeNumber(MINIX_ROOT_INODE_NUMBER);
        dir.setName("..");
        buffer.put(dir.getData());
        buffer.flip();
        this.getApi().write(rootBlock, buffer);
    }

    //

    private void readSuperBlock() throws IOException {
        superBlock = new SuperBlock();
        superBlock.read(this);
        log.info("Read SuperBlock content.");
        if (log.isDebugEnabled()) {
            log.debug(superBlock);
        }
    }

    private void populateBitmaps(long filesystemSize) throws FileSystemException {
        inodeBitmap = new byte[superBlock.getImapBlocks() * BLOCK_SIZE];
        zoneBitmap = new byte[superBlock.getZMapBlocks() * BLOCK_SIZE];
        //
        Arrays.fill(inodeBitmap, (byte) 0xff);
        Arrays.fill(zoneBitmap, (byte) 0xff);
        //
        for (int i = MINIX_ROOT_INODE_NUMBER; i <= superBlock.getInodesCount(); i++) {
            MinixBitmap.unmark(inodeBitmap, i);
        }
        int firstDataZone = superBlock.getFirstDataZone();
        for (int i = firstDataZone; i < filesystemSize; i++) {
            int index = i - firstDataZone + 1;
            MinixBitmap.unmark(zoneBitmap, index);
        }
        //

        ByteBuffer buffer = ByteBuffer.wrap(inodeBitmap);
        //
        try {
            int offset = 2 * BLOCK_SIZE;
            this.getApi().write(offset, buffer);
            offset += superBlock.getImapBlocks() * BLOCK_SIZE;
            buffer = ByteBuffer.wrap(zoneBitmap);
            this.getApi().write(offset, buffer);
        } catch (IOException e) {
            throw new FileSystemException(e);
        }

    }

    private void readBitmaps() {
        inodeBitmap = new byte[superBlock.getImapBlocks() * BLOCK_SIZE];
        zoneBitmap = new byte[superBlock.getZMapBlocks() * BLOCK_SIZE];
        int offset = 2 * BLOCK_SIZE;
        int size = superBlock.getImapBlocks() * BLOCK_SIZE;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        try {
            this.getApi().read(offset, buffer);
            inodeBitmap = buffer.array();
            offset += superBlock.getImapBlocks() * BLOCK_SIZE;
            size = superBlock.getZMapBlocks() * BLOCK_SIZE;
            buffer = ByteBuffer.allocate(size);
            this.getApi().read(offset, buffer);
            zoneBitmap = buffer.array();
        } catch (IOException e) {
            e.printStackTrace(); // To change body of catch statement use File |
                                 // Settings | File Templates.
        }
    }

}
