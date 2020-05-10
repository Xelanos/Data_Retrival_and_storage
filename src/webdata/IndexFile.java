package webdata;

import static webdata.Compress.IndexDir.REVIEWS_DIR;

public enum IndexFile {
    REVIEWS_LENGTH(REVIEWS_DIR + "/" + "reviewsLength"),
    REVIEWS_ENUM(REVIEWS_DIR + "/" + "reviewsNumerator"),
    REVIEWS_DENUM(REVIEWS_DIR + "/" + "reviewsDenominator"),
    REVIEWS_SCORE(REVIEWS_DIR + "/" + "reviewsScore"),
    ;

    private final String text;


    IndexFile(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

}
