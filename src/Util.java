import model.Direction;
import model.TrooperStance;

import java.util.*;

import static model.Direction.*;
import static model.TrooperStance.*;

public class Util {
    public static final List<Direction> DIRECTIONS = Arrays.asList(NORTH, EAST, SOUTH, WEST);

    private Util() {
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
    public static TrooperStance higher(@NotNull TrooperStance stance) {
        switch (stance) {
            case PRONE: return KNEELING;
            case KNEELING: return STANDING;
            case STANDING: return null;
            default: throw new IllegalStateException("You mean, you're flying? " + stance);
        }
    }

    @NotNull
    public static <T> List<T> reverse(@NotNull List<T> list) {
        ArrayList<T> result = new ArrayList<>(list);
        Collections.reverse(result);
        return result;
    }

    @NotNull
    public static <T> Iterable<T> iterable(@NotNull final AbstractIterator<T> iterator) {
        return new Iterable<T>() {
            @Override
            @NotNull
            public Iterator<T> iterator() {
                return iterator;
            }
        };
    }

    public static abstract class AbstractIterator<T> implements Iterator<T> {
        @Override
        public void remove() {
            throw new UnsupportedOperationException("Oh please");
        }
    }
}
