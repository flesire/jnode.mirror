package org.jnode.fs.minix;

import java.io.File;
import java.io.IOException;
import org.jnode.driver.Device;
import org.jnode.driver.block.FileDevice;
import org.jnode.emu.plugin.model.DummyConfigurationElement;
import org.jnode.emu.plugin.model.DummyExtension;
import org.jnode.emu.plugin.model.DummyExtensionPoint;
import org.jnode.emu.plugin.model.DummyPluginDescriptor;
import org.jnode.fs.service.FileSystemService;
import org.jnode.fs.service.def.FileSystemPlugin;
import org.jnode.test.support.TestUtils;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;

public class MinixFileSystemTest {

    private Device device;
    private FileSystemService fss;

    @Before
    public void setUp() throws Exception {
        // create test device.
        device = createTestDisk(false);
        // create file system service.
        fss = createFSService();

    }

    @Test
    public void testCreate() throws Exception {
        MinixFileSystem fs = getFileSystem();
        fs.read();
        MinixEntry entry = fs.createRootEntry();
        assertNotNull(entry);
    }

    //

    private MinixFileSystem getFileSystem() throws Exception {
        MinixFileSystemType type = fss.getFileSystemType(MinixFileSystemType.ID);
        MinixFileSystem fs = new MinixFileSystem(device, false, type);
        fs.create(SuperBlock.Version.V2, 30);
        return fs;
    }

    private Device createTestDisk(boolean formatted) throws IOException {
        File file = TestUtils.makeTempFile("minixDevice", "10M");
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
