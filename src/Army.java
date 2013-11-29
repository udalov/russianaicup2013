import model.Direction;
import model.Trooper;
import model.TrooperType;
import model.World;

import java.util.*;

public class Army {
    private final List<Point> wayPoints = new ArrayList<>();
    private int curWayPoint;

    private final Board board;

    private final List<TrooperType> order = new ArrayList<>(TrooperType.values().length);

    private int medicSelfHealed;

    private final Map<TrooperType, Pair<Integer, TurnLocalData>> turnLocalData = new HashMap<>();

    public Army(@NotNull Trooper firstTrooper, @NotNull World world) {
        board = new Board(world);
        buildWayPoints(firstTrooper);

        curWayPoint = 0;

        Debug.log("map: " + board.getKind());
    }

    private void buildWayPoints(Trooper firstTrooper) {
        Point start = Point.create(firstTrooper);
        Point center = Point.center();

        wayPoint(start);
        wayPoint(start.halfwayTo(center));
        wayPoint(center);
        wayPoint(center.halfwayTo(start.opposite()));
        wayPoint(start.opposite());
        wayPoint(start.horizontalOpposite());
        wayPoint(center);
        wayPoint(start.verticalOpposite());
    }

    private boolean wayPoint(@NotNull Point point) {
        return wayPoints.add(findFreePointNearby(point));
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
    public Point getOrUpdateWayPoint(@NotNull Collection<Trooper> allies) {
        Point wayPoint = wayPoints.get(curWayPoint);
        int curDist = 0;
        for (Trooper ally : allies) {
            curDist += Point.create(ally).manhattanDistance(wayPoint);
        }
        if (curDist < 4 * allies.size()) {
            curWayPoint = (curWayPoint + 1) % wayPoints.size();
            wayPoint = wayPoints.get(curWayPoint);
        }
        return wayPoint;
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
