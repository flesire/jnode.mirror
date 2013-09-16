package org.jnode.fs.minix;

import org.apache.log4j.Logger;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.ext2.FSBitmap;

public class MinixBitmap extends FSBitmap {

    private static final Logger log = Logger.getLogger(MinixBitmap.class);

    public static void unmark(byte[] bitmap, int index) throws FileSystemException {
        freeBit(bitmap, index);
    }

    public static void mark(byte[] bitmap, int index) throws FileSystemException {
        setBit(bitmap, index);
    }

    public static long findFreeBlock(byte[] bitmap, int size) {
        long block = -1;
        for (int i = 0; i < size; i++) {
            if (bitmap[i] != 0xff) {
                for (int j = 0; j < 8; j++) {
                    if ((bitmap[i] & (1 << j)) == 0) {
                        block = (i << 3) + j;
                        log.debug("Find free block, index : " + block);
                        return block;

                    }
                }
            }
        }
        return block;

    }

}
