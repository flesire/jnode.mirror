package org.jnode.fs.minix.inode;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.log4j.Logger;
import org.jnode.fs.minix.MinixBitmap;
import org.jnode.fs.minix.MinixFileSystem;
import org.jnode.fs.minix.SuperBlock;

import static org.jnode.fs.minix.SuperBlock.BLOCK_SIZE;

public class INodeFactory {

    private static final Logger log = Logger.getLogger(INodeFactory.class);

    public static INode getINode(MinixFileSystem fs, int iNodeNumber) throws IOException {
        SuperBlock superBlock = fs.getSuperBlock();
        int block =
                2 + superBlock.getImapBlocks() + superBlock.getZMapBlocks() + iNodeNumber /
                        superBlock.getInodePerBlock();
        log.debug("Read block " + block + " corresponding to inode number " + iNodeNumber);
        ByteBuffer blockData = ByteBuffer.allocate(BLOCK_SIZE);
        fs.getApi().read(block * BLOCK_SIZE, blockData);
        blockData.flip();
        byte[] iNodeData = new byte[64];
        blockData.get(iNodeData, 0, 64);
        return new INode(iNodeData);
    }

    public static INode allocateInode(byte[] inodeBitmap, int mode) {
        int inodeNumber = MinixBitmap.findFreeINode(inodeBitmap);
        INode inode = new INode(inodeNumber, mode, 0, 0);
        return inode;
    }

}
