import model.Trooper;
import model.TrooperStance;
import model.TrooperType;

public class Warrior {
    public final Point point;
    public final TrooperStance stance;
    public final TrooperType type;
    public final int index;

    protected final Trooper trooper;

    private Warrior(int index, @NotNull Trooper trooper, @NotNull Point point, @NotNull TrooperStance stance) {
        this.index = index;
        this.trooper = trooper;
        this.point = point;
        this.stance = stance;
        this.type = trooper.getType();
    }

    public Warrior(int index, @NotNull Trooper trooper) {
        this(index, trooper, Point.create(trooper), trooper.getStance());
    }

    public Warrior(@NotNull Warrior self, @NotNull Point point, @NotNull TrooperStance stance) {
        this(self.index, self.trooper, point, stance);
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

    public double getVisionRange() {
        return trooper.getVisionRange();
    }

    @Override
    public int hashCode() {
        return (int) trooper.getId();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Warrior && ((Warrior) obj).trooper.getId() == trooper.getId();
    }

    @Override
    public String toString() {
        return trooper.getActionPoints() + " " + stance + " " + type + " " + trooper.getHitpoints() + "% at " + point;
    }
}
