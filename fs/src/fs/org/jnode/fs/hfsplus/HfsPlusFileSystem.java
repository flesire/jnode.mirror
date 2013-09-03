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

import java.io.IOException;
import java.util.Arrays;
import org.apache.log4j.Logger;
import org.jnode.driver.ApiNotFoundException;
import org.jnode.driver.Device;
import org.jnode.driver.block.BlockDeviceAPI;
import org.jnode.driver.block.FSBlockDeviceAPI;
import org.jnode.fs.FSDirectory;
import org.jnode.fs.FSEntry;
import org.jnode.fs.FSFile;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.hfsplus.catalog.Catalog;
import org.jnode.fs.hfsplus.catalog.CatalogKey;
import org.jnode.fs.hfsplus.catalog.CatalogNodeId;
import org.jnode.fs.hfsplus.extent.Extent;
import org.jnode.fs.hfsplus.tree.LeafRecord;
import org.jnode.fs.spi.AbstractFileSystem;

public class HfsPlusFileSystem extends AbstractFileSystem<HfsPlusEntry> {
	private final Logger log = Logger.getLogger(getClass());

	/** HFS volume header */
	private VolumeHeader volumeHeader;

    private Extent extent;

	/** Catalog special file for this instance */
	private Catalog catalog;

	/**
	 * @param device
	 * @param readOnly
	 * @param type
	 * @throws FileSystemException
	 */
	public HfsPlusFileSystem(final Device device, final boolean readOnly, final HfsPlusFileSystemType type)
			throws FileSystemException {
		super(device, readOnly, type);
	}

	/**
	 * @throws FileSystemException
	 */
	public final void read() throws FileSystemException {
        try {
            readVolumeHeader();
            if (!volumeHeader.isAttribute(VolumeHeader.HFSPLUS_VOL_UNMNT_BIT)) {
                log.info(getDevice().getId() + " Filesystem has not been cleanly unmounted, mounting it readonly");
                setReadOnly(true);
            }
            if (volumeHeader.isAttribute(VolumeHeader.HFSPLUS_VOL_SOFTLOCK_BIT)) {
                log.info(getDevice().getId() + " Filesystem is marked locked, mounting it readonly");
                setReadOnly(true);
            }
            if (volumeHeader.isAttribute(VolumeHeader.HFSPLUS_VOL_JOURNALED_BIT)) {
                log.info(getDevice().getId()
                        + " Filesystem is journaled, write access is not supported. Mounting it readonly");
                setReadOnly(true);
            }
            extent = FileSystemObjectReader.readExtent(this);
			catalog = FileSystemObjectReader.readCatalog(this);
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	protected final FSDirectory createDirectory(final FSEntry entry) throws IOException {
		return entry.getDirectory();
	}

	@Override
	protected final FSFile createFile(final FSEntry entry) throws IOException {
		return entry.getFile();
	}

	@Override
	protected final HfsPlusEntry createRootEntry() throws IOException {
		log.info("Create root entry.");
		LeafRecord record = catalog.getRecord(CatalogNodeId.HFSPLUS_POR_CNID);
		if (record != null) {
			return new HfsPlusEntry(this, null, "/", record);
		}
		log.error("Root entry : No record found.");
		return null;
	}

	public final long getFreeSpace() {
		return volumeHeader.getFreeBlocks() * volumeHeader.getBlockSize();
	}

	public final long getTotalSpace() {
		return volumeHeader.getTotalBlocks() * volumeHeader.getBlockSize();
	}

	public final long getUsableSpace() {
		return -1;
	}

	@Override
	public String getVolumeName() throws IOException {
		LeafRecord record = catalog.getRecord(CatalogNodeId.HFSPLUS_POR_CNID);
		return ((CatalogKey) record.getKey()).getNodeName().getUnicodeString();
	}

	public final Catalog getCatalog() {
		return catalog;
	}

	public final VolumeHeader getVolumeHeader() {
		return volumeHeader;
	}

	/**
	 * Create a new HFS+ file system.
	 * @param params creation parameters
	 * @throws FileSystemException
	 */
	public void create(HFSPlusParams params) throws FileSystemException {
		volumeHeader = new VolumeHeader(this);
		try {
			params.initializeDefaultsValues(this);
			volumeHeader.create(params);
			log.info("Volume header : \n" + volumeHeader.toString());
			long volumeBlockUsed = volumeHeader.getTotalBlocks() - volumeHeader.getFreeBlocks()
					- ((volumeHeader.getBlockSize() == 512) ? 2 : 1);
			// ---
			log.debug("Write allocation bitmap bits to disk.");
			writeAllocationFile((int) volumeBlockUsed);
            // Create and write extent file
            Extent extent = FileSystemObjectReader.createExtent(this, params);
            FileSystemObjectReader.writeExtent(this,extent);
            //
			log.debug("Write Catalog to disk.");
			Catalog catalog = FileSystemObjectReader.createCatalog(this, params);
            catalog.createRootNode(params);
            FileSystemObjectReader.writeCatalog(this, catalog);
			log.debug("Write volume header to disk.");
            writeVolumeHeader();
		} catch (IOException e) {
			throw new FileSystemException("Unable to create HFS+ filesystem", e);
		} catch (ApiNotFoundException e) {
            throw new FileSystemException("Unable to create HFS+ filesystem", e);
        }
    }

	private void writeAllocationFile(int blockUsed) throws IOException, ApiNotFoundException {
		int bytes = blockUsed >> 3;
		int bits = blockUsed & 0x0007;
        if(bits == 0) {
            ++bytes;
        }
        FSBlockDeviceAPI api = (FSBlockDeviceAPI) getDevice().getAPI(BlockDeviceAPI.class);
        int sectorSize = api.getSectorSize();
        int bytesUsed = roundUp(bytes, sectorSize);
        int[] bitmap;
        if (bytesUsed > bytes) {
            bitmap = new int[bytesUsed];
            Arrays.fill(bitmap,0xFF);
            if(bits == 0) {
                bitmap[bytes - 1] = (0xFF00 >> bits) & 0xFF;
            }
            // put OxOO between bytes and bytesUsed indexes
        } else {
            bitmap = new int[bytes];
            Arrays.fill(bitmap,0xFF);
            if(bits == 0) {
                bitmap[bytes - 1] = (0xFF00 >> bits) & 0xFF;
            }
        }
        // Write to disk
    }

    private int roundUp(int x, int u) {
        return (((x) % (u) == 0) ? (x) : ((x)/(u) + 1) * (u));
    }

    private void readVolumeHeader() throws IOException {
        VolumeHeader volumeHeader = new VolumeHeader(this);
        volumeHeader.read(1024,VolumeHeader.SUPERBLOCK_LENGTH);
        volumeHeader.check();
        this.volumeHeader = volumeHeader;
    }

    private void writeVolumeHeader() throws IOException {
        volumeHeader.write(1024);
    }
}
