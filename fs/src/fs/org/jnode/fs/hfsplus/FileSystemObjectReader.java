package org.jnode.fs.hfsplus;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.log4j.Logger;
import org.jnode.fs.hfsplus.catalog.Catalog;
import org.jnode.fs.hfsplus.catalog.CatalogKey;
import org.jnode.fs.hfsplus.catalog.CatalogLeafNode;
import org.jnode.fs.hfsplus.extent.Extent;
import org.jnode.fs.hfsplus.extent.ExtentKey;
import org.jnode.fs.hfsplus.tree.BTHeaderRecord;
import org.jnode.fs.hfsplus.tree.NodeDescriptor;

public class FileSystemObjectReader {

    private final static Logger LOGGER = Logger.getLogger(FileSystemObjectReader.class);

    public static VolumeHeader readVolumeHeader(HfsPlusFileSystem fs) throws IOException {
        VolumeHeader volumeHeader = new VolumeHeader(fs);
        volumeHeader.read(1024,VolumeHeader.SUPERBLOCK_LENGTH);
        volumeHeader.check();
        return volumeHeader;
    }

    public static void writeVolumeHeader(HfsPlusFileSystem fs) throws IOException {
        fs.getApi().write(1024, ByteBuffer.wrap(fs.getVolumeHeader().getBytes()));
        fs.flush();
    }

    public static Catalog readCatalog(HfsPlusFileSystem fs) throws IOException {
        HfsPlusForkData catalogFork = fs.getVolumeHeader().getCatalogFile();
        // read node descriptor
        NodeDescriptor descriptor = new NodeDescriptor(readForkData(fs,catalogFork,0,NodeDescriptor.BT_NODE_DESCRIPTOR_LENGTH),0);
        // Read header record
        BTHeaderRecord headerRecord = new BTHeaderRecord(readForkData(fs,catalogFork,NodeDescriptor.BT_NODE_DESCRIPTOR_LENGTH,BTHeaderRecord.BT_HEADER_RECORD_LENGTH),0);
        // Construct catalog
        HfsPlusForkDataFactory factory = new HfsPlusForkDataFactory(fs);
        return new Catalog(descriptor,headerRecord, factory);
    }

    public static Catalog createCatalog(HfsPlusFileSystem fs, HFSPlusParams params) throws IOException {
        int nodeSize = params.getCatalogNodeSize();
        int bufferLength = 0;
        // create node descriptor
        NodeDescriptor descriptor = new NodeDescriptor(0, 0, NodeDescriptor.BT_HEADER_NODE, 0, 3);
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
        return new Catalog(descriptor,headerRecord, factory);
    }

    /**
     *
     * @param fs
     * @param catalog
     * @throws IOException
     */
    public static void writeCatalog(HfsPlusFileSystem fs, Catalog catalog, CatalogLeafNode rootNode) throws IOException {
        VolumeHeader vh = fs.getVolumeHeader();
        long offset = vh.getCatalogFile().getExtent(0).getStartOffset(vh.getBlockSize());
        ByteBuffer buffer = ByteBuffer.allocate(NodeDescriptor.BT_NODE_DESCRIPTOR_LENGTH + BTHeaderRecord.BT_HEADER_RECORD_LENGTH);
        buffer.put(catalog.getBTNodeDescriptor().getBytes());
        buffer.put(catalog.getBTHeaderRecord().getBytes());
        buffer.flip();
        fs.getApi().write(offset, buffer);
        offset = catalog.getBTHeaderRecord().getRootNodeOffset();
        buffer = ByteBuffer.allocate(catalog.getBTHeaderRecord().getNodeSize());
        buffer.put(rootNode.getBytes());
        buffer.flip();
        fs.getApi().write(offset, buffer);
   }

    public static Extent readExtent(HfsPlusFileSystem fs) throws IOException {
        HfsPlusForkData extentFork = fs.getVolumeHeader().getExtentsFile();
        // read node descriptor
        NodeDescriptor descriptor = new NodeDescriptor(readForkData(fs,extentFork,0,NodeDescriptor.BT_NODE_DESCRIPTOR_LENGTH),0);
        // Read header record
        BTHeaderRecord headerRecord = new BTHeaderRecord(readForkData(fs,extentFork,0,BTHeaderRecord.BT_HEADER_RECORD_LENGTH),0);
        // Construct catalog
        return new Extent(descriptor,headerRecord);
    }

    /**
     *
     * @param fs
     * @param params
     * @return
     */
    public static Extent createExtent(HfsPlusFileSystem fs, HFSPlusParams params) {
        // create node descriptor
        NodeDescriptor descriptor = new NodeDescriptor(0, 0, NodeDescriptor.BT_HEADER_NODE, 0, 3);
        // create BTree header record.
        int nodeSize = params.getExtentNodeSize();
        int totalNodes = params.getExtentClumpSize() / nodeSize;
        int freeNodes = totalNodes - 1;
        BTHeaderRecord headerRecord = new BTHeaderRecord(0, 0, 0, 0, 0, nodeSize,
            ExtentKey.MAXIMUM_KEY_LENGTH, totalNodes, freeNodes,
            params.getExtentClumpSize(), BTHeaderRecord.BT_TYPE_HFS,
            BTHeaderRecord.KEY_COMPARE_TYPE_CASE_FOLDING,
            BTHeaderRecord.BT_BIG_KEYS_MASK);
        //
        long nodeBitsInHeader = 8 * (nodeSize
            - NodeDescriptor.BT_NODE_DESCRIPTOR_LENGTH
            - BTHeaderRecord.BT_HEADER_RECORD_LENGTH
            - 128 //kBTreeHeaderUserBytes
            - 8 );
        if(headerRecord.getTotalNodes() > nodeBitsInHeader) {
            descriptor.setfLink(headerRecord.getLastLeafNode() + 1);
            long nodeBitsInMapNode = 8 * (nodeSize
                - NodeDescriptor.BT_NODE_DESCRIPTOR_LENGTH
                - 4
                - 2 );
            long mapNodes = (headerRecord.getTotalNodes() - nodeBitsInHeader + (nodeBitsInMapNode - 1)) / nodeBitsInMapNode;
            long newFreeNodesValue = headerRecord.getFreeNodes() - mapNodes;
            headerRecord.setFreeNodes((int)newFreeNodesValue);
        }
        // return a new Extent.

        return new Extent(descriptor, headerRecord);
    }

    public static void writeExtent(HfsPlusFileSystem fs, Extent extent) throws IOException {
        VolumeHeader vh = fs.getVolumeHeader();
        long offset = vh.getExtentsFile().getExtent(0).getStartOffset(vh.getBlockSize());
        ByteBuffer buffer = ByteBuffer.wrap(extent.getDescriptor().getBytes());
        fs.getApi().write(offset, buffer);
        buffer = ByteBuffer.wrap(extent.getHeaderRecord().getBytes());
        offset = offset + NodeDescriptor.BT_NODE_DESCRIPTOR_LENGTH;
        fs.getApi().write(offset, buffer);
    }

    public static ByteBuffer readForkData(HfsPlusFileSystem fs, HfsPlusForkData data, int offset, int size) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        data.read(fs, offset, buffer);
        return  buffer;

    }



}
