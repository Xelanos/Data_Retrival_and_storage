package webdata;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.lang.Math.*;

public class ReviewSearch {

    private IndexReader iReader;
    /**
     * Constructor
     */
    public ReviewSearch(IndexReader iReader) {
        this.iReader = iReader;
    }


    /**
     * Returns a list of the id-s of the k most highly ranked reviews for the
     * given query, using the vector space ranking function lnn.ltc (using the
     * SMART notation)
     * The list should be sorted by the ranking
     */
    public Enumeration<Integer> vectorSpaceSearch(Enumeration<String> query, int k) {
        List<String> queryList = Collections.list(query).stream().map(String::toLowerCase).collect(Collectors.toList());
        Set<String> querySet = new HashSet<>(queryList);
        HashMap<Integer, ArrayList<Double>> docVecotors = new HashMap<>();
        int querySize = querySet.size();
        ArrayList<Double> q = new ArrayList<>(Collections.nCopies(querySize, 0.0));

        int i = 0;
        for (String word : querySet) {
            //Query vector
            double logTfValue = 1 + Math.log10(Collections.frequency(queryList, word));
            double standartIdfvalue = Math.log10((double) iReader.getNumberOfReviews()/ iReader.getTokenFrequency(word) );
            q.set(i, logTfValue * standartIdfvalue);

            //Document vectors
            var reviewsConatiningWord = iReader.getReviewsWithToken(word);
            while (reviewsConatiningWord.hasMoreElements()) {
                int reviewId = reviewsConatiningWord.nextElement();
                int reviewTokenFreq = reviewsConatiningWord.nextElement();
                docVecotors.computeIfAbsent(reviewId, key -> new ArrayList<>(Collections.nCopies(querySize, 0.0)))
                        .set(i, 1 + Math.log10(reviewTokenFreq));

            }
            i++;
        }

        //Normalize q vector
        double qNorm =Math.sqrt(q.stream().mapToDouble(value -> Math.pow(value, 2)).sum());
        q.replaceAll(value-> value / qNorm);

        var result = docVecotors.entrySet().stream()
                .sorted(Comparator.comparing(entry -> dotProduct(entry.getValue(), q))) //Sorting by dot product with q vector
                .map(Map.Entry::getKey).collect(Collectors.toList());
        Collections.reverse(result); //from smallest first to biggest first

        return Collections.enumeration(result.stream().limit(k).collect(Collectors.toList()));

    }

    private Double dotProduct(ArrayList<Double> value, ArrayList<Double> q) {
        double result = 0;
        for (int i = 0; i < q.size(); i++) {
            result += value.get(i) * q.get(i);
        }
        return result;
    }


    /**
     * Returns a list of the id-s of the k most highly ranked reviews for the
     * given query, using the language model ranking function, smoothed using a
     * mixture model with the given value of lambda
     * The list should be sorted by the ranking
     */
    public Enumeration<Integer> languageModelSearch(Enumeration<String> query,
                                                    double lambda, int k) {
        List<String> queryList = Collections.list(query).stream().map(String::toLowerCase).collect(Collectors.toList());
        HashMap<Integer, ArrayList<Double>> wordVectors = new HashMap<>();
        ArrayList<Double> wordsCorpusModelList = new ArrayList<>();
        for (String word : queryList){
            double wordCorpusModel = (1 - lambda) * ((double) iReader.getTokenCollectionFrequency(word) / iReader.getTokenSizeOfReviews());
            var reviewsConatiningWord = iReader.getReviewsWithToken(word);
            Set<Integer> reviewIdsSet = new HashSet<>();
            while (reviewsConatiningWord.hasMoreElements()){
                int reviewId = reviewsConatiningWord.nextElement();
                int reviewTokenFreq = reviewsConatiningWord.nextElement();

                reviewIdsSet.add(reviewId);
                double wordReviewModel = lambda * ((double)reviewTokenFreq / iReader.getReviewLength(reviewId));
                wordVectors.computeIfAbsent(reviewId, key -> new ArrayList<>(wordsCorpusModelList)).add(wordCorpusModel + wordReviewModel);
            }

            //Adding just the corpus model to reviews not containing the word
            for (int review : wordVectors.keySet()){
                if (!reviewIdsSet.contains(review)){
                    wordVectors.get(review).add(wordCorpusModel);
                }
            }
            wordsCorpusModelList.add(wordCorpusModel);
        }
        var result = wordVectors.entrySet().stream()
                .sorted(Comparator.comparing(entry -> multiplyAllInList(entry.getValue()))) //Sorting by product of all values in vector
                .map(Map.Entry::getKey).collect(Collectors.toList());

        Collections.reverse(result); //from smallest first to biggest first

        return Collections.enumeration(result.stream().limit(k).collect(Collectors.toList()));
    }

    /**
     * Returns a list of the id-s of the k most highly ranked productIds for the
     * given query using a function of your choice
     * The list should be sorted by the ranking
     */
    public Collection<String> productSearch(Enumeration<String> query, int k) {
        int MAX_BONUS = 40;
        double APPEARANCE_WEIGHT = 0.8;
        double HELPFUL_WEIGHT = 0.2;

        List<String> queryList = Collections.list(query).stream().map(String::toLowerCase).collect(Collectors.toList());
        HashMap<String, Integer> productAppearances = new HashMap<>();
        HashMap<String, Integer> productTotalHelpfulness = new HashMap<>();

        for (String word : queryList){
            var reviewsConatiningWord = iReader.getReviewsWithToken(word);
            while (reviewsConatiningWord.hasMoreElements()) {
                int reviewId = reviewsConatiningWord.nextElement();
                int reviewTokenFreq = reviewsConatiningWord.nextElement();
                String product = iReader.getProductId(reviewId);

                productAppearances.merge(product, 1, Integer::sum);
                productTotalHelpfulness.merge(product, iReader.getReviewHelpfulnessNumerator(reviewId), Integer::sum);

            }
        }

        var rankedReviews = this.vectorSpaceSearch(Collections.enumeration(queryList), MAX_BONUS);
        int i = 0;
        while (rankedReviews.hasMoreElements()){
            int reviewId = rankedReviews.nextElement();
            productAppearances.merge(iReader.getProductId(reviewId), MAX_BONUS - i, Integer::sum);
            i++;
        }

        double apperanceNorm = productAppearances.values().stream().mapToDouble(Integer::doubleValue).max().orElse(1.0);
        double helpfullnessNorm = productTotalHelpfulness.values().stream().mapToDouble(Integer::doubleValue).max().orElse(1.0);

        var result = productAppearances.keySet().stream()
                .sorted(Comparator.comparingDouble(productID ->
                        ((productAppearances.get(productID) / apperanceNorm) *APPEARANCE_WEIGHT) +
                                ((productTotalHelpfulness.get(productID) / helpfullnessNorm) * HELPFUL_WEIGHT)))
                        .collect(Collectors.toList());
        Collections.reverse(result); //from smallest first to biggest first

        return result.stream().limit(k).collect(Collectors.toList());
    }


    private BigDecimal multiplyAllInList (ArrayList<Double> list){
        return list.stream().map(BigDecimal::valueOf).reduce(BigDecimal.ONE, BigDecimal::multiply);
    }
}
