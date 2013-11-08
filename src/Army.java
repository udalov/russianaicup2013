import model.Direction;
import model.Trooper;
import model.TrooperType;
import model.World;

import java.util.*;

public class Army {
    private final Point dislocation;
    private final List<TrooperType> order = new ArrayList<>(TrooperType.values().length);

    public Army(@NotNull Trooper firstTrooper, @NotNull World world) {
        this.dislocation = findFreePointNearby(world, new Point(world.getWidth() / 2, world.getHeight() / 2));
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

    @NotNull
    public List<TrooperType> getOrder() {
        return order;
    }
}
