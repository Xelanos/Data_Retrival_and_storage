package webdata.Compress;

import java.io.File;

public interface Compressor {


    abstract void encode(int[] array, String file);

    abstract int[] decode(String file);
}
