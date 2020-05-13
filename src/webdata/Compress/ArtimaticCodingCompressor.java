package webdata.Compress;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArtimaticCodingCompressor<T extends Comparable<T>> implements Serializable {

    Map<T, Double> symbolsProbabilities;
    double numberOfSymbolsEncoded;

    public ArtimaticCodingCompressor(){
        new ArtimaticCodingCompressor(null);
    }

    public ArtimaticCodingCompressor(Map<T, Double> probabilities) {
        this.symbolsProbabilities = probabilities;
    }


    public double[] encode(T[] array, String file) {
        if (symbolsProbabilities == null) makeProbMap(array);
        this.numberOfSymbolsEncoded = array.length;
        double low = 0;
        double high = 1;
        for (T symbol : array) {
            double[] newProbs = restrict(low, high, symbol);
            low = newProbs[0];
            high = newProbs[1];
        }
        return new double[]{array.length, (low + high)/ 2.0};

    }

    private void makeProbMap(T[] array) {
        double numberOfSymbols = array.length;
        HashMap<T, Double> result = new HashMap<>();
        for (T symbol : array){
            if (!result.containsKey(symbol)){
                result.put(symbol, 0.0);
            }
            result.put(symbol, result.get(symbol) + 1);
        }

        result.replaceAll((k, v) -> v / numberOfSymbols);
        this.symbolsProbabilities = result;
    }


    public T[] decode(String file) {
        return null;
    }

    public List<T> artimaticDecode(double numberOfSymbolsEncoded, double code) {
        double low = 0;
        double high = 1;

        ArrayList<T> result = new ArrayList<>();

        for (int i = 0; i < numberOfSymbolsEncoded; i++) {
            for (Map.Entry<T, Double> entry: symbolsProbabilities.entrySet()){
                double[] newProbs = restrict(low, high, entry.getKey());
                double newLow = newProbs[0];
                double newHigh = newProbs[1];
                if ((code >= newLow) && (code < newHigh)){
                    result.add(entry.getKey());
                    low = newLow;
                    high = newHigh;
                    break;

                }
            }
        }
        return result;
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

    public void saveProbabilitiesTable(String file) {
        try (FileOutputStream fileOut = new FileOutputStream(file);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)){
            out.writeObject(this.symbolsProbabilities);

        }
        catch (IOException e){
            System.err.println("Couldn't save probabilities");
            e.printStackTrace();
        }
    }

    public void loadProbabilitiesFromFile(String file){
        Map <T, Double> table = null;
        try (FileInputStream fileIn = new FileInputStream("/tmp/employee.ser");
             ObjectInputStream in = new ObjectInputStream(fileIn)){
            table = (Map <T, Double> ) in.readObject();
            this.symbolsProbabilities = table;

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Couldn't load probabilites");
            e.printStackTrace();
        }

    }

}
