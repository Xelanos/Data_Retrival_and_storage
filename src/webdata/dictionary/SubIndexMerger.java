package webdata.dictionary;

import webdata.CustomIntList;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;

public class SubIndexMerger {

    private File workingDir;

    public SubIndexMerger(String workingDir) {
        this.workingDir = new File(workingDir);

    }

    public String merge() {
        File[] listOfFiles = workingDir.listFiles();
        while (listOfFiles.length > 1) {
            for (int i = 0; i < listOfFiles.length - 1; i += 2) {
                mergeTwoIndexes(listOfFiles[i].getName(), listOfFiles[i + 1].getName());
                listOfFiles[i].delete();
                listOfFiles[i + 1].delete();
            }

            listOfFiles = workingDir.listFiles();
        }

        return listOfFiles[0].getName();

    }

    private void mergeTwoIndexes(String firstSubIndex, String secondSubIndex) {

        System.gc();

        long avalibleMem = Runtime.getRuntime().freeMemory() - 200000000; //Keep 20MB for a rainy day
        System.out.println(avalibleMem);

        int mem = Long.valueOf(avalibleMem).intValue() > 0 ? Long.valueOf(avalibleMem).intValue() : Integer.MAX_VALUE;

        try (BufferedReader firstFile = new BufferedReader(new FileReader(workingDir + "/" + firstSubIndex), mem / 3);
             BufferedReader secondFile = new BufferedReader(new FileReader(workingDir + "/" + secondSubIndex), mem / 3);
             BufferedWriter output = Files.newBufferedWriter(Paths.get(workingDir + "/" + firstSubIndex + secondSubIndex),
                     StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        ) {

            String line1 = firstFile.readLine();
            String line2 = secondFile.readLine();
            while (line1 != null && line2 != null) {
                var firstEntry = makeEntryFromLine(line1);
                var secondEntry = makeEntryFromLine(line2);

                if (firstEntry.getKey().compareTo(secondEntry.getKey()) < 0) {
                    output.append(firstEntry.toString()).append("\n");
                    line1 = firstFile.readLine();
                } else if (secondEntry.getKey().compareTo(firstEntry.getKey()) < 0) {
                    output.append(secondEntry.toString()).append("\n");
                    line2 = secondFile.readLine();
                } else {

                    output.append(firstEntry.getKey()).append("=")
                            .append(CustomIntList.mergeSorted(firstEntry.getValue(), secondEntry.getValue()).toString())
                            .append("\n");
                    line1 = firstFile.readLine();
                    line2 = secondFile.readLine();
                }
            }

            //Append the rest of the largest file
            while (line1 != null) {
                output.append(line1).append("\n");
                line1 = firstFile.readLine();
            }

            while (line2 != null) {
                output.append(line2).append("\n");
                line2 = secondFile.readLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public Map.Entry<String, CustomIntList> makeEntryFromLine(String line) {
        var split = line.split("=");
        String list = split[1];
        CustomIntList intList = new CustomIntList();
        Arrays.stream(list.substring(1, list.length() - 1).split(", "))
                .mapToInt(Integer::parseInt)
                .forEach(intList::add);

        return new AbstractMap.SimpleEntry<>(split[0], intList);
    }
}
