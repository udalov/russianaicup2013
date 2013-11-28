import model.Bonus;
import model.CellType;
import model.Direction;
import model.World;

import java.util.*;

public class Board {
    public enum Cell {
        FREE,
        BONUS,
        OBSTACLE
    }

    public abstract static class Controller {
        public abstract boolean isEndingPoint(@NotNull Point point);

        public void savePrevious(@NotNull Point from, @NotNull Point to) {}

        public void saveDistanceMap(@NotNull Map<Point, Integer> dist) {}
    }

    public static int WIDTH = -1;
    public static int HEIGHT = -1;

    private final Cell[] cells;

    public Board(@NotNull World world) {
        CellType[][] cells = world.getCells();
        int n = cells.length;
        int m = cells[0].length;
        assert n == WIDTH : "Wrong width: " + n + " != " + WIDTH;
        assert m == HEIGHT : "Wrong height: " + m + " != " + HEIGHT;

        this.cells = new Cell[n * m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                this.cells[i * m + j] = cells[i][j] == CellType.FREE ? Cell.FREE : Cell.OBSTACLE;
            }
        }
        for (Bonus bonus : world.getBonuses()) {
            this.cells[bonus.getX() * m + bonus.getY()] = Cell.BONUS;
        }
    }

    @NotNull
    public Cell get(@NotNull Point point) {
        return cells[point.x * HEIGHT + point.y];
    }

    public boolean isPassable(@NotNull Point point) {
        return get(point) != Cell.OBSTACLE;
    }

    @Nullable
    public List<Point> findPath(@NotNull Point from, @NotNull final Point to) {
        // TODO: optimize Map<Point, *>
        final Map<Point, Point> prev = new HashMap<>();

        launchDijkstra(from, new Controller() {
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
        launchDijkstra(from, new Controller() {
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
    public Point launchDijkstra(@NotNull Point from, @NotNull Controller controller) {
        final Map<Point, Integer> wd = new HashMap<>();
        final Map<Point, Integer> dist = new HashMap<>();
        controller.saveDistanceMap(dist);

        SortedSet<Point> set = new TreeSet<>(new Comparator<Point>() {
            @Override
            public int compare(@NotNull Point o1, @NotNull Point o2) {
                int d = wd.get(o1) - wd.get(o2);
                return d != 0 ? d : o1.compareTo(o2);
            }
        });
        wd.put(from, 0);
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

                Integer curDist = wd.get(next);
                int newDist = wd.get(point) + cellWeight(get(next));
                if (curDist == null || curDist > newDist) {
                    wd.put(next, newDist);
                    dist.put(next, dist.get(point) + 1);
                    controller.savePrevious(next, point);
                    set.add(next);
                }
            }
        }

        return null;
    }

    private int cellWeight(@NotNull Cell cell) {
        switch (cell) {
            case FREE: return 5;
            case BONUS: return 1;
            default: throw new IllegalStateException("You shall not pass: " + cell);
        }
    }
}
