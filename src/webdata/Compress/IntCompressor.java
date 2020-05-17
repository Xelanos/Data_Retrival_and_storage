package webdata.Compress;

import java.io.File;

public interface IntCompressor {


    abstract void encode(int[] array, String file);

    abstract int[] decodeAll(String file);

    abstract int decodeAtIndex(String file, long index);
}
