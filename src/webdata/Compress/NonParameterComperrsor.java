package webdata.Compress;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public abstract class NonParameterComperrsor  implements IntCompressor{

    @Override
    public int decodeAtIndex(String file, long index) {
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");

            long fromByte = getFromByte(index);
            raf.seek(fromByte);

            int numberOfBytes = numberOfBytesNeededForOneNumber(index);
            byte[] bytes = new byte[numberOfBytes];
            raf.read(bytes);

            return decodeOne(bytes, index);

        } catch (FileNotFoundException e) {
            System.err.println("Couldn't find source file.\nreturning default : 0");
            e.printStackTrace();
            return 0;
        } catch (IOException e) {
            System.err.println("Couldn't read source file.\nreturning default : 0");
            e.printStackTrace();
            return 0;
        }
    }

    protected abstract int numberOfBytesNeededForOneNumber(long index);

    protected abstract long getFromByte(long index);

    protected abstract int decodeOne(byte[] bytes, long index);

}
