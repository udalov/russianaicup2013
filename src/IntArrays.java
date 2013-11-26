import model.Trooper;

import java.util.Arrays;
import java.util.List;

public class IntArrays {
    public static final int[] EMPTY = {};

    private IntArrays() {}

    public static int sum(@NotNull int[] a) {
        int result = 0;
        for (int x : a) result += x;
        return result;
    }

    public static int numberOfZeros(@NotNull int[] a) {
        int result = 0;
        for (int x : a) if (x == 0) result++;
        return result;
    }

    @NotNull
    public static int[] replaceElement(@NotNull int[] a, int index, int value) {
        int[] result = Arrays.copyOf(a, a.length);
        result[index] = value;
        return result;
    }

    @NotNull
    public static int[] hitpointsOf(@NotNull List<Trooper> troopers) {
        int n = troopers.size();
        if (n == 0) return EMPTY;
        int[] result = new int[n];
        for (int i = 0; i < n; i++) {
            result[i] = troopers.get(i).getHitpoints();
        }
        return result;
    }
}
