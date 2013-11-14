import model.Direction;
import model.Trooper;
import model.TrooperType;
import model.World;

import java.util.*;

public class Army {
    private List<Point> dislocations;
    private int curDisIndex;

    private final List<TrooperType> order = new ArrayList<>(TrooperType.values().length);

    public Army(@NotNull Trooper firstTrooper, @NotNull World world) {
        int w = world.getWidth();
        int h = world.getHeight();
        int x = firstTrooper.getX();
        int y = firstTrooper.getY();

        this.dislocations = new ArrayList<>();
        dislocations.add(findFreePointNearby(world, Point.byUnit(firstTrooper)));
        dislocations.add(findFreePointNearby(world, new Point(w / 2, h / 2)));
        dislocations.add(findFreePointNearby(world, new Point(w - x, h - y)));
        dislocations.add(findFreePointNearby(world, new Point(w - x, y)));
        dislocations.add(findFreePointNearby(world, new Point(w / 2, h / 2)));
        dislocations.add(findFreePointNearby(world, new Point(w, h - y)));

        this.curDisIndex = 0;
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
        throw new IllegalStateException("Impossible as it may seem, there are no free points nearby: " + p);
    }

    @NotNull
    public Point getOrUpdateDislocation(@NotNull Collection<Trooper> allies) {
        Point dislocation = dislocations.get(curDisIndex);
        int curDist = 0;
        for (Trooper ally : allies) {
            curDist += Point.byUnit(ally).manhattanDistance(dislocation);
        }
        if (curDist < 4 * allies.size()) {
            curDisIndex = (curDisIndex + 1) % dislocations.size();
            dislocation = dislocations.get(curDisIndex);
        }
        return dislocation;
    }

    @NotNull
    public List<TrooperType> getOrder() {
        return order;
    }
}
