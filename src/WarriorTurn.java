import model.*;

import java.util.Random;

public class WarriorTurn {
    private final static Random RANDOM = new Random();

    private final Army army;
    private final Trooper self;
    private final World world;
    private final Game game;

    private final Board board;

    public WarriorTurn(@NotNull Army army, @NotNull Trooper self, @NotNull World world, @NotNull Game game) {
        this.army = army;
        this.self = self;
        this.world = world;
        this.game = game;

        this.board = new Board(world.getCells());
    }

    public void makeTurn(@NotNull Move move) {
        if (!canMove()) return;

        Direction direction = move();
        if (direction == null) {
            direction = Util.DIRECTIONS[RANDOM.nextInt(Util.DIRECTIONS.length)];
        }

        move.setAction(ActionType.MOVE);
        move.setDirection(direction);
    }

    @Nullable
    private Direction move() {
        return new BestPathFinder(board).findFirstMove(Point.byUnit(self), army.getDislocation(board));
    }

    private boolean canMove() {
        return self.getActionPoints() >= getMoveCost();
    }

    private int getMoveCost() {
        TrooperStance stance = self.getStance();
        switch (stance) {
            case PRONE: return game.getProneMoveCost();
            case KNEELING: return game.getKneelingMoveCost();
            case STANDING: return game.getStandingMoveCost();
            default: throw new IllegalStateException("Unknown stance: " + stance);
        }
    }
}
