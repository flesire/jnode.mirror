package org.jnode.fs.minix;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.jnode.fs.FSEntry;
import org.jnode.fs.spi.AbstractFSDirectory;
import org.jnode.fs.spi.FSEntryTable;

public class MinixDirectory extends AbstractFSDirectory {

    private final Logger log = Logger.getLogger(getClass());

    private MinixEntry entry;

    public MinixDirectory(MinixEntry entry) {
        super((MinixFileSystem) entry.getFileSystem());
        this.entry = entry;
    }

    @Override
    protected FSEntryTable readEntries() throws IOException {
        long[] zones = entry.getZones();
        MinixDirectoryEntry dentry = null;
        for (long zone : zones) {
            if (zone == 0) {
                break;
            }
            Block zoneBlock = new Block(zone);
            zoneBlock.read((MinixFileSystem) getFileSystem());
            int inode = -1;
            int i = 0;
            while (inode != 0) {
                dentry =
                        new MinixDirectoryEntry(zoneBlock.getData(i,
                                MinixDirectoryEntry.DENTRY_SIZE));
                inode = dentry.getInodeNumber();
                i += MinixDirectoryEntry.DENTRY_SIZE;
                log.debug("Directory entry found :" + dentry);
            }
        }
        return null;
    }

    @Override
    protected void writeEntries(FSEntryTable entries) throws IOException {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    @Override
    protected FSEntry createFileEntry(String name) throws IOException {
        return null; // To change body of implemented methods use File |
                     // Settings | File Templates.
    }

    @Override
    protected FSEntry createDirectoryEntry(String name) throws IOException {
        if (!canWrite()) {
            throw new IOException("Filesystem or directory is mounted read-only!");
        }
        return null; // To change body of implemented methods use File |
                     // Settings | File Templates.
    }
}
