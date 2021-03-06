import model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyStrategy implements Strategy {
    private static final Map<Long, Army> ARMIES = new HashMap<>(6);

    @Override
    public void move(@NotNull Trooper self, @NotNull World world, @NotNull Game game, @NotNull Move move) {
        if (Board.WIDTH == -1) {
            // Not very great, but hey, they could've exposed these as public static final constants
            Board.WIDTH = world.getWidth();
            Board.HEIGHT = world.getHeight();
        }

        long id = self.getPlayerId();
        Army army = ARMIES.get(id);
        if (army == null) {
            army = new Army(world);
            ARMIES.put(id, army);
        }

        List<TrooperType> order = army.getOrder();
        TrooperType myType = self.getType();
        if (!order.contains(myType)) {
            order.add(myType);
        } else if (!army.isOrderComplete()) {
            army.completeOrder();
        }

        Go go = new MakeTurn(army, self, world, game).makeTurn();
        // go.validate(self, world, game);
        go.execute(move);
    }
}
