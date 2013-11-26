import model.*;

import java.util.*;

import static model.Direction.CURRENT_POINT;
import static model.TrooperStance.STANDING;
import static model.TrooperType.*;

public class WarriorTurn {
    private static final boolean LOCAL = System.getProperty("LOCAL") != null;

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
        for (Trooper trooper : world.getTroopers()) {
            if (trooper.isTeammate()) {
                alliesMap.put(trooper.getType(), trooper);
                allies.add(trooper);
                if (trooper.getType() != self.getType()) {
                    alliesWithoutMe.add(trooper);
                }
            } else {
                if (enemies == null) enemies = new ArrayList<>(15);
                enemies.add(trooper);
            }
        }

        this.enemies = enemies == null ? Collections.<Trooper>emptyList() : enemies;
    }

    @NotNull
    public Go makeTurn() {
        if (!enemies.isEmpty()) {
            Go best = best();
            debug(self + " -> " + best);
            return best;
        }

        Go hideBehindCover = hideBehindCover();
        if (hideBehindCover != null) return hideBehindCover;

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
    private Go best() {
        Position start = startingPosition();
        final Queue<Position> queue = new LinkedList<>();
        queue.add(start);
        final Map<Position, Pair<Go, Position>> prev = new HashMap<>();
        prev.put(start, null);

        class QueueUpdater {
            private final Position cur;
            public QueueUpdater(@NotNull Position cur) { this.cur = cur; }

            private void add(@NotNull Position next, @NotNull Go edge) {
                if (!prev.containsKey(next)) {
                    prev.put(next, new Pair<>(edge, cur));
                    queue.add(next);
                }
            }

            public void run() {
                // Move
                for (Direction direction : Util.DIRECTIONS) {
                    Position next = cur.move(direction);
                    if (next != null) add(next, Go.move(direction));
                }

                // Shoot
                for (int i = 0, size = enemies.size(); i < size; i++) {
                    Position next = cur.shoot(i);
                    if (next != null) add(next, Go.shoot(Point.create(enemies.get(i))));
                }

                // Change stance
                {
                    Position higher = cur.raiseStance();
                    if (higher != null) add(higher, Go.raiseStance());

                    Position lower = cur.lowerStance();
                    if (lower != null) add(lower, Go.lowerStance());
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
            }
        }

        Position best = null;
        double bestValue = -1e100;
        while (!queue.isEmpty()) {
            Position cur = queue.poll();

            double curValue = cur.evaluate();
            if (curValue > bestValue) {
                bestValue = curValue;
                best = cur;
            }

            new QueueUpdater(cur).run();
        }

        if (best == start) return Go.endTurn();

        Position cur = best;
        while (true) {
            Pair<Go, Position> before = prev.get(cur);
            assert before != null : "Nothing before " + cur;
            if (before.second == start) return before.first;
            cur = before.second;
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

        public double evaluate() {
            // TODO: this is the main method of the algorithm
            double result = 0;
            result -= IntArrays.sum(enemyHp);
            result += 30 * IntArrays.numberOfZeros(enemyHp);

            int allies = hpOfAlliesUnderThreshold();
            result += 2 * allies + 0.2 * (IntArrays.sum(allyHp) - allies);

            result += 0.1 * Integer.bitCount(bonuses);
            return result;
        }

        private int hpOfAlliesUnderThreshold() {
            int result = 0;
            for (int hp : allyHp) {
                if (hp <= 85) result += hp;
            }
            return result;
        }

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

        private boolean hasGrenade() { return (bonuses & 1) != 0; }
        private boolean hasMedikit() { return (bonuses & 2) != 0; }
        private boolean hasFieldRation() { return (bonuses & 4) != 0; }
        private boolean has(@NotNull BonusType bonus) {
            switch (bonus) {
                case GRENADE: return hasGrenade();
                case MEDIKIT: return hasMedikit();
                case FIELD_RATION: return hasFieldRation();
                default: throw new IllegalStateException("You've got to be kidding me: " + bonus);
            }
        }

        private int withoutGrenade() { return bonuses & 6; }
        private int withoutMedikit() { return bonuses & 5; }
        private int withoutFieldRation() { return bonuses & 3; }

        private int with(@NotNull BonusType bonus) {
            switch (bonus) {
                case GRENADE: return bonuses | 1;
                case MEDIKIT: return bonuses | 2;
                case FIELD_RATION: return bonuses | 4;
                default: throw new IllegalStateException("You've got to be kidding me: " + bonus);
            }
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
                if (has(bonus.getType())) continue;

                // TODO: avoid these useless creations and add Point.isEqualTo(Unit)?
                if (!Point.create(bonus).equals(point)) continue;

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
            if (!hasGrenade()) return null;
            int ap = actionPoints - game.getGrenadeThrowCost();
            if (ap < 0) return null;
            if (!me.withinEuclidean(target, game.getGrenadeThrowRange())) return null;
            int[] newEnemyHp = grenadeEffect(target, enemyHp, enemies);
            if (Arrays.equals(enemyHp, newEnemyHp)) return null;
            int[] newAllyHp = grenadeEffect(target, allyHp, allies);
            return new Position(me, stance, ap, withoutGrenade(), newEnemyHp, newAllyHp, collected);
        }

        @Nullable
        public Position useMedikit(int ally, @NotNull Point point) {
            if (!hasMedikit()) return null;
            int ap = actionPoints - game.getMedikitUseCost();
            if (ap < 0) return null;
            int[] newAllyHp;
            if (point.equals(me)) newAllyHp = healEffect(ally, game.getMedikitHealSelfBonusHitpoints());
            else if (point.isNeighbor(me)) newAllyHp = healEffect(ally, game.getMedikitBonusHitpoints());
            else return null;
            if (Arrays.equals(allyHp, newAllyHp)) return null;
            return new Position(me, stance, ap, withoutMedikit(), enemyHp, newAllyHp, collected);
        }

        @Nullable
        public Position eatFieldRation() {
            if (!hasFieldRation()) return null;
            if (actionPoints >= self.getInitialActionPoints()) return null;
            int ap = actionPoints - game.getFieldRationEatCost();
            if (ap < 0) return null;
            return new Position(me, stance, ap, withoutFieldRation(), enemyHp, allyHp, collected);
        }

        @Nullable
        public Position heal(int ally, @NotNull Point point) {
            int ap = actionPoints - game.getFieldMedicHealCost();
            if (ap < 0) return null;
            if (!point.equals(me) && !point.isNeighbor(me)) return null;
            int[] newAllyHp;
            if (point.equals(me)) newAllyHp = healEffect(ally, game.getFieldMedicHealSelfBonusHitpoints());
            else if (point.isNeighbor(me)) newAllyHp = healEffect(ally, game.getFieldMedicHealBonusHitpoints());
            else return null;
            if (Arrays.equals(allyHp, newAllyHp)) return null;
            return new Position(me, stance, ap, withoutMedikit(), enemyHp, newAllyHp, collected);
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

    @Nullable
    private Go hideBehindCover() {
        if (can(game.getStanceChangeCost()) && self.getActionPoints() <= game.getStanceChangeCost() + 1) {
            TrooperStance lowered = Util.lower(stance);
            if (lowered != null && howManyEnemiesCanShotMeThere(me, lowered) < howManyEnemiesCanShotMeThere(me, stance)) {
                return Go.lowerStance();
            }
        }

        if (apEqualOrSlightlyGreater(2 * getMoveCost()) && alliesWithoutMe.isEmpty()) {
            // TODO: Util.findMin
            int bestVulnerability = howManyEnemiesCanShotMeThere(me, stance);
            Direction bestFirstStep = null;
            for (Direction firstStep : Util.DIRECTIONS) {
                Point there = me.go(firstStep);
                if (there == null || !board.isPassable(there)) continue;
                int vulnerability = Integer.MAX_VALUE;
                for (Direction secondStep : Util.DIRECTIONS) {
                    Point destination = there.go(secondStep);
                    if (destination == null || !board.isPassable(destination)) continue;
                    vulnerability = Math.min(vulnerability, howManyEnemiesCanShotMeThere(destination, stance));
                }
                if (vulnerability < bestVulnerability) {
                    bestVulnerability = vulnerability;
                    bestFirstStep = firstStep;
                }
            }

            if (bestFirstStep != null) {
                return Go.move(bestFirstStep);
            }
        }

        if (apEqualOrSlightlyGreater(getMoveCost())) {
            Direction best = Util.findMin(Util.DIRECTIONS, new Util.Evaluator<Direction>() {
                @Override
                @Nullable
                public Integer evaluate(@NotNull Direction direction) {
                    Point there = me.go(direction);
                    if (there == null || !board.isPassable(there)) return null;
                    return howManyEnemiesCanShotMeThere(there, stance);
                }
            });
            //noinspection ConstantConditions
            if (best != null && howManyEnemiesCanShotMeThere(me.go(best), stance) < howManyEnemiesCanShotMeThere(me, stance)) return Go.move(best);
        }

        return null;
    }

    private boolean apEqualOrSlightlyGreater(int value) {
        return self.getActionPoints() == value || self.getActionPoints() == value + 1;
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
            if (Point.create(ally).equals(destination)) {
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
