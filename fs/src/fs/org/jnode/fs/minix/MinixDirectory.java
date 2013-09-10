package org.jnode.fs.minix;

import java.io.IOException;
import org.jnode.fs.FSEntry;
import org.jnode.fs.spi.AbstractFSDirectory;
import org.jnode.fs.spi.AbstractFileSystem;
import org.jnode.fs.spi.FSEntryTable;

public class MinixDirectory extends AbstractFSDirectory{

    public MinixDirectory(AbstractFileSystem<?> fs) {
        super(fs);
    }

    public MinixDirectory(AbstractFileSystem<?> fs, boolean root) {
        super(fs, root);
    }

    @Override
    protected FSEntryTable readEntries() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void writeEntries(FSEntryTable entries) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected FSEntry createFileEntry(String name) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected FSEntry createDirectoryEntry(String name) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
