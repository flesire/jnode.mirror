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
import java.nio.ByteBuffer;
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
import org.jnode.fs.hfsplus.catalog.CatalogLeafNode;
import org.jnode.fs.hfsplus.catalog.CatalogNodeId;
import org.jnode.fs.hfsplus.extent.Extent;
import org.jnode.fs.hfsplus.extent.ExtentKey;
import org.jnode.fs.hfsplus.tree.BTHeaderRecord;
import org.jnode.fs.hfsplus.tree.LeafRecord;
import org.jnode.fs.hfsplus.tree.NodeDescriptor;
import org.jnode.fs.hfsplus.tree.NodeType;
import org.jnode.fs.spi.AbstractFileSystem;

public class HfsPlusFileSystem extends AbstractFileSystem<HfsPlusEntry> {
    private final Logger log = Logger.getLogger(getClass());

    /** HFS volume header */
    private VolumeHeader volumeHeader;
    /** Catalog special file for this instance */
    private Catalog catalog;

    private Extent extent;

    public HfsPlusFileSystem(final Device device, final boolean readOnly,
            final HfsPlusFileSystemType type) throws FileSystemException {
        super(device, readOnly, type);
    }

    public final Catalog getCatalog() {
        return catalog;
    }

    public final VolumeHeader getVolumeHeader() {
        return volumeHeader;
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

    public void flush() throws IOException {
    }

    /**
     * @throws FileSystemException
     */
    public final void read() throws FileSystemException {
        readVolumeHeader();
        checkAttributes();
        readCatalog();
        readExtent();
    }

    /**
     * Create a new HFS+ file system.
     * 
     * @param params creation parameters
     * @throws FileSystemException
     */
    public void create(HFSPlusParams params) throws FileSystemException {
        volumeHeader = new VolumeHeader(this);
        try {
            params.initializeDefaultsValues(this);
            volumeHeader.create(params);
            long volumeBlockUsed =
                    volumeHeader.getTotalBlocks() - volumeHeader.getFreeBlocks() -
                            ((volumeHeader.getBlockSize() == 512) ? 2 : 1);
            // ---
            writeAllocationFile((int) volumeBlockUsed);
            // Create and write extent file
            extent = createExtent(this, params);
            writeExtent(this, extent);
            //
            catalog = createCatalog(this, params);
            CatalogLeafNode rootNode = catalog.createRootNode(params);
            writeCatalog(this, catalog, rootNode);
            writeVolumeHeader();
        } catch (IOException e) {
            throw new FileSystemException("Unable to create HFS+ filesystem", e);
        } catch (ApiNotFoundException e) {
            throw new FileSystemException("Unable to create HFS+ filesystem", e);
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
        CatalogNodeId cnid = CatalogNodeId.HFSPLUS_POR_CNID;
        LeafRecord record = catalog.getRecord(cnid);
        if (record != null) {
            return new HfsPlusEntry(this, null, "/", record, cnid);
        }
        log.error("Root entry : No record found.");
        return null;
    }

    private void writeAllocationFile(int blockUsed) throws IOException, ApiNotFoundException {
        int bytes = blockUsed >> 3;
        int bits = blockUsed & 0x0007;
        if (bits == 0) {
            ++bytes;
        }
        FSBlockDeviceAPI api = (FSBlockDeviceAPI) getDevice().getAPI(BlockDeviceAPI.class);
        int sectorSize = api.getSectorSize();
        int bytesUsed = HfsUtils.roundUp(bytes, sectorSize);
        int[] bitmap;
        if (bytesUsed > bytes) {
            bitmap = new int[bytesUsed];
            Arrays.fill(bitmap, 0xFF);
            if (bits == 0) {
                bitmap[bytes - 1] = (0xFF00 >> bits) & 0xFF;
            }
            // put OxOO between bytes and bytesUsed indexes
        } else {
            bitmap = new int[bytes];
            Arrays.fill(bitmap, 0xFF);
            if (bits == 0) {
                bitmap[bytes - 1] = (0xFF00 >> bits) & 0xFF;
            }
        }
        // Write to disk
    }

    /**
     * 
     * @throws FileSystemException
     */
    private void readVolumeHeader() throws FileSystemException {
        volumeHeader = new VolumeHeader(this);
        try {
            volumeHeader.read(1024, VolumeHeader.SUPERBLOCK_LENGTH);
            volumeHeader.check();
        } catch (IOException e) {
            throw new FileSystemException(e);
        }
    }

    private ByteBuffer readForkData(HfsPlusForkData data, int offset, int size) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        data.read(this, offset, buffer);
        return buffer;

    }

    public void readCatalog() throws FileSystemException {
        try {
            HfsPlusForkData catalogFork = volumeHeader.getCatalogFile();
            // read node descriptor
            NodeDescriptor descriptor =
                    new NodeDescriptor(readForkData(catalogFork, 0,
                            NodeDescriptor.BT_NODE_DESCRIPTOR_LENGTH), 0);
            // Read header record
            BTHeaderRecord headerRecord =
                    new BTHeaderRecord(readForkData(catalogFork,
                            NodeDescriptor.BT_NODE_DESCRIPTOR_LENGTH,
                            BTHeaderRecord.BT_HEADER_RECORD_LENGTH), 0);
            // Construct catalog
            HfsPlusForkDataFactory factory = new HfsPlusForkDataFactory(this);
            catalog = new Catalog(descriptor, headerRecord, factory);
        } catch (IOException e) {
            throw new FileSystemException(e);
        }
    }

    public void readExtent() throws FileSystemException {
        try {
            HfsPlusForkData extentFork = volumeHeader.getExtentsFile();
            // read node descriptor
            NodeDescriptor descriptor =
                    new NodeDescriptor(readForkData(extentFork, 0,
                            NodeDescriptor.BT_NODE_DESCRIPTOR_LENGTH), 0);
            // Read header record
            BTHeaderRecord headerRecord =
                    new BTHeaderRecord(readForkData(extentFork, 0,
                            BTHeaderRecord.BT_HEADER_RECORD_LENGTH), 0);
            // Construct extent
            extent = new Extent(descriptor, headerRecord);
        } catch (IOException e) {
            throw new FileSystemException(e);
        }
    }

    /**
     * 
     * @throws FileSystemException
     */
    protected void writeVolumeHeader() throws IOException {
        if (volumeHeader.isDirty()) {
            volumeHeader.write(1024);
            volumeHeader.resetDirty();
            log.info("Volume header is written correctly.");
        }
    }

    private void checkAttributes() {
        if (!volumeHeader.isAttribute(VolumeHeader.HFSPLUS_VOL_UNMNT_BIT)) {
            log.info(getDevice().getId() +
                    " Filesystem has not been cleanly unmounted, mounting it readonly");
            setReadOnly(true);
        }
        if (volumeHeader.isAttribute(VolumeHeader.HFSPLUS_VOL_SOFTLOCK_BIT)) {
            log.info(getDevice().getId() + " Filesystem is marked locked, mounting it readonly");
            setReadOnly(true);
        }
        if (volumeHeader.isAttribute(VolumeHeader.HFSPLUS_VOL_JOURNALED_BIT)) {
            log.info(getDevice().getId() +
                    " Filesystem is journaled, write access is not supported. Mounting it readonly");
            setReadOnly(true);
        }
    }

    private Catalog createCatalog(HfsPlusFileSystem fs, HFSPlusParams params) throws IOException {
        int nodeSize = params.getCatalogNodeSize();
        int bufferLength = 0;
        // create node descriptor
        NodeDescriptor descriptor = new NodeDescriptor(0, 0, NodeType.BT_HEADER_NODE, 0, 3);
        // create header record
        int totalNodes = params.getCatalogClumpSize() / params.getCatalogNodeSize();
        int freeNodes = totalNodes - 2;
        BTHeaderRecord headerRecord =
                new BTHeaderRecord(1, 1, params.getInitializeNumRecords(), 1, 1, nodeSize,
                        CatalogKey.MAXIMUM_KEY_LENGTH, totalNodes, freeNodes,
                        params.getCatalogClumpSize(), BTHeaderRecord.BT_TYPE_HFS,
                        BTHeaderRecord.KEY_COMPARE_TYPE_CASE_FOLDING,
                        BTHeaderRecord.BT_VARIABLE_INDEX_KEYS_MASK +
                                BTHeaderRecord.BT_BIG_KEYS_MASK);
        //
        HfsPlusForkDataFactory factory = new HfsPlusForkDataFactory(fs);
        return new Catalog(descriptor, headerRecord, factory);
    }

    /**
     * 
     * @param fs
     * @param catalog
     * @throws IOException
     */
    private void writeCatalog(HfsPlusFileSystem fs, Catalog catalog, CatalogLeafNode rootNode)
        throws IOException {
        VolumeHeader vh = fs.getVolumeHeader();
        long offset = vh.getCatalogFile().getExtent(0).getStartOffset(vh.getBlockSize());
        ByteBuffer buffer =
                ByteBuffer.allocate(NodeDescriptor.BT_NODE_DESCRIPTOR_LENGTH +
                        BTHeaderRecord.BT_HEADER_RECORD_LENGTH);
        buffer.put(catalog.getBTNodeDescriptor().getBytes());
        buffer.put(catalog.getBTHeaderRecord().getBytes());
        buffer.flip();
        fs.getApi().write(offset, buffer);
        offset += catalog.getBTHeaderRecord().getRootNodeOffset();
        buffer = ByteBuffer.allocate(catalog.getBTHeaderRecord().getNodeSize());
        buffer.put(rootNode.getBytes());
        buffer.flip();
        fs.getApi().write(offset, buffer);
        log.info("catalog is written correctly.");
    }

    /**
     * 
     * @param fs
     * @param params
     * @return
     */
    private Extent createExtent(HfsPlusFileSystem fs, HFSPlusParams params) {
        // create node descriptor
        NodeDescriptor descriptor = new NodeDescriptor(0, 0, NodeType.BT_HEADER_NODE, 0, 3);
        // create BTree header record.
        int nodeSize = params.getExtentNodeSize();
        int totalNodes = params.getExtentClumpSize() / nodeSize;
        int freeNodes = totalNodes - 1;
        BTHeaderRecord headerRecord =
                new BTHeaderRecord(0, 0, 0, 0, 0, nodeSize, ExtentKey.MAXIMUM_KEY_LENGTH,
                        totalNodes, freeNodes, params.getExtentClumpSize(),
                        BTHeaderRecord.BT_TYPE_HFS, BTHeaderRecord.KEY_COMPARE_TYPE_CASE_FOLDING,
                        BTHeaderRecord.BT_BIG_KEYS_MASK);
        //
        long nodeBitsInHeader =
                8 * (nodeSize - NodeDescriptor.BT_NODE_DESCRIPTOR_LENGTH -
                        BTHeaderRecord.BT_HEADER_RECORD_LENGTH - 128 // kBTreeHeaderUserBytes
                - 8);
        if (headerRecord.getTotalNodes() > nodeBitsInHeader) {
            descriptor.setNext(headerRecord.getLastLeafNode() + 1);
            long nodeBitsInMapNode =
                    8 * (nodeSize - NodeDescriptor.BT_NODE_DESCRIPTOR_LENGTH - 4 - 2);
            long mapNodes =
                    (headerRecord.getTotalNodes() - nodeBitsInHeader + (nodeBitsInMapNode - 1)) /
                            nodeBitsInMapNode;
            long newFreeNodesValue = headerRecord.getFreeNodes() - mapNodes;
            headerRecord.setFreeNodes((int) newFreeNodesValue);
        }
        // return a new Extent.

        return new Extent(descriptor, headerRecord);
    }

    private void writeExtent(HfsPlusFileSystem fs, Extent extent) throws IOException {
        VolumeHeader vh = fs.getVolumeHeader();
        long offset = vh.getExtentsFile().getExtent(0).getStartOffset(vh.getBlockSize());
        ByteBuffer buffer = ByteBuffer.wrap(extent.getDescriptor().getBytes());
        fs.getApi().write(offset, buffer);
        buffer = ByteBuffer.wrap(extent.getHeaderRecord().getBytes());
        offset = offset + NodeDescriptor.BT_NODE_DESCRIPTOR_LENGTH;
        fs.getApi().write(offset, buffer);
        log.info("Extents file is written correctly.");
    }

}
