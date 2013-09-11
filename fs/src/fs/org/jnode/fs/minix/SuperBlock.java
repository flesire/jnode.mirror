package org.jnode.fs.minix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import org.jnode.fs.ext2.Ext2Utils;

public class SuperBlock {

    public static final int INODES_ALLOCATION_LIMIT = 65535;
    public static final int MINIX2_MAX_SIZE = 0x7fffffff;
    public static final int MINIX_MAX_SIZE = (7 + 512 + 512 * 512) * 1024;

    enum Version {
        V1, V2
    }

    private static final int SUPERBLOCK_LENGTH = 1024;
    private static final int BLOCK_SIZE_BITS = 10;
    private static final int BLOCK_SIZE = 1024;
    private static final int BITS_PER_BLOCK = (BLOCK_SIZE << 3);
    /** Inode number for the root block */
    private static final int MINIX_ROOT_INO = 1;
    /* original minix fs */
    private static final int MINIX_SUPER_MAGIC = 0x137F;
    /* minix fs, 30 char names */
    private static final int MINIX_SUPER_MAGIC2 = 0x138F;
    /* minix V2 fs */
    private static final int MINIX2_SUPER_MAGIC = 0x2468;
    /* minix V2 fs, 30 char names */
    private static final int MINIX2_SUPER_MAGIC2 = 0x2478;
    public static final int VALID_FS = 0x0001;

    private Version version;

    private byte[] datas;

    public SuperBlock() {
        datas = new byte[SUPERBLOCK_LENGTH];

    }

    public SuperBlock(byte[] datas) {
        this.datas = datas;
    }

    //

    public void setInodesCount(int size) {
        Ext2Utils.set16(datas, 0, size);
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

    public int getInodeBlocks() {
        int inodes = Ext2Utils.get16(datas, 0);
        int inodesPerBlock = getInodePerBlock();
        return ((inodes + ((inodesPerBlock) - 1)) / (inodesPerBlock));
    }

    public int getInodePerBlock() {
        int inodeSize = (version.equals(Version.V2)) ? 64 : 32;
        return BLOCK_SIZE / inodeSize;
    }

    public void setMagic(int magic) {
        Ext2Utils.set16(datas, 36, magic);
    }

    public void setIMapBlocks(int blocks) {
        Ext2Utils.set16(datas, 8, blocks);
    }

    public void setZMapBlocks(int blocks) {
        Ext2Utils.set16(datas, 12, blocks);
    }

    public void setFirstDataZone(int zone) {
        Ext2Utils.set16(datas, 16, zone);
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

    //

    public int getDirSize() {
        return Ext2Utils.get16(datas, 56);
    }

    public int getSize() {
        return SUPERBLOCK_LENGTH;
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
        if (version.equals(Version.V2)) {
            this.setSZones(filesystemSize);
        } else {
            this.setZonesCount((int) filesystemSize);
        }
        this.setState(VALID_FS);
        long maxSize = (version.equals(Version.V2)) ? MINIX2_MAX_SIZE : MINIX_MAX_SIZE;
        this.setMaxSize(maxSize);
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
        this.setInodesCount(requested);

        // Initialize bitmaps
        initBitmaps(filesystemSize, iNodes, requested);

        // Initialize inode table;

    }

    public void createRootBlock(MinixFileSystem fs, int magic) throws IOException {
        // Create root block
        CharBuffer rootBlock = CharBuffer.allocate(BLOCK_SIZE);
        rootBlock.append((char) MINIX_ROOT_INO);
        rootBlock.append('.');
        rootBlock.rewind();
        int dirSize = (magic == MINIX_SUPER_MAGIC2 || magic == MINIX2_SUPER_MAGIC2) ? 32 : 16;
        rootBlock.position(dirSize);
        rootBlock.append((char) MINIX_ROOT_INO);
        rootBlock.append("..");
        Charset latin1Charset = Charset.forName("ISO-8859-1");
        ByteBuffer buffer = latin1Charset.encode(rootBlock);
        fs.getApi().write(MINIX_ROOT_INO * BLOCK_SIZE, buffer);
    }

    private void initBitmaps(long filesystemSize, long iNodes, int requested) {
        int iMaps = (int) upper(iNodes + 1, BITS_PER_BLOCK);
        long size = filesystemSize - (1 + iMaps + getInodeBlocks());
        int zMaps = (int) upper(size, BITS_PER_BLOCK + 1);
        this.setIMapBlocks(iMaps);
        this.setZMapBlocks(zMaps);
        int firstDataZone = 2 + iMaps + zMaps + getInodeBlocks();
        this.setFirstDataZone(firstDataZone);

        byte[] inodeBitmap = new byte[iMaps * BLOCK_SIZE];
        byte[] zoneBitmap = new byte[zMaps * BLOCK_SIZE];

        Arrays.fill(inodeBitmap, (byte) 0xff);
        Arrays.fill(zoneBitmap, (byte) 0xff);

        for (int i = firstDataZone; i < filesystemSize; i++) {
            int n = i - firstDataZone + 1;
            zoneBitmap[n >> 3] &= ~(1 << (n & 7));
        }
        for (int i = MINIX_ROOT_INO; i <= requested; i++) {
            inodeBitmap[i >> 3] &= ~(1 << (i & 7));
        }
    }

    private long upper(long size, int n) {
        return (size + n - 1) & ~(n - 1);
    }

}
