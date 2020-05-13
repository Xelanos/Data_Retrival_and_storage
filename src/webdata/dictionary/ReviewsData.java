package webdata.dictionary;

import java.io.Serializable;

public class ReviewsData implements Serializable {

    int freq;
    int postingListPointer;

    public ReviewsData(int freq, int postingListPointer) {
        this.freq = freq;
        this.postingListPointer = postingListPointer;
    }
}
