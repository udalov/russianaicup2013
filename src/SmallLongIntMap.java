import java.util.Arrays;

public class SmallLongIntMap {
    private static final long NO_ELEMENT = -System.currentTimeMillis();

    private final long[] longs = new long[3];
    private final int[] ints = new int[3];

    public SmallLongIntMap() {
        Arrays.fill(longs, NO_ELEMENT);
    }

    public int get(long x) {
        for (int i = 0; i < 3; i++) {
            if (longs[i] == x) {
                return ints[i];
            }
        }
        return 0;
    }

    public void inc(long x) {
        for (int i = 0; i < 3; i++) {
            if (longs[i] == x) {
                ints[i]++;
                return;
            } else if (longs[i] == NO_ELEMENT) {
                longs[i] = x;
                ints[i] = 1;
                return;
            }
        }
    }
}
