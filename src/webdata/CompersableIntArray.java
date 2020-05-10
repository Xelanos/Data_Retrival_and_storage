package webdata;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.IntStream;

public class CompersableIntArray extends ArrayList<Integer> {

    private File inputFile = null;


    public int[] dump(File file){
        int[] sortedIndices = IntStream.range(0, this.size())
                .boxed().sorted(Comparator.comparing(this::get))
                .mapToInt(ele -> ele).toArray();
        this.sort(Comparator.naturalOrder());

        int[] differenceList = new int[this.size()];
        differenceList[0] = this.get(0);
        for (int i = 1; i < this.size(); i++){
            differenceList[i] = this.get(i) - this.get(i - 1);
        }

        try {
            FileChannel w = FileChannel.open(Paths.get("w"));
            Byte b;
        } catch (IOException e) {
            e.printStackTrace();
        }


        return sortedIndices;
    }

    public int[] toPrimitiveArray(){
        int[] primitive = new int[this.size()];
        for (int i = 0; i < this.size(); i++){
            primitive[i] = this.get(i);
        }
        return primitive;

    }

    public Integer[] toGapsArray() {
        Integer[] differenceList = new Integer[this.size()];
        differenceList[0] = this.get(0);
        for (int i = 1; i < this.size(); i++){
            differenceList[i] = this.get(i) - this.get(i - 1);
        }
        return differenceList;
    }

    public void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }


}
