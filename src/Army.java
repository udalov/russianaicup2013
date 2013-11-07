import model.Direction;
import model.Trooper;
import model.World;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class Army {
    private final Point dislocation;

    public Army(@NotNull Trooper someTrooper, @NotNull World world) {
        this.dislocation = findFreePointNearby(world, new Point(world.getWidth() - someTrooper.getX(), world.getHeight() - someTrooper.getY()));
    }

    @NotNull
    private Point findFreePointNearby(@NotNull World world, @NotNull Point p) {
        Board board = new Board(world);
        Set<Point> visited = new HashSet<>();
        Queue<Point> queue = new LinkedList<>();
        queue.offer(p);
        while (!queue.isEmpty()) {
            Point cur = queue.poll();
            if (board.isPassable(cur)) return cur;
            for (Direction direction : Util.DIRECTIONS) {
                Point next = cur.go(direction);
                if (!visited.contains(next)) {
                    visited.add(next);
                    queue.offer(next);
                }
            }
        }
        throw new IllegalStateException("Impossible as it may seem, there are no free points nearby");
    }

    @NotNull
    public Point getDislocation() {
        return dislocation;
    }
}
