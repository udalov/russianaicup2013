import model.Direction;
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
        if (situation.self.type == FIELD_MEDIC) {
            result -= coeff.medicDistanceToWoundedAllies * distanceToWoundedAllies(p);
        }

        result += coeff.underCommanderAura * underCommanderAura(p);

        result += coeff.pointsSeen * p.seen.size();

        result += situationSpecificScore(p);

        return result;
    }

    private int underCommanderAura(@NotNull Position p) {
        Point commander = null;
        for (Warrior ally : p.allies()) {
            if (ally.type == COMMANDER) commander = ally.point;
        }
        if (commander == null) return 0;

        int result = 0;
        for (Warrior ally : p.allies()) {
            if (ally.type != COMMANDER && ally.type != SCOUT) {
                if (ally.point.euclideanDistance(p.me) <= situation.game.getCommanderAuraRange()) result++;
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
        for (Warrior ally : situation.allies) {
            if (ally.equals(situation.self)) continue;
            Integer dist = situation.board.distance(ally.point, p.me);
            if (dist == null || dist == 0) continue;
            int toHeal = ally.getMaximalHitpoints() - p.allyHp[ally.index];
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
            wayPoint = situation.army.getOrUpdateWayPoint(situation);
        }

        @Override
        protected double situationSpecificScore(@NotNull Position p) {
            double result = 0;

            if (p.has(GRENADE)) result += coeff.hasGrenadeInMovement;
            if (p.has(MEDIKIT)) result += coeff.hasMedikitInMovement;
            if (p.has(FIELD_RATION)) result += coeff.hasFieldRationInMovement;

            result -= coeff.leaderDistanceToWayPoint * distanceToWayPoint(p);

            result -= coeff.leaderFarAwayTeammates * farAwayTeammates(p);

            return result;
        }

        private int farAwayTeammates(@NotNull Position p) {
            int result = 0;
            for (Warrior ally : situation.allies) {
                if (ally.equals(situation.self)) continue;
                Integer distance = situation.board.distance(ally.point, p.me);
                if (distance != null && distance > coeff.leaderCriticalDistanceToAllies) result++;
            }
            return result;
        }

        private int distanceToWayPoint(@NotNull Position p) {
            Integer dist = situation.board.distance(p.me, wayPoint);
            return dist != null ? dist : 1000;
        }
    }

    public static class Follower extends Scorer {
        private final Point leader;
        private final Point wayPoint;
        private final Set<Point> set = new PointSet();
        private final ArrayDeque<Point> queue = new ArrayDeque<>(15);
        private final List<Point> leaderPath;

        public Follower(@NotNull Situation situation, @NotNull Warrior leader) {
            super(situation);
            this.leader = leader.point;
            this.wayPoint = situation.army.getOrUpdateWayPoint(situation);
            List<Point> leaderPath = situation.board.findPath(this.leader, wayPoint);
            this.leaderPath = leaderPath == null ? Collections.<Point>emptyList() : leaderPath;
        }

        @Override
        protected double situationSpecificScore(@NotNull Position p) {
            double result = 0;

            if (p.has(GRENADE)) result += coeff.hasGrenadeInMovement;
            if (p.has(MEDIKIT)) result += coeff.hasMedikitInMovement;
            if (p.has(FIELD_RATION)) result += coeff.hasFieldRationInMovement;

            Integer dist = situation.board.distance(p.me, leader);
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

            for (Warrior ally : p.allies()) {
                set.add(ally.point);
            }
            queue.add(leader);

            int result = 1;
            while (!queue.isEmpty()) {
                Point point = queue.poll();
                for (Direction direction : Util.DIRECTIONS) {
                    Point q = point.go(direction);
                    if (q != null && situation.board.isPassable(q) && !set.contains(q)) {
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
            for (Warrior enemy : situation.enemies) {
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

            if (!situation.lightVersion && situation.army.isOrderComplete()) {
                result += coeff.combatNextAllyTurn * nextAllyTurn(p);
            }

            // TODO: only if high hp?
            result += coeff.combatVisibleEnemies * visibleEnemies(p);

            return result;
        }

        // Returns number of visible enemies or their corpses from those who were seen in the beginning of the turn (situation)
        // Why corpses: because no matter what coefficients are, we don't want our troopers to prefer to see more enemies than to kill :)
        private int visibleEnemies(@NotNull Position p) {
            int result = 0;
            outer: for (EnemyWarrior enemy : situation.enemies) {
                for (Warrior ally : p.allies()) {
                    // TODO: not very accurate, as kneeling or prone enemy sniper decreases (!) our vision range towards him
                    if (situation.isReachable(ally.getVisionRange(), ally.point, ally.stance, enemy.point, enemy.stance)) {
                        result++;
                        continue outer;
                    }
                }
            }
            return result;
        }

        private double nextAllyTurn(@NotNull Position p) {
            Warrior nextAlly = nextAllyToMakeTurn(p);
            if (nextAlly == null) return 0;

            List<Warrior> allies = new ArrayList<>(situation.allies);
            allies.set(situation.self.index, new Warrior(situation.self, p.me, p.stance));

            Situation next = new Situation(situation, nextAlly.type, allies, situation.bonuses /* TODO: some of them are collected */);

            Position start = new Position(next, nextAlly.point, nextAlly.stance, nextAlly.getInitialActionPoints() /* TODO: commander aura */,
                    MakeTurn.computeBonusesBitSet(nextAlly.trooper /* TODO: deprecate? here it's safe though */), p.enemyHp, p.allyHp, p.collected,
                    MakeTurn.computeSeenForSituation(next));

            Pair<Position, List<Go>> best = MakeTurn.best(next, start);

            return next.scorer.evaluate(best.first);
        }

        @Nullable
        private Warrior nextAllyToMakeTurn(@NotNull Position p) {
            List<TrooperType> order = situation.army.getOrder();
            int myIndex = order.indexOf(situation.self.type);
            for (int i = 1; i < order.size(); i++) {
                TrooperType type = order.get((myIndex + i) % order.size());
                for (Warrior ally : p.allies()) {
                    if (p.allyHp[ally.index] > 0 && ally.type == type) return ally;
                }
            }
            return null;
        }

        private int enemyTeamsThatSeeUs(@NotNull Position p) {
            int bitset = 0;
            for (EnemyWarrior enemy : p.aliveEnemies()) {
                for (Warrior ally : p.allies()) {
                    if (situation.isReachable(enemy.getVisionRange(), enemy.point, enemy.stance, ally.point, ally.stance)) {
                        bitset |= 1 << enemyTeams.get(enemy.getPlayerId());
                    }
                }
            }
            return Integer.bitCount(bitset);
        }

        private double closestEnemy(@NotNull Position p) {
            double closestEnemy = 1e100;
            for (EnemyWarrior enemy : p.aliveEnemies()) {
                closestEnemy = Math.min(closestEnemy, enemy.point.euclideanDistance(p.me));
            }
            return closestEnemy;
        }

        private double distanceToAllies(@NotNull Position p) {
            double result = 0;
            for (Warrior ally : situation.allies) {
                if (ally.equals(situation.self)) continue;
                Integer dist = situation.board.distance(ally.point, p.me);
                if (dist != null) result += dist;
            }
            return result;
        }

        private double expectedDamageOnNextTurn(@NotNull Position p) {
            // Assume that all enemies see us, but this is not always true
            // TODO: count number of other teams having at least one trooper who sees us

            int n = situation.allies.size();

            double[] expectedDamage = new double[n];

            for (EnemyWarrior enemy : p.aliveEnemies()) {
                int actionPoints = enemy.getInitialActionPoints();
                if (enemy.type != COMMANDER && enemy.type != SCOUT) {
                    // Assume that the enemy trooper always is in the commander aura
                    actionPoints += situation.game.getCommanderAuraBonusActionPoints();
                }
                if (enemy.isHoldingFieldRation()) {
                    actionPoints += situation.game.getFieldRationBonusActionPoints() - situation.game.getFieldRationEatCost();
                }

                if (!situation.lightVersion) {
                    // Assume that he'll always throw a grenade if he has one
                    if (enemy.isHoldingGrenade() && actionPoints >= situation.game.getGrenadeThrowCost()) {
                        double grenadeThrowRange = situation.game.getGrenadeThrowRange();
                        int[] best = p.allyHp;
                        int maxScore = 0;
                        for (Warrior ally : p.allies()) {
                            Point target = ally.point;
                            if (enemy.point.euclideanDistance(target) <= grenadeThrowRange) {
                                int[] hp = p.grenadeEffectToAllies(target);
                                int score = 0;
                                for (int i = 0; i < n; i++) {
                                    score += p.allyHp[i] - hp[i];
                                    if (hp[i] == 0) score += coeff.killEnemy;
                                }

                                if (score > maxScore) {
                                    maxScore = score;
                                    best = hp;
                                }
                            }
                        }
                        if (maxScore > 60) {
                            actionPoints -= situation.game.getGrenadeThrowCost();
                            for (int i = 0; i < p.allyHp.length; i++) {
                                expectedDamage[i] += p.allyHp[i] - best[i];
                            }
                        }
                    }
                }

                // Assume that he's always shooting right away until the end of his turn
                // TODO: handle the case when he lowers the stance in the beginning
                int maxDamageToAlly = (actionPoints / enemy.getShootCost()) * enemy.getDamage();

                int isReachable = 0;
                int alliesUnderSight = 0;
                for (Warrior ally : p.allies()) {
                    if (situation.isReachable(enemy.getShootingRange(), enemy.point, enemy.stance, ally.point, ally.stance)) {
                        isReachable |= 1 << ally.index;
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
