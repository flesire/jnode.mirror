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

package org.jnode.fs.hfsplus.extent;

import org.apache.log4j.Logger;
import org.jnode.fs.hfsplus.HFSPlusParams;
import org.jnode.fs.hfsplus.tree.BTHeaderRecord;
import org.jnode.fs.hfsplus.tree.NodeDescriptor;
import org.jnode.fs.hfsplus.tree.NodeType;

public class Extent {
    private final Logger log = Logger.getLogger(getClass());

    @SuppressWarnings("unused")
    private NodeDescriptor descriptor;
    @SuppressWarnings("unused")
    private BTHeaderRecord headerRecord;

    public Extent(NodeDescriptor descriptor, BTHeaderRecord headerRecord) {
        this.descriptor = descriptor;
        this.headerRecord = headerRecord;
    }

    public Extent(HFSPlusParams params) {
        log.info("Create B-Tree extent file.");
        descriptor = new NodeDescriptor(0, 0, NodeType.BT_HEADER_NODE, 0, 3);
        //
        int totalNodes = params.getExtentClumpSize() / params.getExtentNodeSize();
        int freeNodes = totalNodes - 1;
        headerRecord =
                new BTHeaderRecord(0, 0, 0, 0, 0, params.getExtentNodeSize(),
                        ExtentKey.MAXIMUM_KEY_LENGTH, totalNodes, freeNodes,
                        params.getExtentClumpSize(), BTHeaderRecord.BT_TYPE_HFS,
                        BTHeaderRecord.KEY_COMPARE_TYPE_CASE_FOLDING,
                        BTHeaderRecord.BT_BIG_KEYS_MASK);

    }

    public NodeDescriptor getDescriptor() {
        return descriptor;
    }

    public BTHeaderRecord getHeaderRecord() {
        return headerRecord;
    }
}
