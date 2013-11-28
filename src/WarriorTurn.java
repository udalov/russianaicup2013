import model.*;

import java.util.*;

import static model.BonusType.*;
import static model.TrooperType.*;

public class WarriorTurn {
    private static final boolean LOCAL = Thread.currentThread().getName().equals("local");

    private final Army army;
    private final Trooper self;
    private final World world;
    private final Game game;

    private final Point me;
    private final List<Bonus> worldBonuses;
    private final Board board;
    private final List<Trooper> enemies;
    private final List<Trooper> allies;
    private final Map<TrooperType, Trooper> alliesMap;
    // Index of me in the 'allies' list
    private final int myIndex;

    public WarriorTurn(@NotNull Army army, @NotNull Trooper self, @NotNull World world, @NotNull Game game) {
        this.army = army;
        this.self = self;
        this.world = world;
        this.game = game;

        me = Point.create(self);
        worldBonuses = Arrays.asList(world.getBonuses());
        board = new Board(world);
        List<Trooper> enemies = null;
        allies = new ArrayList<>(5);
        alliesMap = new EnumMap<>(TrooperType.class);
        int myIndex = -1;
        for (Trooper trooper : world.getTroopers()) {
            if (trooper.isTeammate()) {
                if (trooper.getType() == self.getType()) {
                    myIndex = allies.size();
                }
                allies.add(trooper);
                alliesMap.put(trooper.getType(), trooper);
            } else {
                if (enemies == null) enemies = new ArrayList<>(15);
                enemies.add(trooper);
            }
        }
        assert myIndex >= 0 : "Where am I? " + allies;
        this.myIndex = myIndex;

        this.enemies = enemies == null ? Collections.<Trooper>emptyList() : enemies;
    }

    @NotNull
    public Go makeTurn() {
        TurnLocalData data = army.loadTurnLocalData(world.getMoveIndex(), self.getType());
        // If the board changed between subsequent turns (new enemies are visible), we don't use the old found moves, since they're not the best anymore
        // TODO: take into account all enemies that were seen during the small iteration, don't just delete them
        if (data != null && data.getEnemyIds().equals(ids(enemies))) {
            Go predefined = data.nextMove();
            if (predefined != null) {
                debug("predefined: " + self + " -> " + predefined);
                return predefined;
            }
        }

        Scorer scorer;

        if (!enemies.isEmpty()) {
            scorer = new CombatSituationScorer();
        } else {
            TrooperType leader = findLeader().getType();
            if (leader == self.getType()) {
                scorer = new LeaderScorer();
            } else {
                scorer = new FollowerScorer(leader);
            }
        }

        List<Go> best = best(scorer);
        debug(scorer, best);
        Iterator<Go> it = best.iterator();
        if (!it.hasNext()) return Go.endTurn();
        army.saveTurnLocalData(world.getMoveIndex(), self.getType(), new TurnLocalData(it, ids(enemies)));
        return it.next();
    }

    @NotNull
    private List<Go> best(@NotNull Scorer scorer) {
        Position start = startingPosition();
        // TODO: ArrayDeque?
        final Queue<Position> queue = new LinkedList<>();
        queue.add(start);
        final Map<Position, Pair<Go, Position>> prev = new HashMap<>();
        prev.put(start, null);

        Position best = null;
        double bestValue = -1e100;
        while (!queue.isEmpty()) {
            Position cur = queue.poll();

            double curValue = scorer.evaluate(cur);
            if (curValue > bestValue) {
                bestValue = curValue;
                best = cur;
            }

            new TransitionFinder(cur, queue, prev).run();
        }

        if (best == start) return Collections.emptyList();

        Position cur = best;
        List<Go> result = new ArrayList<>(12);
        while (true) {
            Pair<Go, Position> before = prev.get(cur);
            assert before != null : "Nothing before " + cur;
            result.add(before.first);
            if (before.second == start) return Util.reverse(result);
            cur = before.second;
        }
    }

    private final class TransitionFinder {
        private final Position cur;
        private final Queue<Position> queue;
        private final Map<Position, Pair<Go, Position>> prev;

        public TransitionFinder(@NotNull Position cur, @NotNull Queue<Position> queue, @NotNull Map<Position, Pair<Go, Position>> prev) {
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
            if (self.getType() == FIELD_MEDIC) {
                for (Pair<Integer, Point> pair : cur.allies()) {
                    Position next = cur.heal(pair.first, pair.second);
                    if (next != null) add(next, Go.heal(cur.me.direction(pair.second)));
                }
            }

            // Shoot
            for (int i = 0, size = enemies.size(); i < size; i++) {
                Position next = cur.shoot(i);
                if (next != null) add(next, Go.shoot(Point.create(enemies.get(i))));
            }

            // Throw grenade
            for (Trooper enemy : enemies) {
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

    public final class Position {
        public final Point me;
        public final TrooperStance stance;
        public final int actionPoints;
        // Indexed by BonusType.ordinal()
        public final int bonuses;
        // Indexed by WarriorTurn.enemies
        // TODO: create and save list of points of troopers?
        public final int[] enemyHp;
        // Indexed by WarriorTurn.allies
        public final int[] allyHp;
        // id of collected bonuses
        public final int[] collected;

        private final int hashCode;

        public Position(@NotNull Point me, @NotNull TrooperStance stance, int actionPoints, int bonuses, @NotNull int[] enemyHp, @NotNull int[] allyHp,
                        @NotNull int[] collected) {
            this.me = me;
            this.stance = stance;
            this.actionPoints = actionPoints;
            this.bonuses = bonuses;
            this.enemyHp = enemyHp;
            this.allyHp = allyHp;
            this.collected = collected;

            int hash = me.hashCode();
            hash = 31 * hash + stance.hashCode();
            hash = 31 * hash + actionPoints;
            hash = 31 * hash + bonuses;
            hash = 31 * hash + Arrays.hashCode(enemyHp);
            hash = 31 * hash + Arrays.hashCode(allyHp);
            hash = 31 * hash + Arrays.hashCode(collected);
            this.hashCode = hash;
        }

        @NotNull
        public Iterable<Pair<Integer, Point>> allies() {
            return Util.iterable(new Util.AbstractIterator<Pair<Integer, Point>>() {
                private int i = 0;

                @Override
                public boolean hasNext() {
                    return i < allies.size();
                }

                @Override
                public Pair<Integer, Point> next() {
                    Point ally = i == myIndex ? me : Point.create(allies.get(i));
                    return new Pair<>(i++, ally);
                }
            });
        }

        private boolean has(@NotNull BonusType bonus) {
            return (bonuses & (1 << bonus.ordinal())) != 0;
        }

        private int without(@NotNull BonusType bonus) {
            return bonuses & ~(1 << bonus.ordinal());
        }

        private int with(@NotNull BonusType bonus) {
            return bonuses | (1 << bonus.ordinal());
        }

        private boolean isPassablePoint(@NotNull Point point) {
            if (!board.isPassable(point)) return false;
            for (Pair<Integer, Point> pair : allies()) {
                if (point.equals(pair.second)) return false;
            }
            for (Trooper enemy : enemies) {
                if (point.isEqualTo(enemy)) return false;
            }
            return true;
        }

        private double effectiveShootingRange() {
            double result = self.getShootingRange();
            if (self.getType() == SNIPER) {
                result -= sniperShootingRangeBonus(self.getStance());
                result += sniperShootingRangeBonus(stance);
            }
            return result;
        }

        private double sniperShootingRangeBonus(TrooperStance stance) {
            switch (stance) {
                case STANDING: return 0;
                case KNEELING: return game.getSniperKneelingShootingRangeBonus();
                case PRONE: return game.getSniperProneShootingRangeBonus();
                default: throw new IllegalStateException("Sniper is so stealth, he's " + stance);
            }
        }

        @NotNull
        private int[] grenadeEffectToEnemies(@NotNull Point target) {
            int[] result = IntArrays.copy(enemyHp);
            for (int i = 0, size = enemies.size(); i < size; i++) {
                result[i] = grenadeEffectToTrooper(target, Point.create(enemies.get(i)), enemyHp[i]);
            }
            return result;
        }

        @NotNull
        private int[] grenadeEffectToAllies(@NotNull Point target) {
            int[] result = IntArrays.copy(allyHp);
            for (Pair<Integer, Point> pair : allies()) {
                int i = pair.first;
                result[i] = grenadeEffectToTrooper(target, pair.second, allyHp[i]);
            }
            return result;
        }

        private int grenadeEffectToTrooper(@NotNull Point target, @NotNull Point trooper, int hitpoints) {
            if (trooper.equals(target)) {
                return Math.max(hitpoints - game.getGrenadeDirectDamage(), 0);
            } else if (trooper.isNeighbor(target)) {
                return Math.max(hitpoints - game.getGrenadeCollateralDamage(), 0);
            } else {
                return hitpoints;
            }
        }

        @NotNull
        private int[] healEffect(int ally, int healingBonus) {
            // Relies on the fact that maximal hitpoints are the same for every trooper
            int maxHp = self.getMaximalHitpoints();
            int hp = allyHp[ally];
            if (hp >= maxHp) return allyHp;

            return IntArrays.replace(allyHp, ally, Math.min(hp + healingBonus, maxHp));
        }

        @Nullable
        private Bonus maybeCollectBonus(@NotNull Point point) {
            for (Bonus bonus : worldBonuses) {
                if (!point.isEqualTo(bonus)) continue;
                if (has(bonus.getType())) continue;
                int id = (int) bonus.getId();
                return IntArrays.contains(collected, id) ? null : bonus;
            }
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Position)) return false;
            Position that = (Position) o;

            return hashCode == that.hashCode &&
                    actionPoints == that.actionPoints &&
                    bonuses == that.bonuses &&
                    stance == that.stance &&
                    me.equals(that.me) &&
                    Arrays.equals(allyHp, that.allyHp) &&
                    Arrays.equals(enemyHp, that.enemyHp) &&
                    Arrays.equals(collected, that.collected);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public String toString() {
            return actionPoints + " " + stance + " at " + me;
        }

        // -------------------------------------------------------------------------

        @Nullable
        public Position move(@NotNull Direction direction) {
            int ap = actionPoints - getMoveCost(stance);
            if (ap < 0) return null;
            Point point = me.go(direction);
            if (point == null || !isPassablePoint(point)) return null;
            Bonus bonus = maybeCollectBonus(point);
            int newBonuses = bonus == null ? bonuses : with(bonus.getType());
            int[] newCollected = bonus == null ? collected : IntArrays.add(collected, (int) bonus.getId());
            return new Position(point, stance, ap, newBonuses, enemyHp, allyHp, newCollected);
        }

        @Nullable
        public Position shoot(int enemy) {
            int ap = actionPoints - self.getShootCost();
            if (ap < 0) return null;
            Trooper trooper = enemies.get(enemy);
            if (!isReachable(effectiveShootingRange(), me, stance, trooper)) return null;
            int hp = enemyHp[enemy];
            if (hp == 0) return null;
            int[] newEnemyHp = IntArrays.replace(enemyHp, enemy, Math.max(hp - self.getDamage(stance), 0));
            return new Position(me, stance, ap, bonuses, newEnemyHp, allyHp, collected);
        }

        @Nullable
        public Position raiseStance() {
            int ap = actionPoints - game.getStanceChangeCost();
            if (ap < 0) return null;
            TrooperStance newStance = Util.higher(stance);
            if (newStance == null) return null;
            return new Position(me, newStance, ap, bonuses, enemyHp, allyHp, collected);
        }

        @Nullable
        public Position lowerStance() {
            int ap = actionPoints - game.getStanceChangeCost();
            if (ap < 0) return null;
            TrooperStance newStance = Util.lower(stance);
            if (newStance == null) return null;
            return new Position(me, newStance, ap, bonuses, enemyHp, allyHp, collected);
        }

        @Nullable
        public Position throwGrenade(@NotNull Point target) {
            if (!has(GRENADE)) return null;
            int ap = actionPoints - game.getGrenadeThrowCost();
            if (ap < 0) return null;
            if (!me.withinEuclidean(target, game.getGrenadeThrowRange())) return null;
            int[] newEnemyHp = grenadeEffectToEnemies(target);
            if (Arrays.equals(enemyHp, newEnemyHp)) return null;
            int[] newAllyHp = grenadeEffectToAllies(target);
            return new Position(me, stance, ap, without(GRENADE), newEnemyHp, newAllyHp, collected);
        }

        @Nullable
        public Position useMedikit(int ally, @NotNull Point point) {
            if (!has(MEDIKIT)) return null;
            int ap = actionPoints - game.getMedikitUseCost();
            if (ap < 0) return null;
            int[] newAllyHp;
            if (point.equals(me)) newAllyHp = healEffect(ally, game.getMedikitHealSelfBonusHitpoints());
            else if (point.isNeighbor(me)) newAllyHp = healEffect(ally, game.getMedikitBonusHitpoints());
            else return null;
            if (Arrays.equals(allyHp, newAllyHp)) return null;
            return new Position(me, stance, ap, without(MEDIKIT), enemyHp, newAllyHp, collected);
        }

        @Nullable
        public Position eatFieldRation() {
            if (!has(FIELD_RATION)) return null;
            if (actionPoints >= self.getInitialActionPoints()) return null;
            int ap = actionPoints - game.getFieldRationEatCost();
            if (ap < 0) return null;
            return new Position(me, stance, ap, without(FIELD_RATION), enemyHp, allyHp, collected);
        }

        @Nullable
        public Position heal(int ally, @NotNull Point point) {
            int ap = actionPoints - game.getFieldMedicHealCost();
            if (ap < 0) return null;
            int[] newAllyHp;
            if (point.equals(me)) newAllyHp = healEffect(ally, game.getFieldMedicHealSelfBonusHitpoints());
            else if (point.isNeighbor(me)) newAllyHp = healEffect(ally, game.getFieldMedicHealBonusHitpoints());
            else return null;
            if (Arrays.equals(allyHp, newAllyHp)) return null;
            return new Position(me, stance, ap, without(MEDIKIT), enemyHp, newAllyHp, collected);
        }
    }

    @NotNull
    public Position startingPosition() {
        int bonuses = 0;
        for (BonusType bonus : BonusType.values()) {
            if (isHolding(bonus)) bonuses |= 1 << bonus.ordinal();
        }
        return new Position(
                me,
                self.getStance(),
                self.getActionPoints(),
                bonuses,
                IntArrays.hitpointsOf(enemies),
                IntArrays.hitpointsOf(allies),
                IntArrays.EMPTY
        );
    }

    private abstract class Scorer {
        public final double evaluate(@NotNull Position p) {
            double result = 0;

            result += weightedHpOfAllies(p.allyHp);

            // TODO: or if have a medikit
            if (self.getType() == FIELD_MEDIC) {
                result -= 0.5 * distanceToWoundedAllies(p);
            }

            result += situation(p);

            return result;
        }

        private double weightedHpOfAllies(@NotNull int[] allyHp) {
            double result = 0;
            for (int hp : allyHp) {
                result += 2 * Math.min(hp, 85) + 0.2 * Math.max(hp - 85, 0);
            }
            return result;
        }

        private double distanceToWoundedAllies(@NotNull Position p) {
            double result = 0;
            for (int i = 0, size = allies.size(); i < size; i++) {
                if (i == myIndex) continue;
                Trooper ally = allies.get(i);
                int hp = p.allyHp[i];
                if (hp < 85) {
                    // TODO: shortest path distance?
                    result += Point.create(ally).manhattanDistance(p.me) * (ally.getMaximalHitpoints() - hp);
                }
            }
            return result;
        }

        protected abstract double situation(@NotNull Position p);
    }

    private class CombatSituationScorer extends Scorer {
        @Override
        protected double situation(@NotNull Position p) {
            double result = 0;

            result -= IntArrays.sum(p.enemyHp);
            result += 30 * IntArrays.numberOfZeros(p.enemyHp);

            result -= 0.5 * expectedDamageOnNextTurn(p);

            result += 0.1 * Integer.bitCount(p.bonuses);

            result -= 0.01 * distanceToAllies(p);

            return result;
        }

        private double distanceToAllies(@NotNull Position p) {
            double result = 0;
            for (int i = 0, size = allies.size(); i < size; i++) {
                if (i == myIndex) continue;
                Integer dist = army.getDistanceOnEmptyBoard(Point.create(allies.get(i)), p.me);
                if (dist != null) result += dist;
            }
            return result;
        }

        private double expectedDamageOnNextTurn(@NotNull Position p) {
            // TODO: take into account grenades/medikits that enemies have

            // Assume that all visible enemies also see us, but this is not always true
            // TODO: count number of other teams having at least one trooper who sees us

            int n = allies.size();

            double[] expectedDamage = new double[n];

            for (int i = 0, size = enemies.size(); i < size; i++) {
                Trooper enemy = enemies.get(i);
                if (p.enemyHp[i] == 0) continue;

                // Assume that the enemy trooper always is in the commander aura
                int actionPoints = enemy.getInitialActionPoints() +
                        (enemy.getType() != COMMANDER && enemy.getType() != SCOUT ? game.getCommanderAuraBonusActionPoints() : 0);

                // Assume that he's always shooting right away until the end of his turn
                // TODO: handle the case when he lowers the stance in the beginning
                int maxDamageToAlly = (actionPoints / enemy.getShootCost()) * enemy.getDamage();

                boolean[] isReachable = new boolean[n];
                int alliesUnderSight = 0;
                for (int j = 0; j < n; j++) {
                    Trooper ally = allies.get(j);
                    Point point = j == myIndex ? p.me : Point.create(ally);
                    TrooperStance stance = j == myIndex ? p.stance : ally.getStance();
                    if (isReachable(enemy.getShootingRange(), enemy, point, stance)) {
                        isReachable[j] = true;
                        alliesUnderSight++;
                    }
                }
                if (alliesUnderSight == 0) continue;

                for (int j = 0; j < n; j++) {
                    if (isReachable[j]) expectedDamage[j] += maxDamageToAlly * 1. / alliesUnderSight;
                }
            }

            double result = 0.;
            for (int i = 0; i < n; i++) {
                result += Math.min(expectedDamage[i], p.allyHp[i]);
            }

            return result;
        }
    }

    private class FollowerScorer extends Scorer {
        private final Point leader;
        private final Point dislocation = army.getOrUpdateDislocation(allies);
        private final Set<Point> set = new PointSet();
        private final ArrayDeque<Point> queue = new ArrayDeque<>(16);
        private final List<Point> leaderPath;

        public FollowerScorer(@NotNull TrooperType leaderType) {
            this.leader = Point.create(alliesMap.get(leaderType));
            List<Point> leaderPath = board.findPath(leader, dislocation);
            this.leaderPath = leaderPath == null ? Collections.<Point>emptyList() : leaderPath;
        }

        @Override
        protected double situation(@NotNull Position p) {
            double result = 0;

            // TODO
            result += 3 * Integer.bitCount(p.bonuses);

            Integer dist = army.getDistanceOnEmptyBoard(p.me, leader);
            if (dist != null) result -= dist;

            int freeCells = leaderDegreeOfFreedom(p);
            result -= 100 * Math.max(5 - freeCells, 0);

            if (isBlockingLeader(p)) result -= 10;

            return result;
        }

        private boolean isBlockingLeader(@NotNull Position p) {
            for (Point point : leaderPath) {
                if (p.me.equals(point)) return true;
            }
            return false;
        }

        private int leaderDegreeOfFreedom(@NotNull Position p) {
            set.clear();
            queue.clear();

            for (Pair<Integer, Point> ally : p.allies()) {
                set.add(ally.second);
            }
            queue.add(leader);

            int result = 1;
            while (!queue.isEmpty()) {
                Point point = queue.poll();
                for (Direction direction : Util.DIRECTIONS) {
                    Point q = point.go(direction);
                    if (q != null && board.isPassable(q) && !set.contains(q)) {
                        set.add(q);
                        queue.add(q);
                        if (++result == 5) return result;
                    }
                }
            }

            return result;
        }
    }

    private class LeaderScorer extends Scorer {
        private final Point dislocation = army.getOrUpdateDislocation(allies);

        @Override
        protected double situation(@NotNull Position p) {
            double result = 0;

            // TODO
            result += 3 * Integer.bitCount(p.bonuses);

            result -= distanceToDislocation(p);

            result -= 100 * farAwayTeammates(p);

            return result;
        }

        private int farAwayTeammates(@NotNull Position p) {
            int result = 0;
            for (int i = 0, size = allies.size(); i < size; i++) {
                if (i == myIndex) continue;
                Integer distance = army.getDistanceOnEmptyBoard(Point.create(allies.get(i)), p.me);
                if (distance != null && distance > 5) result++;
            }
            return result;
        }

        private int distanceToDislocation(@NotNull Position p) {
            Integer dist = army.getDistanceOnEmptyBoard(p.me, dislocation);
            return dist != null ? dist : 1000;
        }
    }

    @NotNull
    private List<Long> ids(@NotNull List<Trooper> troopers) {
        List<Long> result = new ArrayList<>(troopers.size());
        for (Trooper trooper : troopers) {
            result.add(trooper.getId());
        }
        return result;
    }

    @NotNull
    private Trooper findLeader() {
        for (TrooperType type : Arrays.asList(SOLDIER, COMMANDER, FIELD_MEDIC, SCOUT, SNIPER)) {
            Trooper trooper = alliesMap.get(type);
            if (trooper != null) return trooper;
        }

        throw new IllegalStateException("No one left alive, who am I then? " + self.getType());
    }

    private int getMoveCost(@NotNull TrooperStance stance) {
        switch (stance) {
            case PRONE: return game.getProneMoveCost();
            case KNEELING: return game.getKneelingMoveCost();
            case STANDING: return game.getStandingMoveCost();
            default: throw new IllegalStateException("Unknown stance: " + stance);
        }
    }

    private boolean isHolding(@NotNull BonusType type) {
        switch (type) {
            case GRENADE: return self.isHoldingGrenade();
            case MEDIKIT: return self.isHoldingMedikit();
            case FIELD_RATION: return self.isHoldingFieldRation();
            default: throw new IllegalStateException("Unknown bonus type: " + type);
        }
    }

    private boolean isReachable(double maxRange, @NotNull Trooper viewer, @NotNull Point object, @NotNull TrooperStance objectStance) {
        return world.isVisible(maxRange, viewer.getX(), viewer.getY(), viewer.getStance(), object.x, object.y, objectStance);
    }

    private boolean isReachable(double maxRange, @NotNull Point viewer, @NotNull TrooperStance viewerStance, @NotNull Trooper object) {
        return world.isVisible(maxRange, viewer.x, viewer.y, viewerStance, object.getX(), object.getY(), object.getStance());
    }

    @NotNull
    @Override
    public String toString() {
        return self + ", turn #" + world.getMoveIndex();
    }

    private void debug(@Nullable Object o) {
        if (LOCAL) {
            System.out.println(o);
        }
    }

    private void debug(@NotNull Scorer scorer, @NotNull List<Go> best) {
        if (!LOCAL) return;

        String s;
        if (scorer instanceof CombatSituationScorer) s = "combat";
        else if (scorer instanceof LeaderScorer) s = "leader";
        else if (scorer instanceof FollowerScorer) s = "follower";
        else throw new UnsupportedOperationException("Unknown scorer: " + scorer);

        System.out.println(s + ": " + self + " -> " + best);
    }
}
