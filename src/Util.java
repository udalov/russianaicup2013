import model.Direction;
import model.TrooperStance;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static model.Direction.*;
import static model.TrooperStance.KNEELING;
import static model.TrooperStance.PRONE;

public class Util {
    public static final List<Direction> DIRECTIONS = Arrays.asList(NORTH, EAST, SOUTH, WEST);

    private Util() {
    }

    @NotNull
    public static Direction inverse(@NotNull Direction direction) {
        switch (direction) {
            case CURRENT_POINT: return CURRENT_POINT;
            case NORTH: return SOUTH;
            case EAST: return WEST;
            case SOUTH: return NORTH;
            case WEST: return EAST;
            default: throw new IllegalStateException("Oh please: " + direction);
        }
    }

    public static boolean anything(@NotNull Object... objects) {
        for (Object object : objects) {
            if (object != null) return true;
        }
        return false;
    }

    @Nullable
    public static TrooperStance lower(@NotNull TrooperStance stance) {
        switch (stance) {
            case PRONE: return null;
            case KNEELING: return PRONE;
            case STANDING: return KNEELING;
            default: throw new IllegalStateException("You mean, you're flying? " + stance);
        }
    }

    @Nullable
    public static <T> T findMin(@NotNull Collection<? extends T> elements, @NotNull Evaluator<? super T> evaluator) {
        if (elements.isEmpty()) return null;
        if (elements.size() == 1) return elements.iterator().next();

        T best = null;
        int bestValue = Integer.MAX_VALUE;
        for (T element : elements) {
            Integer cur = evaluator.evaluate(element);
            if (cur != null && (best == null || cur < bestValue)) {
                best = element;
                bestValue = cur;
            }
        }

        return best;
    }

    @Nullable
    public static <T> T findMax(@NotNull Collection<? extends T> elements, @NotNull final Evaluator<? super T> evaluator) {
        return findMin(elements, new Evaluator<T>() {
            @Nullable
            @Override
            public Integer evaluate(@NotNull T t) {
                Integer result = evaluator.evaluate(t);
                if (result == null) return null;
                if (result == Integer.MIN_VALUE) return Integer.MAX_VALUE;
                return -result;
            }
        });
    }

    public interface Evaluator<Element> {
        @Nullable
        Integer evaluate(@NotNull Element element);
    }
}
