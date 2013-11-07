import model.ActionType;
import model.Direction;
import model.Move;

import static model.ActionType.*;

public class Go {
    private final ActionType type;
    private final Direction direction;
    private final Point point;

    private Go(@NotNull ActionType type, @Nullable Direction direction, @Nullable Point point) {
        this.type = type;
        this.direction = direction;
        this.point = point;
    }

    public void execute(@NotNull Move move) {
        assert direction == null || point == null : "Both direction and point are present for action: " + this;
        move.setAction(type);
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
        StringBuilder sb = new StringBuilder(type.toString());
        if (direction != null) {
            sb.append(" ").append(direction);
        }
        if (point != null) {
            sb.append(" ").append(point);
        }
        return sb.toString();
    }
}
