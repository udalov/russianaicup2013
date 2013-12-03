import model.Direction;
import model.Trooper;
import model.TrooperStance;
import model.TrooperType;

import java.util.*;

import static model.BonusType.*;
import static model.TrooperType.*;

public abstract class Scorer {
    protected final Situation situation;
    protected final Const coeff;

    public Scorer(@NotNull Situation situation) {
        this.situation = situation;
        this.coeff = situation.army.coeff;
    }

    public final double evaluate(@NotNull Position p) {
        double result = 0;

        result += coeff.weightedHpOfAllies * weightedHpOfAllies(p.allyHp);

        // TODO: or if have a medikit
        if (situation.self.getType() == FIELD_MEDIC) {
            result -= coeff.medicDistanceToWoundedAllies * distanceToWoundedAllies(p);
        }

        result += coeff.underCommanderAura * underCommanderAura(p);

        result += situationSpecificScore(p);

        return result;
    }

    private int underCommanderAura(@NotNull Position p) {
        Point commander = null;
        for (Pair<Integer, Point> pair : p.allies()) {
            if (situation.allies.get(pair.first).getType() == COMMANDER) commander = pair.second;
        }
        if (commander == null) return 0;

        int result = 0;
        for (Pair<Integer, Point> pair : p.allies()) {
            TrooperType type = situation.allies.get(pair.first).getType();
            if (type != COMMANDER && type != SCOUT) {
                if (pair.second.euclideanDistance(p.me) <= situation.game.getCommanderAuraRange()) result++;
            }
        }
        return result;
    }

    private double weightedHpOfAllies(@NotNull int[] allyHp) {
        double result = 0;
        for (int hp : allyHp) {
            // TODO: these coefficients
            result += 2 * Math.min(hp, coeff.maxHpToHeal) + 0.2 * Math.max(hp - coeff.maxHpToHeal, 0);
        }
        return result;
    }

    private double distanceToWoundedAllies(@NotNull Position p) {
        double result = 0;
        for (int i = 0, size = situation.allies.size(); i < size; i++) {
            if (i == situation.myIndex) continue;
            Trooper ally = situation.allies.get(i);
            Integer dist = situation.army.board.distance(Point.create(ally), p.me);
            if (dist == null || dist == 0) continue;
            int toHeal = ally.getMaximalHitpoints() - p.allyHp[i];
            if (toHeal > 60) result += 3 * dist;
            else if (toHeal > 15) result += dist;
            else if (toHeal > 0) result += 0.1 * dist;
        }
        return result;
    }

    protected abstract double situationSpecificScore(@NotNull Position p);

    public static class Leader extends Scorer {
        private final Point wayPoint;

        public Leader(@NotNull Situation situation) {
            super(situation);
            wayPoint = situation.army.getOrUpdateWayPoint(situation.allies);
        }

        @Override
        protected double situationSpecificScore(@NotNull Position p) {
            double result = 0;

            result += 3 * Integer.bitCount(p.bonuses);
            if (p.has(GRENADE)) result += coeff.hasGrenadeInMovement;
            if (p.has(MEDIKIT)) result += coeff.hasMedikitInMovement;
            if (p.has(FIELD_RATION)) result += coeff.hasFieldRationInMovement;

            result -= coeff.leaderDistanceToWayPoint * distanceToWayPoint(p);

            result -= coeff.leaderFarAwayTeammates * farAwayTeammates(p);

            return result;
        }

        private int farAwayTeammates(@NotNull Position p) {
            int result = 0;
            for (int i = 0, size = situation.allies.size(); i < size; i++) {
                if (i == situation.myIndex) continue;
                Integer distance = situation.army.board.distance(Point.create(situation.allies.get(i)), p.me);
                if (distance != null && distance > coeff.leaderCriticalDistanceToAllies) result++;
            }
            return result;
        }

        private int distanceToWayPoint(@NotNull Position p) {
            Integer dist = situation.army.board.distance(p.me, wayPoint);
            return dist != null ? dist : 1000;
        }
    }

    public static class Follower extends Scorer {
        private final Point leader;
        private final Point wayPoint;
        private final Set<Point> set = new PointSet();
        private final ArrayDeque<Point> queue = new ArrayDeque<>(15);
        private final List<Point> leaderPath;

        public Follower(@NotNull Situation situation, @NotNull Trooper leader) {
            super(situation);
            this.leader = Point.create(leader);
            this.wayPoint = situation.army.getOrUpdateWayPoint(situation.allies);
            List<Point> leaderPath = situation.army.board.findPath(this.leader, wayPoint);
            this.leaderPath = leaderPath == null ? Collections.<Point>emptyList() : leaderPath;
        }

        @Override
        protected double situationSpecificScore(@NotNull Position p) {
            double result = 0;

            if (p.has(GRENADE)) result += coeff.hasGrenadeInMovement;
            if (p.has(MEDIKIT)) result += coeff.hasMedikitInMovement;
            if (p.has(FIELD_RATION)) result += coeff.hasFieldRationInMovement;

            Integer dist = situation.army.board.distance(p.me, leader);
            if (dist != null) result -= coeff.followerDistanceToLeader * dist;

            int freeCells = leaderDegreeOfFreedom(p);
            result -= coeff.leaderDegreeOfFreedom * Math.max(5 - freeCells, 0);

            if (isBlockingLeader(p)) result -= coeff.isFollowerBlockingLeader;

            return result;
        }

        private boolean isBlockingLeader(@NotNull Position p) {
            for (Point point : leaderPath) {
                if (p.me.equals(point)) return true;
            }
            return false;
        }

        private int leaderDegreeOfFreedom(@NotNull Position p) {
            set.clear();
            queue.clear();

            for (Pair<Integer, Point> ally : p.allies()) {
                set.add(ally.second);
            }
            queue.add(leader);

            int result = 1;
            while (!queue.isEmpty()) {
                Point point = queue.poll();
                for (Direction direction : Util.DIRECTIONS) {
                    Point q = point.go(direction);
                    if (q != null && situation.army.board.isPassable(q) && !set.contains(q)) {
                        set.add(q);
                        queue.add(q);
                        if (++result == 5) return result;
                    }
                }
            }

            return result;
        }
    }

    public static class CombatSituation extends Scorer {
        private final Map<Long, Integer> enemyTeams = new HashMap<>(6);

        public CombatSituation(@NotNull Situation situation) {
            super(situation);
            for (Trooper enemy : situation.enemies) {
                long id = enemy.getPlayerId();
                if (!enemyTeams.containsKey(id)) {
                    enemyTeams.put(id, enemyTeams.size());
                }
            }
        }

        @Override
        protected double situationSpecificScore(@NotNull Position p) {
            double result = 0;

            result -= coeff.enemyHp * IntArrays.sum(p.enemyHp);
            result += coeff.killEnemy * IntArrays.numberOfZeros(p.enemyHp);

            result -= coeff.enemyTeamsThatSeeUs * enemyTeamsThatSeeUs(p);

            result -= coeff.expectedDamageOnNextTurn * expectedDamageOnNextTurn(p);

            result += coeff.bonusInCombat * Integer.bitCount(p.bonuses);

            result -= coeff.distanceToAlliesInCombat * distanceToAllies(p);

            if (closestEnemy(p) < 8 /* TODO */) {
                result -= coeff.combatStance * p.stance.ordinal();
            }

            return result;
        }

        private int enemyTeamsThatSeeUs(@NotNull Position p) {
            int bitset = 0;
            for (Trooper enemy : p.aliveEnemies()) {
                for (Pair<Integer, Point> pair : p.allies()) {
                    TrooperStance stance = pair.first == situation.myIndex ? p.stance : situation.allies.get(pair.first).getStance();
                    if (situation.isReachable(enemy.getVisionRange(), enemy, pair.second, stance)) {
                        bitset |= 1 << enemyTeams.get(enemy.getPlayerId());
                    }
                }
            }
            return Integer.bitCount(bitset);
        }

        private double closestEnemy(@NotNull Position p) {
            double closestEnemy = 1e100;
            for (Trooper enemy : p.aliveEnemies()) {
                closestEnemy = Math.min(closestEnemy, Point.create(enemy).euclideanDistance(p.me));
            }
            return closestEnemy;
        }

        private double distanceToAllies(@NotNull Position p) {
            double result = 0;
            for (int i = 0, size = situation.allies.size(); i < size; i++) {
                if (i == situation.myIndex) continue;
                Integer dist = situation.army.board.distance(Point.create(situation.allies.get(i)), p.me);
                if (dist != null) result += dist;
            }
            return result;
        }

        private double expectedDamageOnNextTurn(@NotNull Position p) {
            // Assume that all enemies see us, but this is not always true
            // TODO: count number of other teams having at least one trooper who sees us

            int n = situation.allies.size();

            double[] expectedDamage = new double[n];

            for (Trooper enemy : p.aliveEnemies()) {
                int actionPoints = enemy.getInitialActionPoints();
                if (enemy.getType() != COMMANDER && enemy.getType() != SCOUT) {
                    // Assume that the enemy trooper always is in the commander aura
                    actionPoints += situation.game.getCommanderAuraBonusActionPoints();
                }
                if (enemy.isHoldingFieldRation()) {
                    actionPoints += situation.game.getFieldRationBonusActionPoints() - situation.game.getFieldRationEatCost();
                }

/*
                // TODO: fix and uncomment expected damage from grenades
                // Assume that he'll always throw a grenade if he has one
                if (enemy.isHoldingGrenade() && actionPoints >= situation.game.getGrenadeThrowCost()) {
                    int[] best = p.allyHp;
                    int bestDamage = 0;
                    for (Pair<Integer, Point> ally : p.allies()) {
                        Point target = ally.second;
                        if (Point.create(enemy).euclideanDistance(target) <= situation.game.getGrenadeThrowRange()) {
                            int[] hp = p.grenadeEffectToAllies(target);
                            int damage = IntArrays.sum(IntArrays.diff(p.allyHp, hp));
                            if (damage > bestDamage) {
                                bestDamage = damage;
                                best = hp;
                            }
                        }
                    }
                    if (bestDamage > 0) {
                        actionPoints -= situation.game.getGrenadeThrowCost();
                        for (int i = 0; i < p.allyHp.length; i++) {
                            expectedDamage[i] += p.allyHp[i] - best[i];
                        }
                    }
                }
*/

                // Assume that he's always shooting right away until the end of his turn
                // TODO: handle the case when he lowers the stance in the beginning
                int maxDamageToAlly = (actionPoints / enemy.getShootCost()) * enemy.getDamage();

                int isReachable = 0;
                int alliesUnderSight = 0;
                for (int j = 0; j < n; j++) {
                    Trooper ally = situation.allies.get(j);
                    Point point = j == situation.myIndex ? p.me : Point.create(ally);
                    TrooperStance stance = j == situation.myIndex ? p.stance : ally.getStance();
                    if (situation.isReachable(enemy.getShootingRange(), enemy, point, stance)) {
                        isReachable |= 1 << j;
                        alliesUnderSight++;
                    }
                }
                if (alliesUnderSight == 0) continue;

                for (int j = 0; j < n; j++) {
                    if ((isReachable & (1 << j)) != 0) {
                        expectedDamage[j] += maxDamageToAlly * 1. / alliesUnderSight;
                    }
                }
            }

            double result = 0.;
            for (int i = 0; i < n; i++) {
                result += Math.min(expectedDamage[i], p.allyHp[i]);
            }

            return result;
        }
    }
}
