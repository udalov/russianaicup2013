import model.Direction;
import model.Trooper;
import model.TrooperType;
import model.World;

import java.util.*;

import static model.TrooperType.COMMANDER;

public class Army {
    private final List<Point> wayPoints = new ArrayList<>(25);
    private int curWayPoint;

    public final Board board;
    public final Const coeff;

    private final List<TrooperType> order = new ArrayList<>(TrooperType.values().length);
    private boolean isOrderComplete;

    private final Map<TrooperType, Pair<Integer, TurnLocalData>> turnLocalData = new HashMap<>();

    public Army(@NotNull World world) {
        board = new Board(world);
        coeff = Const.valueOf(board.kind.toString());

        Trooper commander = null;
        for (Trooper trooper : world.getTroopers()) {
            if (trooper.getType() == COMMANDER && trooper.isTeammate()) {
                commander = trooper;
            }
        }
        assert commander != null : "Where's commander? " + Arrays.toString(world.getTroopers());

        buildWayPoints(commander);

        curWayPoint = 0;

        Debug.log("map: " + board.kind);
    }

    private void buildWayPoints(@NotNull Trooper commander) {
        final Point start = Point.create(commander);

        class WayPoints {
            private final Point[] loc = new Point[25];

            public WayPoints() {
                loc[0] = start;
                loc[4] = start.horizontalOpposite();
                loc[20] = start.verticalOpposite();
                loc[24] = start.opposite();
                loc[12] = Point.center();

                between(0, 2, 4);
                between(0, 10, 20);
                between(4, 14, 24);
                between(20, 22, 24);

                for (int x : Arrays.asList(5, 7, 9, 15, 17, 19)) {
                    between(x - 5, x, x + 5);
                }

                for (int x : Arrays.asList(1, 3, 6, 8, 11, 13, 16, 18, 21, 23)) {
                    between(x - 1, x, x + 1);
                }
            }

            private void between(int first, int result, int second) {
                loc[result] = loc[first].halfwayTo(loc[second]);
            }

            public void set(@NotNull int... wp) {
                for (int i : wp) {
                    wayPoints.add(findFreePointNearby(loc[i]));
                }
            }
        }

        WayPoints wp = new WayPoints();

        /*
          0  1  2  3  4
          5  6  7  8  9
         10 11 12 13 14
         15 16 17 18 19
         20 21 22 23 24
         */
        if (board.kind == Board.Kind.CHEESER) {
            wp.set(0, 6, 2, 8, 14, 18, 24, 14, 4, 8, 2, 6, 10, 16, 20, 10);
        } else if (board.kind == Board.Kind.FEFER) {
            wp.set(0, 6, 0, 1, 2, 7, 2, 3, 4, 24, 12, 20);
        } else if (board.kind == Board.Kind.MAP01) {
            wp.set(0, 6, 16, 6, 2, 4, 13, 24, 22, 17, 16, 6);
        } else if (board.kind == Board.Kind.MAP02) {
            wp.set(0, 11, 1, 2, 1, 11, 20, 22, 24, 13, 4, 2);
        } else if (board.kind == Board.Kind.MAP03) {
            wp.set(0, 1, 2, 3, 4, 3, 2, 7, 11, 15, 22, 19, 13, 7);
        } else if (board.kind == Board.Kind.MAP05) {
            wp.set(0, 2, 4, 2, 0, 6, 16, 20, 22, 24, 18, 3);
        } else if (board.kind == Board.Kind.MAP06) {
            // Start in 6, not 0
            wp.set(6, 7, 8, 7, 12, 17, 18, 16, 17, 12, 14, 10, 0, 2, 7);
        } else {
            wp.set(0, 6, 12, 18, 24, 4, 12, 20);
        }
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
    public Point getOrUpdateWayPoint(@NotNull Situation situation) {
        Collection<Warrior> allies = situation.allies;
        Point wayPoint = wayPoints.get(curWayPoint);
        int curDist = 0;
        for (Warrior ally : allies) {
            curDist += ally.point.manhattanDistance(wayPoint);
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

    public boolean isOrderComplete() {
        return isOrderComplete;
    }

    public void completeOrder() {
        assert !this.isOrderComplete : "Don't do this twice";
        this.isOrderComplete = true;
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
