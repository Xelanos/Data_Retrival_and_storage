package webdata.Compress;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class BitMapCompress<T extends Comparable<T>> {

    List<T> order = new ArrayList<>();
    StringBuilder bitMapBuilder = new StringBuilder();
    T lastOne;
    int size = 0;
    private String binaryString;

    public List<T> getOrder() {
        return order;
    }

    public StringBuilder getBitMapBuilder() {
        return bitMapBuilder;
    }

    public void add(T symbol) {
        if (size == 0 || symbol.compareTo(lastOne) != 0) {
            order.add(symbol);
            lastOne = symbol;
            bitMapBuilder.append("1");
        } else {
            bitMapBuilder.append("0");
        }
        size++;
    }

    public void readBinrayString(String binaryStringFile) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(binaryStringFile));
            this.binaryString = reader.readLine();
        } catch (IOException e) {
            System.err.println("Problem when reading binary string file");
            e.printStackTrace();
        }
    }

    private int countOnesUntilIndex(int index) {
        int count = 0;
        for (int i = 0; i <= index; i++) {
            if (binaryString.charAt(i) == '1') {
                count++;
            }
        }
        return count;
    }



}
