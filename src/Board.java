import model.CellType;
import model.Direction;
import model.World;

import java.util.*;

public class Board {
    public enum Kind {
        UNKNOWN(0),
        DEFAULT(49318912),
        CHEESER(43536704),
        FEFER(-608025536),
        MAP01(-2071390976),
        MAP02(-1500897472),
        MAP03(-1006608576),
        MAP04(1693721344),
        MAP05(2031338624),
        MAP06(-1450008832);

        private final int hashCode;

        private Kind(int hashCode) {
            this.hashCode = hashCode;
        }

        @NotNull
        public static Kind byHashCode(int hashCode) {
            for (Kind kind : values()) {
                if (kind.hashCode == hashCode) {
                    return kind;
                }
            }
            return UNKNOWN;
        }
    }

    public abstract static class Controller {
        public abstract boolean isEndingPoint(@NotNull Point point);

        public void savePrevious(@NotNull Point from, @NotNull Point to) {}

        public void saveDistanceMap(@NotNull Map<Point, Integer> dist) {}
    }

    public static int WIDTH = -1;
    public static int HEIGHT = -1;

    private final Kind kind;
    private final List<Point> passable = new ArrayList<>(WIDTH * HEIGHT);
    private final Set<Point> obstacles = new PointSet();
    private final Map<Point, Map<Point, Integer>> distances = new PointMap<>();

    private final Queue<Point> queue = new ArrayDeque<>(WIDTH * HEIGHT);

    public Board(@NotNull World world) {
        CellType[][] cells = world.getCells();
        int n = cells.length;
        int m = cells[0].length;
        assert n == WIDTH : "Wrong width: " + n + " != " + WIDTH;
        assert m == HEIGHT : "Wrong height: " + m + " != " + HEIGHT;

        this.kind = Kind.byHashCode(worldMapHashCode(cells));

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                Point point = Point.create(i, j);
                if (cells[i][j] == CellType.FREE) {
                    passable.add(point);
                } else {
                    //noinspection ConstantConditions
                    obstacles.add(point);
                }
            }
        }
    }

    @NotNull
    public Kind getKind() {
        return kind;
    }

    @NotNull
    public Iterable<Point> allPassable() {
        return passable;
    }

    private int worldMapHashCode(@NotNull CellType[][] cells) {
        int result = 0;
        for (CellType[] row : cells) {
            for (CellType cell : row) {
                result = result * 31 + cell.ordinal();
            }
        }
        return result;
    }

    public boolean isPassable(@NotNull Point point) {
        return !obstacles.contains(point);
    }

    @Nullable
    public Integer distance(@NotNull Point from, @NotNull Point to) {
        Map<Point, Integer> map = distances.get(from);
        if (map == null) {
            map = findDistances(from);
            distances.put(from, map);
        }
        return map.get(to);
    }

    @Nullable
    public List<Point> findPath(@NotNull Point from, @NotNull final Point to) {
        final Map<Point, Point> prev = new PointMap<>();

        bfs(from, new Controller() {
            @Override
            public boolean isEndingPoint(@NotNull Point point) {
                return point.equals(to);
            }

            @Override
            public void savePrevious(@NotNull Point from, @NotNull Point to) {
                prev.put(from, to);
            }
        });

        List<Point> result = new ArrayList<>();
        result.add(to);

        Point cur = to;
        while (true) {
            Point back = prev.get(cur);
            if (back == null) return null;
            if (back.equals(from)) return Util.reverse(result);
            result.add(back);
            cur = back;
        }
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public Map<Point, Integer> findDistances(@NotNull Point from) {
        final Map[] result = new Map[1];
        bfs(from, new Controller() {
            @Override
            public boolean isEndingPoint(@NotNull Point point) {
                return false;
            }

            @Override
            public void saveDistanceMap(@NotNull Map<Point, Integer> dist) {
                result[0] = dist;
            }
        });
        return result[0];
    }

    @Nullable
    public Point bfs(@NotNull Point from, @NotNull Controller controller) {
        final Map<Point, Integer> dist = new PointMap<>();
        controller.saveDistanceMap(dist);

        dist.put(from, 0);
        queue.clear();
        queue.add(from);

        while (!queue.isEmpty()) {
            Point point = queue.poll();
            if (controller.isEndingPoint(point)) return point;

            int newDist = dist.get(point) + 1;

            for (Direction direction : Util.DIRECTIONS) {
                Point next = point.go(direction);
                if (next != null && isPassable(next) && !dist.containsKey(next)) {
                    dist.put(next, newDist);
                    controller.savePrevious(next, point);
                    queue.add(next);
                }
            }
        }

        return null;
    }
}
