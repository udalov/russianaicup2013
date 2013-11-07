import model.Direction;
import model.Trooper;
import model.World;

public class Army {
    private final Point dislocation;

    public Army(@NotNull Trooper someTrooper, @NotNull World world) {
        Board board = new Board(world);

        Point p = new Point(world.getWidth() - someTrooper.getX(), world.getHeight() - someTrooper.getY());
        // TODO: prevent infinite loop
        while (!board.isPassable(p)) {
            p = p.go(Direction.SOUTH);
        }
        this.dislocation = p;
    }

    @NotNull
    public Point getDislocation() {
        return dislocation;
    }
}
