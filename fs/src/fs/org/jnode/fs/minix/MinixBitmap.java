package org.jnode.fs.minix;

import org.jnode.fs.FileSystemException;
import org.jnode.fs.ext2.FSBitmap;

public class MinixBitmap extends FSBitmap {

    public static void unmark(byte[] bitmap, int index) throws FileSystemException {
        freeBit(bitmap, index);
    }

    public static long findFreeBlock(byte[] bitmap, int size) {
        long block = -1;
        for (int i = 0; i < size * SuperBlock.BLOCK_SIZE; i++) {
            if (bitmap[i] != 0xff) {
                for (int j = 0; j < 8; j++) {
                    if ((bitmap[i] & (1 << j)) == 0) {
                        block = (i << 3) + j;
                    }
                }
            }
        }
        return block;

    }

}
