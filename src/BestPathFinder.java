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
            if (point.equals(to)) break;
            set.remove(point);
            int curd = dist.get(point);

            for (Direction direction : Util.DIRECTIONS) {
                Point next = point.go(direction);
                Board.Cell cell = board.get(next);
                if (cell != null && cell != Board.Cell.OBSTACLE) {
                    Integer curDist = dist.get(next);
                    int newDist = curd + cellWeight(cell);
                    if (curDist == null || curDist > newDist) {
                        dist.put(next, newDist);
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
            if (back == from) {
                Direction result = from.direction(cur);
                assert result != Direction.CURRENT_POINT : "To travel from " + from + " to " + to + " do nothing first, they said";
                return result;
            }
            cur = back;
        }
    }

    private int cellWeight(@NotNull Board.Cell cell) {
        switch (cell) {
            case FREE: return 5;
            case BONUS: return 1;
            case TROOPER: return 20;
            default: throw new IllegalStateException("Unexpected cell: " + cell);
        }
    }
}
