import model.Direction;
import model.Trooper;
import model.TrooperType;
import model.World;

import java.util.*;

public class Army {
    private final List<Point> dislocations = new ArrayList<>();
    private int curDisIndex;

    private final Board firstBoard;
    private final Map<Point, Map<Point, Integer>> distances = new HashMap<>(802);

    private final List<TrooperType> order = new ArrayList<>(TrooperType.values().length);
    private final Map<TrooperType, Inbox> inboxes = new HashMap<>();

    private int medicSelfHealed;

    public Army(@NotNull Trooper firstTrooper, @NotNull World world) {
        firstBoard = new Board(world);
        Point start = Point.create(firstTrooper);
        Point center = Point.center();
        dislocations.add(findFreePointNearby(start));
        dislocations.add(findFreePointNearby(start.halfwayTo(center)));
        dislocations.add(findFreePointNearby(center));
        dislocations.add(findFreePointNearby(center.halfwayTo(start.opposite())));
        dislocations.add(findFreePointNearby(start.opposite()));
        dislocations.add(findFreePointNearby(start.horizontalOpposite()));
        dislocations.add(findFreePointNearby(center));
        dislocations.add(findFreePointNearby(start.verticalOpposite()));

        curDisIndex = 0;

        for (Trooper trooper : world.getTroopers()) {
            inboxes.put(trooper.getType(), new Inbox());
        }
    }

    @NotNull
    private Point findFreePointNearby(@NotNull Point p) {
        Set<Point> visited = new PointSet();
        Queue<Point> queue = new LinkedList<>();
        queue.offer(p);
        while (!queue.isEmpty()) {
            Point cur = queue.poll();
            if (firstBoard.get(cur) != Board.Cell.OBSTACLE) return cur;
            for (Direction direction : Util.DIRECTIONS) {
                Point next = cur.go(direction);
                if (next != null && !visited.contains(next)) {
                    visited.add(next);
                    queue.offer(next);
                }
            }
        }
        throw new IllegalStateException("Impossible as it may seem, there are no free points nearby: " + p);
    }

    @Nullable
    public Integer getDistanceOnEmptyBoard(@NotNull Point from, @NotNull Point to) {
        Map<Point, Integer> map = distances.get(from);
        if (map == null) {
            map = firstBoard.findDistances(from, true);
            distances.put(from, map);
        }
        return map.get(to);
    }

    @NotNull
    public Point getOrUpdateDislocation(@NotNull Collection<Trooper> allies) {
        Point dislocation = dislocations.get(curDisIndex);
        int curDist = 0;
        for (Trooper ally : allies) {
            curDist += Point.create(ally).manhattanDistance(dislocation);
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

    public void sendMessage(@NotNull Trooper ally, @NotNull Message message, int timeToLive) {
        inboxes.get(ally.getType()).add(message, timeToLive);
    }

    @NotNull
    public Inbox getMessages(@NotNull Trooper ally) {
        return inboxes.get(ally.getType());
    }

    public boolean allowMedicSelfHealing() {
        return medicSelfHealed < 30;
    }

    public void medicSelfHealed() {
        medicSelfHealed++;
    }
}
