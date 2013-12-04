import model.Trooper;

public class EnemyWarrior extends Warrior {
    public EnemyWarrior(int index, @NotNull Trooper trooper) {
        super(index, trooper);
    }

    public boolean isHoldingFieldRation() {
        return trooper.isHoldingFieldRation();
    }

    public boolean isHoldingGrenade() {
        return trooper.isHoldingGrenade();
    }

    public int getDamage() {
        return trooper.getDamage();
    }

    public double getVisionRange() {
        return trooper.getVisionRange();
    }
}
