import model.Direction;

import java.util.Arrays;
import java.util.List;

import static model.Direction.*;

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
}
