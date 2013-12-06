import model.*;

import static model.ActionType.*;
import static model.Direction.*;
import static model.TrooperStance.PRONE;
import static model.TrooperStance.STANDING;
import static model.TrooperType.COMMANDER;
import static model.TrooperType.FIELD_MEDIC;

public final class Go {
    private static final Go GO_END_TURN = new Go(END_TURN);
    private static final Go GO_RAISE_STANCE = new Go(RAISE_STANCE);
    private static final Go GO_LOWER_STANCE = new Go(LOWER_STANCE);
    private static final Go GO_EAT_FIELD_RATION = new Go(EAT_FIELD_RATION);

    private static final Go GO_MOVE_CURRENT_POINT = new Go(MOVE, CURRENT_POINT);
    private static final Go GO_MOVE_NORTH = new Go(MOVE, NORTH);
    private static final Go GO_MOVE_EAST = new Go(MOVE, EAST);
    private static final Go GO_MOVE_SOUTH = new Go(MOVE, SOUTH);
    private static final Go GO_MOVE_WEST = new Go(MOVE, WEST);

    private static final Go GO_USE_MEDIKIT_CURRENT_POINT = new Go(USE_MEDIKIT, CURRENT_POINT);
    private static final Go GO_USE_MEDIKIT_NORTH = new Go(USE_MEDIKIT, NORTH);
    private static final Go GO_USE_MEDIKIT_EAST = new Go(USE_MEDIKIT, EAST);
    private static final Go GO_USE_MEDIKIT_SOUTH = new Go(USE_MEDIKIT, SOUTH);
    private static final Go GO_USE_MEDIKIT_WEST = new Go(USE_MEDIKIT, WEST);

    private static final Go GO_HEAL_CURRENT_POINT = new Go(HEAL, CURRENT_POINT);
    private static final Go GO_HEAL_NORTH = new Go(HEAL, NORTH);
    private static final Go GO_HEAL_EAST = new Go(HEAL, EAST);
    private static final Go GO_HEAL_SOUTH = new Go(HEAL, SOUTH);
    private static final Go GO_HEAL_WEST = new Go(HEAL, WEST);

    private final ActionType action;
    private final Direction direction;
    private final Point point;

    private Go(@NotNull ActionType action, @Nullable Direction direction, @Nullable Point point) {
        this.action = action;
        this.direction = direction;
        this.point = point;
    }

    private Go(@NotNull ActionType action, @Nullable Direction direction) {
        this(action, direction, null);
    }

    private Go(@NotNull ActionType action, @Nullable Point point) {
        this(action, null, point);
    }

    private Go(@NotNull ActionType action) {
        this(action, null, null);
    }

    public void execute(@NotNull Move move) {
        assert direction == null || point == null : "Both direction and point are present for action: " + this;
        move.setAction(action);
        if (direction != null) {
            move.setDirection(direction);
        }
        if (point != null) {
            move.setX(point.x);
            move.setY(point.y);
        }
    }

    @NotNull
    public static Go endTurn() {
        return GO_END_TURN;
    }

    @NotNull
    public static Go move(@NotNull Direction direction) {
        switch (direction) {
            case CURRENT_POINT: return GO_MOVE_CURRENT_POINT;
            case NORTH: return GO_MOVE_NORTH;
            case EAST: return GO_MOVE_EAST;
            case SOUTH: return GO_MOVE_SOUTH;
            case WEST: return GO_MOVE_WEST;
            default: return new Go(MOVE, direction);
        }
    }

    @NotNull
    public static Go shoot(@NotNull Point point) {
        return new Go(SHOOT, point);
    }

    @NotNull
    public static Go raiseStance() {
        return GO_RAISE_STANCE;
    }

    @NotNull
    public static Go lowerStance() {
        return GO_LOWER_STANCE;
    }

    @NotNull
    public static Go throwGrenade(@NotNull Point point) {
        return new Go(THROW_GRENADE, point);
    }

    @NotNull
    public static Go useMedikit(@NotNull Direction direction) {
        switch (direction) {
            case CURRENT_POINT: return GO_USE_MEDIKIT_CURRENT_POINT;
            case NORTH: return GO_USE_MEDIKIT_NORTH;
            case EAST: return GO_USE_MEDIKIT_EAST;
            case SOUTH: return GO_USE_MEDIKIT_SOUTH;
            case WEST: return GO_USE_MEDIKIT_WEST;
            default: return new Go(USE_MEDIKIT, direction);
        }
    }

    @NotNull
    public static Go eatFieldRation() {
        return GO_EAT_FIELD_RATION;
    }

    @NotNull
    public static Go heal(@NotNull Direction direction) {
        switch (direction) {
            case CURRENT_POINT: return GO_HEAL_CURRENT_POINT;
            case NORTH: return GO_HEAL_NORTH;
            case EAST: return GO_HEAL_EAST;
            case SOUTH: return GO_HEAL_SOUTH;
            case WEST: return GO_HEAL_WEST;
            default: return new Go(HEAL, direction);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Go)) return false;
        Go that = (Go) o;

        return action == that.action &&
                direction == that.direction &&
                (point == null ? that.point == null : point.equals(that.point));
    }

    @Override
    public int hashCode() {
        int result = action.hashCode();
        result = 31 * result + (direction != null ? direction.hashCode() : 0);
        result = 31 * result + (point != null ? point.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(action.toString());
        if (direction != null) {
            sb.append(" ").append(direction);
        }
        if (point != null) {
            sb.append(" ").append(point);
        }
        return sb.toString();
    }

    public void validate(@NotNull Trooper self, @NotNull World world, @NotNull Game game) {
        new Validator(self, world, game).validate();
    }

    // It's important that this code is in a whole separate place, where sudden bugs from the main procedure can't reach it
    // TODO: validate other things beside action points
    private class Validator {
        private final Trooper self;
        private final World world;
        private final Game game;
        private final CellType[][] cells;

        private Validator(@NotNull Trooper self, @NotNull World world, @NotNull Game game) {
            this.self = self;
            this.world = world;
            this.game = game;
            this.cells = world.getCells();
        }

        public void validate() {
            switch (action) {
                case END_TURN:
                    break;
                case MOVE:
                    validateMove();
                    break;
                case SHOOT:
                    validateShoot();
                    break;
                case RAISE_STANCE:
                    validateRaiseStance();
                    break;
                case LOWER_STANCE:
                    validateLowerStance();
                    break;
                case THROW_GRENADE:
                    validateThrowGrenade();
                    break;
                case USE_MEDIKIT:
                    validateUseMedikit();
                    break;
                case EAT_FIELD_RATION:
                    assert can(game.getFieldRationEatCost());
                    assert self.isHoldingFieldRation();
                    break;
                case HEAL:
                    validateHeal();
                    break;
                case REQUEST_ENEMY_DISPOSITION:
                    assert can(game.getCommanderRequestEnemyDispositionCost());
                    assert self.getType() == COMMANDER;
                    break;
                default:
                    assert false: "Uh-oh: " + action;
            }
        }

        private boolean can(int cost) {
            return self.getActionPoints() >= cost;
        }

        private void validateMove() {
            switch (self.getStance()) {
                case PRONE: assert can(game.getProneMoveCost()); break;
                case KNEELING: assert can(game.getKneelingMoveCost()); break;
                case STANDING: assert can(game.getStandingMoveCost()); break;
                default: assert false: "I am " + self.getStance() + " right here, dude"; break;
            }
        }

        private void validateShoot() {
            assert can(self.getShootCost());
        }

        private void validateRaiseStance() {
            assert can(game.getStanceChangeCost());
            assert self.getStance() != STANDING;
        }

        private void validateLowerStance() {
            assert can(game.getStanceChangeCost());
            assert self.getStance() != PRONE;
        }

        private void validateThrowGrenade() {
            assert can(game.getGrenadeThrowCost());
            assert self.isHoldingGrenade();
        }

        private void validateUseMedikit() {
            assert can(game.getMedikitUseCost());
            assert self.isHoldingMedikit();
        }

        private void validateHeal() {
            assert can(game.getFieldMedicHealCost());
            assert self.getType() == FIELD_MEDIC;
        }
    }
}
