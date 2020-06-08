package webdata;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.IntStream;

public class CustomIntList extends ArrayList<Integer> {


    public int[] dump(File file) {
        int[] sortedIndices = IntStream.range(0, this.size())
                .boxed().sorted(Comparator.comparing(this::get))
                .mapToInt(ele -> ele).toArray();
        this.sort(Comparator.naturalOrder());

        int[] differenceList = new int[this.size()];
        differenceList[0] = this.get(0);
        for (int i = 1; i < this.size(); i++) {
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

    public int[] toPrimitiveArray() {
        int[] primitive = new int[this.size()];
        for (int i = 0; i < this.size(); i++) {
            primitive[i] = this.get(i);
        }
        return primitive;

    }

    public Integer[] toGapsArray() {
        Integer[] differenceList = new Integer[this.size()];
        differenceList[0] = this.get(0);
        for (int i = 1; i < this.size(); i++) {
            differenceList[i] = this.get(i) - this.get(i - 1);
        }
        return differenceList;
    }

    public CustomIntList toGapsEveryTwo() {
        CustomIntList gaps = new CustomIntList();
        gaps.add(this.get(0));
        gaps.add(this.get(1));
        for (int i = 2; i < this.size(); i++) {
            if (i % 2 == 0) {
                gaps.add(this.get(i) - this.get(i - 2));
            }
            else gaps.add(this.get(i));
        }
        return gaps;
    }

    static CustomIntList fromGapsIterable(Iterable<Integer> array) {
        CustomIntList result = new CustomIntList();
        Integer current = 0;
        for (Integer number : array) {
            current += number;
            result.add(current);
        }
        return result;

    }

    static CustomIntList fromGapsEveryTwoIterable(Iterable<Integer> array) {
        CustomIntList result = new CustomIntList();
        Integer memory = 0;
        int i = 0;
        for (Integer number : array) {
            if (i % 2 == 0){
                memory += number;
                result.add(memory);
            } else result.add(number);
            i++;
        }
        return result;

    }

    public static CustomIntList mergeSorted(CustomIntList firstList, CustomIntList secondList){
        CustomIntList result = new CustomIntList();
        if (firstList.get(0) < secondList.get(0)){
            result.addAll(firstList);
            result.addAll(secondList);
        }
        else {
            result.addAll(secondList);
            result.addAll(firstList);
        }
        return result;
    }

}
