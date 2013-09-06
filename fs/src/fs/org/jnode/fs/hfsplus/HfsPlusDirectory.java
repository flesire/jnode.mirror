package org.jnode.fs.hfsplus;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.jnode.fs.FSEntry;
import org.jnode.fs.hfsplus.catalog.Catalog;
import org.jnode.fs.hfsplus.catalog.CatalogFile;
import org.jnode.fs.hfsplus.catalog.CatalogFolder;
import org.jnode.fs.hfsplus.catalog.CatalogKey;
import org.jnode.fs.hfsplus.catalog.CatalogLeafNode;
import org.jnode.fs.hfsplus.catalog.CatalogNodeId;
import org.jnode.fs.hfsplus.tree.LeafRecord;
import org.jnode.fs.spi.AbstractFSDirectory;
import org.jnode.fs.spi.AbstractFileSystem;
import org.jnode.fs.spi.FSEntryTable;

public class HfsPlusDirectory extends AbstractFSDirectory {

    /** The catalog directory record */
    private CatalogFolder folder;

    public HfsPlusDirectory(AbstractFileSystem<?> fs, CatalogFolder folder) {
        super(fs);
        this.folder = folder;
    }

    @Override
    protected FSEntryTable readEntries() throws IOException {
        List<FSEntry> pathList = new LinkedList<FSEntry>();
        HfsPlusFileSystem fs = (HfsPlusFileSystem) getFileSystem();
        if (fs.getVolumeHeader().getFolderCount() > 0) {
            LeafRecord[] records = fs.getCatalog().getRecords(folder.getFolderId());
            for (LeafRecord rec : records) {
                if (rec.getType() == CatalogFolder.RECORD_TYPE_FOLDER ||
                        rec.getType() == CatalogFile.RECORD_TYPE_FILE) {
                    String name = ((CatalogKey) rec.getKey()).getNodeName().getUnicodeString();
                    HfsPlusEntry e = new HfsPlusEntry(fs, this, name, rec, folder.getFolderId());
                    pathList.add(e);
                }
            }
        }
        return new FSEntryTable(((HfsPlusFileSystem) getFileSystem()), pathList);
    }

    @Override
    protected void writeEntries(FSEntryTable entries) throws IOException {
        Iterator<FSEntry> it = entries.iterator();
        while (it.hasNext()) {
            HfsPlusEntry entry = (HfsPlusEntry) it.next();
            if (entry.isDirty()) {
                LeafRecord record = entry.getRecord();
                HfsPlusFileSystem fileSystem = (HfsPlusFileSystem) getFileSystem();
                CatalogLeafNode node = fileSystem.getCatalog().findLeaf(entry.getParentId());
                // updateNode(entry, record, fileSystem, node);
            }
        }
    }

    @Override
    protected FSEntry createFileEntry(String name) throws IOException {
        return null; // To change body of implemented methods use File |
                     // Settings | File Templates.
    }

    @Override
    protected FSEntry createDirectoryEntry(String name) throws IOException {
        Catalog catalog = ((HfsPlusFileSystem) getFileSystem()).getCatalog();
        VolumeHeader volumeHeader = ((HfsPlusFileSystem) getFileSystem()).getVolumeHeader();
        CatalogLeafNode node =
                catalog.createNode(name, this.folder.getFolderId(),
                        new CatalogNodeId(volumeHeader.getNextCatalogId()),
                        CatalogFolder.RECORD_TYPE_FOLDER);
        folder.incrementValence();
        // New entry
        HfsPlusEntry newEntry =
                new HfsPlusEntry((HfsPlusFileSystem) getFileSystem(), this, name,
                        node.getNodeRecord(0), this.folder.getFolderId());
        newEntry.setDirty();
        // Update superblock
        volumeHeader.incrementFolderCount();
        volumeHeader.setDirty();
        ((HfsPlusFileSystem) getFileSystem()).writeVolumeHeader();
        return newEntry;
    }

    // Specific methods

    public int rename(String oldName, String newName) {
        return getEntryTable().rename(oldName, newName);
    }
}
