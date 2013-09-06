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
import org.jnode.fs.FSObject;

public class HfsPlusObject implements FSObject {

    protected HfsPlusFileSystem fs;

    protected byte[] data;

    private boolean dirty;

    public HfsPlusObject(final HfsPlusFileSystem fileSystem) {
        this.fs = fileSystem;
        this.dirty = false;
    }

    public void read(int offset, int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        fs.getApi().read(offset, buffer);
        data = buffer.array();
    }

    public void write(int offset) throws IOException {
        fs.getApi().write(offset, ByteBuffer.wrap(data));
        fs.flush();
    }

    public final HfsPlusFileSystem getFileSystem() {
        return fs;
    }

    public final boolean isValid() {
        return false;
    }

    /**
     * Is this object dirty (ie some data need to be saved to device)
     * 
     * @return if this object is dirty
     * @throws IOException
     */
    public boolean isDirty() throws IOException {
        return dirty;
    }

    /**
     * Mark this object as dirty.
     */
    protected final void setDirty() {
        this.dirty = true;
    }

    /**
     * Mark this object as not dirty.
     */
    protected final void resetDirty() {
        this.dirty = false;
    }

}
