import model.Direction;
import model.Trooper;
import model.World;

public class Army {
    private final Board board;
    private final Point dislocation;
    private Point commanderLocation;

    public Army(@NotNull Trooper someTrooper, @NotNull World world) {
        this.board = new Board(world.getCells());

        Point p = new Point(world.getWidth() - someTrooper.getX(), world.getHeight() - someTrooper.getY());
        while (!board.free(p.x, p.y)) {
            p = p.go(Direction.SOUTH);
        }
        this.dislocation = p;
    }

    @NotNull
    public Point getDislocation(@NotNull Board board) {
        return dislocation;
    }

    @NotNull
    public Point getCommanderLocation() {
        return commanderLocation;
    }

    public void setCommanderLocation(@NotNull Trooper commander) {
        commanderLocation = Point.byUnit(commander);
    }
}
