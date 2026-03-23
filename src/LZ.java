import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class LZ {
    public static class LZ77 {

        public static byte[] code(byte[] data, int bufSize) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            int i = 0;
            while (i < data.length) {
                int shift = 0, matchLen = 0;
                int start = Math.max(0, i - bufSize);
                for (int j = start; j < i; j++) {
                    int len = 0;
                    while (i + len < data.length && data[j + len] == data[i + len] && len < 255) len++;
                    if (len >= matchLen) { matchLen = len; shift = i - j; }
                }
                out.writeShort(shift);
                out.writeByte(matchLen);
                i += matchLen;
                out.writeByte(i < data.length ? data[i++] : 0);
            }
            return baos.toByteArray();
        }

        public static byte[] decode(byte[] compressed, int bufSize) throws IOException {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(compressed));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] window = new byte[bufSize];
            int windowLen = 0;
            while (in.available() > 0) {
                int shift = in.readShort() & 0xFFFF;
                int len = in.readByte() & 0xFF;
                byte next = in.readByte();
                int start = windowLen - shift;
                for (int j = 0; j < len; j++) {
                    byte b = window[start];
                    out.write(b);
                    start++;
                    if (windowLen < bufSize) {
                        window[windowLen] = b;
                        windowLen++;
                    } else {
                        System.arraycopy(window, 1, window, 0, bufSize - 1);
                        window[bufSize - 1] = b;
                        start--;
                    }
                }
                out.write(next);
                if (windowLen < bufSize) {
                    window[windowLen] = next;
                    windowLen++;
                } else {
                    System.arraycopy(window, 1, window, 0, bufSize - 1);
                    window[bufSize - 1] = next;
                }
            }
            return out.toByteArray();
        }
    }

    public class LZSS {
        public static byte[] code(byte[] data, int bufSize) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeInt(data.length);
            int i = 0;
            while (i < data.length) {
                int flag = 0;
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                DataOutputStream bufOut = new DataOutputStream(buf);
                for (int bit = 0; bit < 8 && i < data.length; bit++) {
                    int matchLen = 0;
                    int shift = 0;
                    int start = Math.max(0, i - bufSize);
                    for (int j = start; j < i; j++) {
                        int len = 0;
                        while (i + len < data.length && data[j + len] == data[i + len] && len < 255) {
                            len++;
                        }
                        if (len > matchLen) {
                            matchLen = len;
                            shift = i - j;
                        }
                    }
                    if (matchLen >= 2) {
                        flag |= (1 << bit);
                        bufOut.writeShort(shift);
                        bufOut.writeByte(matchLen);
                        i += matchLen;
                    } else {
                        bufOut.writeByte(data[i]);
                        i++;
                    }
                }
                out.writeByte(flag);
                out.write(buf.toByteArray());
            }
            return baos.toByteArray();
        }

        public static byte[] decode(byte[] compressed, int bufSize) throws IOException {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(compressed));
            int size = in.readInt();
            byte[] result = new byte[size];
            int outPtr = 0;
            while (outPtr < size) {
                int flags = in.readByte() & 0xFF;
                for (int bit = 0; bit < 8 && outPtr < size; bit++) {
                    if ((flags & (1 << bit)) != 0) {
                        int offset = in.readShort() & 0xFFFF;
                        int length = in.readByte() & 0xFF;
                        int start = outPtr - offset;
                        for (int j = 0; j < length; j++) {
                            result[outPtr++] = result[start + j];
                        }
                    } else {
                        result[outPtr++] = in.readByte();
                    }
                }
            }
            return result;
        }
    }

    public static class LZ78 {
        public static byte[] code(byte[] data, int dictSize) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            Map<String, Integer> dict = new HashMap<>();
            int dictIndex = 1;
            StringBuilder currentStr = new StringBuilder();
            for (int i = 0; i < data.length; i++) {
                char c = (char) (data[i] & 0xFF);
                currentStr.append(c);
                if (!dict.containsKey(currentStr.toString())) {
                    String prefix = currentStr.substring(0, currentStr.length() - 1);
                    int prefixIndex = prefix.isEmpty() ? 0 : dict.get(prefix);
                    out.writeInt(prefixIndex);
                    out.writeByte(c);
                    if (dict.size() < dictSize) {
                        dict.put(currentStr.toString(), dictIndex++);
                    } else {
                        dict.clear();
                        dictIndex = 1;
                    }
                    currentStr.setLength(0);
                }
            }
            if (currentStr.length() > 0) {
                out.writeInt(dict.get(currentStr.toString()));
                out.writeByte(0);
            }
            return baos.toByteArray();
        }

        public static byte[] decode(byte[] compressed, int dictSize) throws IOException {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(compressed));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Map<Integer, String> dict = new HashMap<>();
            int dictIndex = 1;
            while (in.available() > 0) {
                int prefixIndex = in.readInt();
                byte c = in.readByte();
                String prefix = (prefixIndex == 0) ? "" : dict.get(prefixIndex);
                if (prefix != null) {
                    for (int i = 0; i < prefix.length(); i++) {
                        out.write((byte) prefix.charAt(i));
                    }
                }
                if (in.available() == 0 && c == 0) {
                    break;
                }
                out.write(c);
                String currentStr = prefix + (char) (c & 0xFF);
                if (dict.size() < dictSize) {
                    dict.put(dictIndex++, currentStr);
                } else {
                    dict.clear();
                    dictIndex = 1;
                }
            }
            return out.toByteArray();
        }
    }

    public static class LZW {
        public static byte[] code(byte[] data, int dictSize) throws IOException {
            if (data.length == 0) return new byte[0];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            Map<String, Integer> dict = new HashMap<>();
            int dictIndex = 256;
            for (int i = 0; i < 256; i++) {
                dict.put(String.valueOf((char) i), i);
            }
            StringBuilder currentStr = new StringBuilder();
            for (byte b : data) {
                char c = (char) (b & 0xFF);
                String strAndC = currentStr.toString() + c;

                if (dict.containsKey(strAndC)) {
                    currentStr.append(c);
                } else {
                    out.writeInt(dict.get(currentStr.toString()));

                    if (dict.size() < dictSize) {
                        dict.put(strAndC, dictIndex++);
                    } else {
                        dict.clear();
                        for (int i = 0; i < 256; i++) {
                            dict.put(String.valueOf((char) i), i);
                        }
                        dictIndex = 256;
                    }
                    currentStr.setLength(0);
                    currentStr.append(c);
                }
            }
            if (!currentStr.isEmpty()) {
                out.writeInt(dict.get(currentStr.toString()));
            }
            return baos.toByteArray();
        }

        public static byte[] decode(byte[] compressed, int dictSize) throws IOException {
            if (compressed.length == 0) return new byte[0];
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(compressed));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Map<Integer, String> dict = new HashMap<>();
            int dictIndex = 256;
            for (int i = 0; i < 256; i++) {
                dict.put(i, String.valueOf((char) i));
            }
            int oldCode = in.readInt();
            String s = dict.get(oldCode);
            for (int i = 0; i < s.length(); i++) {
                out.write((byte) s.charAt(i));
            }
            char c = s.charAt(0);
            while (in.available() > 0) {
                int newCode = in.readInt();
                String entry = "";
                if (dict.containsKey(newCode)) {
                    entry = dict.get(newCode);
                } else if (newCode == dictIndex) {
                    entry = s + c;
                }
                for (int j = 0; j < entry.length(); j++) {
                    out.write((byte) entry.charAt(j));
                }
                c = entry.charAt(0);
                if (dict.size() < dictSize) {
                    dict.put(dictIndex++, s + c);
                } else {
                    dict.clear();
                    for (int i = 0; i < 256; i++) {
                        dict.put(i, String.valueOf((char) i));
                    }
                    dictIndex = 256;
                }
                s = entry;
            }
            return out.toByteArray();
        }
    }
}