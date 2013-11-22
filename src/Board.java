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
    }

    public static int WIDTH = -1;
    public static int HEIGHT = -1;

    private final Cell[][] cells;

    public Board(@NotNull World world) {
        CellType[][] cells = world.getCells();
        int n = cells.length;
        int m = cells[0].length;
        assert n == WIDTH : "Wrong width: " + n + " != " + WIDTH;
        assert m == HEIGHT : "Wrong height: " + m + " != " + HEIGHT;

        this.cells = new Cell[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                this.cells[i][j] = cells[i][j] == CellType.FREE ? Cell.FREE : Cell.OBSTACLE;
            }
        }
        for (Bonus bonus : world.getBonuses()) {
            this.cells[bonus.getX()][bonus.getY()] = Cell.BONUS;
        }
        for (Trooper trooper : world.getTroopers()) {
            this.cells[trooper.getX()][trooper.getY()] = Cell.TROOPER;
        }
    }

    @Nullable
    public Cell get(@NotNull Point point) {
        int x = point.x;
        int y = point.y;
        return 0 <= x && 0 <= y && x < WIDTH && y < HEIGHT ? cells[x][y] : null;
    }

    @Nullable
    public Direction findBestMove(@NotNull Point from, @NotNull final Point to) {
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

    @Nullable
    public Point launchDijkstra(@NotNull Point from, @NotNull Controller controller) {
        final Map<Point, Integer> dist = new HashMap<>();
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
                Cell cell = get(next);
                if (cell != null && cell != Cell.OBSTACLE) {
                    Integer curDist = dist.get(next);
                    int newDist = curd + cellWeight(cell);
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

    private int cellWeight(@NotNull Board.Cell cell) {
        switch (cell) {
            case FREE: return 5;
            case BONUS: return 1;
            case TROOPER: return 100;
            default: throw new IllegalStateException("Unexpected cell: " + cell);
        }
    }
}
