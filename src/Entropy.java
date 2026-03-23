import java.util.*;

public class Entropy {
    public static double calc(byte[] data, int len) {
        Map<Long, Integer> probabilities = new HashMap<>();
        int total = 0;
        for (int i = 0; i <= data.length - len; i += len) {
            long symbol = 0;
            for (int j = 0; j < len; j++) {
                symbol = (symbol << 8) | (data[i + j] & 0xFF);
            }
            probabilities.put(symbol, probabilities.getOrDefault(symbol, 0) + 1);
            total++;
        }
        double entropy = 0;
        for (int count : probabilities.values()) {
            double p = (double) count / total;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }
}
