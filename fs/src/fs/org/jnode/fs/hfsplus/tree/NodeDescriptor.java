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

package org.jnode.fs.hfsplus.tree;

import java.nio.ByteBuffer;
import org.jnode.util.BigEndian;

public class NodeDescriptor {

    /** The size of the node descriptor. */
    public static final int BT_NODE_DESCRIPTOR_LENGTH = 14;

    /** The number of the next node. */
    private int next;

    /** The number of the previous node. */
    private int previous;

    /** The type of the node. */
    private NodeType kind;

    /** The depth of this node in the B-Tree. */
    private int height;

    /** The number of records in this node. */
    private int numRecords;

    /**
     * Creates a new node descriptor.
     * 
     * @param next
     * @param previous
     * @param kind
     * @param height
     * @param numRecords
     */
    public NodeDescriptor(int next, int previous, NodeType kind, int height, int numRecords) {
        this.next = next;
        this.previous = previous;
        this.kind = kind;
        this.height = height;
        this.numRecords = numRecords;
    }

    /**
     * Creates node descriptor from existing data.
     * 
     * @param src byte array contains node descriptor data.
     * @param offset start of node descriptor data.
     */
    public NodeDescriptor(final ByteBuffer src, int offset) {
        byte[] data = new byte[BT_NODE_DESCRIPTOR_LENGTH];
        System.arraycopy(src.array(), offset, data, 0, BT_NODE_DESCRIPTOR_LENGTH);
        next = BigEndian.getInt32(data, 0);
        previous = BigEndian.getInt32(data, 4);
        kind = NodeType.valueOf(BigEndian.getInt8(data, 8));
        height = BigEndian.getInt8(data, 9);
        numRecords = BigEndian.getInt16(data, 10);
    }

    /**
     * 
     * @return the descriptor rendered as bytes
     */
    public byte[] getBytes() {
        byte[] data = new byte[BT_NODE_DESCRIPTOR_LENGTH];
        BigEndian.setInt32(data, 0, next);
        BigEndian.setInt32(data, 4, previous);
        BigEndian.setInt8(data, 8, kind.getIntValue());
        BigEndian.setInt8(data, 9, height);
        BigEndian.setInt16(data, 10, numRecords);
        return data;
    }

    public final String toString() {
        return ("[Previous : " + getFLink() + " " + "Next : " + getBLink() + " " + "Node type: " +
                getKind().getStringValue() + " " + "Height: " + getHeight() + " " + "#records:   " +
                getNumRecords() + "]");
    }

    public int getFLink() {
        return next;
    }

    public void setNext(int next) {
        this.next = next;
    }

    public int getBLink() {
        return previous;
    }

    public NodeType getKind() {
        return kind;
    }

    public boolean isValid(int treeHeigth) {
        /*
         * if (descriptor.getKind().getIntValue() < -1 ||
         * descriptor.getKind().getIntValue() > 2) { return false; }
         */

        if (getHeight() > treeHeigth) {
            return false;
        }
        return true;
    }

    public int getHeight() {
        return height;
    }

    public int getNumRecords() {
        return numRecords;
    }

    public boolean isIndexNode() {
        return kind.equals(NodeType.BT_INDEX_NODE);
    }

    public boolean isLeafNode() {
        return kind.equals(NodeType.BT_LEAF_NODE);
    }

    public boolean isMapNode() {
        return kind.equals(NodeType.BT_MAP_NODE);
    }

}
