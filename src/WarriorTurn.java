import model.*;

import java.util.Random;

public class WarriorTurn {
    private final static Random RANDOM = new Random();

    private final Army army;
    private final Trooper self;
    private final World world;
    private final Game game;

    public WarriorTurn(@NotNull Army army, @NotNull Trooper self, @NotNull World world, @NotNull Game game) {
        this.army = army;
        this.self = self;
        this.world = world;
        this.game = game;
    }

    public void makeTurn(@NotNull Move move) {
        if (!canMove()) return;

        move.setAction(ActionType.MOVE);

        move.setDirection(Direction.values()[RANDOM.nextInt(Direction.values().length)]);
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
