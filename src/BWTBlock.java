import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class BWTBlock {

    public static byte[] code(byte[] data, int blockSize) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        for (int i = 0; i < data.length; i += blockSize) {
            int size = Math.min(blockSize, data.length - i);
            byte[] block = Arrays.copyOfRange(data, i, i + size);
            byte[] encode = codeSA(block);
            dos.writeInt(encode.length);
            dos.write(encode);
        }
        return out.toByteArray();
    }
    public static byte[] codeSA(byte[] block) {
        int n = block.length;
        byte[] str = new byte[n * 2];
        System.arraycopy(block, 0, str,0, n);
        System.arraycopy(block, 0, str, n, n);
        Integer[] sa = new Integer[n];
        int[] rank = new int[n * 2];
        int[] tempRank = new int[n * 2];
        for (int i = 0; i < n * 2; i++) {
            rank[i] = str[i] & 0xFF;
        }
        for (int i = 0; i < n; i++) {
            sa[i] = i;
        }
        for (int k = 1; k < n; k *= 2) {
            final int curK = k;
            final int[] curRank = rank;
            Arrays.sort(sa, (a, b) -> {
                int rankA1 = curRank[a];
                int rankB1 = curRank[b];
                if (rankA1 != rankB1) {
                    return Integer.compare(rankA1, rankB1);
                }
                int rankA2 = curRank[a + curK];
                int rankB2 = curRank[b + curK];
                return Integer.compare(rankA2, rankB2);
            });
            tempRank[sa[0]] = 0;
            tempRank[sa[0] + n] = 0;
            int curRankVal = 0;
            for (int i = 1; i < n; i++) {
                int prev = sa[i - 1];
                int cur = sa[i];
                int rankPrev1 = curRank[prev];
                int rankCur1 = curRank[cur];
                int rankPrev2 = curRank[prev + curK];
                int rankCur2 = curRank[cur + curK];
                if (rankPrev1 != rankCur1 || rankPrev2 != rankCur2) {
                    curRankVal++;
                }
                tempRank[cur] = curRankVal;
                tempRank[cur + n] = curRankVal;
            }
            int[] swap = rank;
            rank = tempRank;
            tempRank = swap;
        }
        byte[] bwtData = new byte[n];
        int originalIndex = 0;
        for (int i = 0; i < n; i++) {
            if (sa[i] == 0) {
                originalIndex = i;
            }
            bwtData[i] = (sa[i] == 0) ? block[n - 1] : block[sa[i] - 1];
        }
        ByteBuffer buffer = ByteBuffer.allocate(4 + n);
        buffer.putInt(originalIndex);
        buffer.put(bwtData);
        return buffer.array();
    }
    public static byte[] decode ( byte[] encodedData) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteBuffer buffer = ByteBuffer.wrap(encodedData);
        while (buffer.hasRemaining()) {
            if (buffer.remaining() < 4) {
                break;
            }
            int bwtBlockLen = buffer.getInt();
            if (bwtBlockLen > buffer.remaining())
            {
                break;
            }
            byte[] bwtBlock = new byte[bwtBlockLen];
            buffer.get(bwtBlock);
            byte[] originalBlock = BWT.decodeLF(bwtBlock);
            out.write(originalBlock);
        }
        return out.toByteArray();
    }
}
