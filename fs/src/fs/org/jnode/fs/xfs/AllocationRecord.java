package org.jnode.fs.xfs;

public class AllocationRecord {

    private long startBlock;
    private long blockCount;

    public AllocationRecord(long startBlock, long blockCount) {
        this.startBlock = startBlock;
        this.blockCount = blockCount;
    }

    public long getStartBlock() {
        return startBlock;
    }

    public long getBlockCount() {
        return blockCount;
    }

}
