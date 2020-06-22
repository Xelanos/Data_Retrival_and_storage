package webdata;

import webdata.Compress.*;
import webdata.dictionary.ReviewsData;
import webdata.dictionary.Trie;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static webdata.IndexFiles.*;

public class IndexReader {

    private String indexDirectory;
    private GroupVarintCompressor groupVarintReader = new GroupVarintCompressor();
    private FixedBitCompressor fixedBitReader = new FixedBitCompressor();
    private OneByteCompressor oneByteReader = new OneByteCompressor();
    private TwoByteCompressor twoByteReader = new TwoByteCompressor();
    private BitMapProductId productId = new BitMapProductId();
    private RandomAccessFile postingListRandomReader;

    private Trie<ReviewsData> dictionary;
    int numberOfreviews;
    long allTokenCount;


    /**
     * Creates an IndexReader which will read from the given directory
     * 3
     */
    public IndexReader(String dir) {
        this.indexDirectory = dir;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(dir + "/" + NUMBER_OF_REVIEWS));
            this.numberOfreviews = Integer.parseInt(reader.readLine());
        } catch (IOException e) {
            System.err.println("couldn't read num of reviews, setting to 0");
            e.printStackTrace();
            this.numberOfreviews = 0;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(dir + "/" + TOTAL_TOKEN_COUNT_FILE));
            this.allTokenCount = Long.parseLong(reader.readLine());
        } catch (IOException e) {
            System.err.println("couldn't read num of tokens, setting to 0");
            e.printStackTrace();
            this.allTokenCount = 0;
        }


        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(dir + "/" + DICTIONARY_FILE));
            this.dictionary = (Trie<ReviewsData>) ois.readObject();
        } catch (Exception e) {
            System.err.println("Couldn't read dictionary file, will fail all dictionary related function");
            e.printStackTrace();
        }

        try {
            postingListRandomReader = new RandomAccessFile(dir + "/" + REVERSE_INDEX_CODES_FILE, "r");
        } catch (FileNotFoundException e) {
            System.err.println("Couldn't read posting list file");
            e.printStackTrace();
        }


        productId.readBinrayString(indexDirectory + "/" + REVIEWS_PRODUCT_BITMAP_FILE);
    }

    /**
     * Returns the product identifier for the given review
     * Returns null if there is no review with the given identifier
     */
    public String getProductId(int reviewId) {
        reviewId -= 1;
        if (reviewId < 0 || reviewId > numberOfreviews) return null;
        return productId.decodeAtIndex(indexDirectory + "/" + REVIEWS_PRODUCT_ORDER_FILE, reviewId);
    }

    /**
     * Returns the score for a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewScore(int reviewId) {
        reviewId -= 1;
        if (reviewId < 0 || reviewId > numberOfreviews) return -1;
        return fixedBitReader.decodeAtIndex(indexDirectory + "/" + REVIEWS_SCORE_FILE, reviewId);
    }

    /**
     * Returns the numerator for the helpfulness of a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewHelpfulnessNumerator(int reviewId) {
        reviewId -= 1;
        if (reviewId < 0 || reviewId > numberOfreviews) return -1;
        return oneByteReader.decodeAtIndex(indexDirectory + "/" + REVIEWS_NUMERATOR_FILE, reviewId);
    }

    /**
     * Returns the denominator for the helpfulness of a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewHelpfulnessDenominator(int reviewId) {
        reviewId -= 1;
        if (reviewId < 0 || reviewId > numberOfreviews) return -1;
        return oneByteReader.decodeAtIndex(indexDirectory + "/" + REVIEWS_DENUM_FILE, reviewId);
    }

    /**
     * Returns the number of tokens in a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewLength(int reviewId) {
        reviewId -= 1;
        if (reviewId < 0 || reviewId > numberOfreviews) return -1;
        return twoByteReader.decodeAtIndex(indexDirectory + "/" + REVIEWS_LENGTH_FILE, reviewId);
    }

    /**
     * Return the number of reviews containing a given token (i.e., word)
     * Returns 0 if there are no reviews containing this token
     */
    public int getTokenFrequency(String token) {
        var node = dictionary.find(token);
        if (node == null) {
            return 0;
        }
        return twoByteReader.decodeAtIndex(indexDirectory + "/" + REVIEWS_CONTATING_TOKEN_FILE, node.getData().postingListPointer);
    }

    /**
     * Return the number of times that a given token (i.e., word) appears in
     * the reviews indexed
     * Returns 0 if there are no reviews containing this token
     */
    public int getTokenCollectionFrequency(String token) {
        var node = dictionary.find(token);
        if (node == null) {
            return 0;
        }
        return node.getData().freq;
    }

    /**
     * Return a series of integers of the form id-1, freq-1, id-2, freq-2, ... such
     * that id-n is the n-th review containing the given token and freq-n is the
     * number of times that the token appears in review id-n
     * Note that the integers should be sorted by id
     * <p>
     * Returns an empty Enumeration if there are no reviews containing this token
     */
    public Enumeration<Integer> getReviewsWithToken(String token) {
        var node = dictionary.find(token);
        if (node == null) {
            return Collections.enumeration(new ArrayList<Integer>());
        }

        long start = node.getData().postingListStart;
        long end = node.getData().postingListEnd;

        byte[] coded = new byte[(int) (end - start)];
        try {
            postingListRandomReader.seek(start);
            postingListRandomReader.read(coded);
        } catch (IOException e) {
            System.err.println("Couln't read from codes file");
            e.printStackTrace();
        }

        var postingList = CustomIntList.fromGapsEveryTwoIterable(groupVarintReader.decodeAllBytes(coded));

        return Collections.enumeration(postingList);
    }


    /**
     * Return the number of product reviews available in the system
     */
    public int getNumberOfReviews() {
        return numberOfreviews;
    }

    /**
     * Return the number of number of tokens in the system
     * (Tokens should be counted as many times as they appear)
     */
    public int getTokenSizeOfReviews() {
        return (int) allTokenCount;
    }

    /**
     * Return the ids of the reviews for a given product identifier
     * Note that the integers returned should be sorted by id
     * <p>
     * Returns an empty Enumeration if there are no reviews for this product
     */
    public Enumeration<Integer> getProductReviews(String productId) {
        List<Integer> result = new ArrayList<>();
        Pattern pattern = Pattern.compile(productId);
        String order;
        try (BufferedReader reader = Files.newBufferedReader(Paths.get( indexDirectory + "/" + REVIEWS_PRODUCT_ORDER_FILE))){
            order = reader.readLine();

        } catch (IOException e) {
            System.err.println("Couldn't read productId info");
            e.printStackTrace();
            return Collections.enumeration(result);
        }
        Matcher matcher = pattern.matcher(order);
        // Check all occurrences
        while (matcher.find()) {
            int index = matcher.start();
            result.addAll(this.productId.getAllIdsForIndex(index));
        }
        return Collections.enumeration(result);
    }
}