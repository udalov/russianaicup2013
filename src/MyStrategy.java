import model.Game;
import model.Move;
import model.Trooper;
import model.World;

import java.util.HashMap;
import java.util.Map;

public class MyStrategy implements Strategy {
    private static final Map<Long, Warrior> WARRIORS = new HashMap<>();
    private static final Army ARMY = new Army();

    @Override
    public void move(@NotNull Trooper self, @NotNull World world, @NotNull Game game, @NotNull Move move) {
        Warrior warrior = WARRIORS.get(self.getId());
        if (warrior == null) {
            warrior = new Warrior(ARMY);
            WARRIORS.put(self.getId(), warrior);
        }
        warrior.move(self, world, game, move);
    }
}
