package org.jnode.fs.xfs;

import org.jnode.util.BigEndian;

public class BTreeBlock {
    public static long BTB_MAGIC_1 = 0x41425442; // ABTB
    public static long BTB_MAGIC_2 = 0x41425442; // ABTC

    private long magic;
    private int level;
    private int recordCount;
    private long leftSibling;
    private long rightSibling;

    public BTreeBlock(byte[] datas) {
        magic = BigEndian.getUInt32(datas, 4);
        level = BigEndian.getUInt16(datas, 6);
        recordCount = BigEndian.getUInt16(datas, 8);
        leftSibling = BigEndian.getUInt32(datas, 12);
        rightSibling = BigEndian.getUInt32(datas, 16);
    }

    public long getMagic() {
        return magic;
    }

    public int getLevel() {
        return level;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public long getLeftSibling() {
        return leftSibling;
    }

    public long getRightSibling() {
        return rightSibling;
    }

}
