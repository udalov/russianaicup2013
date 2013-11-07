import model.*;

import java.util.*;

import static model.TrooperType.*;

public class WarriorTurn {
    private final static Random RANDOM = new Random();

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
        this.board = new Board(world.getCells());
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
        if (world.getMoveIndex() >= 1) {
            if (self.getStance() != TrooperStance.KNEELING && self.getActionPoints() >= game.getStanceChangeCost()) {
                return Go.lowerStance();
            }
        }

        Direction useMedikit = useMedikit();
        if (useMedikit != null) return Go.useMedikit(useMedikit);

        Direction heal = heal();
        if (heal != null) return Go.heal(heal);

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

        for (Trooper trooper : allies.values()) {
            if (trooper.getHitpoints() < trooper.getMaximalHitpoints() / 2) {
                Point wounded = Point.byUnit(trooper);
                if (wounded.isNeighbor(me)) {
                    return me.direction(wounded);
                }
            }
        }

        if (self.getHitpoints() < self.getMaximalHitpoints() / 2) {
            return Direction.CURRENT_POINT;
        }

        return null;
    }

    @Nullable
    private Direction heal() {
        if (self.getType() != FIELD_MEDIC) return null;
        if (self.getActionPoints() < game.getFieldMedicHealCost()) return null;

        for (Trooper trooper : allies.values()) {
            if (trooper.getHitpoints() < trooper.getMaximalHitpoints()) {
                Point wounded = Point.byUnit(trooper);
                if (wounded.isNeighbor(me)) {
                    return me.direction(wounded);
                }
            }
        }

        if (self.getHitpoints() < self.getMaximalHitpoints()) {
            return Direction.CURRENT_POINT;
        }

        return null;
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
        if (self.getActionPoints() < self.getShotCost()) return null;

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

        Trooper leader = getLeader();

        Point target = self.getType() == leader.getType()
                ? army.getDislocation(board)
                : Point.byUnit(leader);

        return new BestPathFinder(board).findFirstMove(me, target);
    }

    @NotNull
    private Trooper getLeader() {
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
