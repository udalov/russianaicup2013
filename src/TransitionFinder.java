import model.Direction;

import java.util.Map;
import java.util.Queue;

import static model.TrooperType.FIELD_MEDIC;

public class TransitionFinder {
    private final Situation situation;
    private final Position cur;
    private final Queue<Position> queue;
    private final Map<Position, Pair<Go, Position>> prev;

    public TransitionFinder(@NotNull Situation situation, @NotNull Position cur, @NotNull Queue<Position> queue,
                            @NotNull Map<Position, Pair<Go, Position>> prev) {
        this.situation = situation;
        this.cur = cur;
        this.queue = queue;
        this.prev = prev;
    }

    private void add(@NotNull Position next, @NotNull Go edge) {
        if (!prev.containsKey(next)) {
            prev.put(next, new Pair<>(edge, cur));
            queue.add(next);
        }
    }

    public void run() {
        // Field ration
        {
            Position next = cur.eatFieldRation();
            if (next != null) add(next, Go.eatFieldRation());
        }

        // Heal
        if (situation.self.type == FIELD_MEDIC) {
            for (Warrior ally : cur.allies) {
                Position next = cur.heal(ally);
                if (next != null) add(next, Go.heal(cur.me.direction(ally.point)));
            }
        }

        // Shoot
        for (EnemyWarrior enemy : situation.enemies) {
            Position next = cur.shoot(enemy);
            if (next != null) add(next, Go.shoot(enemy.point));
        }

        // Throw grenade
        for (Warrior enemy : situation.enemies) {
            for (Direction direction : Direction.values()) {
                Point target = enemy.point.go(direction);
                if (target != null) {
                    Position next = cur.throwGrenade(target);
                    if (next != null) add(next, Go.throwGrenade(target));
                }
            }
        }

        // Use medikit
        for (Warrior ally : cur.allies) {
            Position next = cur.useMedikit(ally);
            if (next != null) add(next, Go.useMedikit(cur.me.direction(ally.point)));
        }

        // Change stance
        {
            Position higher = cur.raiseStance();
            if (higher != null) add(higher, Go.raiseStance());

            Position lower = cur.lowerStance();
            if (lower != null) add(lower, Go.lowerStance());
        }

        // Move
        for (Direction direction : Util.DIRECTIONS) {
            Position next = cur.move(direction);
            if (next != null) add(next, Go.move(direction));
        }
    }
}
