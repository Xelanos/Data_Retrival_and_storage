package webdata;

import webdata.Compress.FixedBitCompressor;
import webdata.Compress.GroupVarintCompressor;
import webdata.Compress.OneByteCompressor;

import java.io.*;
import java.util.*;

import static webdata.IndexFile.*;

public class IndexReader {

    private String indexDirectory;
    private GroupVarintCompressor groupVarintReader = new GroupVarintCompressor();
    private FixedBitCompressor fixedBitReader = new FixedBitCompressor();
    private OneByteCompressor oneByteReader = new OneByteCompressor();


    /**
     * Creates an IndexReader which will read from the given directory
     * 3
     */
    public IndexReader(String dir) {
        this.indexDirectory = dir;
    }

    /**
     * Returns the product identifier for the given review
     * Returns null if there is no review with the given identifier
     */
    public String getProductId(int reviewId) {
        return "0";
    }

    /**
     * Returns the score for a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewScore(int reviewId) {
        return fixedBitReader.decode(indexDirectory + "/" + REVIEWS_SCORE_FILE)[reviewId];
    }

    /**
     * Returns the numerator for the helpfulness of a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewHelpfulnessNumerator(int reviewId) {
        return oneByteReader.decode(indexDirectory + "/" + REVIEWS_NUMERATOR_FILE)[reviewId];
    }

    /**
     * Returns the denominator for the helpfulness of a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewHelpfulnessDenominator(int reviewId) {
        return oneByteReader.decode(indexDirectory + "/" + REVIEWS_DENUM_FILE)[reviewId];
    }

    /**
     * Returns the number of tokens in a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewLength(int reviewId) {
        try {
            DataInputStream in = new DataInputStream(new FileInputStream(indexDirectory + "/" + REVIEWS_LENGTH_FILE + "Code"));
            double numberOfElements = in.readDouble();
            double code = in.readDouble();
            System.out.println("code");


        }
        catch (IOException e) {
            System.err.println("Problem when reading the file");
            e.printStackTrace();
        }
        return 1;

    }

    /**
     * Return the number of reviews containing a given token (i.e., word)
     * Returns 0 if there are no reviews containing this token
     */
    public int getTokenFrequency(String token) {
        return 0;
    }

    /**
     * Return the number of times that a given token (i.e., word) appears in
     * the reviews indexed
     * Returns 0 if there are no reviews containing this token
     */
    public int getTokenCollectionFrequency(String token) {
        return 0;
    }

    /**
     * Return a series of integers of the form id-1, freq-1, id-2, freq-2, ... such
     * that id-n is the n-th review containing the given token and freq-n is the
     * number of times that the token appears in review id-n
     * Note that the integers should be sorted by id
     * <p>
     * Returns an empty Enumeration if there are no reviews containing this token
     * /
     * public Enumeration<Integer> getReviewsWithToken(String token) {}
     * /**
     * Return the number of product reviews available in the system
     */
    public int getNumberOfReviews() {
        return 0;
    }

    /**
     * Return the number of number of tokens in the system
     * (Tokens should be counted as many times as they appear)
     */
    public int getTokenSizeOfReviews() {
        return 0;
    }

    /**
     * Return the ids of the reviews for a given product identifier
     * Note that the integers returned should be sorted by id
     * <p>
     * Returns an empty Enumeration if there are no reviews for this product
     */
    public Enumeration<Integer> getProductReviews(String productId) {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(indexDirectory + "/test.ob"));
            TreeMap<String, TreeSet<Integer>> dict = (TreeMap) objectInputStream.readObject();
            return Collections.enumeration(dict.get(productId));
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return Collections.enumeration(new ArrayList<Integer>());
        }
    }
}