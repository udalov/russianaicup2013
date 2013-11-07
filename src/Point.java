import model.Direction;
import model.Unit;

import static model.Direction.*;

public final class Point implements Comparable<Point> {
    public final int x;
    public final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @NotNull
    public Point go(@NotNull Direction direction) {
        switch (direction) {
            case CURRENT_POINT: return this;
            case NORTH: return new Point(x, y - 1);
            case EAST: return new Point(x + 1, y);
            case SOUTH: return new Point(x, y + 1);
            case WEST: return new Point(x - 1, y);
            default: throw new IllegalStateException("Unknown direction: " + direction);
        }
    }

    @NotNull
    public static Point byUnit(@NotNull Unit unit) {
        return new Point(unit.getX(), unit.getY());
    }

    @NotNull
    public Direction direction(@NotNull Point neighbor) {
        int dx = neighbor.x - x;
        if (dx > 0) return EAST;
        if (dx < 0) return WEST;
        int dy = neighbor.y - y;
        if (dy > 0) return SOUTH;
        if (dy < 0) return NORTH;
        return CURRENT_POINT;
    }

    public boolean isNeighbor(@NotNull Point that) {
        return manhattanDistance(that) == 1;
    }

    public int manhattanDistance(@NotNull Point that) {
        return Math.abs(x - that.x) + Math.abs(y - that.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point)) return false;
        Point point = (Point) o;
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode() {
        return 239 * x + y;
    }

    @Override
    public int compareTo(@NotNull Point o) {
        int dx = x - o.x;
        return dx != 0 ? dx : y - o.y;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
