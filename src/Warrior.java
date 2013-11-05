import model.*;

import java.util.Random;

public class Warrior {
    private final Random random = new Random();

    private final Army army;

    public Warrior(@NotNull Army army) {
        this.army = army;
    }

    public void move(@NotNull Trooper self, @NotNull World world, @NotNull Game game, @NotNull Move move) {
        if (self.getActionPoints() < game.getStandingMoveCost()) return;

        move.setAction(ActionType.MOVE);

        move.setDirection(Direction.values()[random.nextInt(Direction.values().length)]);
    }
}
