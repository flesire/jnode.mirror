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

import java.nio.ByteBuffer;
import org.jnode.fs.hfsplus.tree.AbstractNode;
import org.jnode.fs.hfsplus.tree.IndexRecord;
import org.jnode.fs.hfsplus.tree.NodeDescriptor;

public class ExtentNode extends AbstractNode<IndexRecord> {

    public ExtentNode(NodeDescriptor descriptor, final int nodeSize) {
        super(descriptor, nodeSize);
    }

    public ExtentNode(final ByteBuffer nodeData, final int nodeSize) {
        super(nodeData, nodeSize);
    }

    @Override
    public IndexRecord getNodeRecord(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void loadRecords(byte[] nodeData) {
        // TODO Auto-generated method stub

    }

}
