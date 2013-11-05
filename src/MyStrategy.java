import model.Game;
import model.Move;
import model.Trooper;
import model.World;

public class MyStrategy implements Strategy {
    private static final Army ARMY = new Army();

    @Override
    public void move(@NotNull Trooper self, @NotNull World world, @NotNull Game game, @NotNull Move move) {
        new WarriorTurn(ARMY, self, world, game).makeTurn(move);
    }
}
