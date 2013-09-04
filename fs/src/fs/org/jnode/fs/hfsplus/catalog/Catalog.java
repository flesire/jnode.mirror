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
 
package org.jnode.fs.hfsplus.catalog;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;
import org.jnode.fs.hfsplus.HFSPlusParams;
import org.jnode.fs.hfsplus.HfsPlusForkData;
import org.jnode.fs.hfsplus.HfsPlusForkDataFactory;
import org.jnode.fs.hfsplus.HfsUnicodeString;
import org.jnode.fs.hfsplus.tree.BTHeaderRecord;
import org.jnode.fs.hfsplus.tree.IndexRecord;
import org.jnode.fs.hfsplus.tree.LeafRecord;
import org.jnode.fs.hfsplus.tree.NodeDescriptor;

public class Catalog {

    public static final int HFS_FILE_THREAD_EXISTS = 0x002;
    private final Logger log = Logger.getLogger(getClass());

    /**
     * B-Tree node descriptor
     */
    private NodeDescriptor descriptor;

    /**
     * B-Tree Header record
     */
    private BTHeaderRecord headerRecord;

    private HfsPlusForkDataFactory factory;

    private ByteBuffer buffer;

    /**
     * Default catalog constructor.
     * @param descriptor The node descriptor of the catalog.
     * @param headerRecord The BTree header record of the catalog.
     * @throws IOException
     */
    public Catalog(NodeDescriptor descriptor, BTHeaderRecord headerRecord, HfsPlusForkDataFactory factory) throws IOException {
        this.descriptor = descriptor;
        this.headerRecord = headerRecord;
        this.factory = factory;
    }

    public CatalogLeafNode createRootNode(HFSPlusParams params){
    	int nodeSize = params.getCatalogNodeSize();
    	NodeDescriptor nd =
                new NodeDescriptor(0, 0, NodeDescriptor.BT_LEAF_NODE, 1,
                        params.getInitializeNumRecords());
        CatalogLeafNode rootNode = new CatalogLeafNode(nd, nodeSize);
        // First record (folder)
        HfsUnicodeString name = new HfsUnicodeString(params.getVolumeName());
        CatalogKey ck = new CatalogKey(CatalogNodeId.HFSPLUS_POR_CNID, name);
        CatalogFolder folder =
                new CatalogFolder(params.isJournaled() ? 2 : 0, CatalogNodeId.HFSPLUS_ROOT_CNID);
        LeafRecord record = new LeafRecord(ck, folder.getBytes());
        rootNode.addNodeRecord(record);
        // Second record (thread)
        CatalogKey tck = new CatalogKey(CatalogNodeId.HFSPLUS_POR_CNID, name);
        CatalogThread ct =
                new CatalogThread(CatalogFolder.RECORD_TYPE_FOLDER_THREAD,
                        CatalogNodeId.HFSPLUS_ROOT_CNID, new HfsUnicodeString(""));
        record = new LeafRecord(tck, ct.getBytes());
        rootNode.addNodeRecord(record);
        log.debug(rootNode.toString());
        return rootNode;
    }

    /**
     * Create a new node in the catalog B-Tree.
     * 
     * @param filename
     * @param parentId
     * @param nodeId
     * @param nodeType
     * @return the new node instance
     */
    public CatalogLeafNode createNode(String filename, CatalogNodeId parentId, CatalogNodeId nodeId,
            int nodeType) throws IOException {
    	CatalogLeafNode node;
        HfsUnicodeString name = new HfsUnicodeString(filename);
        // find parent leaf record.
        LeafRecord record = this.getRecord(parentId, name);
        if (record == null) {
            NodeDescriptor nd = new NodeDescriptor(0, 0, NodeDescriptor.BT_LEAF_NODE, 1, 2);
            node = new CatalogLeafNode(nd, 8192);
            // Normal record
            record = createRecord(parentId, name, nodeType);
            node.addNodeRecord(record);
            // Thread record
            record = createThread(parentId, name, nodeType);
            node.addNodeRecord(record);
            
        } else {
            throw new IOException("Leaf record for parent (" + parentId.getId() + ") doesn't exist.");
        }
        return node;
    }

    private LeafRecord createThread(CatalogNodeId parentId, HfsUnicodeString name, int nodeType)    {
        CatalogKey key = new CatalogKey(parentId, name);
        int threadType;
        if (nodeType == CatalogFolder.RECORD_TYPE_FOLDER) {
            threadType = CatalogFolder.RECORD_TYPE_FOLDER_THREAD;
        } else {
            threadType = CatalogFile.RECORD_TYPE_FILE_THREAD;
        }
        CatalogThread thread =  new CatalogThread(threadType, parentId, name);
        return new LeafRecord(key, thread.getBytes());
    }

    private LeafRecord createRecord(CatalogNodeId parentId, HfsUnicodeString name, int nodeType) {
        CatalogKey key = new CatalogKey(parentId, name);
        if (nodeType == CatalogFolder.RECORD_TYPE_FOLDER) {
            CatalogFolder folder = new CatalogFolder(0, parentId);
            key = new CatalogKey(parentId, name);
            return new LeafRecord(key, folder.getBytes());
        } else {
            CatalogFile file = new CatalogFile(HFS_FILE_THREAD_EXISTS,parentId,null,null);
            return null;
        }
    }

    /**
     * @param parentID
     * @return the leaf record, or possibly {code null}.
     * @throws IOException
     */
    public final LeafRecord getRecord(final CatalogNodeId parentID) throws IOException {
        LeafRecord lr = null;
        int nodeSize = headerRecord.getNodeSize();
        ByteBuffer data = getNodeData(headerRecord.getRootNodeOffset(), nodeSize);
        NodeDescriptor nd = new NodeDescriptor(data, 0);
        IndexRecord record = null;
        CatalogIndexNode node = null;
        while (nd.isIndexNode()) {
            node = new CatalogIndexNode(data, nodeSize);
            record = (IndexRecord) node.find(parentID);
            data = getNodeData(record.getIndex(), nodeSize);
            nd = new NodeDescriptor(data, 0);
        }
        if (nd.isLeafNode()) {
            CatalogLeafNode leafNode = new CatalogLeafNode(data, nodeSize);
            lr = (LeafRecord) leafNode.find(parentID);
        }
        return lr;
    }

    /**
     * Find leaf records corresponding to parentID. The search begin at the root
     * node of the tree.
     * 
     * @param parentID Parent node id
     * @return Array of LeafRecord
     * @throws IOException
     */
    public final LeafRecord[] getRecords(final CatalogNodeId parentID) throws IOException {
        return getRecords(parentID, getBTHeaderRecord().getRootNode());
    }

    /**
     * Find leaf records corresponding to parentID. The search begin at the node
     * corresponding to the index passed as parameter.
     * 
     * @param parentID Parent node id
     * @param nodeNumber Index of node where the search begin.
     * @return Array of LeafRecord
     * @throws IOException
     */
    public final LeafRecord[] getRecords(final CatalogNodeId parentID, final int nodeNumber)
        throws IOException {
        // Get node data
        int nodeSize = getBTHeaderRecord().getNodeSize();
        ByteBuffer nodeData = getNodeData(nodeNumber, nodeSize);
        // extract descriptor
        NodeDescriptor descriptor = new NodeDescriptor(nodeData, 0);
        if (descriptor.isIndexNode()) {
            CatalogIndexNode node = new CatalogIndexNode(nodeData, nodeSize);
            IndexRecord[] records = (IndexRecord[]) node.findAll(parentID);
            List<LeafRecord> lfList = new LinkedList<LeafRecord>();
            for (IndexRecord rec : records) {
                LeafRecord[] lfr = getRecords(parentID, rec.getIndex());
                Collections.addAll(lfList, lfr);
            }
            return lfList.toArray(new LeafRecord[lfList.size()]);
        } else if (descriptor.isLeafNode()) {
            CatalogLeafNode node = new CatalogLeafNode(nodeData, nodeSize);
            return (LeafRecord[]) node.findAll(parentID);
        } else {
            return null;
        }

    }

    /**
     * @param parentID
     * @param nodeName
     * @return the leaf node or {@code null}
     * @throws IOException
     */
    public final LeafRecord getRecord(final CatalogNodeId parentID, final HfsUnicodeString nodeName)
        throws IOException {
        int nodeSize = getBTHeaderRecord().getNodeSize();
        ByteBuffer nodeData = getNodeData(getBTHeaderRecord().getRootNode(), nodeSize);
        NodeDescriptor nd = new NodeDescriptor(nodeData, 0);
        CatalogKey cKey = new CatalogKey(parentID, nodeName);
        while (nd.isIndexNode()) {
            CatalogIndexNode node = new CatalogIndexNode(nodeData, nodeSize);
            IndexRecord record = node.find(cKey);
            nodeData = getNodeData(record.getIndex(), nodeSize);
            node = new CatalogIndexNode(nodeData, nodeSize);
        }
        LeafRecord lr = null;
        if (nd.isLeafNode()) {
            CatalogLeafNode node = new CatalogLeafNode(nodeData, nodeSize);
            lr = node.find(parentID);
        }
        return lr;
    }

    public final NodeDescriptor getBTNodeDescriptor() {
        return descriptor;
    }

    public final BTHeaderRecord getBTHeaderRecord() {
        return headerRecord;
    }

    public ByteBuffer getBytes() {
        buffer = ByteBuffer.allocate(BTHeaderRecord.BT_HEADER_RECORD_LENGTH + NodeDescriptor.BT_NODE_DESCRIPTOR_LENGTH);
        buffer.put(descriptor.getBytes());
        buffer.put(headerRecord.getBytes());
        return buffer;
    }

    private ByteBuffer getNodeData(int offset, int nodeSize) throws IOException {
        HfsPlusForkData catalogFile = factory.getVolumeHeader().getCatalogFile();
        return factory.readForkData(catalogFile, offset, nodeSize);
    }

}
