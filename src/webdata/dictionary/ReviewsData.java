package webdata.dictionary;

import java.io.Serializable;

public class ReviewsData implements Serializable {

    public int freq;
    public int postingListPointer;

    public ReviewsData(int freq, int postingListPointer) {
        this.freq = freq;
        this.postingListPointer = postingListPointer;
    }
}
