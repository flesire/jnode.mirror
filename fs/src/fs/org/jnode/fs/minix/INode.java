package org.jnode.fs.minix;

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
    public static final int INODE_V2_SIZE = 64;

    private int number;

    private byte[] datas;

    public int getNumber() {
        return number;
    }

    public INode(int number, int mode) {
        this.datas = new byte[INODE_V2_SIZE];
        this.number = number;
        Ext2Utils.set16(datas, 0, mode);
    }

    public INode(byte[] data) {
        this.datas = data;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getMode() {
        return Ext2Utils.get16(datas, 0);
    }

    public int getNumberLinks() {
        return Ext2Utils.get16(datas, 2);
    }

    public int getUid() {
        return Ext2Utils.get16(datas, 4);
    }

    public int getGid() {
        return Ext2Utils.get16(datas, 6);
    }

    public long getFileSize() {
        return Ext2Utils.get32(datas, 8);
    }

    public long getATime() {
        return Ext2Utils.get32(datas, 12);
    }

    public long getMTime() {
        return Ext2Utils.get32(datas, 16);
    }

    public long getCTime() {
        return Ext2Utils.get32(datas, 20);
    }

    public void setZone(int index, long value) {
        Ext2Utils.set32(datas, 24 + (index * 4), value);
    }

    public long[] getZones() {
        long[] zones = new long[7];
        for (int i = 0; i < 7; i++) {
            zones[i] = Ext2Utils.get32(datas, 24 + (i * 4));
        }
        return zones;
    }

    public long getIndirectionZone() {
        return Ext2Utils.get32(datas, 52);
    }

    public long getDoubleIndirectionZone() {
        return Ext2Utils.get32(datas, 56);
    }

    public int getMaskedMode() {
        return getMode() & S_IFMT;
    }
}
