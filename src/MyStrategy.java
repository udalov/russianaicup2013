import model.*;

import java.util.List;

public class MyStrategy implements Strategy {
    private static Army ARMY;

    @Override
    public void move(@NotNull Trooper self, @NotNull World world, @NotNull Game game, @NotNull Move move) {
        if (ARMY == null) {
            ARMY = new Army(self, world);
        }

        List<TrooperType> order = ARMY.getOrder();
        TrooperType myType = self.getType();
        if (!order.contains(myType)) {
            order.add(myType);
        }

        new WarriorTurn(ARMY, self, world, game).makeTurn().execute(move);
    }
}
