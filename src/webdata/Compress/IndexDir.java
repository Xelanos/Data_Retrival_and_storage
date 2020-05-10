package webdata.Compress;

public enum IndexDir {
    REVIEWS_DIR("reviews");


    private final String text;


    IndexDir(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
