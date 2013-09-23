package org.jnode.fs.minix;

import java.io.File;
import java.io.IOException;
import org.jnode.driver.Device;
import org.jnode.driver.block.FileDevice;
import org.jnode.emu.plugin.model.DummyConfigurationElement;
import org.jnode.emu.plugin.model.DummyExtension;
import org.jnode.emu.plugin.model.DummyExtensionPoint;
import org.jnode.emu.plugin.model.DummyPluginDescriptor;
import org.jnode.fs.FSDirectory;
import org.jnode.fs.service.FileSystemService;
import org.jnode.fs.service.def.FileSystemPlugin;
import org.jnode.test.support.TestUtils;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static org.jnode.fs.minix.MinixVersion.V2;

public class MinixFileSystemTest {

    private Device newDevice;
    private Device existingDevice;
    private FileSystemService fss;

    @Before
    public void setUp() throws Exception {
        // create test newDevice.
        newDevice = createTestDisk(false);
        existingDevice = getExistingDisk();
        // create file system service.
        fss = createFSService();

    }

    @Test
    public void testCreate() throws Exception {
        MinixFileSystemType type = fss.getFileSystemType(MinixFileSystemType.ID);
        MinixFileSystem fs = new MinixFileSystem(newDevice, false, type);
        fs.create(V2, 30);
        fs.close();
        fs = new MinixFileSystem(newDevice, false, type);
        fs.read();
        MinixEntry entry = fs.createRootEntry();
        assertNotNull(entry);
    }

    @Test
    public void readAnExistingFileSystem() throws Exception {
        MinixFileSystemType type = fss.getFileSystemType(MinixFileSystemType.ID);
        MinixFileSystem fs = new MinixFileSystem(existingDevice, false, type);
        fs.read();
        MinixEntry entry = fs.createRootEntry();
        assertNotNull(entry);
        FSDirectory directory = entry.getDirectory();
        entry = (MinixEntry) directory.getEntry(".");
        assertNotNull(entry);
    }

    //

    private Device createTestDisk(boolean formatted) throws IOException {
        File file = TestUtils.makeTempFile("minixDevice", "10M");
        Device device = new FileDevice(file, "rw");
        return device;

    }

    private Device getExistingDisk() throws IOException {
        File file = new File("/home/flesire/minix");
        Device device = new FileDevice(file, "rw");
        return device;

    }

    private FileSystemService createFSService() {
        DummyPluginDescriptor desc = new DummyPluginDescriptor(true);
        DummyExtensionPoint ep = new DummyExtensionPoint("types", "org.jnode.fs.types", "types");
        desc.addExtensionPoint(ep);
        DummyExtension extension = new DummyExtension();
        DummyConfigurationElement element = new DummyConfigurationElement();
        element.addAttribute("class", MinixFileSystemType.class.getName());
        extension.addElement(element);
        ep.addExtension(extension);
        return new FileSystemPlugin(desc);
    }
}
