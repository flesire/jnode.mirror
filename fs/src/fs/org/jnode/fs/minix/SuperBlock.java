package org.jnode.fs.minix;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.log4j.Logger;
import org.jnode.fs.ext2.Ext2Utils;

public class SuperBlock {

    private final Logger log = Logger.getLogger(getClass());

    public static final int INODES_ALLOCATION_LIMIT = 65535;
    public static final int MINIX2_MAX_SIZE = 0x7fffffff;
    public static final int MINIX_MAX_SIZE = (7 + 512 + 512 * 512) * 1024;

    public enum Version {
        V1, V2
    }

    private static final int SUPERBLOCK_LENGTH = 1024;
    private static final int BLOCK_SIZE_BITS = 10;
    public static final int BLOCK_SIZE = 1024;
    private static final int BITS_PER_BLOCK = (BLOCK_SIZE << 3);
    /** Inode number for the root block */
    public static final int MINIX_ROOT_INODE_NUMBER = 1;
    /* original minix fs */
    public static final int MINIX_SUPER_MAGIC = 0x137F;
    /* minix fs, 30 char names */
    public static final int MINIX_SUPER_MAGIC2 = 0x138F;
    /* minix V2 fs */
    public static final int MINIX2_SUPER_MAGIC = 0x2468;
    /* minix V2 fs, 30 char names */
    public static final int MINIX2_SUPER_MAGIC2 = 0x2478;
    public static final int VALID_FS = 0x0001;

    private Version version = Version.V1;

    private byte[] datas;

    public SuperBlock() {
        datas = new byte[SUPERBLOCK_LENGTH];

    }

    public SuperBlock(byte[] datas) {
        this.datas = datas;
        if (getMagic() == MINIX2_SUPER_MAGIC || getMagic() == MINIX2_SUPER_MAGIC2) {
            version = Version.V2;
        }
    }

    //

    public void setInodesCount(int size) {
        Ext2Utils.set16(datas, 0, size);
    }

    public int getInodesCount() {
        return Ext2Utils.get16(datas, 0);
    }

    /**
     * Set device size in blocks. This field is set only in version 1 of minix
     * file system.
     * 
     * @param size size in blocks.
     */
    public void setZonesCount(int size) {
        Ext2Utils.set16(datas, 4, size);
    }

    public int getInodePerBlock() {
        int inodeSize = (version.equals(Version.V2)) ? 64 : 32;
        return BLOCK_SIZE / inodeSize;
    }

    public void setMagic(int magic) {
        Ext2Utils.set16(datas, 36, magic);
    }

    private int getMagic() {
        return Ext2Utils.get16(datas, 36);
    }

    public void setIMapBlocks(int blocks) {
        Ext2Utils.set16(datas, 8, blocks);
    }

    public int getImapBlocks() {
        return Ext2Utils.get16(datas, 8);
    }

    public void setZMapBlocks(int blocks) {
        Ext2Utils.set16(datas, 12, blocks);
    }

    public int getZMapBlocks() {
        return Ext2Utils.get16(datas, 12);
    }

    public void setFirstDataZone(int zone) {
        Ext2Utils.set16(datas, 16, zone);
    }

    public int getFirstDataZone() {
        return Ext2Utils.get16(datas, 16);
    }

    /**
     * Set device size in blocks. This field is set only in version 2 of minix
     * file system.
     * 
     * @param size size in blocks.
     */
    public void setSZones(long size) {
        Ext2Utils.set32(datas, 44, size);
    }

    public void setState(int state) {
        Ext2Utils.set16(datas, 40, state);
    }

    public void setMaxSize(long size) {
        Ext2Utils.set32(datas, 28, size);
    }

    public String toString() {
        return "[Version : " + version + ", Magic : 0x" + Integer.toHexString(getMagic()) +
                ", inodes/block : " + getInodePerBlock() + "]";
    }

    public ByteBuffer toByteBuffer() {
        ByteBuffer buffer = ByteBuffer.wrap(datas);
        return buffer;
    }

    //

    public int getDirSize() {
        return Ext2Utils.get16(datas, 56);
    }

    public int getSize() {
        return SUPERBLOCK_LENGTH;
    }

    public int getInodeBlocks() {
        int inodes = Ext2Utils.get16(datas, 0);
        int inodesPerBlock = getInodePerBlock();
        return ((inodes + ((inodesPerBlock) - 1)) / (inodesPerBlock));
    }

    /**
     * 
     * @param version Version of the file system.
     * @param magic Value to check filesystem is a valid minix file system.
     * @param filesystemSize size of the filesystem in blocks.
     * @param iNodes number of iNodes to allocate.
     */
    public void create(Version version, int magic, long filesystemSize, long iNodes)
        throws IOException {
        this.version = version;
        this.setMagic(magic);
        this.setState(VALID_FS);
        long maxSize = (version.equals(Version.V2)) ? MINIX2_MAX_SIZE : MINIX_MAX_SIZE;
        this.setMaxSize(maxSize);

        // Set device size in blocks depending on the file system version;
        if (version.equals(Version.V2)) {
            this.setSZones(filesystemSize);
        } else {
            this.setZonesCount((int) filesystemSize);
        }

        // Calculate number of iNodes to allocate.
        if (iNodes != 0) {
            iNodes = filesystemSize / 3;
        }
        iNodes = upper(iNodes, getInodePerBlock());
        if (iNodes > INODES_ALLOCATION_LIMIT)
            iNodes = INODES_ALLOCATION_LIMIT;
        int requested = (int) (filesystemSize * 9 / 10 + 5);
        if (getInodeBlocks() > requested) {
            throw new IOException("Too many iNodes requested (requested : " + requested +
                    " available : " + getInodeBlocks());
        }
        log.debug("Allocated inodes : " + requested);
        this.setInodesCount(requested);

        // Initialize bitmaps
        int iMaps = (int) upper(iNodes + 1, BITS_PER_BLOCK);
        long size = filesystemSize - (1 + iMaps + getInodeBlocks());
        int zMaps = (int) upper(size, BITS_PER_BLOCK + 1);
        this.setIMapBlocks(iMaps);
        this.setZMapBlocks(zMaps);
        int firstDataZone = 2 + iMaps + zMaps + getInodeBlocks();
        this.setFirstDataZone(firstDataZone);

        // Initialize inode table;

    }

    public ByteBuffer createRootBlock(int magic) throws IOException {
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
        return buffer;
    }

    private long upper(long size, int n) {
        return (size + n - 1) & ~(n - 1);
    }

}
