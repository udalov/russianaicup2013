import model.Game;
import model.Trooper;
import model.TrooperStance;
import model.World;

import java.util.*;

import static model.BonusType.*;
import static model.TrooperStance.STANDING;

public class MakeTurn {
    private final Army army;
    private final Trooper self;
    private final World world;
    private final Game game;

    private final Point me;
    private final List<Trooper> enemies;
    private final List<Trooper> allies;

    public MakeTurn(@NotNull Army army, @NotNull Trooper self, @NotNull World world, @NotNull Game game) {
        this.army = army;
        this.self = self;
        this.world = world;
        this.game = game;

        me = Point.create(self);
        List<Trooper> enemies = null;
        allies = new ArrayList<>(5);
        for (Trooper trooper : world.getTroopers()) {
            if (trooper.isTeammate()) {
                allies.add(trooper);
            } else {
                if (enemies == null) enemies = new ArrayList<>(15);
                enemies.add(trooper);
            }
        }

        TurnLocalData data = army.loadTurnLocalData(world.getMoveIndex(), self.getType());
        if (data == null) {
            data = new TurnLocalData();
            army.saveTurnLocalData(world.getMoveIndex(), self.getType(), data);
        }
        this.enemies = data.updateEnemies(enemies == null ? Collections.<Trooper>emptyList() : enemies);
    }

    @NotNull
    public Go makeTurn() {
        Situation situation = new Situation(game, world, army, self.getType(), allies, enemies, Arrays.asList(world.getBonuses()));

        Position start = new Position(
                situation,
                me,
                self.getStance(),
                self.getActionPoints(),
                computeBonusesBitSet(self),
                IntArrays.hitpointsOf(enemies),
                IntArrays.hitpointsOf(allies),
                IntArrays.EMPTY,
                computeSeenForSituation(situation)
        );

        List<Go> best = best(situation, start).second;
        debug(situation.scorer, best);
        return best.isEmpty() ? Go.endTurn() : best.iterator().next();
    }

    @NotNull
    public static Pair<Position, List<Go>> best(@NotNull Situation situation, @NotNull Position start) {
        // TODO: ArrayDeque?
        final Queue<Position> queue = new LinkedList<>();
        queue.add(start);
        final Map<Position, Pair<Go, Position>> prev = new HashMap<>();
        prev.put(start, null);

        Position best = null;
        double bestValue = -1e100;
        while (!queue.isEmpty()) {
            Position cur = queue.poll();

            double curValue = situation.scorer.evaluate(cur);
            if (curValue > bestValue) {
                bestValue = curValue;
                best = cur;
            }

            new TransitionFinder(situation, cur, queue, prev).run();
        }

        assert best != null : "Where's best? " + start;

        if (best == start) return new Pair<>(best, Collections.<Go>emptyList());

        Position cur = best;
        List<Go> result = new ArrayList<>(12);
        while (true) {
            Pair<Go, Position> before = prev.get(cur);
            assert before != null : "Nothing before " + cur;
            result.add(before.first);
            if (before.second == start) return new Pair<>(best, Util.reverse(result));
            cur = before.second;
        }
    }

    @NotNull
    public static PointSet computeSeenForSituation(@NotNull Situation situation) {
        PointSet result = new PointSet();
        for (Warrior ally : situation.allies) {
            Point point = ally.point;
            TrooperStance stance = ally.stance;
            double visionRange = ally.getVisionRange();
            for (Point object : situation.board.allPassable()) {
                if (situation.isReachable(visionRange, point, stance, object, STANDING)) {
                    result.add(object);
                }
            }
        }
        return result;
    }

    @NotNull
    public static PointSet computeSeenForPosition(@NotNull Situation situation, @NotNull Point viewer, @NotNull TrooperStance viewerStance,
                                                  @NotNull PointSet given) {
        if (situation.lightVersion) return given;

        double visionRange = situation.self.getVisionRange();
        PointSet result = null;
        for (Point object : situation.board.allPassable()) {
            if (situation.isReachable(visionRange, viewer, viewerStance, object, STANDING)) {
                if (!given.contains(object)) {
                    if (result == null) {
                        result = given.copy();
                    }
                    result.add(object);
                }
            }
        }

        return result != null ? result : given;
    }

    public static int computeBonusesBitSet(@NotNull Trooper self) {
        int bonuses = 0;
        if (self.isHoldingGrenade()) bonuses |= 1 << GRENADE.ordinal();
        if (self.isHoldingMedikit()) bonuses |= 1 << MEDIKIT.ordinal();
        if (self.isHoldingFieldRation()) bonuses |= 1 << FIELD_RATION.ordinal();
        return bonuses;
    }

    @NotNull
    @Override
    public String toString() {
        return self + ", turn #" + world.getMoveIndex();
    }

    private void debug(@NotNull Scorer scorer, @NotNull List<Go> best) {
        if (!Debug.ENABLED) return;

        String s;
        if (scorer instanceof Scorer.CombatSituation) s = "combat";
        else if (scorer instanceof Scorer.Leader) s = "leader";
        else if (scorer instanceof Scorer.Follower) s = "follower";
        else throw new UnsupportedOperationException("Unknown scorer: " + scorer);

        Debug.log(world.getMoveIndex() + " " + s + ": " + self + " -> " + best);
    }
}
