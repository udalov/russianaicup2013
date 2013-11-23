import model.*;

import java.util.*;

import static model.TrooperStance.KNEELING;
import static model.TrooperStance.STANDING;
import static model.TrooperType.*;

public class WarriorTurn {
    private final Army army;
    private final Trooper self;
    private final World world;
    private final Game game;

    private final Point me;
    private final TrooperStance stance;
    private final Board board;
    private final List<Trooper> enemies;
    private final Map<TrooperType, Trooper> allies;
    private final List<Trooper> alliesWithoutMe;

    public WarriorTurn(@NotNull Army army, @NotNull Trooper self, @NotNull World world, @NotNull Game game) {
        this.army = army;
        this.self = self;
        this.world = world;
        this.game = game;

        me = Point.create(self);
        stance = self.getStance();
        board = new Board(world);
        List<Trooper> enemies = null;
        allies = new EnumMap<>(TrooperType.class);
        alliesWithoutMe = new ArrayList<>(4);
        for (Trooper trooper : world.getTroopers()) {
            if (trooper.isTeammate()) {
                allies.put(trooper.getType(), trooper);
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
        Go hideBehindCover = hideBehindCover();
        if (hideBehindCover != null) return hideBehindCover;

        Go messageBased = readMessages();
        if (messageBased != null) return eatFieldRationOr(messageBased);

        maybeRequestHelp();

        Direction useMedikit = useMedikit();
        if (useMedikit != null) return eatFieldRationOr(Go.useMedikit(useMedikit));

        Direction heal = heal();
        if (heal != null) return eatFieldRationOr(Go.heal(heal));

        Direction runToWounded = runToWounded();
        if (runToWounded != null) return eatFieldRationOr(Go.move(runToWounded));

        Point grenade = throwGrenade();
        Point shoot = shoot();

        if (grenade != null || shoot != null) {
            if (can(10) && stance == KNEELING) return Go.lowerStance();
            if (can(8) && stance == STANDING) return Go.lowerStance();
        }

        if (grenade != null) return eatFieldRationOr(Go.throwGrenade(grenade));
        if (shoot != null) return eatFieldRationOr(Go.shoot(shoot));

        if (can(8) && stance != STANDING && howManyEnemiesCanShotMeThere(me, STANDING) == 0) return Go.raiseStance();

        Direction runToFight = runToFight();
        if (runToFight != null) return eatFieldRationOr(Go.move(runToFight));

        Direction move = move();
        if (move != null) return Go.move(move);

        return Go.endTurn();
    }

    private void maybeRequestHelp() {
        // TODO: enable this
/*
        for (Trooper enemy : enemies) {
            if (isVisible(enemy.getVisionRange(), enemy, self)) {
                army.requestHelp(me, self.getType(), alliesWithoutMe, 10);
            }
        }
*/
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
            } else if (message.getKind() == Message.Kind.NEED_HELP) {
                Point caller = message.getData();
                if (me.manhattanDistance(caller) <= 3) {
                    // If we're already close: if there's an enemy we can shoot, shoot this enemy instead
                    for (Trooper enemy : enemies) {
                        if (isReachable(self.getShootingRange(), enemy)) return null;
                    }
                }
                Direction direction = board.findBestMove(me, caller, false);
                if (direction != null) {
                    return Go.move(direction);
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

        if (self.getActionPoints() == 2 * getMoveCost() && alliesWithoutMe.isEmpty()) {
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

            if (bestFirstStep != null) return Go.move(bestFirstStep);
        }

        if (can(getMoveCost()) && self.getActionPoints() <= getMoveCost() + 1) {
            Point best = me;
            for (Direction direction : Util.DIRECTIONS) {
                Point there = me.go(direction);
                if (there == null || !board.isPassable(there)) continue;
                if (howManyEnemiesCanShotMeThere(there, stance) < howManyEnemiesCanShotMeThere(best, stance)) {
                    best = there;
                }
            }

            if (best != me) return Go.move(me.direction(best));
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

        return findWoundedNeighbor(self.getMaximalHitpoints() / 2);
    }

    @Nullable
    private Direction heal() {
        if (self.getType() != FIELD_MEDIC) return null;
        if (!can(game.getFieldMedicHealCost())) return null;

        return findWoundedNeighbor(self.getMaximalHitpoints() * 9 / 10);
    }

    @Nullable
    private Direction findWoundedNeighbor(int maximalHitpoints) {
        Point wounded = findNearestWounded(maximalHitpoints);
        return wounded != null && me.manhattanDistance(wounded) <= 1 ? me.direction(wounded) : null;
    }

    @Nullable
    private Direction runToWounded() {
        if (self.getType() != FIELD_MEDIC) return null;
        if (!can(getMoveCost())) return null;

        Point wounded = findNearestWounded(self.getMaximalHitpoints() * 2 / 3);
        return wounded != null ? board.findBestMove(me, wounded, false) : null;
    }

    @Nullable
    private Point findNearestWounded(int maximalHitpoints) {
        Point worst = null;
        int minDistance = Integer.MAX_VALUE;
        for (Trooper trooper : allies.values()) {
            if (trooper.getHitpoints() < maximalHitpoints) {
                Point wounded = Point.create(trooper);
                int dist = wounded.manhattanDistance(me);
                if (dist < minDistance) {
                    minDistance = dist;
                    worst = wounded;
                }
            }
        }

        return worst;
    }

    @Nullable
    private Direction runToFight() {
        if (!can(getMoveCost())) return null;

        final Trooper enemy = findMostDangerousEnemyTrooper();
        if (enemy == null) return null;

        Point runTo = board.launchDijkstra(me, false, new Board.Controller() {
            @Override
            public boolean isEndingPoint(@NotNull Point point) {
                return board.isPassable(point) && world.isVisible(self.getShootingRange(), point.x, point.y, stance, enemy.getX(), enemy.getY(), enemy.getStance());
            }
        });
        if (runTo == null) return null;

        return board.findBestMove(me, runTo, false);
    }

    @Nullable
    private Trooper findMostDangerousEnemyTrooper() {
        if (enemies.isEmpty()) return null;
        if (enemies.size() == 1) return enemies.get(0);

        final SmallLongIntMap visibleTroopers = new SmallLongIntMap();
        for (Trooper enemy : enemies) {
            visibleTroopers.inc(enemy.getPlayerId());
        }

        final SmallLongIntMap alliesSeenBy = new SmallLongIntMap();
        for (Trooper ally : allies.values()) {
            for (Trooper enemy : enemies) {
                if (isReachable(enemy.getShootingRange(), enemy, ally)) {
                    alliesSeenBy.inc(enemy.getPlayerId());
                }
            }
        }

        List<Trooper> enemies = new ArrayList<>(this.enemies);
        Collections.sort(enemies, new Comparator<Trooper>() {
            @Override
            public int compare(@NotNull Trooper a, @NotNull Trooper b) {
                long idA = a.getPlayerId(), idB = b.getPlayerId();

                int army = visibleTroopers.get(idA) - visibleTroopers.get(idB);
                if (army != 0) return army;

                int alliesSeen = alliesSeenBy.get(idA) - alliesSeenBy.get(idB);
                if (alliesSeen != 0) return alliesSeen;

                // TODO
                return a.getType().ordinal() - b.getType().ordinal();
            }
        });

        return enemies.get(enemies.size() - 1);
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
    private Point throwGrenade() {
        if (!self.isHoldingGrenade()) return null;
        if (!can(game.getGrenadeThrowCost())) return null;

        Point bestTarget = null;
        int bestDamage = Integer.MIN_VALUE;
        for (Trooper enemy : enemies) {
            Point enemyPoint = Point.create(enemy);
            for (Direction direction : Direction.values()) {
                Point target = enemyPoint.go(direction);
                if (target == null || !me.withinEuclidean(target, game.getGrenadeThrowRange())) continue;

                int cur = grenadeDamage(target);
                if (cur > bestDamage) {
                    bestDamage = cur;
                    bestTarget = target;
                }
            }
        }

        return bestTarget;
    }

    private int grenadeDamage(@NotNull Point target) {
        int result = 0;
        for (Trooper enemy : enemies) {
            int damage = grenadeDamageToTrooper(target, enemy);
            result += damage;
            if (damage == enemy.getHitpoints()) {
                result += 25;
            }
        }
        // 1 hitpoint of an ally = 4 (?) hitpoints of an enemy
        for (Trooper ally : allies.values()) {
            result -= 4 * grenadeDamageToTrooper(target, ally);
        }
        return result;
    }

    private int grenadeDamageToTrooper(@NotNull Point grenade, @NotNull Trooper trooper) {
        Point point = Point.create(trooper);
        if (point.equals(grenade)) {
            return Math.min(game.getGrenadeDirectDamage(), trooper.getHitpoints());
        } else if (point.isNeighbor(grenade)) {
            return Math.min(game.getGrenadeCollateralDamage(), trooper.getHitpoints());
        }
        return 0;
    }

    @Nullable
    private Point shoot() {
        if (!can(self.getShootCost())) return null;

        if (enemies.isEmpty()) return null;

        List<Trooper> targets = findSortedTargetsToShoot();
        return !targets.isEmpty() ? Point.create(targets.get(0)) : null;
    }

    @NotNull
    private List<Trooper> findSortedTargetsToShoot() {
        List<Trooper> result = new ArrayList<>(enemies.size());
        for (Trooper enemy : enemies) {
            if (isReachable(self.getShootingRange(), enemy)) {
                result.add(enemy);
            }
        }

        if (result.size() > 1) {
            Collections.sort(result, new Comparator<Trooper>() {
                @Override
                public int compare(@NotNull Trooper o1, @NotNull Trooper o2) {
                    return o1.getHitpoints() - o2.getHitpoints();
                }
            });
        }

        return result;
    }

    @Nullable
    private Direction move() {
        if (!can(getMoveCost())) return null;

        Trooper leader = findLeader();

        if (self.getType() == leader.getType()) {
            Pair<Point, Integer> bonusDist = findClosestRelevantBonus();
            if (bonusDist != null && bonusDist.second <= 4) {
                Direction move = board.findBestMove(me, bonusDist.first, false);
                if (move != null) return clearPath(move);
            }

            Direction move = board.findBestMove(me, army.getOrUpdateDislocation(allies.values()), false);
            if (move != null) return clearPath(move);

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
            Trooper trooper = allies.get(type);
            if (trooper != null) return trooper;
        }

        throw new IllegalStateException("No one left alive, who am I then? " + self.getType());
    }

    private boolean can(int cost) {
        return self.getActionPoints() >= cost;
    }

    private int getMoveCost() {
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

    private boolean isReachable(double maxRange, @NotNull Trooper enemy) {
        return isReachable(maxRange, self, enemy);
    }

    private boolean isReachable(double maxRange, @NotNull Trooper viewer, @NotNull Trooper object) {
        return world.isVisible(maxRange, viewer.getX(), viewer.getY(), viewer.getStance(), object.getX(), object.getY(), object.getStance());
    }

    @NotNull
    @Override
    public String toString() {
        return self.getActionPoints() + " " + stance + " " + self.getType() + " at " + me + ", turn #" + world.getMoveIndex();
    }
}
