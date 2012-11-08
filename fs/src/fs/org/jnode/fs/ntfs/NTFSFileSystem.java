/*
 * $Id$
 *
 * Copyright (C) 2003-2012 JNode.org
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
 
package org.jnode.fs.ntfs;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.jnode.driver.Device;
import org.jnode.fs.FSDirectory;
import org.jnode.fs.FSEntry;
import org.jnode.fs.FSFile;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.spi.AbstractFileSystem;

/**
 * NTFS filesystem implementation.
 * 
 * @author Chira
 * @author Ewout Prangsma (epr@users.sourceforge.net)
 */
public class NTFSFileSystem extends AbstractFileSystem<FSEntry> {

    private final NTFSVolume volume;
    private FSEntry root;

    /**
     * @see org.jnode.fs.FileSystem#getDevice()
     */
    public NTFSFileSystem(Device device, boolean readOnly, NTFSFileSystemType type)
        throws FileSystemException {
        super(device, readOnly, type);

        try {
            // initialize the NTFE volume
            volume = new NTFSVolume(getApi());
        } catch (IOException e) {
            throw new FileSystemException(e);
        }
    }

    /**
     * @see org.jnode.fs.FileSystem#getRootEntry()
     */
    public FSEntry getRootEntry() throws IOException {
        if (root == null) {
            root = new NTFSDirectory(this, volume.getRootDirectory()).getEntry(".");
        }
        return root;
    }

    /**
     * @return Returns the volume.
     */
    public NTFSVolume getNTFSVolume() {
        return this.volume;
    }

    /**
     * Gets the NTFS volume name.
     *
     * @return Returns the volume name.
     * @throws IOException if an error occurs reading the volume name.
     */
    public String getNTFSVolumeName() throws IOException {
        NTFSEntry entry = (NTFSEntry) getRootEntry().getDirectory().getEntry("$Volume");
        if (entry == null) {
            return null;
        }

        NTFSAttribute attribute = entry.getFileRecord().findAttributeByType(NTFSAttribute.Types.VOLUME_NAME);

        if (attribute instanceof NTFSResidentAttribute) {
            NTFSResidentAttribute residentAttribute = (NTFSResidentAttribute) attribute;
            byte[] nameBuffer = new byte[residentAttribute.getAttributeLength()];

            residentAttribute.getData(residentAttribute.getAttributeOffset(), nameBuffer, 0, nameBuffer.length);

            try {
                //XXX: For Java 6, should use the version that accepts a Charset.
                return new String(nameBuffer, "UTF-16LE");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("UTF-16LE charset missing from JRE", e);
            }
        }

        return null;
    }

    /**
     * Flush all data.
     */
    public void flush() throws IOException {
        // TODO Auto-generated method stub
    }

    /**
     * 
     */
    protected FSFile createFile(FSEntry entry) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 
     */
    protected FSDirectory createDirectory(FSEntry entry) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 
     */
    protected NTFSEntry createRootEntry() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public long getFreeSpace() {
        // TODO implement me
        return -1;
    }

    public long getTotalSpace() {
        // TODO implement me
        return -1;
    }

    public long getUsableSpace() {
        // TODO implement me
        return -1;
    }
}
