import model.*;

import java.util.*;

public class Board {
    // Not very accurate: in case when a cell contains both a trooper and a bonus, it's TROOPER
    public enum Cell {
        FREE,
        BONUS,
        TROOPER,
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
        for (Trooper trooper : world.getTroopers()) {
            this.cells[trooper.getX() * m + trooper.getY()] = Cell.TROOPER;
        }
    }

    @NotNull
    public Cell get(@NotNull Point point) {
        return cells[point.x * HEIGHT + point.y];
    }

    @Nullable
    public Direction findBestMove(@NotNull Point from, @NotNull final Point to, final boolean moveThroughPeople) {
        // TODO: optimize Map<Point, *>
        final Map<Point, Point> prev = new HashMap<>();

        launchDijkstra(from, moveThroughPeople, new Controller() {
            @Override
            public boolean isEndingPoint(@NotNull Point point) {
                return point.equals(to);
            }

            @Override
            public void savePrevious(@NotNull Point from, @NotNull Point to) {
                prev.put(from, to);
            }
        });

        Point cur = to;
        while (true) {
            Point back = prev.get(cur);
            if (back == null) return null;
            if (back == from) {
                Direction result = from.direction(cur);
                assert result != Direction.CURRENT_POINT : "To travel from " + from + " to " + to + " do nothing first, they said";
                return result;
            }
            cur = back;
        }
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public Map<Point, Integer> findDistances(@NotNull Point from, boolean moveThroughPeople) {
        final Map[] result = new Map[1];
        launchDijkstra(from, moveThroughPeople, new Controller() {
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
    public Integer findDistanceTo(@NotNull Point from, @NotNull final Point to, boolean moveThroughPeople) {
        final Map[] result = new Map[1];
        launchDijkstra(from, moveThroughPeople, new Controller() {
            @Override
            public boolean isEndingPoint(@NotNull Point point) {
                return point.equals(to);
            }

            @Override
            public void saveDistanceMap(@NotNull Map<Point, Integer> dist) {
                result[0] = dist;
            }
        });
        return (Integer) result[0].get(from);
    }

    @Nullable
    public Point launchDijkstra(@NotNull Point from, boolean moveThroughPeople, @NotNull Controller controller) {
        final Map<Point, Integer> dist = new HashMap<>();
        controller.saveDistanceMap(dist);

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
            int curd = dist.get(point);

            for (Direction direction : Util.DIRECTIONS) {
                Point next = point.go(direction);
                if (next == null) continue;

                Cell cell = get(next);
                if (cell != Cell.OBSTACLE) {
                    Integer curDist = dist.get(next);
                    int newDist = curd + cellWeight(cell, moveThroughPeople);
                    if (curDist == null || curDist > newDist) {
                        dist.put(next, newDist);
                        controller.savePrevious(next, point);
                        set.add(next);
                    }
                }
            }
        }

        return null;
    }

    private int cellWeight(@NotNull Cell cell, boolean moveThroughPeople) {
        switch (cell) {
            case FREE: return 5;
            case BONUS: return 1;
            case TROOPER: return moveThroughPeople ? 5 : 100;
            default: throw new IllegalStateException("Unexpected cell: " + cell);
        }
    }
}
