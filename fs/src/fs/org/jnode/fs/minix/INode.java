package org.jnode.fs.minix;

import java.nio.ByteBuffer;
import java.util.Date;
import org.jnode.fs.ext2.Ext2Utils;

public class INode {

    public static final int S_IFMT = 0xF000; // format mask
    public static final int S_IFSOCK = 0xC000; // socket
    public static final int S_IFLNK = 0xA000; // symbolic link
    public static final int S_IFREG = 0x8000; // regular file
    public static final int S_IFBLK = 0x6000; // block device
    public static final int S_IFDIR = 0x4000; // directory
    public static final int S_IFCHR = 0x2000; // character device
    public static final int S_IFIFO = 0x1000; // fifo
    public static final int INODE_V1_SIZE = 32;
    public static final int INODE_V2_SIZE = 64;

    private int number;
    private int mode;
    private int links;
    private int uid;
    private int gid;
    /** Current size in bytes. */
    private long size;

    private long atime;
    private long ctime;
    private long mtime;

    private long[] zones;
    private long indirectionZone;
    private long doubleIndirectionZone;

    public INode(int number, int mode, int links, long size) {
        this.number = number;
        this.mode = mode;
        this.links = links;
        this.size = size;
        // Set dates
        long now = new Date().getTime();
        atime = now;
        ctime = now;
        mtime = now;
        zones = new long[7];
    }

    public INode(byte[] data) {
        mode = Ext2Utils.get16(data, 0);
        links = Ext2Utils.get16(data, 2);
        uid = Ext2Utils.get16(data, 4);
        gid = Ext2Utils.get16(data, 6);
        size = Ext2Utils.get32(data, 8);
        // Set dates
        atime = Ext2Utils.get32(data, 12);
        ctime = Ext2Utils.get32(data, 16);
        mtime = Ext2Utils.get32(data, 20);
        // Set zones
        zones = new long[7];
        for (int i = 0; i < 7; i++) {
            zones[i] = Ext2Utils.get32(data, 24 + (i * 4));
        }
        indirectionZone = Ext2Utils.get32(data, 52);
        doubleIndirectionZone = Ext2Utils.get32(data, 56);
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getLinks() {
        return links;
    }

    public void setLinks(int links) {
        this.links = links;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getGid() {
        return gid;
    }

    public void setGid(int gid) {
        this.gid = gid;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getAtime() {
        return atime;
    }

    public void setAtime(long atime) {
        this.atime = atime;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public long getMtime() {
        return mtime;
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
    }

    public void setZone(int index, long value) {
        zones[index] = value;
    }

    public long[] getZones() {
        return zones;
    }

    public int getMaskedMode() {
        return mode & S_IFMT;
    }

    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(toByteArray());
    }

    private byte[] toByteArray() {
        byte[] data = new byte[INODE_V2_SIZE];
        Ext2Utils.set16(data, 0, mode);
        Ext2Utils.set16(data, 2, links);
        Ext2Utils.set16(data, 4, uid);
        Ext2Utils.set16(data, 6, gid);
        Ext2Utils.set32(data, 8, size);
        Ext2Utils.set32(data, 12, atime);
        Ext2Utils.set32(data, 16, ctime);
        Ext2Utils.set32(data, 20, mtime);
        for (int i = 0; i < 7; i++) {
            Ext2Utils.set32(data, 24 + (i * 4), zones[i]);
        }
        return data;
    }
}
