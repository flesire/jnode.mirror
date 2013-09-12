package org.jnode.fs.minix;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.jnode.driver.Device;
import org.jnode.driver.block.FSBlockDeviceAPI;
import org.jnode.fs.BlockDeviceFileSystemType;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.ext2.Ext2Utils;
import org.jnode.partitions.PartitionTableEntry;

public class MinixFileSystemType implements BlockDeviceFileSystemType<MinixFileSystem> {

    public static final Class<MinixFileSystemType> ID = MinixFileSystemType.class;

    @Override
    public boolean supports(PartitionTableEntry pte, byte[] firstSector, FSBlockDeviceAPI devApi) {
        ByteBuffer magic = ByteBuffer.allocate(2);
        try {
            devApi.read(1024 + 36, magic);
        } catch (IOException e) {
            return false;
        }
        int fsMagic = Ext2Utils.get16(magic.array(), 0);
        return (fsMagic == SuperBlock.MINIX_SUPER_MAGIC ||
                fsMagic == SuperBlock.MINIX_SUPER_MAGIC2 ||
                fsMagic == SuperBlock.MINIX2_SUPER_MAGIC || fsMagic == SuperBlock.MINIX2_SUPER_MAGIC2);

    }

    @Override
    public String getName() {
        return "MINIX"; // To change body of implemented methods use File |
                        // Settings | File Templates.
    }

    @Override
    public MinixFileSystem create(Device device, boolean readOnly) throws FileSystemException {
        MinixFileSystem fs = new MinixFileSystem(device, readOnly, this);
        fs.read();
        return fs;
    }
}
