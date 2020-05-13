package webdata;

public enum IndexDirs {
    REVIEWS_DIR("reviews");


    private final String text;


    IndexDirs(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
