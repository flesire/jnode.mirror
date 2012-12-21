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
 
package org.jnode.fs.ext2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jnode.driver.Device;
import org.jnode.fs.FSDirectory;
import org.jnode.fs.FSEntry;
import org.jnode.fs.FSFile;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.ReadOnlyFileSystemException;
import org.jnode.fs.ext2.cache.Block;
import org.jnode.fs.ext2.cache.BlockCache;
import org.jnode.fs.ext2.cache.INodeCache;
import org.jnode.fs.spi.AbstractFileSystem;

/**
 * @author Andras Nagy
 * 
 */
public class Ext2FileSystem extends AbstractFileSystem<Ext2Entry> {
    /** Class logger */
    private final Logger log = Logger.getLogger(getClass());
    /** File system metadata information block */
    private Superblock superblock;

    private GroupDescriptor groupDescriptors[];

    private INodeTable iNodeTables[];

    private int groupCount;

    private BlockCache blockCache;

    private INodeCache inodeCache;

    // TODO: SYNC_WRITE should be made a parameter
    /** if true, writeBlock() does not return until the block is written to disk */
    private boolean SYNC_WRITE = true;

    /**
     * Constructor for Ext2FileSystem.
     *
     * @param device
     * @param readOnly
     * @param type
     * 
     * @throws FileSystemException
     */
    public Ext2FileSystem(Device device, boolean readOnly, Ext2FileSystemType type) throws FileSystemException {
        super(device, readOnly, type);
        blockCache = new BlockCache(50, 0.75f);
        inodeCache = new INodeCache(50, 0.75f);
        superblock = new Superblock();
    }

    /**
     * Read file system metadata from the device.
     *
     * @throws FileSystemException
     */
    public void read() throws FileSystemException {
        try {
            superblock.read(this);

            // read the group descriptors
            groupCount = superblock.getGroupCount();
            groupDescriptors = new GroupDescriptor[groupCount];
            iNodeTables = new INodeTable[groupCount];

            GroupDescriptor groupDescriptor = new GroupDescriptor();
            for (int i = 0; i < groupCount; i++) {
                groupDescriptors[i] = groupDescriptor;
                groupDescriptors[i].read(i, this);
                iNodeTables[i] = new INodeTable(this, groupDescriptors[i].getInodeTable());
            }

        } catch (FileSystemException e) {
            throw e;
        } catch (Exception e) {
            throw new FileSystemException(e);
        }

        checkFeatures();


        // if the filesystem has been mounted R/W, set it to "unclean"
        if (!isReadOnly()) {
            log.info(getDevice().getId() + " mounting fs r/w");
            superblock.setState(Ext2Constants.EXT2_ERROR_FS);

            // Mount successfull, update some superblock informations.
            superblock.setMntCount(superblock.getMntCount() + 1);
            superblock.setMTime(Ext2Utils.encodeDate(new Date()));
            superblock.setWTime(Ext2Utils.encodeDate(new Date()));
        }

        log.debug(superblock.toString());
    }

    /**
     * Create metadata information for a new file system.
     * @param blockSize Size of a block.
     *
     * @throws FileSystemException
     */
    public void create(BlockSize blockSize) throws FileSystemException {
        try {
            // create the superblock
            superblock.create(blockSize, this);
            groupCount = superblock.getGroupCount();
            // create the group descriptors
            groupDescriptors = new GroupDescriptor[groupCount];
            iNodeTables = new INodeTable[groupCount];
            GroupDescriptor groupDescriptor = new GroupDescriptor();
            for (int i = 0; i < groupCount; i++) {
                groupDescriptors[i] = groupDescriptor;
                groupDescriptors[i].create(i, this);
            }

            // create each block group:
            for (int i = 0; i < groupCount; i++) {
                createGroup(blockSize.getSize(), i);
            }

            buildRootEntry();

            // write everything to disk
            flush();

        } catch (IOException ioe) {
            throw new FileSystemException("Unable to create filesystem", ioe);
        }

    }

    /**
     * Create block bitmap, inode bitmap and fill the corresponding inode table with zeroes.
     * @param blockSize Size defined for a block in the current file system.
     * @param index Index of the group.
     * @throws IOException
     */
    private void createGroup(int blockSize, int index) throws IOException {
        log.debug("creating group " + index);
        // create the block bitmap
        // create the inode bitmap
        byte[] blockBitmap = new byte[blockSize];
        byte[] inodeBitmap = new byte[blockSize];
        // update the block bitmap: mark the metadata blocks allocated
        long iNodeTableBlock = groupDescriptors[index].getInodeTable();
        long firstNonMetadataBlock = iNodeTableBlock + INodeTable.getSizeInBlocks(this);
        int metadataLength =
                (int) (firstNonMetadataBlock - (superblock.getFirstDataBlock() + index *
                        superblock.getBlocksPerGroup()));
        for (int j = 0; j < metadataLength; j++) {
            BlockBitmap.setBit(blockBitmap, j);
        }
        // set the padding at the end of the last block group
        if (index == groupCount - 1) {
            for (long k = superblock.getBlocksCount(); k < groupCount * superblock.getBlocksPerGroup(); k++)
                BlockBitmap.setBit(blockBitmap, (int) (k % superblock.getBlocksPerGroup()));
        }
        // fill the inode table with zeroes
        // update the inode bitmap: mark the special inodes allocated in
        // the first block group
        if (index == 0)
            for (int j = 0; j < superblock.getFirstInode() - 1; j++)
                INodeBitmap.setBit(inodeBitmap, j);

        // create an empty inode table
        byte[] emptyBlock = new byte[blockSize];
        for (long j = iNodeTableBlock; j < firstNonMetadataBlock; j++)
            writeBlock(j, emptyBlock, false);

        iNodeTables[index] = new INodeTable(this, (int) iNodeTableBlock);

        writeBlock(groupDescriptors[index].getBlockBitmap(), blockBitmap, false);
        writeBlock(groupDescriptors[index].getInodeBitmap(), inodeBitmap, false);
    }

    /**
     * Flush all changed structures to the device.
     * 
     * @throws IOException
     */
    public void flush() throws IOException {
        log.info("Flushing the contents of the filesystem");
        // update the inodes
        synchronized (inodeCache) {
            try {
                log.debug("inodecache size: " + inodeCache.size());
                for (INode iNode : inodeCache.values()) {
                    iNode.flush();
                }
            } catch (FileSystemException ex) {
                final IOException ioe = new IOException();
                ioe.initCause(ex);
                throw ioe;
            }
        }

        // update the group descriptors and the superblock copies
        updateFS();

        // flush the blocks
        synchronized (blockCache) {
            for (Block block : blockCache.values()) {
                block.flush();
            }
        }

        log.info("Filesystem flushed");
    }

    protected void updateFS() throws IOException {
        // updating one group descriptor updates all its copies
        for (int i = 0; i < groupCount; i++)
            groupDescriptors[i].updateGroupDescriptor();
        superblock.update();
    }

    public void close() throws IOException {
        // mark the filesystem clean
        superblock.setState(Ext2Constants.EXT2_VALID_FS);
        super.close();
    }

    /**
     * @see org.jnode.fs.spi.AbstractFileSystem#createRootEntry()
     */
    public Ext2Entry createRootEntry() throws IOException {
        try {
            return new Ext2Entry(getINode(Ext2Constants.EXT2_ROOT_INO), "/", Ext2Constants.EXT2_FT_DIR, this, null);
        } catch (FileSystemException ex) {
            final IOException ioe = new IOException();
            ioe.initCause(ex);
            throw ioe;
        }
    }

    /**
     * Return the block size of the file system
     */
    public int getBlockSize() {
        return superblock.getBlockSize();
    }

    /**
     * Gets the group descriptors for the file system.
     *
     * @return the group descriptors.
     */
    public GroupDescriptor[] getGroupDescriptors() {
        return groupDescriptors;
    }

    /**
     * Read a data block and put it in the cache if it is not yet cached,
     * otherwise get it from the cache.
     * 
     * Synchronized access to the blockCache is important as the bitmap
     * operations are synchronized to the blocks (actually, to Block.getData()),
     * so at any point in time it has to be sure that no two copies of the same
     * block are stored in the cache.
     *
     * @param blockNumber Number identifying the block.
     * @return Byte array contains block data.
     */
    protected byte[] getBlock(long blockNumber) throws IOException {
        if (isClosed())
            throw new IOException("FS closed (fs instance: " + this + ")");
        // log.debug("blockCache size: "+blockCache.size());

        int blockSize = superblock.getBlockSize();
        Block result;

        Integer key = (int) blockNumber;
        synchronized (blockCache) {
            // check if the block has already been retrieved
            if (blockCache.containsKey(key)) {
                result = blockCache.get(key);
                return result.getData();
            }
        }

        // perform the time-consuming disk read outside of the synchronized
        // block
        // advantage:
        // -the lock is held for a shorter time, so other blocks that are
        // already in the cache can be returned immediately and
        // do not have to wait for a long disk read
        // disadvantage:
        // -a single block can be retrieved more than once. However,
        // the block will be put in the cache only once in the second
        // synchronized block
        ByteBuffer data = ByteBuffer.allocate(blockSize);
        log.debug("Reading block " + blockNumber + " (offset: " + blockNumber * blockSize + ") from disk");
        getApi().read(blockNumber * blockSize, data);

        // synchronize again
        synchronized (blockCache) {
            // check if the block has already been retrieved
            if (!blockCache.containsKey(key)) {
                result = new Block(this, blockNumber, data.array());
                blockCache.put(key, result);
                return result.getData();
            } else {
                // it is important to ALWAYS return the block that is in
                // the cache (it is used in synchronization)
                result = blockCache.get(key);
                return result.getData();
            }
        }
    }

    /**
     * Update the block in cache, or write the block to disk
     * 
     * @param nr block number
     * @param data block data
     * @param forceWrite if forceWrite is false, the block is only updated in
     *            the cache (if it was in the cache). If forceWrite is true, or
     *            the block is not in the cache, write it to disk.
     * @throws IOException
     */
    public void writeBlock(long nr, byte[] data, boolean forceWrite) throws IOException {
        if (isClosed())
            throw new IOException("FS closed");

        if (isReadOnly())
            throw new ReadOnlyFileSystemException("Filesystem is mounted read-only!");

        Block block;

        Integer key = (int) nr;
        int blockSize = superblock.getBlockSize();
        // check if the block is in the cache
        synchronized (blockCache) {
            if (blockCache.containsKey(key)) {
                block = blockCache.get(key);
                // update the data in the cache
                block.setData(data);
                if (forceWrite || SYNC_WRITE) {
                    // write the block to disk
                    ByteBuffer dataBuf = ByteBuffer.wrap(data, 0, blockSize);
                    getApi().write(nr * blockSize, dataBuf);
                    // timedWrite(nr, data);
                    block.setDirty(false);

                    log.debug("writing block " + nr + " to disk");
                } else
                    block.setDirty(true);
            } else {
                // If the block was not in the cache, I see no reason to put it
                // in the cache when it is written.
                // It is simply written to disk.
                ByteBuffer dataBuf = ByteBuffer.wrap(data, 0, blockSize);
                getApi().write(nr * blockSize, dataBuf);
                // timedWrite(nr, data);
            }
        }
    }

    public Superblock getSuperblock() {
        return superblock;
    }

    /**
     * Return the inode numbered inodeNr (the first inode is #1)
     * 
     * Synchronized access to the inodeCache is important as the file/directory
     * operations are synchronized to the inodes, so at any point in time it has
     * to be sure that only one instance of any inode is present in the
     * filesystem.
     */
    public INode getINode(int iNodeNr) throws IOException, FileSystemException {
        if ((iNodeNr < 1) || (iNodeNr > superblock.getINodesCount()))
            throw new FileSystemException("INode number (" + iNodeNr + ") out of range (0-" +
                    superblock.getINodesCount() + ")");

        Integer key = iNodeNr;

        log.debug("iNodeCache size: " + inodeCache.size());

        synchronized (inodeCache) {
            // check if the inode is already in the cache
            if (inodeCache.containsKey(key))
                return inodeCache.get(key);
        }

        // move the time consuming disk read out of the synchronized block
        // (see comments at getBlock())

        int group = (int) ((iNodeNr - 1) / superblock.getINodesPerGroup());
        int index = (int) ((iNodeNr - 1) % superblock.getINodesPerGroup());

        // get the part of the inode table that contains the inode
        INodeTable iNodeTable = iNodeTables[group];
        INode result = new INode(this, new INodeDescriptor(iNodeTable, iNodeNr, group, index));
        result.read(iNodeTable.getInodeData(index));

        synchronized (inodeCache) {
            // check if the inode is still not in the cache
            if (!inodeCache.containsKey(key)) {
                inodeCache.put(key, result);
                return result;
            } else
                return inodeCache.get(key);
        }
    }

    /**
     * Checks whether block <code>blockNr</code> is free, and if it is, then
     * allocates it with preallocation.
     * 
     * @param blockNr
     * @return the block reservation
     * @throws IOException
     */
    public BlockReservation testAndSetBlock(long blockNr) throws IOException {

        if (blockNr < superblock.getFirstDataBlock() || blockNr >= superblock.getBlocksCount())
            return new BlockReservation(false, -1, -1);
        int group = translateToGroup(blockNr);
        int index = translateToIndex(blockNr);

        /*
         * Return false if the block is not a data block but a filesystem
         * metadata block, as the beginning of each block group is filesystem
         * metadata: superblock copy (if present) block bitmap inode bitmap
         * inode table Free blocks begin after the inode table.
         */
        long iNodeTableBlock = groupDescriptors[group].getInodeTable();
        long firstNonMetadataBlock = iNodeTableBlock + INodeTable.getSizeInBlocks(this);

        if (blockNr < firstNonMetadataBlock)
            return new BlockReservation(false, -1, -1);

        // synchronize to the blockCache to avoid flushing the block between
        // reading it
        // and synchronizing to it
        synchronized (blockCache) {
            byte[] bitmap = getBlock(groupDescriptors[group].getBlockBitmap());
            synchronized (bitmap) {
                BlockReservation result = BlockBitmap.testAndSetBlock(bitmap, index);
                // update the block bitmap
                if (result.isSuccessful()) {
                    writeBlock(groupDescriptors[group].getBlockBitmap(), bitmap, false);
                    modifyFreeBlocksCount(group, -1 - result.getPreallocCount());
                    // result.setBlock(
                    // result.getBlock()+superblock.getFirstDataBlock() );
                    result.setBlock(blockNr);
                }
                return result;
            }
        }

    }

    /**
     * Create a new INode
     * 
     * @param preferredBlockBroup first try to allocate the inode in this block
     *            group
     * @return the INode
     */
    protected INode createINode(int preferredBlockBroup, int fileFormat, int accessRights, int uid, int gid)
        throws FileSystemException, IOException {
        if (preferredBlockBroup >= superblock.getBlocksCount())
            throw new FileSystemException("Block group " + preferredBlockBroup + " does not exist");

        int groupNr = preferredBlockBroup;
        // first check the preferred block group, if it has any free inodes
        INodeReservation res = findFreeINode(groupNr);

        // if no free inode has been found in the preferred block group, then
        // try the others
        if (!res.isSuccessful()) {
            for (groupNr = 0; groupNr < superblock.getBlockGroupNr(); groupNr++) {
                res = findFreeINode(groupNr);
                if (res.isSuccessful()) {
                    break;
                }
            }
        }

        if (!res.isSuccessful())
            throw new FileSystemException("No free inodes found!");

        // a free inode has been found: create the inode and write it into the
        // inode table
        INodeTable iNodeTable = iNodeTables[preferredBlockBroup];
        // byte[] iNodeData = new byte[INode.INODE_LENGTH];
        int iNodeNr = res.getINodeNr((int) superblock.getINodesPerGroup());
        INode iNode = new INode(this, new INodeDescriptor(iNodeTable, iNodeNr, groupNr, res.getIndex()));
        iNode.create(fileFormat, accessRights, uid, gid);
        // trigger a write to disk
        iNode.update();

        log.debug("** New inode allocated with number " + iNode.getINodeNr());

        // put the inode into the cache
        synchronized (inodeCache) {
            Integer key = iNodeNr;
            if (inodeCache.containsKey(key))
                throw new FileSystemException("Newly allocated inode is already in the inode cache");
            else
                inodeCache.put(key, iNode);
        }

        return iNode;
    }

    /**
     * Find a free INode in the inode bitmap and allocate it
     * 
     * @param blockGroup
     * @return the INode reservation
     * @throws IOException
     */
    protected INodeReservation findFreeINode(int blockGroup) throws IOException {
        GroupDescriptor gdesc = groupDescriptors[blockGroup];
        if (gdesc.getFreeInodesCount() > 0) {
            // synchronize to the blockCache to avoid flushing the block between
            // reading it
            // and synchronizing to it
            synchronized (blockCache) {
                byte[] bitmap = getBlock(gdesc.getInodeBitmap());

                synchronized (bitmap) {
                    INodeReservation result = INodeBitmap.findFreeINode(bitmap);

                    if (result.isSuccessful()) {
                        // update the inode bitmap
                        writeBlock(gdesc.getInodeBitmap(), bitmap, true);
                        modifyFreeInodesCount(blockGroup, -1);

                        result.setGroup(blockGroup);

                        return result;
                    }
                }
            }
        }
        return new INodeReservation(false, -1);
    }

    protected int translateToGroup(long i) {
        return (int) ((i - superblock.getFirstDataBlock()) / superblock.getBlocksPerGroup());
    }

    protected int translateToIndex(long i) {
        return (int) ((i - superblock.getFirstDataBlock()) % superblock.getBlocksPerGroup());
    }

    /**
     * Modify the number of free blocks in the block group
     * 
     * @param group
     * @param diff can be positive or negative
     */
    protected void modifyFreeBlocksCount(int group, int diff) {
        GroupDescriptor gdesc = groupDescriptors[group];
        gdesc.setFreeBlocksCount(gdesc.getFreeBlocksCount() + diff);

        superblock.setFreeBlocksCount(superblock.getFreeBlocksCount() + diff);
    }

    /**
     * Modify the number of free inodes in the block group
     * 
     * @param group
     * @param diff can be positive or negative
     */
    protected void modifyFreeInodesCount(int group, int diff) {
        GroupDescriptor gdesc = groupDescriptors[group];
        gdesc.setFreeInodesCount(gdesc.getFreeInodesCount() + diff);

        superblock.setFreeInodesCount(superblock.getFreeInodesCount() + diff);
    }

    /**
     * Modify the number of used directories in a block group
     * 
     * @param group
     * @param diff
     */
    protected void modifyUsedDirsCount(int group, int diff) {
        GroupDescriptor gdesc = groupDescriptors[group];
        gdesc.setUsedDirsCount(gdesc.getUsedDirsCount() + diff);
    }

    /**
     * Free up a block in the block bitmap.
     * 
     * @param blockNr
     * @throws FileSystemException
     * @throws IOException
     */
    public void freeBlock(long blockNr) throws FileSystemException, IOException {
        if (blockNr < 0 || blockNr >= superblock.getBlocksCount())
            throw new FileSystemException("Attempt to free nonexisting block (" + blockNr + ")");

        int group = translateToGroup(blockNr);
        int index = translateToIndex(blockNr);
        GroupDescriptor gdesc = groupDescriptors[group];

        /*
         * Throw an exception if an attempt is made to free up a filesystem
         * metadata block (the beginning of each block group is filesystem
         * metadata): superblock copy (if present) block bitmap inode bitmap
         * inode table Free blocks begin after the inode table.
         */
        long iNodeTableBlock = groupDescriptors[group].getInodeTable();
        long firstNonMetadataBlock = iNodeTableBlock + INodeTable.getSizeInBlocks(this);

        if (blockNr < firstNonMetadataBlock)
            throw new FileSystemException("Attempt to free a filesystem metadata block!");

        // synchronize to the blockCache to avoid flushing the block between
        // reading it
        // and synchronizing to it
        synchronized (blockCache) {
            byte[] bitmap = getBlock(gdesc.getBlockBitmap());

            // at any time, only one copy of the Block exists in the cache, so
            // it is
            // safe to synchronize to the bitmapBlock object (it's part of
            // Block)
            synchronized (bitmap) {
                BlockBitmap.freeBit(bitmap, index);
                // update the bitmap block
                writeBlock(groupDescriptors[group].getBlockBitmap(), bitmap, false);
                modifyFreeBlocksCount(group, 1);
            }
        }
    }

    /**
     * Find free blocks in the block group <code>group</code>'s block bitmap.
     * First check for a whole byte of free blocks (0x00) in the bitmap, then
     * check for any free bit. If blocks are found, mark them as allocated.
     * 
     * @return the index of the block (from the beginning of the partition)
     * @param group the block group to check
     * @param threshold find the free blocks only if there are at least
     *            <code>threshold</code> number of free blocks
     */
    public BlockReservation findFreeBlocks(int group, long threshold) throws IOException {
        GroupDescriptor gdesc = groupDescriptors[group];
        // see if it's worth to check the block group at all
        if (gdesc.getFreeBlocksCount() < threshold)
            return new BlockReservation(false, -1, -1, gdesc.getFreeBlocksCount());

        /*
         * Return false if the block is not a data block but a filesystem
         * metadata block, as the beginning of each block group is filesystem
         * metadata: superblock copy (if present) block bitmap inode bitmap
         * inode table Free blocks begin after the inode table.
         */
        long iNodeTableBlock = groupDescriptors[group].getInodeTable();
        long firstNonMetadataBlock = iNodeTableBlock + INodeTable.getSizeInBlocks(this);
        int metadataLength =
                (int) (firstNonMetadataBlock - (superblock.getFirstDataBlock() + group *
                        superblock.getBlocksPerGroup()));
        log.debug("group[" + group + "].getInodeTable()=" + iNodeTableBlock +
                ", iNodeTable.getSizeInBlocks()=" + INodeTable.getSizeInBlocks(this));
        log.debug("metadata length for block group(" + group + "): " + metadataLength);

        BlockReservation result;

        // synchronize to the blockCache to avoid flushing the block between
        // reading it
        // and synchronizing to it
        synchronized (blockCache) {
            byte[] bitmapBlock = getBlock(gdesc.getBlockBitmap());

            // at any time, only one copy of the Block exists in the cache, so
            // it is
            // safe to synchronize to the bitmapBlock object (it's part of
            // Block)
            synchronized (bitmapBlock) {
                result = BlockBitmap.findFreeBlocks(bitmapBlock, metadataLength);

                // if the reservation was successful, write the bitmap data to
                // disk
                // within the same synchronized block
                if (result.isSuccessful()) {
                    writeBlock(groupDescriptors[group].getBlockBitmap(), bitmapBlock, true);
                    modifyFreeBlocksCount(group, -1 - result.getPreallocCount());
                }
            }
        }

        if (result.isSuccessful()) {
            result.setBlock(group * superblock.getBlocksPerGroup() +
                    superblock.getFirstDataBlock() + result.getBlock());
            result.setFreeBlocksCount(gdesc.getFreeBlocksCount());
        }

        return result;
    }

    /**
     * Returns the number of groups.
     * 
     * @return int
     */
    protected int getGroupCount() {
        return groupCount;
    }

    /**
     * utility function for determining if a given block group has superblock
     * and group descriptor copies
     * 
     * @param a positive integer
     * @param b positive integer > 1
     * @return true if an n integer exists such that a=b^n; false otherwise
     */
    private boolean checkPow(int a, int b) {
        if (a <= 1)
            return true;
        while (true) {
            if (a == b)
                return true;
            if (a % b == 0) {
                a = a / b;
                continue;
            }
            return false;
        }
    }

    /**
     * With the sparse_super option set, a filesystem does not have a superblock
     * and group descriptor copy in every block group.
     * 
     * @param groupNr
     * @return true if the block group <code>groupNr</code> has a superblock
     *         and a group descriptor copy, otherwise false
     */
    protected boolean groupHasDescriptors(int groupNr) {
        if (hasROFeature(Ext2Constants.EXT2_FEATURE_RO_COMPAT_SPARSE_SUPER))
            return (checkPow(groupNr, 3) || checkPow(groupNr, 5) || checkPow(groupNr, 7));
        else
            return true;
    }

    /**
     * 
     */
    protected FSFile createFile(FSEntry entry) throws IOException {
        Ext2Entry e = (Ext2Entry) entry;
        return new Ext2File(e.getINode());
    }

    /**
     * 
     */
    protected FSDirectory createDirectory(FSEntry entry) throws IOException {
        Ext2Entry e = (Ext2Entry) entry;
        return new Ext2Directory(e);
    }

    protected FSEntry buildRootEntry() throws IOException {
        // a free inode has been found: create the inode and write it into the
        // inode table
        INodeTable iNodeTable = iNodeTables[0];
        // byte[] iNodeData = new byte[INode.INODE_LENGTH];
        int iNodeNr = Ext2Constants.EXT2_ROOT_INO;
        INode iNode = new INode(this, new INodeDescriptor(iNodeTable, iNodeNr, 0, iNodeNr - 1));
        int rights = 0xFFFF & (Ext2Constants.EXT2_S_IRWXU | Ext2Constants.EXT2_S_IRWXG | Ext2Constants.EXT2_S_IRWXO);
        iNode.create(Ext2Constants.EXT2_S_IFDIR, rights, 0, 0);
        // trigger a write to disk
        iNode.update();

        // add the inode to the inode cache
        synchronized (inodeCache) {
            inodeCache.put(Ext2Constants.EXT2_ROOT_INO, iNode);
        }

        modifyUsedDirsCount(0, 1);

        Ext2Entry rootEntry = new Ext2Entry(iNode, "/", Ext2Constants.EXT2_FT_DIR, this, null);
        ((Ext2Directory) rootEntry.getDirectory())
                .addINode(Ext2Constants.EXT2_ROOT_INO, ".", Ext2Constants.EXT2_FT_DIR);
        ((Ext2Directory) rootEntry.getDirectory()).addINode(Ext2Constants.EXT2_ROOT_INO, "..",
                Ext2Constants.EXT2_FT_DIR);
        rootEntry.getDirectory().addDirectory("lost+found");
        return rootEntry;
    }

    protected void handleFSError(Exception e) {
        // mark the fs as having errors
        superblock.setState(Ext2Constants.EXT2_ERROR_FS);
        if (superblock.getErrors() == Ext2Constants.EXT2_ERRORS_RO)
            setReadOnly(true); // remount readonly

        if (superblock.getErrors() == Ext2Constants.EXT2_ERRORS_PANIC)
            throw new RuntimeException("EXT2 FileSystem exception", e);
    }

    /**
     * @return Returns the blockCache (outside of this class only used to
     *         synchronize to)
     */
    protected synchronized BlockCache getBlockCache() {
        return blockCache;
    }

    /**
     * @return Returns the inodeCache (outside of this class only used to
     *         syncronized to)
     */
    protected synchronized INodeCache getInodeCache() {
        return inodeCache;
    }

    public long getFreeSpace() {
        return superblock.getFreeBlocksCount() * superblock.getBlockSize();
    }

    public long getTotalSpace() {
        return superblock.getBlocksCount() * superblock.getBlockSize();
    }

    public long getUsableSpace() {
        // TODO implement me
        return -1;
    }

    @Override
    public String getVolumeName() throws IOException {
        return superblock.getVolumeName();
    }

    // Private methods

    /**
     * check for unsupported filesystem options. The are two unsupported features :
     * <ul>
     * <li>An unsupported INCOMPAT feature means that the fs may not be mounted at all </li>
     * <li>An unsupported RO_COMPAT feature means that the filesystem can only be mounted readonly</li>
     * </ul>
     * @throws FileSystemException occurs if an unsupported feature is found.
     */
    private void checkFeatures() throws FileSystemException {

        if (hasIncompatFeature(Ext2Constants.EXT2_FEATURE_INCOMPAT_COMPRESSION))
            throw new FileSystemException(getDevice().getId() +
                " Unsupported filesystem feature (COMPRESSION) disallows mounting");
        if (hasIncompatFeature(Ext2Constants.EXT2_FEATURE_INCOMPAT_META_BG))
            throw new FileSystemException(getDevice().getId() +
                " Unsupported filesystem feature (META_BG) disallows mounting");
        if (hasIncompatFeature(Ext2Constants.EXT3_FEATURE_INCOMPAT_JOURNAL_DEV))
            throw new FileSystemException(getDevice().getId() +
                " Unsupported filesystem feature (JOURNAL_DEV) disallows mounting");
        if (hasIncompatFeature(Ext2Constants.EXT3_FEATURE_INCOMPAT_RECOVER))
            throw new FileSystemException(getDevice().getId() +
                " Unsupported filesystem feature (RECOVER) disallows mounting");

        // an unsupported RO_COMPAT feature means that the filesystem can only
        // be mounted readonly
        if (hasROFeature(Ext2Constants.EXT2_FEATURE_RO_COMPAT_LARGE_FILE)) {
            log.info(getDevice().getId() + " Unsupported filesystem feature (LARGE_FILE) forces readonly mode");
            setReadOnly(true);
        }
        if (hasROFeature(Ext2Constants.EXT2_FEATURE_RO_COMPAT_BTREE_DIR)) {
            log.info(getDevice().getId() + " Unsupported filesystem feature (BTREE_DIR) forces readonly mode");
            setReadOnly(true);
        }

        // if the filesystem has not been cleanly unmounted, mount it readonly
        if (superblock.getState() == Ext2Constants.EXT2_ERROR_FS) {
            log.info(getDevice().getId() + " Filesystem has not been cleanly unmounted, mounting it readonly");
            setReadOnly(true);
        }
    }

    /**
     * Check whether the filesystem uses the given RO feature
     * (S_FEATURE_RO_COMPAT)
     *
     * @param mask
     * @return {@code true} if the filesystem uses the feature, otherwise {@code false}.
     */
    private boolean hasROFeature(long mask) {
        return (mask & superblock.getFeatureROCompat()) != 0;
    }

    /**
     * Check whether the filesystem uses the given COMPAT feature
     * (S_FEATURE_INCOMPAT)
     *
     * @param mask
     * @return {@code true} if the filesystem uses the feature, otherwise {@code false}.
     */
    protected boolean hasIncompatFeature(long mask) {
        return (mask & superblock.getFeatureIncompat()) != 0;
    }
}
