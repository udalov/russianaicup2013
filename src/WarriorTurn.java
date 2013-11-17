import model.*;

import java.util.*;

import static model.TrooperStance.KNEELING;
import static model.TrooperStance.STANDING;
import static model.TrooperType.FIELD_MEDIC;

public class WarriorTurn {
    private final Army army;
    private final Trooper self;
    private final World world;
    private final Game game;

    private final Point me;
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
        Go messageBased = readMessages();

        Direction useMedikit = useMedikit();
        Direction heal = heal();
        Direction runToWounded = runToWounded();
        Point grenade = throwGrenade();
        Point shoot = shoot();

        if (Util.anything(useMedikit, heal, runToWounded, grenade, shoot, messageBased)) {
            if (eatFieldRation()) return Go.eatFieldRation();
        }

        if (messageBased != null) return messageBased;

        maybeRequestHelp();

        if (useMedikit != null) return Go.useMedikit(useMedikit);
        if (heal != null) return Go.heal(heal);
        if (runToWounded != null) return Go.move(runToWounded);

        if (grenade != null || shoot != null) {
            if (can(10) && self.getStance() == KNEELING) return Go.lowerStance();
            if (can(8) && self.getStance() == STANDING) return Go.lowerStance();
        } else {
            if (can(8) && self.getStance() != STANDING) return Go.raiseStance();
        }

        if (grenade != null) return Go.throwGrenade(grenade);
        if (shoot != null) return Go.shoot(shoot);

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
                    Board.Cell cell = board.get(cur);
                    if (cell == Board.Cell.FREE || cell == Board.Cell.BONUS) {
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
                Direction direction = board.findBestMove(me, caller);
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
    private Direction useMedikit() {
        if (!self.isHoldingMedikit()) return null;
        if (!can(game.getMedikitUseCost())) return null;

        return findWoundedNeighbor(self.getMaximalHitpoints() / 2);
    }

    @Nullable
    private Direction heal() {
        if (self.getType() != FIELD_MEDIC) return null;
        if (!can(game.getFieldMedicHealCost())) return null;

        return findWoundedNeighbor(self.getMaximalHitpoints());
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
        return wounded != null ? board.findBestMove(me, wounded) : null;
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
                if (!me.withinEuclidean(target, game.getGrenadeThrowRange())) continue;

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
            result += grenadeDamageToTrooper(target, enemy);
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

        List<Trooper> targets = findSortedTargetsToShoot(self.getShootingRange());
        return !targets.isEmpty() ? Point.create(targets.get(0)) : null;
    }

    @NotNull
    private List<Trooper> findSortedTargetsToShoot(double range) {
        List<Trooper> result = new ArrayList<>(enemies.size());
        for (Trooper enemy : enemies) {
            if (isReachable(range, enemy)) {
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

        Point bonus = findClosestRelevantBonus();
        if (bonus != null && me.manhattanDistance(bonus) <= 4) {
            return board.findBestMove(me, bonus);
        }

        Trooper leader = findLeader();

        if (self.getType() == leader.getType()) {
            Direction move = board.findBestMove(me, army.getOrUpdateDislocation(allies.values()));
            if (move == null) return null;

            Point destination = me.go(move);
            for (Trooper ally : alliesWithoutMe) {
                if (Point.create(ally).equals(destination)) {
                    army.sendMessage(ally, new Message(Message.Kind.OUT_OF_THE_WAY, me), 4);
                }
            }

            return move;
        }

        Point target = Point.create(leader);
        if (me.manhattanDistance(target) > 1) {
            return board.findBestMove(me, target);
        }

        return null;
    }

    @Nullable
    private Point findClosestRelevantBonus() {
        Point result = null;
        int mind = Integer.MAX_VALUE;

        for (Bonus bonus : world.getBonuses()) {
            if (!isHolding(bonus.getType())) {
                Point that = Point.create(bonus);
                int dist = me.manhattanDistance(that);
                if (dist < mind) {
                    mind = dist;
                    result = that;
                }
            }
        }

        return result;
    }

    @NotNull
    private Trooper findLeader() {
        for (TrooperType type : army.getOrder()) {
            Trooper trooper = allies.get(type);
            if (trooper != null) return trooper;
        }

        throw new IllegalStateException("No one left alive, who am I then? " + self.getType());
    }

    private boolean can(int cost) {
        return self.getActionPoints() >= cost;
    }

    private int getMoveCost() {
        TrooperStance stance = self.getStance();
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
        return world.isVisible(maxRange, self.getX(), self.getY(), self.getStance(), enemy.getX(), enemy.getY(), enemy.getStance());
    }
}
