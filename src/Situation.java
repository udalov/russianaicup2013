import model.*;

import java.util.Arrays;
import java.util.List;

import static model.TrooperType.*;

public class Situation {
    public final Game game;
    // Should be used only for reachability check
    public final World world;
    public final Army army;
    public final Trooper self;
    public final List<Trooper> allies;
    // Index of me in the 'allies' list
    public final int myIndex;
    public final List<Trooper> enemies;
    public final List<Bonus> bonuses;

    public final Scorer scorer;

    public Situation(@NotNull Game game, @NotNull World world, @NotNull Army army, @NotNull Trooper self, @NotNull List<Trooper> allies,
                     @NotNull List<Trooper> enemies, @NotNull List<Bonus> bonuses) {
        this.game = game;
        this.world = world;
        this.army = army;
        this.self = self;
        this.allies = allies;
        this.enemies = enemies;
        this.bonuses = bonuses;

        int myIndex = -1;
        for (int i = 0, n = allies.size(); i < n; i++) {
            if (allies.get(i).getType() == self.getType()) {
                myIndex = i;
            }
        }
        assert myIndex >= 0 : "Where am I? " + allies;
        this.myIndex = myIndex;

        if (!enemies.isEmpty()) {
            this.scorer = new Scorer.CombatSituation(this);
        } else {
            Trooper leader = findLeader();
            if (leader.getType() == self.getType()) {
                this.scorer = new Scorer.Leader(this);
            } else {
                this.scorer = new Scorer.Follower(this, leader);
            }
        }
    }

    @NotNull
    private Trooper findLeader() {
        // TODO: this is a hack to make medic follow sniper in the beginning on MAP03
        List<TrooperType> leaderOrder = army.board.getKind() == Board.Kind.MAP03 && world.getMoveIndex() <= 3 ?
                Arrays.asList(SNIPER, FIELD_MEDIC, SOLDIER, COMMANDER, SCOUT) :
                Arrays.asList(SOLDIER, COMMANDER, FIELD_MEDIC, SCOUT, SNIPER);

        for (TrooperType type : leaderOrder) {
            for (Trooper ally : allies) {
                if (ally.getType() == type) return ally;
            }
        }

        throw new IllegalStateException("No one left alive, who am I then? " + self.getType());
    }

    public int getMoveCost(@NotNull TrooperStance stance) {
        switch (stance) {
            case PRONE: return game.getProneMoveCost();
            case KNEELING: return game.getKneelingMoveCost();
            case STANDING: return game.getStandingMoveCost();
            default: throw new IllegalStateException("Unknown stance: " + stance);
        }
    }

    public boolean isReachable(double maxRange, @NotNull Point viewer, @NotNull TrooperStance viewerStance, @NotNull Trooper object) {
        return world.isVisible(maxRange, viewer.x, viewer.y, viewerStance, object.getX(), object.getY(), object.getStance());
    }

    public boolean isReachable(double maxRange, @NotNull Trooper viewer, @NotNull Point object, @NotNull TrooperStance objectStance) {
        return world.isVisible(maxRange, viewer.getX(), viewer.getY(), viewer.getStance(), object.x, object.y, objectStance);
    }
}
