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
        Board board = new Board(world);
        Point start = Point.byUnit(firstTrooper);
        Point center = Point.center();
        this.dislocations = new ArrayList<>();
        dislocations.add(findFreePointNearby(board, start));
        dislocations.add(findFreePointNearby(board, start.halfwayTo(center)));
        dislocations.add(findFreePointNearby(board, center));
        dislocations.add(findFreePointNearby(board, center.halfwayTo(start.opposite())));
        dislocations.add(findFreePointNearby(board, start.opposite()));
        dislocations.add(findFreePointNearby(board, start.horizontalOpposite()));
        dislocations.add(findFreePointNearby(board, center));
        dislocations.add(findFreePointNearby(board, start.verticalOpposite()));

        this.curDisIndex = 0;
    }

    @NotNull
    private Point findFreePointNearby(@NotNull Board board, @NotNull Point p) {
        Set<Point> visited = new HashSet<>();
        Queue<Point> queue = new LinkedList<>();
        queue.offer(p);
        while (!queue.isEmpty()) {
            Point cur = queue.poll();
            Board.Cell cell = board.get(cur);
            if (cell != null && cell != Board.Cell.OBSTACLE) return cur;
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
