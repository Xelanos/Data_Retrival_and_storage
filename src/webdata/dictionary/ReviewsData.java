package webdata.dictionary;

import java.io.Serializable;

public class ReviewsData implements Serializable {

    public int freq;
    public int postingListPointer;
    public long postingListStart;
    public long postingListEnd;

    public ReviewsData(int freq, int postingListPointer, long postingListStart, long postingListEnd) {
        this.freq = freq;
        this.postingListPointer = postingListPointer;
        this.postingListStart = postingListStart;
        this.postingListEnd = postingListEnd;
    }

    public ReviewsData(int freq, int postingListPointer) {
        this.freq = freq;
        this.postingListPointer = postingListPointer;
    }
}
