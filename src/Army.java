import model.Direction;
import model.Trooper;
import model.TrooperType;
import model.World;

import java.util.*;

public class Army {
    private final List<Point> dislocations = new ArrayList<>();
    private int curDisIndex;

    private final Board board;

    private final List<TrooperType> order = new ArrayList<>(TrooperType.values().length);

    private int medicSelfHealed;

    private final Map<TrooperType, Pair<Integer, TurnLocalData>> turnLocalData = new HashMap<>();

    public Army(@NotNull Trooper firstTrooper, @NotNull World world) {
        board = new Board(world);
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
    }

    @NotNull
    private Point findFreePointNearby(@NotNull Point p) {
        Set<Point> visited = new PointSet();
        Queue<Point> queue = new LinkedList<>();
        queue.offer(p);
        while (!queue.isEmpty()) {
            Point cur = queue.poll();
            if (board.isPassable(cur)) return cur;
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

    @NotNull
    public Board getBoard() {
        return board;
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

    public boolean allowMedicSelfHealing() {
        return medicSelfHealed < 30;
    }

    public void medicSelfHealed() {
        medicSelfHealed++;
    }

    @Nullable
    public TurnLocalData loadTurnLocalData(int moveIndex, @NotNull TrooperType type) {
        Pair<Integer, TurnLocalData> pair = turnLocalData.get(type);
        return pair == null || pair.first != moveIndex ? null : pair.second;
    }

    public void saveTurnLocalData(int moveIndex, @NotNull TrooperType type, @NotNull TurnLocalData data) {
        turnLocalData.put(type, new Pair<>(moveIndex, data));
    }
}
