package org.jnode.fs.xfs;

import org.jnode.util.BigEndian;

public class AGFreeSpaceBlock {

    private static final long AGF_MAGIC = 0x58414746; // XAGF
    private static final long AGF_VERSION = 1;
    private static final int XFS_BTNUM_AGF = 2;

    private long magic;
    private long version;
    private long sequenceNumber;
    private long length;
    private long[] roots = new long[XFS_BTNUM_AGF];
    private long[] levels = new long[XFS_BTNUM_AGF];
    private long first;
    private long last;
    private long count;
    private long freeBlocks;
    private long longest;
    private long bTreeBlocks;

    public AGFreeSpaceBlock(byte[] datas) {
        magic = BigEndian.getUInt32(datas, 0);
        version = BigEndian.getUInt32(datas, 4);
        sequenceNumber = BigEndian.getUInt32(datas, 8);
        length = BigEndian.getUInt32(datas, 12);
    }

    public AGFreeSpaceBlock() {
        this.version = AGF_VERSION;
    }

    public long getMagic() {
        return magic;
    }

    public long getVersion() {
        return version;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public long getLength() {
        return length;
    }

    public long[] getRoots() {
        return roots;
    }

    public long[] getLevels() {
        return levels;
    }

    /**
     * @return Index of first "free list" block.
     */
    public long getFirst() {
        return first;
    }

    /**
     * @return Index of last "free list" block.
     */
    public long getLast() {
        return last;
    }

    /**
     * @return Number of "free list" blocks.
     */
    public long getCount() {
        return count;
    }

    public long getFreeBlocks() {
        return freeBlocks;
    }

    public long getLongest() {
        return longest;
    }

    public long getbTreeBlocks() {
        return bTreeBlocks;
    }
}
