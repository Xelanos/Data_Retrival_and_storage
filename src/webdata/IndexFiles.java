package webdata;

import static webdata.IndexDirs.*;

public enum IndexFiles {
    NUMBER_OF_REVIEWS(REVIEWS_DATA_DIR + "/" + "reviewsNum"),
    REVIEWS_LENGTH_FILE(REVIEWS_DATA_DIR + "/" + "reviewsLength"),
    REVIEWS_NUMERATOR_FILE(REVIEWS_DATA_DIR + "/" + "reviewsNumerator"),
    REVIEWS_DENUM_FILE(REVIEWS_DATA_DIR + "/" + "reviewsDenominator"),
    REVIEWS_SCORE_FILE(REVIEWS_DATA_DIR + "/" + "reviewsScore"),
    REVIEWS_PRODUCT_BITMAP_FILE(REVIEWS_DATA_DIR + "/" + "bitMap"),
    REVIEWS_PRODUCT_ORDER_FILE(REVIEWS_DATA_DIR + "/" + "order"),

    REVIEWS_CONTATING_TOKEN_FILE(WORDS_DICTIONARY_DIR + "/" + "reviewsPerTokenCount"),
    TOTAL_TOKEN_COUNT_FILE(WORDS_DICTIONARY_DIR + "/" + "allTokenCount"),
    REVERSE_INDEX_CODES_FILE(WORDS_DICTIONARY_DIR + "/" + "codes"),
    REVERSE_INDEX_KEYSET_FILE(WORDS_DICTIONARY_DIR + "/" + "keyset"),
    DICTIONARY_FILE(WORDS_DICTIONARY_DIR + "/" + "dictionary"),

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
