package webdata.Compress;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public class TwoByteCompressor  extends NonParameterComperrsor{

    @Override
    protected int numberOfBytesNeededForOneNumber(long index) {
        return 2;
    }

    @Override
    protected long getFromByte(long index) {
        return index * 2;
    }

    @Override
    protected int decodeOne(byte[] bytes, long index) {
        String numberBitString = toBitString(bytes);
        return  Integer.parseInt(numberBitString, 2);
    }

    @Override
    public void encode(int[] array, String file) {
        for (int value : array) {
            byte[] bytesToWrite;
            byte[] numberBytes = BigInteger.valueOf(value).toByteArray();
            if (numberBytes.length < 2) {
                bytesToWrite = new byte[]{0, numberBytes[0]};
            } else {
                bytesToWrite = numberBytes;
            }
            try {
                Files.write(Paths.get(file), bytesToWrite, StandardOpenOption.APPEND);
            } catch (IOException e) {
                System.err.println("Couldn't write the numbers " + Arrays.toString(array) + " to file.");
                e.printStackTrace();
            }
        }
    }

    @Override
    public int[] decodeAll(String file) {
        byte[] allBytes;
        try {
            allBytes = Files.readAllBytes(Paths.get(file));
        } catch (IOException e) {
            e.printStackTrace();
            return new int[0];
        }

        int[] result = new int[allBytes.length / 2];
        byte[] numberBytes = new byte[2];
        for (int i = 0; i < allBytes.length; i++) {
            numberBytes[i % 2] = allBytes[i];
            if (i % 2 == 1){
                result[i / 2] = decodeOne(numberBytes, 0);
            }
        }
        return result;
    }


    private String toBitString(final byte[] b) {
        final char[] bits = new char[8 * b.length];
        for (int i = 0; i < b.length; i++) {
            final byte byteval = b[i];
            int bytei = i << 3;
            int mask = 0x1;
            for (int j = 7; j >= 0; j--) {
                final int bitval = byteval & mask;
                if (bitval == 0) {
                    bits[bytei + j] = '0';
                } else {
                    bits[bytei + j] = '1';
                }
                mask <<= 1;
            }
        }
        return String.valueOf(bits);
    }
}
