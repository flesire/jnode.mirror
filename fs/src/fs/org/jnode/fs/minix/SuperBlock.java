package org.jnode.fs.minix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import org.jnode.fs.ext2.Ext2Utils;

public class SuperBlock {

    enum Version {
        V1,V2
    }

    private static final int SUPERBLOCK_LENGTH = 1024;
    private static final int BLOCK_SIZE_BITS = 10;
    private static final int BLOCK_SIZE = (1<<BLOCK_SIZE_BITS);
    /** Inode number for the root block */
    private static final int  MINIX_ROOT_INO = 1;
    /* original minix fs */
    private static final int MINIX_SUPER_MAGIC  =  0x137F;
    /* minix fs, 30 char names */
    private static final int MINIX_SUPER_MAGIC2 =  0x138F;
    /* minix V2 fs */
    private static final int MINIX2_SUPER_MAGIC  = 0x2468;
    /* minix V2 fs, 30 char names */
    private static final int MINIX2_SUPER_MAGIC2 = 0x2478;
    public static final int VALID_FS = 0x0001;

    private byte[] datas;

    public SuperBlock() {
        datas = new byte[SUPERBLOCK_LENGTH];

    }

    public SuperBlock(byte[] datas) {
        this.datas = datas;
    }

    /**
     *
     * @param version
     * @param blockSize
     * @param inodes
     */
    public void create(MinixFileSystem fs, Version version, int magic, long blockSize, long inodes) throws IOException {
        this.setMagic(magic);
        this.setSZones(blockSize);
        this.setState(VALID_FS);
        long maxSize = (version.equals(Version.V2)) ? 0x7fffffff : (7+512+512*512) * 1024 ;
        this.setMaxSize(maxSize);
        if(inodes != 0){
            inodes = blockSize / 3;
        }
        //TODO inodes number computation
        if (inodes > 65535) inodes = 65535;
        //
        CharBuffer rootBlock = CharBuffer.allocate(BLOCK_SIZE);
        rootBlock.append((char)MINIX_ROOT_INO);
        rootBlock.append('.');
        rootBlock.rewind();
        int dirSize = (magic == MINIX_SUPER_MAGIC2 || magic == MINIX2_SUPER_MAGIC2) ? 32 : 16;
        rootBlock.position(dirSize);
        rootBlock.append((char)MINIX_ROOT_INO);
        rootBlock.append("..");
        Charset latin1Charset = Charset.forName("ISO-8859-1");
        ByteBuffer buffer = latin1Charset.encode(rootBlock);
        fs.getApi().write(MINIX_ROOT_INO*BLOCK_SIZE,buffer);
    }

    //

    public void setMagic(int magic) {
        Ext2Utils.set16(datas,36,magic);
    }

    public void setSZones(long size) {
       Ext2Utils.set32(datas,44, size);
    }

    public void setState(int state) {
        Ext2Utils.set16(datas,40,state);
    }

    public void setMaxSize(long size) {
        Ext2Utils.set32(datas,28,size);
    }

    //

    public int getDirSize() {
        return Ext2Utils.get16(datas,56);
    }

    public int getSize(){
        return SUPERBLOCK_LENGTH;
    }

}
