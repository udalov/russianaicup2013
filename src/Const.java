public enum Const {
    UNKNOWN,

    DEFAULT,

    CHEESER {{
        leaderCriticalDistanceToAllies = 6;
    }},

    FEFER,

    MAP01,

    MAP02,

    MAP03,

    MAP04,

    MAP05,

    MAP06;

    public double weightedHpOfAllies = 1;
    public double medicDistanceToWoundedAllies = 0.5;
    public double underCommanderAura = 5;
    public double pointsSeen = 0.02;
    public double stance = 1;

    public double maxHpToHeal = 85;

    public double enemyHp = 1;
    public double killEnemy = 30;
    public double enemyTeamsThatSeeUs = 10;
    public double expectedDamageOnNextTurn = 0.5;
    public double bonusInCombat = 0.1;
    public double distanceToAlliesInCombat = 0.01;
    public double combatNextAllyTurn = 0.05;
    public double combatVisibleEnemies = 20;
    public double shootablePoints = 0.005;

    public double hasGrenadeInMovement = 1;
    public double hasMedikitInMovement = 1;
    public double hasFieldRationInMovement = 11;

    public double followerDistanceToLeader = 0.4;
    public double isFollowerBlockingLeader = 10;

    public double leaderDistanceToWayPoint = 1;
    public double leaderFarAwayTeammates = 100;
    public double leaderCriticalDistanceToAllies = 5;
}
