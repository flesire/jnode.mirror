package org.jnode.fs.minix;

import org.jnode.fs.ext2.Ext2Utils;

public class MinixDirectoryEntry {

    public final static int DENTRY_SIZE = 32;

    private byte[] data;

    public MinixDirectoryEntry() {
        data = new byte[DENTRY_SIZE];
    }

    public MinixDirectoryEntry(byte[] data) {
        this.data = data;
    }

    public void setInodeNumber(int number) {
        Ext2Utils.set16(data, 0, number);
    }

    public void setName(String name) {
        for (int i = 0; i < name.length(); i++)
            Ext2Utils.set8(data, 2 + i, name.charAt(i));
    }

    public int getInodeNumber() {
        return Ext2Utils.get16(data, 0);
    }

    public String getName() {
        char[] buffer = new char[30];
        for (int i = 0; i < 30; i++) {
            buffer[i] = (char) Ext2Utils.get8(data, 2 + i);
        }
        return new String(buffer);
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "MinixDirectoryEntry{" + "inode=" + getInodeNumber() + "name=" + getName() + '}';
    }
}
