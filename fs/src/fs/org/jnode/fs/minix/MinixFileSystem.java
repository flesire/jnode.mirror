package org.jnode.fs.minix;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.log4j.Logger;
import org.jnode.driver.Device;
import org.jnode.fs.FSDirectory;
import org.jnode.fs.FSEntry;
import org.jnode.fs.FSFile;
import org.jnode.fs.FileSystem;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.FileSystemType;
import org.jnode.fs.spi.AbstractFileSystem;

public class MinixFileSystem extends AbstractFileSystem<MinixEntry> {

    private final Logger log = Logger.getLogger(getClass());

    private SuperBlock superBlock;

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
        INode rootINode = getINode(1);
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
            long filesystemSize = this.getApi().getLength() / SuperBlock.BLOCK_SIZE;
            superBlock.create(version, magic, filesystemSize, 0);
            log.debug("SuperBlock :" + superBlock.toString());
            this.getApi().write(1024, superBlock.toByteBuffer());
            ByteBuffer rootBlock = superBlock.createRootBlock(magic);
            rootBlock.flip();
            // TODO write rootBlock
            this.getApi().write(1024 + SuperBlock.BLOCK_SIZE, rootBlock);
        } catch (IOException e) {
            throw new FileSystemException(e);
        }
    }

    //

    private void readSuperBlock() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(superBlock.getSize());
        this.getApi().read(1024, buffer);
        superBlock = new SuperBlock(buffer.array());
        log.debug("SuperBlock :" + superBlock.toString());
    }

    private INode getINode(int iNodeNumber) throws IOException {
        int block =
                2 + superBlock.getImapBlocks() + superBlock.getZMapBlocks() + iNodeNumber /
                        superBlock.getInodePerBlock();
        ByteBuffer blockData = ByteBuffer.allocate(SuperBlock.BLOCK_SIZE);
        this.getApi().read(block, blockData);
        blockData.flip();
        byte[] iNodeData = new byte[64];
        blockData.get(iNodeData, 0, 64);
        return new INode(iNodeData);
    }
}
