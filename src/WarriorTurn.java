import model.*;

import java.util.*;

import static model.TrooperStance.KNEELING;
import static model.TrooperStance.STANDING;
import static model.TrooperType.FIELD_MEDIC;

public class WarriorTurn {
    private final static Random RANDOM = new Random(42);

    private final Army army;
    private final Trooper self;
    private final World world;
    private final Game game;

    private final Point me;
    private final Board board;
    private final List<Trooper> enemies;
    private final Map<TrooperType, Trooper> allies;

    public WarriorTurn(@NotNull Army army, @NotNull Trooper self, @NotNull World world, @NotNull Game game) {
        this.army = army;
        this.self = self;
        this.world = world;
        this.game = game;

        this.me = Point.byUnit(self);
        this.board = new Board(world);
        this.enemies = new ArrayList<>(15);
        this.allies = new EnumMap<>(TrooperType.class);
        for (Trooper trooper : world.getTroopers()) {
            if (trooper.isTeammate()) {
                this.allies.put(trooper.getType(), trooper);
            } else {
                this.enemies.add(trooper);
            }
        }
    }

    @NotNull
    public Go makeTurn() {
        Direction useMedikit = useMedikit();
        Direction heal = heal();
        Direction runToWounded = runToWounded();
        Point grenade = throwGrenade();
        Point shoot = shoot();

        if (useMedikit != null || heal != null || runToWounded != null || grenade != null || shoot != null) {
            if (eatFieldRation()) return Go.eatFieldRation();
        }

        if (useMedikit != null) return Go.useMedikit(useMedikit);
        if (heal != null) return Go.heal(heal);
        if (runToWounded != null) return Go.move(runToWounded);

        if (grenade != null || shoot != null) {
            if (self.getActionPoints() >= 10 && self.getStance() == KNEELING) return Go.lowerStance();
            if (self.getActionPoints() >= 8 && self.getStance() == STANDING) return Go.lowerStance();
        } else {
            if (self.getActionPoints() >= 8 && self.getStance() != STANDING) return Go.raiseStance();
        }

        if (grenade != null) return Go.throwGrenade(grenade);
        if (shoot != null) return Go.shoot(shoot);

        Direction move = move();
        if (move != null) return Go.move(move);

        return Go.endTurn();
    }

    @Nullable
    private Direction useMedikit() {
        if (!self.isHoldingMedikit()) return null;
        if (self.getActionPoints() < game.getMedikitUseCost()) return null;

        return findWoundedNeighbor(self.getMaximalHitpoints() / 2);
    }

    @Nullable
    private Direction heal() {
        if (self.getType() != FIELD_MEDIC) return null;
        if (self.getActionPoints() < game.getFieldMedicHealCost()) return null;

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
        if (self.getActionPoints() < getMoveCost()) return null;

        Point wounded = findNearestWounded(self.getMaximalHitpoints() * 2 / 3);
        return wounded != null ? board.findBestMove(me, wounded) : null;
    }

    @Nullable
    private Point findNearestWounded(int maximalHitpoints) {
        Point worst = null;
        int minDistance = Integer.MAX_VALUE;
        for (Trooper trooper : allies.values()) {
            if (trooper.getHitpoints() < maximalHitpoints) {
                Point wounded = Point.byUnit(trooper);
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
        return self.isHoldingFieldRation()
                && self.getActionPoints() >= game.getFieldRationEatCost()
                && self.getActionPoints() <= self.getInitialActionPoints() - game.getFieldRationBonusActionPoints() + game.getFieldRationEatCost();
    }

    @Nullable
    private Point throwGrenade() {
        if (!self.isHoldingGrenade()) return null;
        if (self.getActionPoints() < game.getGrenadeThrowCost()) return null;

        List<Trooper> targets = findSortedTargetsToShoot(game.getGrenadeThrowRange());
        return !targets.isEmpty() ? Point.byUnit(targets.get(targets.size() - 1)) : null;
    }

    @Nullable
    private Point shoot() {
        if (self.getActionPoints() < self.getShootCost()) return null;

        List<Trooper> targets = findSortedTargetsToShoot(self.getShootingRange());
        return !targets.isEmpty() ? Point.byUnit(targets.get(0)) : null;
    }

    @NotNull
    private List<Trooper> findSortedTargetsToShoot(double range) {
        List<Trooper> result = new ArrayList<>(15);
        for (Trooper enemy : enemies) {
            if (isVisible(range, enemy)) {
                result.add(enemy);
            }
        }

        if (result.size() <= 1) return result;

        Collections.sort(result, new Comparator<Trooper>() {
            @Override
            public int compare(@NotNull Trooper o1, @NotNull Trooper o2) {
                return o1.getHitpoints() - o2.getHitpoints();
            }
        });

        return result;
    }

    @Nullable
    private Direction move() {
        if (self.getActionPoints() < getMoveCost()) return null;

        Point bonus = findClosestRelevantBonus();
        if (bonus != null && me.manhattanDistance(bonus) <= 4) {
            return board.findBestMove(me, bonus);
        }

        Trooper leader = findLeader();

        if (self.getType() == leader.getType()) {
            return board.findBestMove(me, army.getOrUpdateDislocation(allies.values()));
        }

        Point target = Point.byUnit(leader);
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
                Point that = Point.byUnit(bonus);
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

    private boolean isVisible(double maxRange, @NotNull Trooper enemy) {
        return world.isVisible(maxRange, self.getX(), self.getY(), self.getStance(), enemy.getX(), enemy.getY(), enemy.getStance());
    }
}
