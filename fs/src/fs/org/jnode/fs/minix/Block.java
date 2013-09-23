package org.jnode.fs.minix;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.jnode.fs.spi.AbstractFSObject;

public class Block extends AbstractFSObject {

    /** Size of a block in a minix file system is fixed to 1024 bytes. */
    public static final int BLOCK_SIZE = 1024;

    private byte[] data;

    private long number;

    public Block(long number) {
        this.number = number;
        data = new byte[BLOCK_SIZE];
    }

    /*
     * public void setInode(INode inode, int offset) { byte[] src =
     * inode.toByteArray(); System.arraycopy(src,0,data,offset,src.length); }
     */

    public void read(MinixFileSystem fs) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);
        fs.getApi().read(number * BLOCK_SIZE, buffer);
        data = buffer.array();
    }

    public void write(MinixFileSystem fs) throws IOException {
        fs.getApi().write(number * BLOCK_SIZE, ByteBuffer.wrap(data));
    }

    public byte[] getData(int offset, int size) {
        byte[] dest = new byte[size];
        System.arraycopy(data, offset, dest, 0, size);
        return dest;
    }

    public long getNumber() {
        return number;
    }

}
