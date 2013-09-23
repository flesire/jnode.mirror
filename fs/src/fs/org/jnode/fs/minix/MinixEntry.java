package org.jnode.fs.minix;

import java.io.IOException;
import org.jnode.fs.FSDirectory;
import org.jnode.fs.spi.AbstractFSEntry;

public class MinixEntry extends AbstractFSEntry {

    private INode iNode;

    public MinixEntry(INode iNode, String name, MinixFileSystem fs, FSDirectory parent)
            throws IOException {
        super(fs, null, parent, name, getFSEntryType(name, iNode));
        this.iNode = iNode;
        this.setLastModified(iNode.getMtime());
    }

    public INode getiNode() {
        return iNode;
    }

    public void incrementLinks() {
        iNode.setLinks(iNode.getLinks() + 1);
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
