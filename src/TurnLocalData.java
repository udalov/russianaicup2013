import model.Trooper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TurnLocalData {
    private final Iterator<Go> predefinedMoves;
    private final List<Long> enemyIds;

    public TurnLocalData(@NotNull Iterator<Go> predefinedMoves, @NotNull List<Long> enemyIds) {
        this.predefinedMoves = predefinedMoves;
        this.enemyIds = enemyIds;
    }

    @Nullable
    public Go nextMove() {
        return predefinedMoves.hasNext() ? predefinedMoves.next() : null;
    }

    @NotNull
    public List<Long> getEnemyIds() {
        return enemyIds;
    }

    @NotNull
    public static List<Long> ids(@NotNull List<Trooper> troopers) {
        List<Long> result = new ArrayList<>(troopers.size());
        for (Trooper trooper : troopers) {
            result.add(trooper.getId());
        }
        return result;
    }
}
