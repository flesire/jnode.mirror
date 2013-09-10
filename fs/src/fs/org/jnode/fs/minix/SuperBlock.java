package org.jnode.fs.minix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import org.jnode.fs.ext2.Ext2Utils;

public class SuperBlock {

    enum Version {
        V1, V2
    }

    private static final int SUPERBLOCK_LENGTH = 1024;
    private static final int BLOCK_SIZE_BITS = 10;
    private static final int BLOCK_SIZE = (1 << BLOCK_SIZE_BITS);
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
     * @param version
     * @param blockSize
     * @param inodes
     */
    public void create(MinixFileSystem fs, Version version, int magic, long blockSize, long inodes)
        throws IOException {
        this.version = version;
        this.setMagic(magic);
        if (version.equals(Version.V2)) {
            this.setSZones(blockSize);
        } else {
            this.setZonesCount((int) blockSize);
        }
        this.setState(VALID_FS);
        long maxSize = (version.equals(Version.V2)) ? 0x7fffffff : (7 + 512 + 512 * 512) * 1024;
        this.setMaxSize(maxSize);
        // Calculate number of inodes to allocate.
        if (inodes != 0) {
            inodes = blockSize / 3;
        }
        inodes = ((inodes + getInodePerBlock() - 1) & ~(getInodePerBlock() - 1));
        if (inodes > 65535)
            inodes = 65535;
        long requested = blockSize * 9 / 10 + 5;
        if (getInodeBlocks() > requested) {
            throw new IOException("Too many inodes requested (requested : " + requested +
                    " available : " + getInodeBlocks());
        }
        // Create root block
        // TODO move this part out of the superblock creation.
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

}
