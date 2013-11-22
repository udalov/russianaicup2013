import model.*;

import static model.ActionType.*;
import static model.TrooperStance.PRONE;
import static model.TrooperStance.STANDING;
import static model.TrooperType.COMMANDER;
import static model.TrooperType.FIELD_MEDIC;

public class Go {
    private final ActionType action;
    private final Direction direction;
    private final Point point;

    private Go(@NotNull ActionType action, @Nullable Direction direction, @Nullable Point point) {
        this.action = action;
        this.direction = direction;
        this.point = point;
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
        return new Go(END_TURN, null, null);
    }

    @NotNull
    public static Go move(@NotNull Direction direction) {
        return new Go(MOVE, direction, null);
    }

    @NotNull
    public static Go shoot(@NotNull Point point) {
        return new Go(SHOOT, null, point);
    }

    @NotNull
    public static Go raiseStance() {
        return new Go(RAISE_STANCE, null, null);
    }

    @NotNull
    public static Go lowerStance() {
        return new Go(LOWER_STANCE, null, null);
    }

    @NotNull
    public static Go throwGrenade(@NotNull Point point) {
        return new Go(THROW_GRENADE, null, point);
    }

    @NotNull
    public static Go useMedikit(@NotNull Direction direction) {
        return new Go(USE_MEDIKIT, direction, null);
    }

    @NotNull
    public static Go eatFieldRation() {
        return new Go(EAT_FIELD_RATION, null, null);
    }

    @NotNull
    public static Go heal(@NotNull Direction direction) {
        return new Go(HEAL, direction, null);
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
