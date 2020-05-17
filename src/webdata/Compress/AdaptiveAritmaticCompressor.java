package webdata.Compress;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.RoundingMode;
import java.util.*;
import java.math.BigDecimal;

public class AdaptiveAritmaticCompressor<T extends Comparable<T>> {

    Map<T, BigDecimal> symbolsAppearing;
    Map<T, BigDecimal> symbolsProbabilities;
    double numberOfSymbolsEncoded;
    Set<T> possibleSymbols;
    int seenSymbols;
    int scale;

    public AdaptiveAritmaticCompressor(Set<T> possibleSymbols) {
        this.possibleSymbols = possibleSymbols;
        makeMaps();
    }

    public BigDecimal[] encode(T[] array, String file) {
        this.numberOfSymbolsEncoded = array.length;
        BigDecimal low = new BigDecimal(0);
        BigDecimal high = new BigDecimal(1);
        for (T symbol : array) {
            BigDecimal[] newProbs = restrict(low, high, symbol);
            low = newProbs[0];
            high = newProbs[1];
            updateMaps(symbol);
        }
        return new BigDecimal[]{new BigDecimal(array.length), low};

    }

    private void updateMaps(T symbol){
        seenSymbols += 1;
        symbolsAppearing.put(symbol, symbolsAppearing.get(symbol).add(BigDecimal.valueOf(1)));
        BigDecimal numSymbols = new BigDecimal(seenSymbols);
        symbolsProbabilities.replaceAll((k, v) -> symbolsAppearing.get(k).divide(numSymbols, scale, RoundingMode.HALF_DOWN));

    }



    private void makeMaps() {
        seenSymbols = 0;
        symbolsProbabilities = new HashMap<>();
        symbolsAppearing = new HashMap<>();
        for (T symbol : possibleSymbols){
            seenSymbols += 1;
            symbolsProbabilities.put(symbol, BigDecimal.valueOf(1.0 / possibleSymbols.size()));
            symbolsAppearing.put(symbol, new BigDecimal(1));

        }
        this.scale = symbolsProbabilities.get(possibleSymbols.toArray()[0]).scale() * 3;

    }

    private BigDecimal[] restrict(BigDecimal low, BigDecimal high, T symbol) {
        BigDecimal range = high.subtract(low);

        BigDecimal symbolProb = symbolsProbabilities.get(symbol);
        BigDecimal lowBound = symbolsProbabilities.entrySet().stream().
                filter(entry -> entry.getKey().compareTo(symbol) < 0).
                map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal highBound = lowBound.add(symbolProb);
        BigDecimal newLow = low.add(range.multiply(lowBound)).setScale(scale, RoundingMode.DOWN);
        BigDecimal newHigh = low.add(range.multiply(highBound)).setScale(scale, RoundingMode.DOWN);

        return new BigDecimal[]{newLow, newHigh};

    }

    public List<T> artimaticDecode(BigDecimal numberOfSymbolsEncoded, BigDecimal code) {
        BigDecimal low = new BigDecimal(0);
        BigDecimal high = new BigDecimal(1);

        ArrayList<T> result = new ArrayList<>();

        for (int i = 0; i < numberOfSymbolsEncoded.intValue(); i++) {
            for (T symbol: symbolsProbabilities.keySet()){
                BigDecimal[] newProbs = restrict(low, high, symbol);
                BigDecimal newLow = newProbs[0];
                BigDecimal newHigh = newProbs[1];
                if ((code.compareTo(newLow) >= 0) && (code.compareTo(newHigh) < 0)){
                    result.add(symbol);
                    low = newLow;
                    high = newHigh;
                    updateMaps(symbol);
                    break;

                }
            }
        }
        return result;
    }

    public void savePossibleSymbols(String file){
        try (FileOutputStream fileOut = new FileOutputStream(file);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)){
            out.writeObject(this.possibleSymbols);

        }
        catch (IOException e){
            System.err.println("Couldn't save probabilities");
            e.printStackTrace();
        }
    }

}
