import model.Bonus;
import model.BonusType;
import model.Direction;
import model.TrooperStance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public final int[] enemyHp;
    // Indexed by WarriorTurn.allies
    public final int[] allyHp;
    // id of collected bonuses
    public final int[] collected;
    public final PointSet seen;

    public final List<Warrior> allies;

    private final int hashCode;

    public Position(@NotNull Situation situation, @NotNull Point me, @NotNull TrooperStance stance, int actionPoints, int bonuses,
                    @NotNull int[] enemyHp, @NotNull int[] allyHp, @NotNull int[] collected, @NotNull PointSet seen) {
        this.situation = situation;
        this.me = me;
        this.stance = stance;
        this.actionPoints = actionPoints;
        this.bonuses = bonuses;
        this.enemyHp = enemyHp;
        this.allyHp = allyHp;
        this.collected = collected;
        this.seen = seen;

        this.allies = new ArrayList<>(situation.allies);
        this.allies.set(situation.self.index, new Warrior(situation.self, me, stance));

        int hash = me.hashCode();
        hash = 31 * hash + stance.hashCode();
        hash = 31 * hash + actionPoints;
        hash = 31 * hash + bonuses;
        hash = 31 * hash + Arrays.hashCode(enemyHp);
        hash = 31 * hash + Arrays.hashCode(allyHp);
        hash = 31 * hash + Arrays.hashCode(collected);
        this.hashCode = hash;
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
        if (!situation.board.isPassable(point)) return false;
        for (Warrior ally : allies) {
            if (point.equals(ally.point)) return false;
        }
        for (EnemyWarrior enemy : situation.enemies) {
            if (enemyHp[enemy.index] > 0 && point.equals(enemy.point)) return false;
        }
        return true;
    }

    private double effectiveShootingRange() {
        double result = situation.self.getShootingRange();
        if (situation.self.type == SNIPER) {
            result -= sniperShootingRangeBonus(situation.self.stance);
            result += sniperShootingRangeBonus(stance);
        }
        return result;
    }

    private double sniperShootingRangeBonus(@NotNull TrooperStance stance) {
        switch (stance) {
            case STANDING: return situation.game.getSniperStandingShootingRangeBonus();
            case KNEELING: return situation.game.getSniperKneelingShootingRangeBonus();
            case PRONE: return situation.game.getSniperProneShootingRangeBonus();
            default: throw new IllegalStateException("Sniper is so stealth, he's " + stance);
        }
    }

    @NotNull
    public int[] grenadeEffect(@NotNull Point target, @NotNull List<? extends Warrior> warriors, @NotNull int[] hp) {
        int[] result = null;
        for (Warrior warrior : warriors) {
            int was = hp[warrior.index];
            int now = grenadeEffectToTrooper(target, warrior.point, was);
            if (was != now) {
                if (result == null) {
                    result = Arrays.copyOf(hp, hp.length);
                }
                result[warrior.index] = now;
            }
        }
        return result != null ? result : hp;
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
        StringBuilder sb = new StringBuilder(actionPoints + " " + stance + " at " + me);
        if (bonuses != 0) {
            sb.append(" with");
            for (BonusType bonus : BonusType.values()) {
                sb.append(" ").append(bonus);
            }
        }
        return sb.toString();
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
        PointSet newSeen = MakeTurn.computeSeenForPosition(situation, point, stance, seen);
        return new Position(situation, point, stance, ap, newBonuses, enemyHp, allyHp, newCollected, newSeen);
    }

    @Nullable
    public Position shoot(@NotNull EnemyWarrior enemy) {
        int ap = actionPoints - situation.self.getShootCost();
        if (ap < 0) return null;
        int hp = enemyHp[enemy.index];
        if (hp == 0) return null;
        if (!situation.isReachable(effectiveShootingRange(), me, stance, enemy.point, enemy.stance)) return null;
        int[] newEnemyHp = IntArrays.replace(enemyHp, enemy.index, Math.max(hp - situation.self.getDamage(stance), 0));
        return new Position(situation, me, stance, ap, bonuses, newEnemyHp, allyHp, collected, seen);
    }

    @Nullable
    public Position raiseStance() {
        int ap = actionPoints - situation.game.getStanceChangeCost();
        if (ap < 0) return null;
        TrooperStance newStance = Util.higher(stance);
        if (newStance == null) return null;
        PointSet newSeen = MakeTurn.computeSeenForPosition(situation, me, newStance, seen);
        return new Position(situation, me, newStance, ap, bonuses, enemyHp, allyHp, collected, newSeen);
    }

    @Nullable
    public Position lowerStance() {
        int ap = actionPoints - situation.game.getStanceChangeCost();
        if (ap < 0) return null;
        TrooperStance newStance = Util.lower(stance);
        if (newStance == null) return null;
        PointSet newSeen = MakeTurn.computeSeenForPosition(situation, me, newStance, seen);
        return new Position(situation, me, newStance, ap, bonuses, enemyHp, allyHp, collected, newSeen);
    }

    @Nullable
    public Position throwGrenade(@NotNull Point target) {
        if (!has(GRENADE)) return null;
        int ap = actionPoints - situation.game.getGrenadeThrowCost();
        if (ap < 0) return null;
        if (!me.withinEuclidean(target, situation.game.getGrenadeThrowRange())) return null;
        int[] newEnemyHp = grenadeEffect(target, situation.enemies, enemyHp);
        if (Arrays.equals(enemyHp, newEnemyHp)) return null;
        int[] newAllyHp = grenadeEffect(target, allies, allyHp);
        // TODO: update collected
        return new Position(situation, me, stance, ap, without(GRENADE), newEnemyHp, newAllyHp, collected, seen);
    }

    @Nullable
    public Position useMedikit(@NotNull Warrior ally) {
        if (!has(MEDIKIT)) return null;
        int ap = actionPoints - situation.game.getMedikitUseCost();
        if (ap < 0) return null;
        int[] newAllyHp;
        if (ally.point.equals(me)) newAllyHp = healEffect(ally.index, situation.game.getMedikitHealSelfBonusHitpoints());
        else if (ally.point.isNeighbor(me)) newAllyHp = healEffect(ally.index, situation.game.getMedikitBonusHitpoints());
        else return null;
        if (Arrays.equals(allyHp, newAllyHp)) return null;
        // TODO: update collected
        return new Position(situation, me, stance, ap, without(MEDIKIT), enemyHp, newAllyHp, collected, seen);
    }

    @Nullable
    public Position eatFieldRation() {
        if (!has(FIELD_RATION)) return null;
        if (actionPoints >= situation.self.getInitialActionPoints()) return null;
        int ap = actionPoints - situation.game.getFieldRationEatCost();
        if (ap < 0) return null;
        ap = Math.min(situation.self.getInitialActionPoints(), ap + situation.game.getFieldRationBonusActionPoints());
        // TODO: update collected
        return new Position(situation, me, stance, ap, without(FIELD_RATION), enemyHp, allyHp, collected, seen);
    }

    @Nullable
    public Position heal(@NotNull Warrior ally) {
        int ap = actionPoints - situation.game.getFieldMedicHealCost();
        if (ap < 0) return null;
        int[] newAllyHp;
        if (ally.point.equals(me)) newAllyHp = healEffect(ally.index, situation.game.getFieldMedicHealSelfBonusHitpoints());
        else if (ally.point.isNeighbor(me)) newAllyHp = healEffect(ally.index, situation.game.getFieldMedicHealBonusHitpoints());
        else return null;
        if (Arrays.equals(allyHp, newAllyHp)) return null;
        return new Position(situation, me, stance, ap, bonuses, enemyHp, newAllyHp, collected, seen);
    }
}
