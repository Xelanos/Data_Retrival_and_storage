package webdata;

import webdata.Compress.BitMapProductId;
import webdata.Compress.FixedBitCompressor;
import webdata.Compress.OneByteCompressor;
import webdata.Compress.TwoByteCompressor;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static webdata.IndexDirs.TEMP;
import static webdata.IndexFiles.*;
import static webdata.IndexFiles.REVIEWS_LENGTH_FILE;


public class IndexWriter {
    private final Pattern PRODUCT_ID_PATTERN = Pattern.compile("product\\/productId:\\s*([\\S\\n]+)\\s*\\n");
    private final Pattern HELPFULLNESS_PATTERN = Pattern.compile("review\\/helpfulness:\\s*(\\d+)\\s*\\/\\s*(\\d+)\\s*review\\/score");
    private final Pattern SCORE_PATTERN = Pattern.compile("review\\/score:\\s*(\\d+)\\.?\\d+\\s*review\\/time");
    private final Pattern TEXT_PATTERN = Pattern.compile("review\\/text:\\s*([\\S\\s]+)");


    //Data holders
    private Map<String, CustomIntList> wordsDict = new TreeMap<>();
    private CustomIntList reviewsLength = new CustomIntList();
    private CustomIntList reviewsDenominator = new CustomIntList();
    private CustomIntList reviewsNumerator = new CustomIntList();
    private CustomIntList reviewsScore = new CustomIntList();
    private BitMapProductId productDict = new BitMapProductId();

    private long totalTokenCount = 0;

    //Compression agents
    FixedBitCompressor fixedBitCompressor = new FixedBitCompressor();
    OneByteCompressor oneByteCompressor = new OneByteCompressor();
    TwoByteCompressor twoByteCompressor = new TwoByteCompressor();


    /**
     * Given product review data, creates an on disk index
     * inputFile is the path to the file containing the review data
     * dir is the directory in which all index files will be created
     * if the directory does not exist, it should be created
     */
    public void write(String inputFile, String dir) {
        makeAllfilesAndDirs(dir);
        int subIndexId = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            int reviewID = 1;
            String review = "";
            while ((review=getNextReview(reader))!=null && review.length()!=0) {
                index(review, reviewID);
                if (reviewID % 10000 == 0){
                    System.out.println(reviewID);
                }
                if (Runtime.getRuntime().freeMemory() < 20000000){
                    flush(dir, subIndexId);
                    subIndexId++;
                }
                reviewID++;
            }
            flush(dir, subIndexId);
            writeFinalStatitics();
            makeFinalDictionary();

        } catch (FileNotFoundException e) {
            System.err.println("Didn't find input file");
            e.printStackTrace();
            return;
        } catch (IOException e) {
            System.err.println("Problem when reading input / writing index");
            e.printStackTrace();
            return;
        }
    }

    private void writeFinalStatitics() {
        //TODO
    }

    private void makeFinalDictionary() {
        //TODO
    }

    private void flush(String dir, int subIndexId) throws IOException{
        System.out.println("Flushing");

        //Count tokens
        totalTokenCount += reviewsLength.stream().mapToInt(Integer::intValue).sum();

        //write to file
        fixedBitCompressor.encode(reviewsScore.toPrimitiveArray(), dir + "/" + REVIEWS_SCORE_FILE);
        oneByteCompressor.encode(reviewsDenominator.toPrimitiveArray(), dir + "/" + REVIEWS_DENUM_FILE);
        oneByteCompressor.encode(reviewsNumerator.toPrimitiveArray(), dir + "/" + REVIEWS_NUMERATOR_FILE);
        twoByteCompressor.encode(reviewsLength.toPrimitiveArray(), dir + "/" + REVIEWS_LENGTH_FILE);
        productDict.flush(dir +"/" + REVIEWS_PRODUCT_BITMAP_FILE, dir+"/" + REVIEWS_PRODUCT_ORDER_FILE);

        //reset data holders
        reviewsScore = new CustomIntList();
        reviewsDenominator = new CustomIntList();
        reviewsNumerator = new CustomIntList();
        reviewsLength = new CustomIntList();

        //Suggest garbage collector to run in order to get max free memory
        System.gc();

        //flush and reset dict
        File subIndexDict = new File(dir + "/" + TEMP + "/" + "subIndex" + subIndexId);
        subIndexDict.createNewFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(subIndexDict))){
            for (var entry : wordsDict.entrySet()){
                writer.append(entry.toString()).append("\n");
            }
        }

        wordsDict = new TreeMap<>();


        //Suggest garbage collector to run in order to get max free memory
        System.gc();

    }


    private String getNextReview(BufferedReader reader) throws IOException{
        String line = "";
        StringBuilder review = new StringBuilder();
        while((line=reader.readLine()) != null && line.length()!=0) {
            review.append(line).append("\n");
        }

        return review.toString();
    }


    private void index(String review, int reviewId) {
        String productID;
        int helpfulnessNumerator;
        int helpfulnessDenominator;
        int score;
        String text;

        Matcher idMatcher = PRODUCT_ID_PATTERN.matcher(review);
        if (idMatcher.find()) {
            productID = idMatcher.group(1).replaceAll("\\s+", "");
        } else {
            System.out.println("Couldn't find product ID for review number:" + reviewId +
                    ". setting default ID 0000000000");
            productID = "0000000000";
        }

        Matcher helpMatcher = HELPFULLNESS_PATTERN.matcher(review);
        if (helpMatcher.find()) {
            helpfulnessNumerator = Integer.parseInt(helpMatcher.group(1).strip());
            if (helpfulnessNumerator > 100) helpfulnessNumerator = 100;
            if (helpfulnessNumerator < 0) helpfulnessNumerator = 0;
            helpfulnessDenominator = Integer.parseInt(helpMatcher.group(2).strip());
            if (helpfulnessDenominator > 100) helpfulnessDenominator = 100;
            if (helpfulnessDenominator < 0) helpfulnessDenominator = 0;
        } else {
            System.out.println("Couldn't find helpfulness for review number:" + reviewId +
                    ". setting default 0/0");
            helpfulnessNumerator = 0;
            helpfulnessDenominator = 0;
        }

        Matcher scoreMatcher = SCORE_PATTERN.matcher(review);
        if (scoreMatcher.find()) {
            score = Integer.parseInt(scoreMatcher.group(1).strip());
            if (score > 5) score = 5;
            if (score < 1) score = 1;
        } else {
            System.out.println("Couldn't find score for review number:" + reviewId +
                    ". setting default 0");
            score = 0;
        }

        Matcher textMatcher = TEXT_PATTERN.matcher(review);
        if (textMatcher.find()) {
            text = textMatcher.group(1).replaceAll("[\\n\\t ]", " ");

        } else {
            System.out.println("Couldn't find text for review number:" + reviewId +
                    ". setting default \"null\"");
            text = "null";
        }

        String[] words = text.toLowerCase().split("[^a-zA-Z\\d]");

        HashMap<String, Integer> wordCount = new HashMap<>();

        int reviewLength = 0;

        for (String word : words) {
            if (word.equals("")) continue;
            reviewLength++;
            wordCount.merge(word, 1, Integer::sum);
        }

        fillDataHolders(reviewId, productID, helpfulnessNumerator, helpfulnessDenominator, score, wordCount, reviewLength);
    }


    private void fillDataHolders(int reviewId, String productID, int helpfulnessNumerator, int helpfulnessDenominator,
                                 int score, HashMap<String, Integer> wordCount, int reviewLength) {
        reviewsNumerator.add(helpfulnessNumerator);
        reviewsDenominator.add(helpfulnessDenominator);
        reviewsScore.add(score);
        productDict.add(productID);
        reviewsLength.add(reviewLength);

        for (var entry : wordCount.entrySet()){
            if (!wordsDict.containsKey(entry.getKey())){
                wordsDict.put(entry.getKey(), new CustomIntList());
            }
            CustomIntList postingList = wordsDict.get(entry.getKey());
            postingList.add(reviewId);
            postingList.add(entry.getValue());
        }
    }




    private void writeEntryToDisk(String word, CustomIntList postingList, int subIndexId){

    }



    private void makeAllfilesAndDirs(String dir) {
        try {

            File mainDir = new File(dir);
            mainDir.mkdir();
            for (IndexDirs directory : IndexDirs.values()) {
                File f = new File(dir + "/" + directory.toString());
                f.mkdir();
            }

            for (IndexFiles file : IndexFiles.values()) {
                File f = new File(dir + "/" + file.toString());
                f.createNewFile();
            }


        } catch (IOException e) {
            System.err.println("Couldn't make index dirs and files, reason: ");
            e.printStackTrace();
        }

    }



    /**
     * Delete all index files by removing the given directory
     */
    public void removeIndex(String dir) {
        for (IndexFiles file : IndexFiles.values()) {
            File f = new File(dir + "/" + file.toString());
            f.delete();
        }


        for (IndexDirs directory : IndexDirs.values()) {
            File f = new File(dir + "/" + directory.toString());
            f.delete();
        }

        File mainDir = new File(dir);
        mainDir.delete();

    }

}
