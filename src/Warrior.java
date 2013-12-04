import model.Trooper;
import model.TrooperStance;
import model.TrooperType;

public class Warrior {
    private final Trooper trooper;

    public final Point point;
    public final TrooperStance stance;
    public final TrooperType type;
    public final int index;

    public Warrior(@NotNull Trooper trooper, int index) {
        this.trooper = trooper;
        this.point = Point.create(trooper);
        this.stance = trooper.getStance();
        this.type = trooper.getType();
        this.index = index;
    }

    @NotNull
    @Deprecated
    public Trooper trooper() {
        return trooper;
    }

    public double getShootingRange() {
        return trooper.getShootingRange();
    }

    public int getMaximalHitpoints() {
        return trooper.getMaximalHitpoints();
    }

    public int getShootCost() {
        return trooper.getShootCost();
    }

    public int getDamage(@NotNull TrooperStance stance) {
        return trooper.getDamage(stance);
    }

    public int getInitialActionPoints() {
        return trooper.getInitialActionPoints();
    }

    public long getPlayerId() {
        return trooper.getPlayerId();
    }

    @Override
    public String toString() {
        return trooper.getActionPoints() + " " + stance + " " + type + " " + trooper.getHitpoints() + "% at " + point;
    }
}
