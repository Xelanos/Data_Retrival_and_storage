package webdata;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class IndexWriter {
    private final Pattern PRODUCT_ID_PATTERN = Pattern.compile("product\\/productId:\\s*([\\S\\n]+)\\s*review\\/userId");
    private final Pattern HELPFULLNESS_PATTERN = Pattern.compile("review\\/helpfulness:\\s*(\\d+)\\s*\\/\\s*(\\d+)\\s*review\\/score");
    private final Pattern SCORE_PATTERN = Pattern.compile("review\\/score:\\s*(\\d+)\\.?\\d+\\s*review\\/time");
    private final Pattern TEXT_PATTERN = Pattern.compile("review\\/text:\\s*([\\S\\s]+)");


    /**
     * Given product review data, creates an on disk index
     * inputFile is the path to the file containing the review data
     * dir is the directory in which all index files will be created
     * if the directory does not exist, it should be created
     */
    public void write(String inputFile, String dir) {
        makeAllfilesAndDirs(dir);
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            for (int i = 1; i <= 100; i++) {
                String review = getNextReview(reader);

                System.out.println(review);
            }

        } catch (FileNotFoundException e) {
            System.err.println("Didn't find input file");
            e.printStackTrace();
            return;
        } catch (IOException e) {
            System.err.println("Couldn't read from input file");
            e.printStackTrace();
            return;
        }
    }

    private String getNextReview(BufferedReader reader) throws IOException{
        String line = "\n";
        StringBuilder review = new StringBuilder();
        while (!line.equals("")){
            review.append(line);
            line = reader.readLine();
        }

        return review.toString();
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
