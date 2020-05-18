package webdata;

public enum IndexDirs {
    REVIEWS_DATA_DIR("reviews"),
    WORDS_DICTIONARY_DIR("wordsDict"),
    ;


    private final String text;


    IndexDirs(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
