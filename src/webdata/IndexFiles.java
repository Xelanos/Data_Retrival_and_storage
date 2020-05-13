package webdata;

import static webdata.IndexDirs.REVIEWS_DIR;

public enum IndexFiles {
    REVIEWS_LENGTH_FILE(REVIEWS_DIR + "/" + "reviewsLength"),
    REVIEWS_NUMERATOR_FILE(REVIEWS_DIR + "/" + "reviewsNumerator"),
    REVIEWS_DENUM_FILE(REVIEWS_DIR + "/" + "reviewsDenominator"),
    REVIEWS_SCORE_FILE(REVIEWS_DIR + "/" + "reviewsScore"),
    TEST_FILE("TEST")
    ;

    private final String text;


    IndexFiles(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

}
