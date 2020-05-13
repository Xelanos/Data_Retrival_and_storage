package webdata;

import webdata.Compress.AdaptiveAritmaticCompressor;
import webdata.Compress.ArtimaticCodingCompressor;
import webdata.Compress.FixedBitCompressor;
import webdata.Compress.OneByteCompressor;
import webdata.dictionary.ReviewsData;
import webdata.dictionary.Trie;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static webdata.IndexFiles.*;


public class SlowIndexWriter {


    private final Pattern PRODUCT_ID_PATTERN = Pattern.compile("product\\/productId:\\s*([\\S\\n]+)\\s*review\\/userId");
    private final Pattern HELPFULLNESS_PATTERN = Pattern.compile("review\\/helpfulness:\\s*(\\d+)\\s*\\/\\s*(\\d+)\\s*review\\/score");
    private final Pattern SCORE_PATTERN = Pattern.compile("review\\/score:\\s*(\\d+)\\.?\\d+\\s*review\\/time");
    private final Pattern TEXT_PATTERN = Pattern.compile("review\\/text:\\s*([\\S\\s]+)");

    private TreeMap<String, TreeMap<Integer, Integer>> wordsDictionary = new TreeMap<>();
    private TreeMap<String, TreeSet<Integer>> productIdDictionary = new TreeMap<>();

    private CompersableIntArray reviewsLength = new CompersableIntArray();
    private CompersableIntArray reviewsDenominator = new CompersableIntArray();
    private CompersableIntArray reviewsNumerator = new CompersableIntArray();
    private CompersableIntArray reviewsScore = new CompersableIntArray();

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
        int runningReviewIndex = 0;
        for (String review : reviews.split("\n\n")) {
            index(review, runningReviewIndex);
            runningReviewIndex++;
        }

        numberOfReviews = runningReviewIndex + 1;

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

        FixedBitCompressor fixedBitCompressor = new FixedBitCompressor();
        fixedBitCompressor.encode(reviewsScore.toPrimitiveArray(), dir + "/" + REVIEWS_SCORE_FILE);

        OneByteCompressor oneByteCompressor = new OneByteCompressor();
        oneByteCompressor.encode(reviewsDenominator.toPrimitiveArray(), dir + "/" + REVIEWS_DENUM_FILE);
        oneByteCompressor.encode(reviewsNumerator.toPrimitiveArray(), dir + "/" + REVIEWS_NUMERATOR_FILE);

        TreeMap<String, CompersableIntArray> newWordDict = new TreeMap<>();

        for (String word : wordsDictionary.keySet()) {
            CompersableIntArray list = new CompersableIntArray();
            for (var entry : wordsDictionary.get(word).entrySet()) {
                list.add(entry.getKey());
                list.add(entry.getValue());
            }
            newWordDict.put(word, list.toGapsEveryTwo());
        }

        HashMap<Integer, Double> probailities = new HashMap<>();
        int numberOfElements = 0;
        for (CompersableIntArray list : newWordDict.values()) {
            for (Integer number : list) {
                if (!probailities.containsKey(number)) {
                    probailities.put(number, 0.0);
                }
                probailities.put(number, probailities.get(number) + 1);
                numberOfElements++;
            }
        }

        for (Map.Entry<Integer, Double> entry : probailities.entrySet()) {
            entry.setValue(entry.getValue() / numberOfElements);
        }

        ArtimaticCodingCompressor<Integer> a = new ArtimaticCodingCompressor<>(probailities);
        a.saveProbabilitiesTable(dir + "/" + TEST_FILE);


        TreeMap<String, double[]> finalDict = new TreeMap<>();
        for (var entry : newWordDict.entrySet()) {
            finalDict.put(entry.getKey(), a.encode(entry.getValue().toArray(new Integer[0]), "a"));
        }


        Set<Integer> keyset = new HashSet<>(probailities.keySet());
        AdaptiveAritmaticCompressor<Integer> code = new AdaptiveAritmaticCompressor<>(keyset);
        double[] res = code.encode(newWordDict.get("0").toArray(new Integer[0]), "A");

        AdaptiveAritmaticCompressor<Integer> decode = new AdaptiveAritmaticCompressor<>(keyset);
        var decoded = decode.artimaticDecode(res[0], res[1]);




        System.out.println("g");

        Trie<ReviewsData> trie = new Trie<>();
        for (var entry : wordsDictionary.entrySet()) {
            trie.add(entry.getKey(), new ReviewsData(entry.getValue().size(), 3));

        }



//        ArtimaticCodingCompressor<Integer> writer = new ArtimaticCodingCompressor<Integer>();
//        double[] code = writer.encode(reviewsLength.toGapsArray(), "d");
//        writer.saveProbabilitiesTable(dir +"/" + IndexFile.REVIEWS_LENGTH + "Arit");
        try (FileOutputStream fileOut = new FileOutputStream(dir + "/" + TEST_FILE);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(trie);

        } catch (IOException e) {
            System.err.println("Couldn't save probabilities");
            e.printStackTrace();
        }
//        GroupVarintCompressor writer = new GroupVarintCompressor();
//        writer.encode(reviewsLength.toPrimitiveArray(), dir + "/" + IndexFile.REVIEWS_LENGTH);

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

//        System.out.println(review + "\n");
//        System.out.printf("productId : %s\nhelpfulness: %d/%d \nscore: %d\n\n",
//                productID, helpfulnessNumerator, helpfulnessDenominator, score);

        fillDataHolders(reviewIndex, productID, helpfulnessNumerator, helpfulnessDenominator, score, words);


    }

    private void fillDataHolders(int reviewIndex, String productID, int helpfulnessNumerator, int helpfulnessDenominator,
                                 int score, String[] words) {

        reviewsLength.add(words.length);
        reviewsNumerator.add(helpfulnessNumerator);
        reviewsDenominator.add(helpfulnessDenominator);
        reviewsScore.add(score);

        if (!productIdDictionary.containsKey(productID)) {
            productIdDictionary.put(productID, new TreeSet<>());
        }
        productIdDictionary.get(productID).add(reviewIndex);

        for (String word : words) {
            if (word.equals("")) continue;
            if (!wordsDictionary.containsKey(word)) {
                wordsDictionary.put(word, new TreeMap<>());
            }
            if (!wordsDictionary.get(word).containsKey(reviewIndex)) {
                wordsDictionary.get(word).put(reviewIndex, 0);

            }
            wordsDictionary.get(word).put(reviewIndex, wordsDictionary.get(word).get(reviewIndex) + 1);
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

    private void makeAndSetDirectory(String dir) {

        this.indexDirectory = dir;

    }

}