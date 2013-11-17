import model.*;

import java.util.List;

public class MyStrategy implements Strategy {
    private static Army ARMY;

    @Override
    public void move(@NotNull Trooper self, @NotNull World world, @NotNull Game game, @NotNull Move move) {
        if (Board.WIDTH == -1) {
            // Not very great, but hey, they could've exposed these as public static final constants
            Board.WIDTH = world.getWidth();
            Board.HEIGHT = world.getHeight();
        }

        if (ARMY == null) {
            ARMY = new Army(self, world);
        }

        List<TrooperType> order = ARMY.getOrder();
        TrooperType myType = self.getType();
        if (!order.contains(myType)) {
            order.add(myType);
        }

        Go go = new WarriorTurn(ARMY, self, world, game).makeTurn();
        // TODO: only validate locally, do nothing or end turn on the server
        go.validate(self, world, game);
        go.execute(move);
    }
}
