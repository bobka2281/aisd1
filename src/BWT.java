import java.nio.ByteBuffer;
import java.util.Arrays;

public class BWT{
    public static byte[] code(byte[] data) {
        int n = data.length;
        if (n == 0) return new byte[0];
        byte[][] matrix = new byte[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = data[(i + j) % n];
            }
        }
        Arrays.sort(matrix, BWT::compare);
        int originalIndex = -1;
        byte[] lastColumn = new byte[n];
        for (int i = 0; i < n; i++) {
            lastColumn[i] = matrix[i][n - 1];
            if (Arrays.equals(matrix[i], data)) {
                originalIndex = i;
            }
        }
        ByteBuffer buffer = ByteBuffer.allocate(4 + n);
        buffer.putInt(originalIndex);
        buffer.put(lastColumn);
        return buffer.array();
    }

    public static byte[] decode(byte[] encodedData) {
        if (encodedData.length == 0) return new byte[0];
        ByteBuffer buffer = ByteBuffer.wrap(encodedData);
        int originalIndex = buffer.getInt();
        int n = encodedData.length - 4;
        byte[] L = new byte[n];
        buffer.get(L);
        byte[][] matrix = new byte[n][0];
        for (int len = 0; len < n; len++) {
            for (int i = 0; i < n; i++) {
                byte[] newRow = new byte[len + 1];
                newRow[0] = L[i];
                System.arraycopy(matrix[i], 0, newRow, 1, len);
                matrix[i] = newRow;
            }
            Arrays.sort(matrix, BWT::compare);
        }
        return matrix[originalIndex];
    }

    private static int compare(byte[] a, byte[] b) {
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            int cmp = Integer.compare(Byte.toUnsignedInt(a[i]), Byte.toUnsignedInt(b[i]));
            if (cmp != 0) return cmp;
        }
        return Integer.compare(a.length, b.length);
    }

    public static byte[] decodeLF(byte[] encodedData) {
        if (encodedData.length <= 4) return new byte[0];
        ByteBuffer buffer = ByteBuffer.wrap(encodedData);
        int originalIndex = buffer.getInt();
        int n = encodedData.length - 4;
        byte[] L = new byte[n];
        buffer.get(L);
        int[] count = new int[256];
        for (int i = 0; i < n; i++) {
            count[L[i] & 0xFF]++;
        }
        int[] offset = new int[256];
        int sum = 0;
        for (int i = 0; i < 256; i++) {
            offset[i] = sum;
            sum += count[i];
        }
        int[] T = new int[n];
        for (int i = 0; i < n; i++) {
            int charIndex = L[i] & 0xFF;
            T[i] = offset[charIndex];
            offset[charIndex]++;
        }
        byte[] result = new byte[n];
        int curr = originalIndex;
        for (int i = n - 1; i >= 0; i--) {
            result[i] = L[curr];
            curr = T[curr];
        }
        return result;
    }
}