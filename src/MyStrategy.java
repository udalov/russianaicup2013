import model.Game;
import model.Move;
import model.Trooper;
import model.World;

public class MyStrategy implements Strategy {
    private static Army ARMY;

    @Override
    public void move(@NotNull Trooper self, @NotNull World world, @NotNull Game game, @NotNull Move move) {
        if (ARMY == null) {
            ARMY = new Army(world.getWidth(), world.getHeight());
        }
        new WarriorTurn(ARMY, self, world, game).makeTurn(move);
    }
}
