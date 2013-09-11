package org.jnode.fs.minix;

public class INode {

    public static final int S_IFMT = 0xF000; // format mask
    public static final int S_IFSOCK = 0xC000; // socket
    public static final int S_IFLNK = 0xA000; // symbolic link
    public static final int S_IFREG = 0x8000; // regular file
    public static final int S_IFBLK = 0x6000; // block device
    public static final int S_IFDIR = 0x4000; // directory
    public static final int S_IFCHR = 0x2000; // character device
    public static final int S_IFIFO = 0x1000; // fifo

    private int number;

    private int mode;

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getMaskedMode() {
        return mode & S_IFMT;
    }
}
