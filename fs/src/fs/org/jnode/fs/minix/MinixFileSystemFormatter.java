package org.jnode.fs.minix;

import javax.naming.NameNotFoundException;
import org.jnode.driver.Device;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.Formatter;
import org.jnode.fs.service.FileSystemService;
import org.jnode.naming.InitialNaming;

import static org.jnode.fs.minix.MinixVersion.V2;

public class MinixFileSystemFormatter extends Formatter<MinixFileSystem> {

    private MinixVersion version;
    private int nameLen;

    public MinixFileSystemFormatter() {
        super(new MinixFileSystemType());
    }

    @Override
    public MinixFileSystem format(Device device) throws FileSystemException {
        try {
            FileSystemService fSS = InitialNaming.lookup(FileSystemService.NAME);
            MinixFileSystemType type = fSS.getFileSystemType(MinixFileSystemType.ID);
            MinixFileSystem fs = new MinixFileSystem(device, false, type);
            fs.create(V2, 30);
            return fs;
        } catch (NameNotFoundException e) {
            throw new FileSystemException(e);
        }
    }
}
