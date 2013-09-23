package org.jnode.fs.minix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Hashtable;
import org.apache.log4j.Logger;
import org.jnode.fs.FileSystemException;

import static org.jnode.fs.minix.INode.S_IFDIR;
import static org.jnode.fs.minix.MinixFileSystem.MINIX_ROOT_INODE_NUMBER;
import static org.jnode.fs.minix.SuperBlock.BLOCK_SIZE;

public class MinixFileSystemFactory {

    private final Logger log = Logger.getLogger(MinixFileSystemFactory.class);

    private MinixFileSystem fs;

    private byte[] inodeBitmap;

    private byte[] zoneBitmap;

    private Hashtable<Integer, INode> inodeCache;

    public MinixFileSystemFactory(MinixFileSystem fs) {
        this.fs = fs;
        inodeCache = new Hashtable<Integer, INode>(50, (float) 0.75);
    }

    public void readSuperBlock() throws IOException {
        fs.superBlock.read(fs);
        log.info("Read SuperBlock content.");
        if (log.isDebugEnabled()) {
            log.debug(fs.superBlock);
        }
    }

    public void readBitmaps() {
        SuperBlock superBlock = fs.superBlock;
        inodeBitmap = new byte[superBlock.getImapBlocks() * BLOCK_SIZE];
        zoneBitmap = new byte[superBlock.getZMapBlocks() * BLOCK_SIZE];
        int offset = 2 * BLOCK_SIZE;
        int size = superBlock.getImapBlocks() * BLOCK_SIZE;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        try {
            fs.getApi().read(offset, buffer);
            inodeBitmap = buffer.array();
            offset += superBlock.getImapBlocks() * BLOCK_SIZE;
            size = superBlock.getZMapBlocks() * BLOCK_SIZE;
            buffer = ByteBuffer.allocate(size);
            fs.getApi().read(offset, buffer);
            zoneBitmap = buffer.array();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void populateBitmaps(long filesystemSize) throws FileSystemException {
        SuperBlock superBlock = fs.superBlock;
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
            fs.getApi().write(offset, buffer);
            offset += superBlock.getImapBlocks() * BLOCK_SIZE;
            buffer = ByteBuffer.wrap(zoneBitmap);
            fs.getApi().write(offset, buffer);
        } catch (IOException e) {
            throw new FileSystemException(e);
        }

    }

    public void createRootBlock() throws IOException, FileSystemException {
        SuperBlock superBlock = fs.superBlock;
        MinixBitmap.mark(inodeBitmap, MINIX_ROOT_INODE_NUMBER);
        long rootBlock =
                MinixBitmap.findFreeBlock(zoneBitmap, superBlock.getZMapBlocks()) +
                        superBlock.getFirstDataZone() - 1;
        INode inode = new INode(MINIX_ROOT_INODE_NUMBER, S_IFDIR, 64, 2);
        inode.setZone(0, rootBlock);
        // FIXME no correct (WIP)
        int rootInodeOffset = 2 + superBlock.getImapBlocks() + superBlock.getZMapBlocks();
        fs.getApi().write(rootInodeOffset * BLOCK_SIZE, inode.toByteBuffer());
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
        fs.getApi().write(rootBlock, buffer);
    }

    // INodes operations

    public INode allocateInode(byte[] inodeBitmap, int mode) {
        int inodeNumber = MinixBitmap.findFreeINode(inodeBitmap);
        INode inode = new INode(inodeNumber, mode, 0, 0);
        return inode;
    }

    public INode getINode(int number) throws IOException {

        if (inodeCache.containsKey(number)) {
            log.debug("Get inode " + number + " from the cache.");
            return inodeCache.get(number);
        }

        INode iNode = readInode(number);
        iNode.setNumber(number);
        inodeCache.put(number, iNode);
        return iNode;
    }

    /*
     * public INode getNewInode() { SuperBlock superBlock = fs.superBlock; for
     * (int i = 0; i < superBlock.getImapBlocks(); i++) { bh = sbi->s_imap[i]; j
     * = minix_find_first_zero_bit(bh->b_data, BITS_PER_BLOCK); if (j <
     * BITS_PER_BLOCK) break; } }
     */

    private INode readInode(int number) throws IOException {
        SuperBlock superBlock = fs.getSuperBlock();
        int block =
                2 + superBlock.getImapBlocks() + superBlock.getZMapBlocks() + number /
                        superBlock.getInodePerBlock();
        log.debug("Read block " + block + " corresponding to inode number " + number);
        ByteBuffer blockData = ByteBuffer.allocate(BLOCK_SIZE);
        fs.getApi().read(block * BLOCK_SIZE, blockData);
        blockData.flip();
        byte[] iNodeData = new byte[64];
        blockData.get(iNodeData, 0, 64);
        return new INode(iNodeData);
    }

}
