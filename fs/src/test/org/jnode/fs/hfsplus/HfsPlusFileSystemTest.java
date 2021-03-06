/*
 * $Id$
 *
 * Copyright (C) 2003-2013 JNode.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jnode.fs.hfsplus;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jnode.driver.Device;
import org.jnode.driver.block.FileDevice;
import org.jnode.emu.plugin.model.DummyConfigurationElement;
import org.jnode.emu.plugin.model.DummyExtension;
import org.jnode.emu.plugin.model.DummyExtensionPoint;
import org.jnode.emu.plugin.model.DummyPluginDescriptor;
import org.jnode.fs.FSDirectory;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.service.FileSystemService;
import org.jnode.fs.service.def.FileSystemPlugin;
import org.jnode.test.support.TestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HfsPlusFileSystemTest {

    Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private Device device;
    private FileSystemService fss;

    @Before
    public void setUp() throws Exception {
        logger.setLevel(Level.ALL);
        // create test device.
        device = createTestDisk(false);
        // create file system service.
        fss = createFSService();

    }

    @Test
    public void testCreate() throws Exception {
        HfsPlusFileSystem fs = getFileSystem();
        VolumeHeader vh = fs.getVolumeHeader();
        vh.check();
        assertEquals(4096, vh.getBlockSize());

    }

    private HfsPlusFileSystem getFileSystem() throws FileSystemException {
        HfsPlusFileSystemType type = fss.getFileSystemType(HfsPlusFileSystemType.ID);
        HfsPlusFileSystem fs = new HfsPlusFileSystem(device, false, type);
        HFSPlusParams params = new HFSPlusParams();
        params.setVolumeName("testdrive");
        params.setBlockSize(HFSPlusParams.OPTIMAL_BLOCK_SIZE);
        params.setJournaled(false);
        params.setJournalSize(HFSPlusParams.DEFAULT_JOURNAL_SIZE);
        fs.create(params);
        return fs;
    }

    @Test
    public void testRead() throws Exception {
        HfsPlusFileSystem fs = getFileSystem();
        FSDirectory root = fs.getRootEntry().getDirectory();
        assertFalse("Must be empty", root.iterator().hasNext());
        fs.close();
        fs = new HfsPlusFileSystemType().create(device, false);
        fs.read();
        root = fs.getRootEntry().getDirectory();
        assertFalse("Must be empty", root.iterator().hasNext());
        root.addDirectory("test");
        fs.flush();
        fs.close();
        fs = new HfsPlusFileSystemType().create(device, false);
        fs.read();
        assertEquals(1, fs.getVolumeHeader().getFolderCount());
        fs.createRootEntry();
        root = fs.getRootEntry().getDirectory();
        assertTrue("Must contains one directory", root.iterator().hasNext());
    }

    private Device createTestDisk(boolean formatted) throws IOException {
        File file = TestUtils.makeTempFile("hfsDevice", "10M");
        Device device = new FileDevice(file, "rw");
        return device;

    }

    private FileSystemService createFSService() {
        DummyPluginDescriptor desc = new DummyPluginDescriptor(true);
        DummyExtensionPoint ep = new DummyExtensionPoint("types", "org.jnode.fs.types", "types");
        desc.addExtensionPoint(ep);
        DummyExtension extension = new DummyExtension();
        DummyConfigurationElement element = new DummyConfigurationElement();
        element.addAttribute("class", HfsPlusFileSystemType.class.getName());
        extension.addElement(element);
        ep.addExtension(extension);
        return new FileSystemPlugin(desc);
    }

}
