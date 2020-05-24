package webdata;

import webdata.Compress.*;
import webdata.dictionary.ReviewsData;
import webdata.dictionary.Trie;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static webdata.IndexFiles.*;


public class SlowIndexWriter {


    private final Pattern PRODUCT_ID_PATTERN = Pattern.compile("product\\/productId:\\s*([\\S\\n]+)\\s*review\\/userId");
    private final Pattern HELPFULLNESS_PATTERN = Pattern.compile("review\\/helpfulness:\\s*(\\d+)\\s*\\/\\s*(\\d+)\\s*review\\/score");
    private final Pattern SCORE_PATTERN = Pattern.compile("review\\/score:\\s*(\\d+)\\.?\\d+\\s*review\\/time");
    private final Pattern TEXT_PATTERN = Pattern.compile("review\\/text:\\s*([\\S\\s]+)");

    private TreeMap<String, TreeMap<Integer, Integer>> wordsDictionary = new TreeMap<>();
    private TreeMap<String, TreeSet<Integer>> productIdDictionary = new TreeMap<>();

    private CustomIntList reviewsLength = new CustomIntList();
    private CustomIntList reviewsDenominator = new CustomIntList();
    private CustomIntList reviewsNumerator = new CustomIntList();
    private CustomIntList reviewsScore = new CustomIntList();
    private BitMapProductId productDict = new BitMapProductId();

    private int numberOfReviews;
    private String indexDirectory;


    public SlowIndexWriter() {
    }


    /**
     * Given product review data, creates an on disk index
     * inputFile is the path to the file containing the review data
     * dir is the directory in which all index files will be created
     * if the directory does not exist, it should be created
     */
    public void slowWrite(String inputFile, String dir) {
        makeAllfilesAndDirs(dir);

        //Creating directory and reading input file
        String reviews = null;
        try {
            File directory = new File(Paths.get(dir).toUri());
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    System.err.println("Couldn't make requested director");
                    return;
                }
            }
            reviews = readInputFile(inputFile);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        //Indexing file
        int runningReviewIndex = 1;
        for (String review : reviews.split("\n\n")) {
            index(review, runningReviewIndex);
            runningReviewIndex++;
        }

        numberOfReviews = runningReviewIndex - 1;

        try {
            writeToDisk(dir);
        } catch (IOException e) {
            System.err.println("Failed while writing to disk, cause:");
            e.printStackTrace();
        }

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

    private void writeToDisk(String dir) throws IOException {

        FileWriter numOfreviewsWrite = new FileWriter(dir + "/" + NUMBER_OF_REVIEWS);
        numOfreviewsWrite.write(Integer.valueOf(numberOfReviews).toString());
        numOfreviewsWrite.close();

        int totalTokenCount = reviewsLength.stream().mapToInt(Integer::intValue).sum();
        FileWriter numOfTokensWrite = new FileWriter(dir + "/" + TOTAL_TOKEN_COUNT_FILE);
        numOfTokensWrite.write(Integer.valueOf(totalTokenCount).toString());
        numOfTokensWrite.close();




        productDict.writeTofiles(dir +"/" + REVIEWS_PRODUCT_BITMAP_FILE, dir+"/" + REVIEWS_PRODUCT_ORDER_FILE);

        FixedBitCompressor fixedBitCompressor = new FixedBitCompressor();
        fixedBitCompressor.encode(reviewsScore.toPrimitiveArray(), dir + "/" + REVIEWS_SCORE_FILE);

        OneByteCompressor oneByteCompressor = new OneByteCompressor();
        oneByteCompressor.encode(reviewsDenominator.toPrimitiveArray(), dir + "/" + REVIEWS_DENUM_FILE);
        oneByteCompressor.encode(reviewsNumerator.toPrimitiveArray(), dir + "/" + REVIEWS_NUMERATOR_FILE);

        TwoByteCompressor twoByteCompressor = new TwoByteCompressor();
        twoByteCompressor.encode(reviewsLength.toPrimitiveArray(), dir + "/" + REVIEWS_LENGTH_FILE);

        TreeMap<String, CustomIntList> newWordDict = new TreeMap<>();

        for (String word : wordsDictionary.keySet()) {
            CustomIntList list = new CustomIntList();
            for (var entry : wordsDictionary.get(word).entrySet()) {
                list.add(entry.getKey());
                list.add(entry.getValue());
            }
            newWordDict.put(word, list);
        }

        Set<Integer> keyset = newWordDict.values()
                .stream()
                .flatMap(list -> list.toGapsEveryTwo().stream())
                .collect(Collectors.toSet());


        CustomIntList numOfReviewsCoded = new CustomIntList();
        ArrayList<BigDecimal> codes = new ArrayList<>();

        AdaptiveAritmaticCompressor<Integer> enc = new AdaptiveAritmaticCompressor<>(keyset);
        enc.savePossibleSymbols(dir + "/" + REVERSE_INDEX_KEYSET_FILE);

        Trie<ReviewsData> trie = new Trie<>();
        int postingListIndex = 0;
        for (var entry : newWordDict.entrySet()) {
            AdaptiveAritmaticCompressor<Integer> encoder = new AdaptiveAritmaticCompressor<>(keyset);
            encoder.setScale(entry.getValue().size() * 2);
            numOfReviewsCoded.add(entry.getValue().size() / 2);
            var code = encoder.encode(entry.getValue().toGapsEveryTwo().toArray(new Integer[0]), "a");
            codes.add(code[1]);
            trie.add(entry.getKey(), new ReviewsData(calculateFreqFromList(entry.getValue()), postingListIndex));
            postingListIndex++;
        }


        twoByteCompressor.encode(numOfReviewsCoded.toPrimitiveArray(), dir + "/" + REVIEWS_CONTATING_TOKEN_FILE);



        try (FileOutputStream fileOut = new FileOutputStream(dir + "/" + DICTIONARY_FILE);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(trie);

        } catch (IOException e) {
            System.err.println("Couldn't save probabilities");
            e.printStackTrace();
        }

        try (FileOutputStream fileOut = new FileOutputStream(dir + "/" + REVERSE_INDEX_CODES_FILE);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(codes);

        } catch (IOException e) {
            System.err.println("Couldn't save probabilities");
            e.printStackTrace();
        }


    }

    private int calculateFreqFromList(CustomIntList list) {
        int freq = 0;
        for (int i = 1; i < list.size(); i+=2) {
            freq += list.get(i);
        }
        return freq;
    }

    private void index(String review, int reviewIndex) {
        String productID;
        int helpfulnessNumerator;
        int helpfulnessDenominator;
        int score;
        String text;

        Matcher idMatcher = PRODUCT_ID_PATTERN.matcher(review);
        if (idMatcher.find()) {
            productID = idMatcher.group(1).replaceAll("\\s+", "");
        } else {
            System.out.println("Couldn't find product ID for review number:" + reviewIndex +
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
            System.out.println("Couldn't find helpfulness for review number:" + reviewIndex +
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
            System.out.println("Couldn't find score for review number:" + reviewIndex +
                    ". setting default 0");
            score = 0;
        }

        Matcher textMatcher = TEXT_PATTERN.matcher(review);
        if (textMatcher.find()) {
            text = textMatcher.group(1).replaceAll("[\\n\\t ]", " ");

        } else {
            System.out.println("Couldn't find text for review number:" + reviewIndex +
                    ". setting default \"null\"");
            text = "null";
        }
        String[] words = text.toLowerCase().split("[^a-zA-Z\\d]");

        fillDataHolders(reviewIndex, productID, helpfulnessNumerator, helpfulnessDenominator, score, words);


    }

    private void fillDataHolders(int reviewIndex, String productID, int helpfulnessNumerator, int helpfulnessDenominator,
                                 int score, String[] words) {
        int reviewLength = 0;
        reviewsNumerator.add(helpfulnessNumerator);
        reviewsDenominator.add(helpfulnessDenominator);
        reviewsScore.add(score);
        productDict.add(productID);

        if (!productIdDictionary.containsKey(productID)) {
            productIdDictionary.put(productID, new TreeSet<>());
        }
        productIdDictionary.get(productID).add(reviewIndex);

        for (String word : words) {
            if (word.equals("")) continue;
            reviewLength++;
            if (!wordsDictionary.containsKey(word)) {
                wordsDictionary.put(word, new TreeMap<>());
            }
            if (!wordsDictionary.get(word).containsKey(reviewIndex)) {
                wordsDictionary.get(word).put(reviewIndex, 0);

            }
            wordsDictionary.get(word).put(reviewIndex, wordsDictionary.get(word).get(reviewIndex) + 1);
        }
        reviewsLength.add(reviewLength);

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


    private String readInputFile(String filePath) throws IOException {
        StringBuilder lines = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line = reader.readLine();
        while (line != null) {
            lines.append(line).append("\n");
            // read next line
            line = reader.readLine();
        }
        reader.close();
        return lines.toString();
    }


}