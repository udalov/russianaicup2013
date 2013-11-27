import model.*;

import java.util.*;

import static model.BonusType.*;
import static model.Direction.CURRENT_POINT;
import static model.TrooperStance.STANDING;
import static model.TrooperType.*;

public class WarriorTurn {
    private static final boolean LOCAL = Arrays.toString(Thread.currentThread().getStackTrace()).contains("LocalPackage");

    private final Army army;
    private final Trooper self;
    private final World world;
    private final Game game;

    private final Point me;
    private final TrooperStance stance;
    private final List<Bonus> worldBonuses;
    private final Board board;
    private final List<Trooper> enemies;
    private final List<Trooper> allies;
    private final List<Trooper> alliesWithoutMe;
    private final Map<TrooperType, Trooper> alliesMap;
    // Index of me in the 'allies' list
    private final int myIndex;

    public WarriorTurn(@NotNull Army army, @NotNull Trooper self, @NotNull World world, @NotNull Game game) {
        this.army = army;
        this.self = self;
        this.world = world;
        this.game = game;

        me = Point.create(self);
        stance = self.getStance();
        worldBonuses = Arrays.asList(world.getBonuses());
        board = new Board(world);
        List<Trooper> enemies = null;
        allies = new ArrayList<>(5);
        alliesWithoutMe = new ArrayList<>(4);
        alliesMap = new EnumMap<>(TrooperType.class);
        int myIndex = -1;
        for (Trooper trooper : world.getTroopers()) {
            if (trooper.isTeammate()) {
                if (trooper.getType() == self.getType()) {
                    myIndex = allies.size();
                } else {
                    alliesWithoutMe.add(trooper);
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
        if (!enemies.isEmpty()) {
            List<Go> best = best(new CombatSituationScorer());
            debug(self + " -> " + best);
            return best.get(0);
        }

        Go messageBased = readMessages();
        if (messageBased != null) return eatFieldRationOr(messageBased);

        Direction useMedikit = useMedikit();
        if (useMedikit != null) return eatFieldRationOr(Go.useMedikit(useMedikit));

        Direction heal = heal();
        if (heal != null) return eatFieldRationOr(Go.heal(heal));

        Direction runToWounded = runToWounded();
        if (runToWounded != null) return eatFieldRationOr(Go.move(runToWounded));

        if (can(8) && stance != STANDING && howManyEnemiesCanShotMeThere(me, STANDING) == 0) return Go.raiseStance();

        Direction move = move();
        if (move != null) return Go.move(move);

        return Go.endTurn();
    }

    @NotNull
    private List<Go> best(@NotNull Scorer scorer) {
        Position start = startingPosition();
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

        if (best == start) return Collections.singletonList(Go.endTurn());

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
                for (int i = 0, size = allies.size(); i < size; i++) {
                    Point point = Point.create(allies.get(i));
                    Position next = cur.heal(i, point);
                    if (next != null) add(next, Go.heal(me.direction(point)));
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
            for (int i = 0, size = allies.size(); i < size; i++) {
                Point point = Point.create(allies.get(i));
                Position next = cur.useMedikit(i, point);
                if (next != null) add(next, Go.useMedikit(me.direction(point)));
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

        private boolean has(@NotNull BonusType bonus) {
            return (bonuses & (1 << bonus.ordinal())) != 0;
        }

        private int without(@NotNull BonusType bonus) {
            return bonuses & ~(1 << bonus.ordinal());
        }

        private int with(@NotNull BonusType bonus) {
            return bonuses | (1 << bonus.ordinal());
        }

        @NotNull
        private int[] grenadeEffect(@NotNull Point target, @NotNull int[] hp, @NotNull List<Trooper> troopers) {
            int[] result = IntArrays.copy(hp);
            for (int i = 0, size = troopers.size(); i < size; i++) {
                Point point = Point.create(troopers.get(i));
                if (point.equals(target)) {
                    result[i] = Math.max(hp[i] - game.getGrenadeDirectDamage(), 0);
                } else if (point.isNeighbor(target)) {
                    result[i] = Math.max(hp[i] - game.getGrenadeCollateralDamage(), 0);
                } else {
                    result[i] = hp[i];
                }
            }
            return result;
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
            if (point == null || !board.isPassable(point)) return null;
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
            if (!isReachable(self.getShootingRange(), me, stance, trooper)) return null;
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
            int[] newEnemyHp = grenadeEffect(target, enemyHp, enemies);
            if (Arrays.equals(enemyHp, newEnemyHp)) return null;
            int[] newAllyHp = grenadeEffect(target, allyHp, allies);
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
        public abstract double evaluate(@NotNull Position p);
    }

    private class CombatSituationScorer extends Scorer {
        public double evaluate(@NotNull Position p) {
            double result = 0;

            result -= IntArrays.sum(p.enemyHp);
            result += 30 * IntArrays.numberOfZeros(p.enemyHp);

            int allies = hpOfAlliesUnderThreshold(p.allyHp);
            result += 2 * allies + 0.2 * (IntArrays.sum(p.allyHp) - allies);

            result -= 0.5 * expectedDamageOnNextTurn(p);

            result += 0.1 * Integer.bitCount(p.bonuses);

            // TODO: or if have a medikit
            if (self.getType() == FIELD_MEDIC) {
                result -= distanceToWoundedAllies(p);
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
                    result += Point.create(ally).manhattanDistance(p.me) * (ally.getMaximalHitpoints() - hp);
                }
            }
            return result;
        }

        private int hpOfAlliesUnderThreshold(@NotNull int[] allyHp) {
            int result = 0;
            for (int hp : allyHp) {
                if (hp < 85) result += hp;
            }
            return result;
        }

        public double expectedDamageOnNextTurn(@NotNull Position p) {
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

    @Nullable
    private Go readMessages() {
        if (!can(getMoveCost())) return null;

        for (Message message : army.getMessages(self)) {
            if (message.getKind() == Message.Kind.OUT_OF_THE_WAY) {
                Point whereFrom = message.getData();
                Point best = null;
                for (Direction direction : Util.DIRECTIONS) {
                    Point cur = me.go(direction);
                    if (cur != null && board.isPassable(cur)) {
                        if (best == null || best.manhattanDistance(whereFrom) < cur.manhattanDistance(whereFrom)) {
                            best = cur;
                        }
                    }
                }
                if (best != null) {
                    return Go.move(me.direction(best));
                }
            } else {
                throw new UnsupportedOperationException("What's that supposed to mean: " + message);
            }
        }

        return null;
    }

    private int howManyEnemiesCanShotMeThere(@NotNull Point point, @NotNull TrooperStance stance) {
        int result = 0;
        for (Trooper enemy : enemies) {
            if (world.isVisible(enemy.getShootingRange(), enemy.getX(), enemy.getY(), enemy.getStance(), point.x, point.y, stance)) {
                result++;
            }
        }
        return result;
    }

    @Nullable
    private Direction useMedikit() {
        if (!self.isHoldingMedikit()) return null;
        if (!can(game.getMedikitUseCost())) return null;

        Trooper ally = Util.findMax(allies, new Util.Evaluator<Trooper>() {
            @Nullable
            @Override
            public Integer evaluate(@NotNull Trooper ally) {
                Point point = Point.create(ally);
                int heal;
                if (point.isNeighbor(me)) {
                    heal = game.getMedikitBonusHitpoints();
                } else if (point.equals(me)) {
                    heal = game.getMedikitHealSelfBonusHitpoints();
                } else return null;
                int result = Math.min(ally.getMaximalHitpoints() - ally.getHitpoints(), heal);
                return result < 30 ? null : result;
            }
        });

        return ally != null ? me.direction(Point.create(ally)) : null;
    }

    @Nullable
    private Direction heal() {
        if (self.getType() != FIELD_MEDIC) return null;
        if (!can(game.getFieldMedicHealCost())) return null;

        Point wounded = findNearestWounded(self.getMaximalHitpoints() * 9 / 10, army.allowMedicSelfHealing());
        if (wounded == null || me.manhattanDistance(wounded) > 1) return null;

        if (wounded.equals(me)) {
            army.medicSelfHealed();
            return CURRENT_POINT;
        }

        return me.direction(wounded);
    }

    @Nullable
    private Direction runToWounded() {
        if (self.getType() != FIELD_MEDIC) return null;
        if (!can(getMoveCost())) return null;

        Point wounded = findNearestWounded(self.getMaximalHitpoints() * 2 / 3, false);
        if (wounded == null) return null;

        return board.findBestMove(me, wounded, false);
    }

    @Nullable
    private Point findNearestWounded(int maximalHitpoints, boolean includeSelf) {
        Point worst = null;
        int minDistance = Integer.MAX_VALUE;
        for (Trooper ally : allies) {
            if (ally.getHitpoints() >= maximalHitpoints) continue;
            if ((ally.getType() == self.getType()) && !includeSelf) continue;

            Point wounded = Point.create(ally);
            Integer dist = board.findDistanceTo(me, wounded, false);
            if (dist != null && dist < minDistance) {
                minDistance = dist;
                worst = wounded;
            }
        }

        return worst;
    }

    @NotNull
    private Go eatFieldRationOr(@NotNull Go action) {
        return eatFieldRation() ? Go.eatFieldRation() : action;
    }

    private boolean eatFieldRation() {
        return self.isHoldingFieldRation() && can(game.getFieldRationEatCost())
                && self.getActionPoints() <= self.getInitialActionPoints() - game.getFieldRationBonusActionPoints() + game.getFieldRationEatCost();
    }

    @Nullable
    private Direction move() {
        if (!can(getMoveCost())) return null;

        Trooper leader = findLeader();

        if (self.getType() == leader.getType()) {
            List<Direction> possibleMoves = new ArrayList<>(2);

            Pair<Point, Integer> bonusDist = findClosestRelevantBonus();
            if (bonusDist != null && bonusDist.second <= 4) {
                Direction move = board.findBestMove(me, bonusDist.first, false);
                if (move != null) possibleMoves.add(move);
            }

            Direction move = board.findBestMove(me, army.getOrUpdateDislocation(allies), false);
            if (move != null) possibleMoves.add(move);

            for (Direction direction : possibleMoves) {
                //noinspection ConstantConditions
                if (farAwayFromAllAllies(me.go(direction))) continue;
                return clearPath(direction);
            }

            return null;
        }

        Pair<Point, Integer> bonusDist = findClosestRelevantBonus();
        if (bonusDist != null && bonusDist.second <= 10) {
            Direction move = board.findBestMove(me, bonusDist.first, false);
            if (move != null) return move;
        }

        Point target = Point.create(leader);
        if (me.manhattanDistance(target) > 1) {
            return board.findBestMove(me, target, false);
        }

        return null;
    }

    private boolean farAwayFromAllAllies(@NotNull Point point) {
        if (alliesWithoutMe.isEmpty()) return false;
        for (Trooper ally : alliesWithoutMe) {
            Integer d = board.findDistanceTo(Point.create(ally), (point), true);
            if (d == null || d <= 4) return false;
        }
        return true;
    }

    @NotNull
    private Direction clearPath(@NotNull Direction direction) {
        Point destination = me.go(direction);
        for (Trooper ally : alliesWithoutMe) {
            //noinspection ConstantConditions
            if (destination.isEqualTo(ally)) {
                army.sendMessage(ally, new Message(Message.Kind.OUT_OF_THE_WAY, me), 4);
            }
        }
        return direction;
    }

    @Nullable
    private Pair<Point, Integer> findClosestRelevantBonus() {
        Point result = null;
        int mind = Integer.MAX_VALUE;

        Map<Point, Integer> dist = board.findDistances(me, true);
        Trooper leader = findLeader();

        for (Bonus bonus : world.getBonuses()) {
            if (isHolding(bonus.getType())) continue;
            Point bonusPoint = Point.create(bonus);
            Integer distToBonus = dist.get(bonusPoint);
            if (distToBonus == null) continue;

            int totalDistance = distToBonus;
            if (totalDistance >= mind) continue;

            if (leader.getType() != self.getType()) {
                Integer backToLeader = board.findDistanceTo(bonusPoint, Point.create(leader), true);
                if (backToLeader == null) continue;
                totalDistance += backToLeader;
            }

            if (totalDistance < mind) {
                mind = totalDistance;
                result = bonusPoint;
            }
        }

        return result == null ? null : new Pair<>(result, mind);
    }

    @NotNull
    private Trooper findLeader() {
        for (TrooperType type : Arrays.asList(SOLDIER, COMMANDER, FIELD_MEDIC, SCOUT, SNIPER)) {
            Trooper trooper = alliesMap.get(type);
            if (trooper != null) return trooper;
        }

        throw new IllegalStateException("No one left alive, who am I then? " + self.getType());
    }

    private boolean can(int cost) {
        return self.getActionPoints() >= cost;
    }

    private int getMoveCost() {
        return getMoveCost(stance);
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
}
