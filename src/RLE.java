import java.io.*;
import java.util.*;

public class RLE {
    static class Writer {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0, count = 0;
        void write(long val, int n) {
            for (int i = n - 1; i >= 0; i--) {
                buffer = (buffer << 1) | (int) ((val >> i) & 1);
                if (++count == 8) {
                    out.write(buffer);
                    buffer = 0; count = 0;
                }
            }
        }
        byte[] toArray() {
            if (count > 0) out.write(buffer << (8 - count));
            return out.toByteArray();
        }
    }

    static class Reader {
        byte[] data;
        int pos = 0;
        Reader(byte[] d) { data = d; }
        long read(int n) {
            long val = 0;
            for (int i = 0; i < n; i++) {
                if (pos >= data.length * 8) return -1;
                int b = data[pos / 8] & 0xFF;
                val = (val << 1) | ((b >> (7 - (pos % 8))) & 1);
                pos++;
            }
            return val;
        }
    }

    public static byte[] code(long[] d, int ms, int mc) {
        Writer out = new Writer();
        out.write(ms, 8);
        out.write(mc, 8);
        int maxLen = (1 << ms) - 1;
        int i = 0;
        while (i < d.length) {
            int len = 1;
            while (i + len < d.length && d[i] == d[i + len] && len < maxLen) len++;
            if (len > 1) {
                out.write(0, 1);
                out.write(len, ms);
                out.write(d[i], mc);
                i += len;
            } else {
                int start = i;
                while (i < d.length && (i - start) < maxLen) {
                    if (i + 1 < d.length && d[i] == d[i + 1]) break;
                    i++;
                }
                out.write(1, 1);
                out.write(i - start, ms);
                for (int j = start; j < i; j++) out.write(d[j], mc);
            }
        }
        return out.toArray();
    }
    public static List<Long> decode(byte[] data) {
        Reader in = new Reader(data);
        int ms = (int) in.read(8);
        int mc = (int) in.read(8);
        List<Long> res = new ArrayList<>();
        while (true) {
            long type = in.read(1);
            long len = in.read(ms);
            if (len <= 0) break;
            if (type == 0) {
                long val = in.read(mc);
                for (int j = 0; j < len; j++) res.add(val);
            } else {
                for (int j = 0; j < len; j++) res.add(in.read(mc));
            }
        }
        return res;
    }
}