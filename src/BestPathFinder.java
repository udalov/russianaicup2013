import model.Direction;

import java.util.*;

public class BestPathFinder {
    private final Board board;

    public BestPathFinder(@NotNull Board board) {
        this.board = board;
    }

    @Nullable
    public Direction findFirstMove(@NotNull Point from, @NotNull Point to) {
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

        Map<Point, Point> prev = new HashMap<>();

        while (!set.isEmpty()) {
            Point point = set.first();
            if (point == to) break;
            set.remove(point);
            int curd = dist.get(point);

            for (Direction direction : Util.DIRECTIONS) {
                Point next = point.go(direction);
                if (board.free(next.x, next.y)) {
                    Integer d = dist.get(next);
                    if (d == null || d > curd + 1) {
                        dist.put(next, curd + 1);
                        prev.put(next, point);
                        set.add(next);
                    }
                }
            }
        }

        Point cur = to;
        while (true) {
            Point back = prev.get(cur);
            if (back == null) return null;
            if (back == from) return from.direction(cur);
            cur = back;
        }
    }
}
