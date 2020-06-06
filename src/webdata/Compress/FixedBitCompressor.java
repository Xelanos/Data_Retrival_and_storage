package webdata.Compress;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public class FixedBitCompressor extends NonParameterComperrsor {


    @Override
    public void encode(int[] array, String file) {
        ArrayList<Byte> bytes = new ArrayList<>();
        StringBuilder byteString = new StringBuilder();
        int i = 0;
        for (int number : array) {
            String binary;
            switch (number) {
                case 1:
                    binary = "001";
                    break;
                case 2:
                    binary = "010";
                    break;
                case 3:
                    binary = "011";
                    break;
                case 4:
                    binary = "100";
                    break;
                case 5:
                    binary = "101";
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + number);
            }
            byteString.append(binary);
            i++;
            if (i % 2 == 0) {
                byteString.append("0".repeat(Math.max(0, 8 - byteString.length())));
                bytes.add((byte) Integer.parseInt(byteString.toString(), 2));
                byteString = new StringBuilder();

            }
        }
        if (i % 2 != 0) { //one last number to write
            byteString.append("00001"); //writing 1 as EOF
            bytes.add((byte) Integer.parseInt(byteString.toString(), 2));
        }

            // Converting to array and flushing
            byte[] byteArray = new byte[bytes.size()];
            int index = 0;
            for (byte b : bytes) {
                byteArray[index++] = b;
            }
            try {
                Files.write(Paths.get(file), byteArray, StandardOpenOption.APPEND);
            } catch (IOException e) {
                System.err.println("Couldn't write to file");
                e.printStackTrace();
            }
        }


    @Override
    public int[] decodeAll(String file) {
        ArrayList<Integer> result = new ArrayList<>();
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(file));
            for (Byte twoNums : bytes){
                int firstNum = (twoNums >> 5) & 7;
                result.add(firstNum);

                int lastByte = twoNums & 1;
                if (lastByte == 1) {
                    break;
                }
                int secondNumber = (twoNums >> 2) & 7;
                result.add(secondNumber);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    protected int numberOfBytesNeededForOneNumber(long index) {
        return 1;
    }

    @Override
    protected long getFromByte(long index) {
        return index / 2;
    }

    @Override
    protected int decodeOne(byte[] bytes, long index) {
        byte twoNums = bytes[0];
        if (index % 2 == 0) {
            return (twoNums >> 5) & 7;
        }
        else return (twoNums >> 2) & 7;
    }
}
