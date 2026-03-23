import java.io.*;
import java.util.*;
public class Huffman {
    static class Node implements Comparable<Node> {
        byte value;
        int freq;
        Node left, right;
        Node(byte value, int freq) { this.value = value; this.freq = freq; }
        Node(int freq, Node left, Node right) { this.freq = freq; this.left = left; this.right = right; }
        boolean isLeaf() { return left == null && right == null; }
        @Override
        public int compareTo(Node other) { return this.freq - other.freq; }
    }

    static class Writer {
        private OutputStream out;
        private int buff = 0;
        private int bits = 0;
        public Writer(OutputStream out) { this.out = out; }
        public void writeBit(int bit) throws IOException {
            buff = (buff << 1) | bit;
            bits++;
            if (bits == 8) {
                out.write(buff);
                buff = 0;
                bits = 0;
            }
        }
        public void end() throws IOException {
            if (bits > 0) {
                out.write(buff << (8 - bits));
            }
            out.flush();
        }
    }

    static class Reader {
        private InputStream in;
        private int buff = 0;
        private int bits = 0;
        public Reader(InputStream in) { this.in = in; }
        public int readBit() throws IOException {
            if (bits == 0) {
                buff = in.read();
                if (buff == -1) return -1;
                bits = 8;
            }
            int bit = (buff >> (bits - 1)) & 1;
            bits--;
            return bit;
        }
    }

    public static byte[] code(byte[] data, Map<Byte, Integer> freq) throws IOException {
        if (data.length == 0) return new byte[0];
        Node root = buildTree(freq);
        Map<Byte, String> codes = new HashMap<>();
        generateCodes(root, "", codes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer bw = new Writer(baos);
        for (byte b : data) {
            String code = codes.get(b);
            for (int i = 0; i < code.length(); i++) {
                bw.writeBit(code.charAt(i) == '1' ? 1 : 0);
            }
        }
        bw.end();
        return baos.toByteArray();
    }

    public static byte[] decode(byte[] comp, Map<Byte, Integer> freq) throws IOException {
        Node root = buildTree(freq);
        ByteArrayInputStream bais = new ByteArrayInputStream(comp);
        Reader br = new Reader(bais);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (true) {
            Node cur = root;
            boolean isEOF = false;
            while (!cur.isLeaf()) {
                int bit = br.readBit();
                if (bit == -1) {
                    isEOF = true;
                    break;
                }
                cur = (bit == 0) ? cur.left : cur.right;
            }
            if (cur.isLeaf()) {
                out.write(cur.value);
            }
            if (isEOF) {
                break;
            }
        }
        return out.toByteArray();
    }

    private static Node buildTree(Map<Byte, Integer> freq) {
        PriorityQueue<Node> pq = new PriorityQueue<>();
        for (var entry : freq.entrySet()) {
            pq.add(new Node(entry.getKey(), entry.getValue()));
        }
        while (pq.size() > 1) {
            Node left = pq.poll();
            Node right = pq.poll();
            pq.add(new Node(left.freq + right.freq, left, right));
        }
        return pq.poll();
    }

    private static void generateCodes(Node node, String code, Map<Byte, String> codes) {
        if (node == null) return;
        if (node.isLeaf()) {
            codes.put(node.value, code);
            return;
        }
        generateCodes(node.left, code + "0", codes);
        generateCodes(node.right, code + "1", codes);
    }

    private static void fillLen(Node node, int depth, Map<Byte, Integer> lengths) {
        if (node == null) return;
        if (node.isLeaf()) {
            lengths.put(node.value, depth);
            return;
        }
        fillLen(node.left, depth + 1, lengths);
        fillLen(node.right, depth + 1, lengths);
    }

    public static Map<Byte, Integer> getLen(Node root) {
        Map<Byte, Integer> lengths = new HashMap<>();
        fillLen(root, 0, lengths);
        return lengths;
    }

    public static Map<Byte, String> createCanon(Map<Byte, Integer> lengths) {
        Map<Byte, String> codes = new HashMap<>();
        int minLen = 32, maxLen = 0;
        for (int len : lengths.values()) {
            if (len < minLen) minLen = len;
            if (len > maxLen) maxLen = len;
        }
        long code = 0;
        int lastLen = minLen;
        boolean first = true;
        for (int curLen = minLen; curLen <= maxLen; curLen++) {
            for (int i = 0; i < 256; i++) {
                byte b = (byte) i;
                if (lengths.getOrDefault(b, 0) == curLen) {
                    if (first) {
                        code = 0;
                        first = false;
                    } else {
                        code = (code + 1) << (curLen - lastLen);
                    }
                    lastLen = curLen;
                    String str = Long.toBinaryString(code);
                    while (str.length() < curLen) str = "0" + str;
                    codes.put(b, str);
                }
            }
        }
        return codes;
    }

    public static byte[] codeCanon(byte[] data, Map<Byte, Integer> lengths) throws IOException {
        Map<Byte, String> codes = createCanon(lengths);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer writer = new Writer(baos);
        for (byte b : data) {
            String code = codes.get(b);
            for (int i = 0; i < code.length(); i++) {
                writer.writeBit(code.charAt(i) == '1' ? 1 : 0);
            }
        }
        writer.end();
        return baos.toByteArray();
    }

    public static byte[] decodeCanon(byte[] compressed, Map<Byte, Integer> lengths) throws IOException {
        Map<Byte, String> codes = createCanon(lengths);
        Map<String, Byte> reversecodes = new HashMap<>();
        for (var entry : codes.entrySet()) reversecodes.put(entry.getValue(), entry.getKey());
        Reader reader = new Reader(new ByteArrayInputStream(compressed));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StringBuilder curBit = new StringBuilder();
        while (true) {
            int bit = reader.readBit();
            if (bit == -1) break;
            curBit.append(bit);
            if (reversecodes.containsKey(curBit.toString())) {
                out.write(reversecodes.get(curBit.toString()));
                curBit.setLength(0);
            }
        }
        return out.toByteArray();
    }
}