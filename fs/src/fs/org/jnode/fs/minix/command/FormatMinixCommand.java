package org.jnode.fs.minix.command;

import org.jnode.fs.Formatter;
import org.jnode.fs.command.AbstractFormatCommand;
import org.jnode.fs.minix.MinixFileSystem;
import org.jnode.fs.minix.MinixFileSystemFormatter;

public class FormatMinixCommand extends AbstractFormatCommand<MinixFileSystem> {

    public FormatMinixCommand(String description) {
        super("Format a block device with Minix v2 filesystem");
    }

    @Override
    protected Formatter<MinixFileSystem> getFormatter() {
        return new MinixFileSystemFormatter();
    }
}
