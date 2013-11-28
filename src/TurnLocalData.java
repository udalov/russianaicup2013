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
}
