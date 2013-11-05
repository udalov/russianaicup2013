import model.Direction;

public class Army {
    private final int worldWidth;
    private final int worldHeight;

    private Point dislocation;

    public Army(int worldWidth, int worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }

    @NotNull
    public Point getDislocation(@NotNull Board board) {
        if (dislocation == null) {
            Point p = new Point(worldWidth / 2, worldHeight / 2);
            while (!board.free(p.x, p.y)) {
                p = p.go(Direction.SOUTH);
            }
            dislocation = p;
        }
        return dislocation;
    }
}
