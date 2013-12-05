import model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static model.TrooperType.*;

public class Situation {
    public final Game game;
    // Should be used only for reachability check and moveIndex
    public final World world;
    public final Army army;
    public final Warrior self;
    public final List<Warrior> allies;
    public final List<EnemyWarrior> enemies;
    public final List<Bonus> bonuses;

    public final boolean lightVersion;
    public final Scorer scorer;

    public Situation(@NotNull Game game, @NotNull World world, @NotNull Army army, @NotNull TrooperType selfType, @NotNull List<Trooper> allies,
                     @NotNull List<Trooper> enemies, @NotNull List<Bonus> bonuses) {
        this.game = game;
        this.world = world;
        this.army = army;
        this.allies = new ArrayList<>(allies.size());
        this.enemies = new ArrayList<>(enemies.size());
        this.bonuses = bonuses;

        for (int i = 0, n = allies.size(); i < n; i++) {
            this.allies.add(new Warrior(i, allies.get(i)));
        }

        this.self = findMyself(selfType, this.allies);

        for (int i = 0, n = enemies.size(); i < n; i++) {
            this.enemies.add(new EnemyWarrior(i, enemies.get(i)));
        }

        this.lightVersion = false;
        this.scorer = createScorer();
    }

    public Situation(@NotNull Situation situation, @NotNull TrooperType selfType, @NotNull List<Warrior> allies, @NotNull List<Bonus> bonuses) {
        this.game = situation.game;
        this.world = situation.world;
        this.army = situation.army;
        this.self = findMyself(selfType, allies);
        this.allies = allies;
        this.enemies = situation.enemies;
        this.bonuses = bonuses;
        this.lightVersion = true;
        this.scorer = createScorer();
    }

    @NotNull
    private static Warrior findMyself(@NotNull TrooperType selfType, @NotNull List<Warrior> allies) {
        for (Warrior ally : allies) {
            if (ally.type == selfType) return ally;
        }
        throw new IllegalStateException("Where am I? " + allies);
    }

    @NotNull
    private Scorer createScorer() {
        if (!enemies.isEmpty()) {
            return new Scorer.CombatSituation(this);
        }

        Warrior leader = findLeader();
        if (leader.type == self.type) {
            return new Scorer.Leader(this);
        } else {
            return new Scorer.Follower(this, leader);
        }
    }

    @NotNull
    private Warrior findLeader() {
        // TODO: this is a hack to make medic follow sniper in the beginning on MAP03
        List<TrooperType> leaderOrder = army.board.getKind() == Board.Kind.MAP03 && world.getMoveIndex() <= 3 ?
                Arrays.asList(SNIPER, FIELD_MEDIC, SOLDIER, COMMANDER, SCOUT) :
                Arrays.asList(SOLDIER, COMMANDER, FIELD_MEDIC, SCOUT, SNIPER);

        for (TrooperType type : leaderOrder) {
            for (Warrior ally : allies) {
                if (ally.type == type) return ally;
            }
        }

        throw new IllegalStateException("No one left alive, who am I then? " + self.type);
    }

    public int getMoveCost(@NotNull TrooperStance stance) {
        switch (stance) {
            case PRONE: return game.getProneMoveCost();
            case KNEELING: return game.getKneelingMoveCost();
            case STANDING: return game.getStandingMoveCost();
            default: throw new IllegalStateException("Unknown stance: " + stance);
        }
    }

    public boolean isReachable(double maxRange, @NotNull Point viewer, @NotNull TrooperStance viewerStance,
                               @NotNull Point object, @NotNull TrooperStance objectStance) {
        return world.isVisible(maxRange, viewer.x, viewer.y, viewerStance, object.x, object.y, objectStance);
    }

    @NotNull
    @Override
    public String toString() {
        return self + ", turn #" + world.getMoveIndex();
    }
}
