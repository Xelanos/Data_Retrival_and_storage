package webdata.Compress;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AlphaNumericAdaptive extends AdaptiveAritmaticCompressor<Character>{

    public AlphaNumericAdaptive() {
        this.possibleSymbols = makeAlphaNumericSet();
        makeMaps();
    }

    private Set<Character> makeAlphaNumericSet() {
        char[] alphabet = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
        Set<Character> result = new HashSet();
        for (char c : alphabet) {
            result.add(c);
        }
        return result;
    }


    public BigDecimal[] encode(char[] array, String file) {
        Character[] boxed = new Character[array.length];

        for (int i = 0; i < array.length; i++) {
            boxed[i] = array[i];
        }
        return this.encode(boxed, file);
    }
}
