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
import org.jnode.fs.spi.AbstractFileSystem;

import static org.jnode.fs.minix.INode.S_IFDIR;
import static org.jnode.fs.minix.SuperBlock.BLOCK_SIZE;
import static org.jnode.fs.minix.SuperBlock.SUPERBLOCK_LENGTH;

public class MinixFileSystem extends AbstractFileSystem<MinixEntry> {

    /** Inode number for the root block */
    public static final int MINIX_ROOT_INODE_NUMBER = 1;

    private final Logger log = Logger.getLogger(getClass());

    private SuperBlock superBlock;

    private byte[] inodeBitmap;

    private byte[] zoneBitmap;

    // TODO INode bitmap
    // TODO Zones bitmap

    /**
     * Construct an AbstractFileSystem in specified readOnly mode
     * 
     * @param device device contains file system. This paramter is mandatory.
     * @param readOnly file system should be read-only.
     * @throws org.jnode.fs.FileSystemException device is null or device has no
     *             {@link org.jnode.driver.block.BlockDeviceAPI} defined.
     */
    public MinixFileSystem(Device device, boolean readOnly,
            FileSystemType<? extends FileSystem<MinixEntry>> type) throws FileSystemException {
        super(device, readOnly, type);

    }

    @Override
    protected FSFile createFile(FSEntry entry) throws IOException {
        return null;
    }

    @Override
    protected FSDirectory createDirectory(FSEntry entry) throws IOException {
        return null;
    }

    @Override
    protected MinixEntry createRootEntry() throws IOException {
        INode rootINode = getINode(MINIX_ROOT_INODE_NUMBER);
        long rootBlock = rootINode.getZones()[0];
        MinixEntry entry = new MinixEntry(rootINode, "/", this, null);
        return entry;
    }

    @Override
    public long getTotalSpace() throws IOException {
        return 0;
    }

    @Override
    public long getFreeSpace() throws IOException {
        return 0;
    }

    @Override
    public long getUsableSpace() throws IOException {
        return 0;
    }

    @Override
    public String getVolumeName() throws IOException {
        return null;
    }

    public void read() throws FileSystemException {
        try {
            readSuperBlock();
            readBitmaps();
        } catch (IOException e) {
            throw new FileSystemException(e);
        }
    }

    public void create(SuperBlock.Version version, int namelen) throws FileSystemException {

        int magic;
        if (version.equals(SuperBlock.Version.V2)) {
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

        superBlock = new SuperBlock();
        try {
            long filesystemSize = this.getApi().getLength() / BLOCK_SIZE;
            superBlock.create(version, magic, filesystemSize, 0);
            log.debug("SuperBlock :" + superBlock.toString());
            populateBitmaps(filesystemSize);
            this.getApi().write(1024, superBlock.toByteBuffer());
            ByteBuffer rootBlock = superBlock.createRootBlock(magic);
            rootBlock.flip();
            this.getApi().write(1024 + BLOCK_SIZE, rootBlock);
        } catch (IOException e) {
            throw new FileSystemException(e);
        }
    }

    //

    private void readSuperBlock() throws IOException {

        ByteBuffer buffer = ByteBuffer.allocate(SUPERBLOCK_LENGTH);
        this.getApi().read(1024, buffer);
        superBlock = new SuperBlock(buffer.array());
        log.info("Read SuperBlock content.");
        if (log.isDebugEnabled()) {
            log.debug(superBlock);
        }
    }

    private INode getINode(int iNodeNumber) throws IOException {
        int block =
                2 + superBlock.getImapBlocks() + superBlock.getZMapBlocks() + iNodeNumber /
                        superBlock.getInodePerBlock();
        log.debug("Read block " + block);
        ByteBuffer blockData = ByteBuffer.allocate(BLOCK_SIZE);
        this.getApi().read(block * BLOCK_SIZE, blockData);
        blockData.flip();
        byte[] iNodeData = new byte[64];
        blockData.get(iNodeData, 0, 64);
        return new INode(iNodeData);
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
        MinixBitmap.mark(inodeBitmap, MINIX_ROOT_INODE_NUMBER);
        long rootBlock =
                MinixBitmap.findFreeBlock(zoneBitmap, superBlock.getZMapBlocks() + firstDataZone -
                        1);
        INode inode = new INode(MINIX_ROOT_INODE_NUMBER, S_IFDIR);
        inode.setZone(0, rootBlock);
        MinixBitmap.mark(zoneBitmap, (int) rootBlock);

        ByteBuffer buffer = ByteBuffer.wrap(inodeBitmap);
        //
        try {
            int offset = 2 * BLOCK_SIZE;
            this.getApi().write(offset, buffer);
            offset += superBlock.getImapBlocks() * BLOCK_SIZE;
            buffer = ByteBuffer.wrap(zoneBitmap);
            this.getApi().write(offset, buffer);
        } catch (IOException e) {
            e.printStackTrace(); // To change body of catch statement use File |
                                 // Settings | File Templates.
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
