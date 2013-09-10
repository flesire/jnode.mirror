package org.jnode.fs.minix;

import org.jnode.fs.spi.AbstractFSEntry;
import org.jnode.fs.spi.AbstractFileSystem;

public class MinixEntry extends AbstractFSEntry {

    public MinixEntry(AbstractFileSystem<?> fs) {
        super(fs);
    }
}
