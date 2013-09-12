package org.jnode.fs.minix;

import org.jnode.fs.ext2.Ext2Utils;

public class MinixDirectoryEntry {

    private byte[] data;

    public MinixDirectoryEntry() {
        data = new byte[32];
    }

    public void setInodeNumber(int number) {
        Ext2Utils.set16(data, 0, number);
    }

    public void setName(String name) {
        for (int i = 0; i < name.length(); i++)
            Ext2Utils.set8(data, 2 + i, name.charAt(i));
    }

    public byte[] getData() {
        return data;
    }

}
