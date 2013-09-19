package org.jnode.fs.minix;

import java.io.IOException;
import org.jnode.fs.FSDirectory;
import org.jnode.fs.minix.inode.INode;
import org.jnode.fs.spi.AbstractFSEntry;

public class MinixEntry extends AbstractFSEntry {

    private INode iNode;

    public MinixEntry(INode iNode, String name, MinixFileSystem fs, FSDirectory parent)
            throws IOException {
        super(fs, null, parent, name, getFSEntryType(name, iNode));
        this.iNode = iNode;
        this.setLastModified(iNode.getMTime());
    }

    public int getNumber() {
        return iNode.getNumber();
    }

    public long[] getZones() {
        return iNode.getZones();
    }

    public long getFileSize() {
        return iNode.getFileSize();
    }

    //

    private static int getFSEntryType(String name, INode iNode) {
        int mode = iNode.getMaskedMode();
        if ("/".equals(name))
            return AbstractFSEntry.ROOT_ENTRY;
        else if (mode == INode.S_IFDIR)
            return AbstractFSEntry.DIR_ENTRY;
        else if (mode == INode.S_IFREG || mode == INode.S_IFLNK || mode == INode.S_IFIFO ||
                mode == INode.S_IFCHR || mode == INode.S_IFBLK)
            return AbstractFSEntry.FILE_ENTRY;
        else
            return AbstractFSEntry.OTHER_ENTRY;
    }
}
