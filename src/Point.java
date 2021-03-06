import model.Direction;
import model.Unit;

import static model.Direction.*;

public final class Point {
    public final int x;
    public final int y;

    // TODO: create 30x20 points in the beginning and reuse them?
    private Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Nullable
    public Point go(@NotNull Direction direction) {
        switch (direction) {
            case CURRENT_POINT: return this;
            case NORTH: return create(x, y - 1);
            case EAST: return create(x + 1, y);
            case SOUTH: return create(x, y + 1);
            case WEST: return create(x - 1, y);
            default: throw new IllegalStateException("Unknown direction: " + direction);
        }
    }

    @Nullable
    public static Point create(int x, int y) {
        return 0 <= x && x < Board.WIDTH && 0 <= y && y < Board.HEIGHT ? new Point(x, y) : null;
    }

    @NotNull
    public static Point create(@NotNull Unit unit) {
        return new Point(unit.getX(), unit.getY());
    }

    public int index() {
        return x * Board.HEIGHT + y;
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

    public double euclideanDistance(@NotNull Point that) {
        return Math.hypot(x - that.x, y - that.y);
    }

    public boolean withinEuclidean(@NotNull Point that, double distance) {
        return (x - that.x) * (x - that.x) + (y - that.y) * (y - that.y) <= distance * distance;
    }

    private static Point center;

    @NotNull
    public static Point center() {
        if (center == null) {
            center = new Point(Board.WIDTH / 2, Board.HEIGHT / 2);
        }
        return center;
    }

    @NotNull
    public Point opposite() {
        return new Point(Board.WIDTH - 1 - x, Board.HEIGHT - 1 - y);
    }

    @NotNull
    public Point horizontalOpposite() {
        return new Point(Board.WIDTH - 1 - x, y);
    }

    @NotNull
    public Point verticalOpposite() {
        return new Point(x, Board.HEIGHT - 1 - y);
    }

    @NotNull
    public Point halfwayTo(@NotNull Point other) {
        return new Point((x + other.x) / 2, (y + other.y) / 2);
    }

    public boolean isEqualTo(@NotNull Unit unit) {
        return unit.getX() == x && unit.getY() == y;
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
        return index() + 1;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
