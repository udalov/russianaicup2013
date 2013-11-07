import model.*;

public class MyStrategy implements Strategy {
    private static Army ARMY;

    @Override
    public void move(@NotNull Trooper self, @NotNull World world, @NotNull Game game, @NotNull Move move) {
        if (ARMY == null) {
            ARMY = new Army(self, world);
        }

        if (self.getType() == TrooperType.COMMANDER) {
            ARMY.setCommanderLocation(self);
        }

        new WarriorTurn(ARMY, self, world, game).makeTurn(move);
    }
}
