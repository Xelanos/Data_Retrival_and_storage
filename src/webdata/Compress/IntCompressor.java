package webdata.Compress;

import java.io.File;

public interface IntCompressor {


    abstract void encode(int[] array, String file);

    abstract int[] decode(String file);
}
