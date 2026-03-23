import java.util.*;
public class MTF {
    public static byte[] code(byte[] data) {
        LinkedList<Byte> list = new LinkedList<>();
        for (int i = 0; i < 256; i++) list.add((byte) i);
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            if ((i!=0) && (data[i] == data[i - 1])) {
                result[i] = 0;
            }
            else{
                byte cur = data[i]; int index = list.indexOf(cur);
                result[i] = (byte) index;
                list.remove(index);
                list.addFirst(cur);
            }
        }
        return result;
    }
    public static byte[] decode(byte[] data) {
        LinkedList<Byte> list = new LinkedList<>();
        for (int i = 0; i < 256; i++) list.add((byte) i);
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            int index = data[i] & 0xFF;
            if (index == 0) {
                result[i] = list.getFirst();
            }
            else{
                byte value = list.get(index);
                result[i] = value;
                list.remove(index);
                list.addFirst(value);
            }
        }
        return result;
    }
}