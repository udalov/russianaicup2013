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
    private final Map<Point, Map<Point, Integer>> distances = new PointMap<>();

    private final Queue<Point> queue = new ArrayDeque<>(WIDTH * HEIGHT);

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
