import model.Direction;
import model.Trooper;

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
        if (situation.self.getType() == FIELD_MEDIC) {
            for (Pair<Integer, Point> pair : cur.allies()) {
                Position next = cur.heal(pair.first, pair.second);
                if (next != null) add(next, Go.heal(cur.me.direction(pair.second)));
            }
        }

        // Shoot
        for (int i = 0, size = situation.enemies.size(); i < size; i++) {
            Position next = cur.shoot(i);
            if (next != null) add(next, Go.shoot(Point.create(situation.enemies.get(i))));
        }

        // Throw grenade
        for (Trooper enemy : situation.enemies) {
            Point point = Point.create(enemy);
            for (Direction direction : Direction.values()) {
                Point target = point.go(direction);
                if (target != null) {
                    Position next = cur.throwGrenade(target);
                    if (next != null) add(next, Go.throwGrenade(target));
                }
            }
        }

        // Use medikit
        for (Pair<Integer, Point> pair : cur.allies()) {
            Position next = cur.useMedikit(pair.first, pair.second);
            if (next != null) add(next, Go.useMedikit(cur.me.direction(pair.second)));
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
