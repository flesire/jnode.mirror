package org.jnode.fs.hfsplus;

import java.io.IOException;
import java.nio.ByteBuffer;

public class HfsPlusForkDataFactory {

    private HfsPlusFileSystem fileSystem;

    public HfsPlusForkDataFactory(HfsPlusFileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public ByteBuffer readForkData(HfsPlusForkData data, int offset, int size) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        data.read(fileSystem, offset, buffer);
        return buffer;
    }

    public VolumeHeader getVolumeHeader() {
        return fileSystem.getVolumeHeader();
    }
}
