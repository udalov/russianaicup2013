import model.Direction;
import model.TrooperStance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
}
