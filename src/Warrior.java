import model.*;

import java.util.Random;

public class Warrior {
    private final Random random = new Random();

    private final Army army;

    public Warrior(Army army) {
        this.army = army;
    }

    public void move(Trooper self, World world, Game game, Move move) {
        if (self.getActionPoints() < game.getStandingMoveCost()) return;

        move.setAction(ActionType.MOVE);

        move.setDirection(Direction.values()[random.nextInt(Direction.values().length)]);
    }
}
