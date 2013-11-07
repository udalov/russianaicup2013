import model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WarriorTurn {
    private final static Random RANDOM = new Random();

    private final Army army;
    private final Trooper self;
    private final World world;
    private final Game game;

    private final Board board;

    private final List<Trooper> enemies;

    public WarriorTurn(@NotNull Army army, @NotNull Trooper self, @NotNull World world, @NotNull Game game) {
        this.army = army;
        this.self = self;
        this.world = world;
        this.game = game;

        this.board = new Board(world.getCells());

        this.enemies = new ArrayList<>(15);
        for (Trooper trooper : world.getTroopers()) {
            if (!trooper.isTeammate()) {
                this.enemies.add(trooper);
            }
        }
    }

    public void makeTurn(@NotNull Move move) {
        if (world.getMoveIndex() > 2) {
            if (self.getStance() != TrooperStance.KNEELING && self.getActionPoints() >= game.getStanceChangeCost()) {
                move.setAction(ActionType.LOWER_STANCE);
                return;
            }
        }

        Point grenade = throwGrenade();
        if (grenade != null) {
            move.setAction(ActionType.THROW_GRENADE);
            move.setX(grenade.x);
            move.setY(grenade.y);
            return;
        }

        Point shoot = shoot();
        if (shoot != null) {
            move.setAction(ActionType.SHOOT);
            move.setX(shoot.x);
            move.setY(shoot.y);
            return;
        }

        Direction direction = move();
        if (direction != null) {
            move.setAction(ActionType.MOVE);
            move.setDirection(direction);
        }
    }

    @Nullable
    private Point throwGrenade() {
        if (!self.isHoldingGrenade()) return null;
        if (self.getActionPoints() < game.getGrenadeThrowCost()) return null;

        for (Trooper enemy : enemies) {
            if (isVisible(game.getGrenadeThrowRange(), enemy)) {
                return Point.byUnit(enemy);
            }
        }

        return null;
    }

    @Nullable
    private Point shoot() {
        if (self.getActionPoints() < self.getShotCost()) return null;

        for (Trooper enemy : enemies) {
            if (isVisible(self.getShootingRange(), enemy)) {
                return Point.byUnit(enemy);
            }
        }

        return null;
    }

    @Nullable
    private Direction move() {
        if (self.getActionPoints() < getMoveCost()) return null;

        Point target = self.getType() == TrooperType.COMMANDER
                ? army.getDislocation(board)
                : army.getCommanderLocation();

        return new BestPathFinder(board).findFirstMove(Point.byUnit(self), target);
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
