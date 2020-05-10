package webdata.Compress;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

public class OneByteCompressor implements IntCompressor{

    @Override
    public void encode(int[] array, String file) {
        byte[] bytes = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            bytes[i] = BigInteger.valueOf(array[i]).toByteArray()[0];
        }

        try {
            Files.write(Paths.get(file), bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public int[] decode(String file) {
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
}
