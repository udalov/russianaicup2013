import model.*;

import java.util.Arrays;

import static model.BonusType.*;
import static model.TrooperType.SNIPER;

public class Position {
    public final Situation situation;
    public final Point me;
    public final TrooperStance stance;
    public final int actionPoints;
    // Indexed by BonusType.ordinal()
    public final int bonuses;
    // Indexed by WarriorTurn.enemies
    // TODO: create and save list of points of troopers?
    public final int[] enemyHp;
    // Indexed by WarriorTurn.allies
    public final int[] allyHp;
    // id of collected bonuses
    public final int[] collected;

    private final int hashCode;

    public Position(@NotNull Situation situation, @NotNull Point me, @NotNull TrooperStance stance, int actionPoints, int bonuses, @NotNull int[] enemyHp,
                    @NotNull int[] allyHp, @NotNull int[] collected) {
        this.situation = situation;
        this.me = me;
        this.stance = stance;
        this.actionPoints = actionPoints;
        this.bonuses = bonuses;
        this.enemyHp = enemyHp;
        this.allyHp = allyHp;
        this.collected = collected;

        int hash = me.hashCode();
        hash = 31 * hash + stance.hashCode();
        hash = 31 * hash + actionPoints;
        hash = 31 * hash + bonuses;
        hash = 31 * hash + Arrays.hashCode(enemyHp);
        hash = 31 * hash + Arrays.hashCode(allyHp);
        hash = 31 * hash + Arrays.hashCode(collected);
        this.hashCode = hash;
    }

    @NotNull
    public Iterable<Pair<Integer, Point>> allies() {
        return Util.iterable(new Util.AbstractIterator<Pair<Integer, Point>>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < situation.allies.size();
            }

            @Override
            public Pair<Integer, Point> next() {
                Point ally = i == situation.myIndex ? me : Point.create(situation.allies.get(i));
                return new Pair<>(i++, ally);
            }
        });
    }

    @NotNull
    public Iterable<Trooper> aliveEnemies() {
        return Util.iterable(new Util.AbstractIterator<Trooper>() {
            private final int size = situation.enemies.size();
            private int i = 0;

            @Override
            public boolean hasNext() {
                while (i < size) {
                    if (enemyHp[i] > 0) return true;
                    i++;
                }
                return false;
            }

            @Override
            public Trooper next() {
                while (!hasNext());
                return situation.enemies.get(i++);
            }
        });
    }

    public boolean has(@NotNull BonusType bonus) {
        return (bonuses & (1 << bonus.ordinal())) != 0;
    }

    private int without(@NotNull BonusType bonus) {
        return bonuses & ~(1 << bonus.ordinal());
    }

    private int with(@NotNull BonusType bonus) {
        return bonuses | (1 << bonus.ordinal());
    }

    private boolean isPassablePoint(@NotNull Point point) {
        if (!situation.army.board.isPassable(point)) return false;
        for (Pair<Integer, Point> pair : allies()) {
            if (point.equals(pair.second)) return false;
        }
        for (Trooper enemy : aliveEnemies()) {
            if (point.isEqualTo(enemy)) return false;
        }
        return true;
    }

    private double effectiveShootingRange() {
        double result = situation.self.getShootingRange();
        if (situation.self.getType() == SNIPER) {
            result -= sniperShootingRangeBonus(situation.self.getStance());
            result += sniperShootingRangeBonus(stance);
        }
        return result;
    }

    private double sniperShootingRangeBonus(@NotNull TrooperStance stance) {
        switch (stance) {
            case STANDING: return 0;
            case KNEELING: return situation.game.getSniperKneelingShootingRangeBonus();
            case PRONE: return situation.game.getSniperProneShootingRangeBonus();
            default: throw new IllegalStateException("Sniper is so stealth, he's " + stance);
        }
    }

    @NotNull
    private int[] grenadeEffectToEnemies(@NotNull Point target) {
        int size = enemyHp.length;
        int[] result = new int[size];
        for (int i = 0; i < size; i++) {
            result[i] = grenadeEffectToTrooper(target, Point.create(situation.enemies.get(i)), enemyHp[i]);
        }
        return result;
    }

    @NotNull
    public int[] grenadeEffectToAllies(@NotNull Point target) {
        int[] result = new int[allyHp.length];
        for (Pair<Integer, Point> pair : allies()) {
            int i = pair.first;
            result[i] = grenadeEffectToTrooper(target, pair.second, allyHp[i]);
        }
        return result;
    }

    private int grenadeEffectToTrooper(@NotNull Point target, @NotNull Point trooper, int hitpoints) {
        if (trooper.equals(target)) {
            return Math.max(hitpoints - situation.game.getGrenadeDirectDamage(), 0);
        } else if (trooper.isNeighbor(target)) {
            return Math.max(hitpoints - situation.game.getGrenadeCollateralDamage(), 0);
        } else {
            return hitpoints;
        }
    }

    @NotNull
    private int[] healEffect(int ally, int healingBonus) {
        // Relies on the fact that maximal hitpoints are the same for every trooper
        int maxHp = situation.self.getMaximalHitpoints();
        int hp = allyHp[ally];
        if (hp >= maxHp) return allyHp;

        return IntArrays.replace(allyHp, ally, Math.min(hp + healingBonus, maxHp));
    }

    @Nullable
    private Bonus maybeCollectBonus(@NotNull Point point) {
        for (Bonus bonus : situation.bonuses) {
            if (point.isEqualTo(bonus)) {
                return has(bonus.getType()) || IntArrays.contains(collected, (int) bonus.getId()) ? null : bonus;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position that = (Position) o;

        return hashCode == that.hashCode &&
                actionPoints == that.actionPoints &&
                bonuses == that.bonuses &&
                stance == that.stance &&
                me.equals(that.me) &&
                Arrays.equals(allyHp, that.allyHp) &&
                Arrays.equals(enemyHp, that.enemyHp) &&
                Arrays.equals(collected, that.collected);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return actionPoints + " " + stance + " at " + me;
    }

    // -------------------------------------------------------------------------

    @Nullable
    public Position move(@NotNull Direction direction) {
        int ap = actionPoints - situation.getMoveCost(stance);
        if (ap < 0) return null;
        Point point = me.go(direction);
        if (point == null || !isPassablePoint(point)) return null;
        Bonus bonus = maybeCollectBonus(point);
        int newBonuses = bonus == null ? bonuses : with(bonus.getType());
        int[] newCollected = bonus == null ? collected : IntArrays.add(collected, (int) bonus.getId());
        return new Position(situation, point, stance, ap, newBonuses, enemyHp, allyHp, newCollected);
    }

    @Nullable
    public Position shoot(int enemy) {
        int ap = actionPoints - situation.self.getShootCost();
        if (ap < 0) return null;
        int hp = enemyHp[enemy];
        if (hp == 0) return null;
        Trooper trooper = situation.enemies.get(enemy);
        if (!situation.isReachable(effectiveShootingRange(), me, stance, trooper)) return null;
        int[] newEnemyHp = IntArrays.replace(enemyHp, enemy, Math.max(hp - situation.self.getDamage(stance), 0));
        return new Position(situation, me, stance, ap, bonuses, newEnemyHp, allyHp, collected);
    }

    @Nullable
    public Position raiseStance() {
        int ap = actionPoints - situation.game.getStanceChangeCost();
        if (ap < 0) return null;
        TrooperStance newStance = Util.higher(stance);
        if (newStance == null) return null;
        return new Position(situation, me, newStance, ap, bonuses, enemyHp, allyHp, collected);
    }

    @Nullable
    public Position lowerStance() {
        int ap = actionPoints - situation.game.getStanceChangeCost();
        if (ap < 0) return null;
        TrooperStance newStance = Util.lower(stance);
        if (newStance == null) return null;
        return new Position(situation, me, newStance, ap, bonuses, enemyHp, allyHp, collected);
    }

    @Nullable
    public Position throwGrenade(@NotNull Point target) {
        if (!has(GRENADE)) return null;
        int ap = actionPoints - situation.game.getGrenadeThrowCost();
        if (ap < 0) return null;
        if (!me.withinEuclidean(target, situation.game.getGrenadeThrowRange())) return null;
        int[] newEnemyHp = grenadeEffectToEnemies(target);
        if (Arrays.equals(enemyHp, newEnemyHp)) return null;
        int[] newAllyHp = grenadeEffectToAllies(target);
        return new Position(situation, me, stance, ap, without(GRENADE), newEnemyHp, newAllyHp, collected);
    }

    @Nullable
    public Position useMedikit(int ally, @NotNull Point point) {
        if (!has(MEDIKIT)) return null;
        int ap = actionPoints - situation.game.getMedikitUseCost();
        if (ap < 0) return null;
        int[] newAllyHp;
        if (point.equals(me)) newAllyHp = healEffect(ally, situation.game.getMedikitHealSelfBonusHitpoints());
        else if (point.isNeighbor(me)) newAllyHp = healEffect(ally, situation.game.getMedikitBonusHitpoints());
        else return null;
        if (Arrays.equals(allyHp, newAllyHp)) return null;
        return new Position(situation, me, stance, ap, without(MEDIKIT), enemyHp, newAllyHp, collected);
    }

    @Nullable
    public Position eatFieldRation() {
        if (!has(FIELD_RATION)) return null;
        if (actionPoints >= situation.self.getInitialActionPoints()) return null;
        int ap = actionPoints - situation.game.getFieldRationEatCost();
        if (ap < 0) return null;
        return new Position(situation, me, stance, ap, without(FIELD_RATION), enemyHp, allyHp, collected);
    }

    @Nullable
    public Position heal(int ally, @NotNull Point point) {
        int ap = actionPoints - situation.game.getFieldMedicHealCost();
        if (ap < 0) return null;
        int[] newAllyHp;
        if (point.equals(me)) newAllyHp = healEffect(ally, situation.game.getFieldMedicHealSelfBonusHitpoints());
        else if (point.isNeighbor(me)) newAllyHp = healEffect(ally, situation.game.getFieldMedicHealBonusHitpoints());
        else return null;
        if (Arrays.equals(allyHp, newAllyHp)) return null;
        return new Position(situation, me, stance, ap, without(MEDIKIT), enemyHp, newAllyHp, collected);
    }
}
