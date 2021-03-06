package webdata.Compress;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BitMapProductId extends BitMapCompress<String> {


    public void writeTofiles(String bitMapFile, String orderFile) throws IOException {
        FileWriter bitMapWriter = new FileWriter(bitMapFile, true);
        bitMapWriter.write(bitMapBuilder.toString());
        bitMapWriter.close();

        FileWriter orderWriter = new FileWriter(orderFile, true);
        orderWriter.write(String.join("", order));
        orderWriter.close();

    }

    public void flush(String bitMapFile, String orderFile) throws IOException {
        FileWriter bitMapWriter = new FileWriter(bitMapFile, true);
        bitMapWriter.write(bitMapBuilder.toString());
        bitMapWriter.close();
        bitMapBuilder = new StringBuilder();

        FileWriter orderWriter = new FileWriter(orderFile, true);
        orderWriter.write(String.join("", order));
        orderWriter.close();

        order = new ArrayList<>();

    }

    public String decodeAtIndex(String file, long index) {
        try {
            index = countOnesUntilIndex((int) index) - 1;
            RandomAccessFile raf = new RandomAccessFile(file, "r");

            long fromByte = index * 10;
            raf.seek(fromByte);

            int numberOfBytes = 10;
            byte[] bytes = new byte[numberOfBytes];
            raf.read(bytes);

            return new String(bytes, StandardCharsets.UTF_8);

        } catch (FileNotFoundException e) {
            System.err.println("Couldn't find source file.\nreturning default : 0");
            e.printStackTrace();
            return "";
        } catch (IOException e) {
            System.err.println("Couldn't read source file.\nreturning default : 0");
            e.printStackTrace();
            return "";
        }
    }

    public List<Integer> getAllIdsForIndex(int index){
        ArrayList<Integer> result = new ArrayList<>();
        int correctOneToCountFrom = (index / 10) + 1;
        int numberOfOnesSeen = 0;
        for (int i = 0; i < binaryString.length(); i++) {
            if (binaryString.charAt(i) == '1'){
                numberOfOnesSeen++;
            }
            if (numberOfOnesSeen == correctOneToCountFrom){
                result.add(i + 1);
                i++;
                while (binaryString.charAt(i) != '1'){
                    result.add(i + 1);
                    i++;
                }
                return result;
            }


        }
        return result;
    }


}
