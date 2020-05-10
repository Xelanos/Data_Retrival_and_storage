package webdata.Compress;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;

public class GroupVarintCompressor implements Compressor {
    private static final int BUFFER_SIZE = 4096;

    private static final int NUMBER_OF_GROUPS = 4;

    @Override
    public void encode(int[] array, String file) {

        int[] group = new int[NUMBER_OF_GROUPS];
        int groupIndex = 0;
        for (int i : array) {
            group[groupIndex] = i;
            groupIndex++;
            if (groupIndex == NUMBER_OF_GROUPS) {
                writeGroup(group, file);
                group = new int[NUMBER_OF_GROUPS];
                groupIndex = 0;
            }

        }
        if (groupIndex != 0) {
            writeGroup(Arrays.copyOfRange(group, 0, groupIndex), file);
        }

    }

    private void writeGroup(int[] array, String file) {
        StringBuilder firstByteBitString = new StringBuilder();
        ArrayList<Byte> bytesToWrite = new ArrayList<>();
        for (int num : array) {
            // getting the bytes
            int numOfBytesRequired = (int) Math.ceil((double) Integer.toBinaryString(num).length() / 8);
            byte[] bytes = ByteBuffer.allocate(4).putInt(num).array();
            for (int i = 4 - numOfBytesRequired; i < 4; i++) {
                bytesToWrite.add(bytes[i]);
            }

            //Appending to first byte
            var binaryStringLenghth = Integer.toBinaryString(numOfBytesRequired);
            if (binaryStringLenghth.equals("1")) {
                firstByteBitString.append("01");
            } else if (binaryStringLenghth.equals("100")) {
                firstByteBitString.append("00");
            } else {
                firstByteBitString.append(binaryStringLenghth);
            }

        }
        for (int i = firstByteBitString.length(); i < 8; i++) {
            firstByteBitString.append("0");
        }

        bytesToWrite.add(0, (byte) Integer.parseInt(firstByteBitString.toString(), 2));

        // Converting to array and flushing
        byte[] byteArray = new byte[bytesToWrite.size()];
        int index = 0;
        for (byte b : bytesToWrite) {
            byteArray[index++] = b;
        }

        try {
            Files.write(Paths.get(file), byteArray, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Couldn't write the numbers " + Arrays.toString(array) + " to file.");
            e.printStackTrace();
        }
    }


    @Override
    public int[] decode(String file) {
        ArrayList<Integer> decodedList = new ArrayList<>();
        int index = 0;
        try {
            byte[] allbytes = Files.readAllBytes(Paths.get(file));
            while (index < allbytes.length) {
                index = decodeGroupAtIndex(index, allbytes, decodedList);
            }

        } catch (IOException e) {
            System.err.println("Couldn't open file, reason:");
            e.printStackTrace();
        }
        return decodedList.stream().mapToInt(Integer::intValue).toArray();
    }

    private int decodeGroupAtIndex(int index, byte[] allbytes, ArrayList<Integer> decodedList) {
        byte lengthByte = allbytes[index];
        index += 1;
        for (int i = 3; i > -1; i--) {
            byte bytesForNumber = (byte) ((lengthByte >> ((i * 2))) & 3); // shifting 2 at a time and taking 2 last bits
            if (bytesForNumber == 0) {
                bytesForNumber = 4;
            }
            if (index + bytesForNumber > allbytes.length) break;
            String numberBitString = toBitString(Arrays.copyOfRange(allbytes, index, index + bytesForNumber));
            decodedList.add(Integer.parseInt(numberBitString, 2));
            index += bytesForNumber;
        }
        return index;

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
