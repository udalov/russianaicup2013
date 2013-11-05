import model.*;

import java.util.Random;

public class MyStrategy implements Strategy {
    private final Random random = new Random();

    @Override
    public void move(Trooper self, World world, Game game, Move move) {
        if (self.getActionPoints() < game.getStandingMoveCost()) return;

        move.setAction(ActionType.MOVE);

        move.setDirection(Direction.values()[random.nextInt(Direction.values().length)]);
    }
}
