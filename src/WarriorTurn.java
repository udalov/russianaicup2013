import model.*;

import java.util.*;

import static model.TrooperType.*;

public class WarriorTurn {
    private final static Random RANDOM = new Random(42);

    private final static List<TrooperType> LEADERSHIP_ORDER = Arrays.asList(COMMANDER, SOLDIER, SNIPER, FIELD_MEDIC, SCOUT);

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
        if (useMedikit != null) return Go.useMedikit(useMedikit);

        Direction heal = heal();
        if (heal != null) return Go.heal(heal);

        if (self.getType() == FIELD_MEDIC && self.getActionPoints() >= getMoveCost()) {
            Point wounded = findNearestWounded(self.getMaximalHitpoints() * 2 / 3);
            if (wounded != null) {
                Direction move = new BestPathFinder(board).findFirstMove(me, wounded);
                if (move != null) {
                    return Go.move(move);
                }
            }
        }

        if (findTargetToShoot(self.getShootingRange()) != null) {
            if (eatFieldRation()) return Go.eatFieldRation();
        }

        Point grenade = throwGrenade();
        if (grenade != null) return Go.throwGrenade(grenade);

        Point shoot = shoot();
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
        return self.isHoldingFieldRation() && self.getActionPoints() >= game.getFieldRationEatCost();
    }

    @Nullable
    private Point throwGrenade() {
        if (!self.isHoldingGrenade()) return null;
        if (self.getActionPoints() < game.getGrenadeThrowCost()) return null;

        return findTargetToShoot(game.getGrenadeThrowRange());
    }

    @Nullable
    private Point shoot() {
        if (self.getActionPoints() < self.getShootCost()) return null;

        return findTargetToShoot(self.getShootingRange());
    }

    @Nullable
    private Point findTargetToShoot(double range) {
        for (Trooper enemy : enemies) {
            if (isVisible(range, enemy)) {
                return Point.byUnit(enemy);
            }
        }

        return null;
    }

    @Nullable
    private Direction move() {
        if (self.getActionPoints() < getMoveCost()) return null;

        BestPathFinder finder = new BestPathFinder(board);
        Trooper leader = findLeader();

        if (self.getType() == leader.getType()) {
            return finder.findFirstMove(me, army.getDislocation());
        }

        Point target = Point.byUnit(leader);
        if (me.manhattanDistance(target) <= 4) {
            return finder.findFirstMove(me, army.getDislocation());
        }

        return finder.findFirstMove(me, target);
    }

    @NotNull
    private Trooper findLeader() {
        for (TrooperType type : LEADERSHIP_ORDER) {
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

    private boolean isVisible(double maxRange, @NotNull Trooper enemy) {
        return world.isVisible(maxRange, self.getX(), self.getY(), self.getStance(), enemy.getX(), enemy.getY(), enemy.getStance());
    }
}
