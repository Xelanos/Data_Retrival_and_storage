package webdata.Compress;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class OneByteCompressor extends NonParameterComperrsor{

    @Override
    public void encode(int[] array, String file) {
        byte[] bytes = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            bytes[i] = BigInteger.valueOf(array[i]).toByteArray()[0];
        }

        try {
            Files.write(Paths.get(file), bytes, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public int[] decodeAll(String file) {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Paths.get(file));
        } catch (IOException e) {
            e.printStackTrace();
            return new int[0];
        }

        int[] result = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = bytes[i];
        }
        return result;
    }

    @Override
    protected int numberOfBytesNeededForOneNumber(long index) {
        return 1;
    }

    @Override
    protected long getFromByte(long index) {
        return index;
    }

    @Override
    protected int decodeOne(byte[] bytes, long index) {
        return bytes[0];
    }
}
