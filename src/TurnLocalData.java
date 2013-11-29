import model.Trooper;

import java.util.ArrayList;
import java.util.List;

public class TurnLocalData {
    // TODO: also store bonuses seen in the beginning of the turn?
    private final List<Trooper> enemies = new ArrayList<>(15);

    @NotNull
    public List<Trooper> updateEnemies(@NotNull List<Trooper> moreEnemies) {
        outer: for (Trooper enemy : moreEnemies) {
            for (int i = 0, size = enemies.size(); i < size; i++) {
                Trooper trooper = enemies.get(i);
                if (trooper.getId() == enemy.getId()) {
                    // Just in case
                    enemies.set(i, enemy);
                    continue outer;
                }
            }
            enemies.add(enemy);
        }
        return enemies;
    }
}
