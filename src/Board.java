import model.CellType;
import model.Direction;
import model.World;

import java.util.*;

public class Board {
    public abstract static class Controller {
        public abstract boolean isEndingPoint(@NotNull Point point);

        public void savePrevious(@NotNull Point from, @NotNull Point to) {}

        public void saveDistanceMap(@NotNull Map<Point, Integer> dist) {}
    }

    public static int WIDTH = -1;
    public static int HEIGHT = -1;

    private final PointSet obstacles;

    public Board(@NotNull World world) {
        CellType[][] cells = world.getCells();
        int n = cells.length;
        int m = cells[0].length;
        assert n == WIDTH : "Wrong width: " + n + " != " + WIDTH;
        assert m == HEIGHT : "Wrong height: " + m + " != " + HEIGHT;

        obstacles = new PointSet();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (cells[i][j] != CellType.FREE) {
                    //noinspection ConstantConditions
                    obstacles.add(Point.create(i, j));
                }
            }
        }
    }

    public boolean isPassable(@NotNull Point point) {
        return !obstacles.contains(point);
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

        // TODO: drop
        SortedSet<Point> set = new TreeSet<>(new Comparator<Point>() {
            @Override
            public int compare(@NotNull Point o1, @NotNull Point o2) {
                int d = dist.get(o1) - dist.get(o2);
                return d != 0 ? d : o1.compareTo(o2);
            }
        });
        dist.put(from, 0);
        set.add(from);

        while (!set.isEmpty()) {
            Point point = set.first();
            if (controller.isEndingPoint(point)) {
                return point;
            }
            set.remove(point);

            for (Direction direction : Util.DIRECTIONS) {
                Point next = point.go(direction);
                if (next == null || !isPassable(next)) continue;

                Integer curDist = dist.get(next);
                int newDist = dist.get(point) + 1;
                if (curDist == null || curDist > newDist) {
                    dist.put(next, newDist);
                    controller.savePrevious(next, point);
                    set.add(next);
                }
            }
        }

        return null;
    }
}
