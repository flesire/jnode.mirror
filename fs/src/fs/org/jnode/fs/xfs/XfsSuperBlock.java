package org.jnode.fs.xfs;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jnode.driver.block.BlockDeviceAPI;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.ext2.Superblock;
import org.jnode.fs.spi.AbstractFSObject;

public class XfsSuperBlock extends AbstractFSObject {

    public final static int XFS_SB_MAGIC = 0x58465342; // "XFSB"

    public static final int SUPERBLOCK_LENGTH = 1024;

    private byte[] datas;

    public void read(BlockDeviceAPI api) throws FileSystemException {
        ByteBuffer data;
        data = ByteBuffer.allocate(Superblock.SUPERBLOCK_LENGTH);
        try {
            api.read(0, data);
            System.arraycopy(data.array(), 0, datas, 0, SUPERBLOCK_LENGTH);
        } catch (IOException e) {
            throw new FileSystemException(e);
        }
    }
}
