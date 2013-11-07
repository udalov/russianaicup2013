import model.Direction;
import model.Trooper;
import model.World;

import java.util.Arrays;

public class Army {
    private final Point dislocation;

    public Army(@NotNull Trooper someTrooper, @NotNull World world) {
        this.dislocation = findFreePointNearby(world, new Point(world.getWidth() - someTrooper.getX(), world.getHeight() - someTrooper.getY()));
    }

    @NotNull
    private Point findFreePointNearby(@NotNull World world, @NotNull Point p) {
        Board board = new Board(world);
        for (Direction direction : Util.DIRECTIONS) {
            for (int it = 0; it < 10; it++) {
                if (board.isPassable(p)) return p;
                p = p.go(direction);
            }
        }
        throw new IllegalStateException("Impossible as it may seem, there are no free points nearby");
    }

    @NotNull
    public Point getDislocation() {
        return dislocation;
    }
}
