package webdata.Compress;

import java.util.*;

public class AdaptiveAritmaticCompressor<T extends Comparable<T>> {

    Map<T, Double> symbolsAppearing;
    Map<T, Double> symbolsProbabilities;
    double numberOfSymbolsEncoded;
    Set<T> possibleSymbols;
    double seenSymbols;

    public AdaptiveAritmaticCompressor(Set<T> possibleSymbols) {
        this.possibleSymbols = possibleSymbols;
        makeMaps();
    }

    public double[] encode(T[] array, String file) {
        this.numberOfSymbolsEncoded = array.length;
        double low = 0;
        double high = 1;
        for (T symbol : array) {
            double[] newProbs = restrict(low, high, symbol);
            low = newProbs[0];
            high = newProbs[1];
            updateMaps(symbol);
        }
        return new double[]{array.length, low};

    }

    private void updateMaps(T symbol){
        seenSymbols += 1;
        symbolsAppearing.put(symbol, symbolsAppearing.get(symbol) + 1);
        symbolsProbabilities.replaceAll((k, v) -> symbolsAppearing.get(k) / seenSymbols);

    }



    private void makeMaps() {
        seenSymbols = 0;
        symbolsProbabilities = new HashMap<>();
        symbolsAppearing = new HashMap<>();
        for (T symbol : possibleSymbols){
            seenSymbols += 1;
            symbolsProbabilities.put(symbol, 1.0 / possibleSymbols.size());
            symbolsAppearing.put(symbol, 1.0);

        }

    }

    private double[] restrict(double low, double high, T symbol) {
        double range = high - low;

        double symbolProb = symbolsProbabilities.get(symbol);
        double lowBound = symbolsProbabilities.entrySet().stream().
                filter(entry -> entry.getKey().compareTo(symbol) < 0).
                map(Map.Entry::getValue).mapToDouble(Double::doubleValue)
                .sum();

        double highBound = lowBound + symbolProb;
        double newLow = low + (range * lowBound);
        double newHigh = low + (range * highBound);

        return new double[]{newLow, newHigh};

    }

    public List<T> artimaticDecode(double numberOfSymbolsEncoded, double code) {
        double low = 0;
        double high = 1;

        ArrayList<T> result = new ArrayList<>();

        for (int i = 0; i < numberOfSymbolsEncoded; i++) {
            for (T symbol: symbolsProbabilities.keySet()){
                double[] newProbs = restrict(low, high, symbol);
                double newLow = newProbs[0];
                double newHigh = newProbs[1];
                if ((code >= newLow) && (code < newHigh)){
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

}
