import model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static model.TrooperType.*;

public class Situation {
    public final Game game;
    // Should be used only for reachability check
    public final World world;
    public final Army army;
    public final Warrior self;
    public final List<Warrior> allies;
    // Index of me in the 'allies' list
    public final List<Warrior> enemies;
    public final List<Bonus> bonuses;

    public final Scorer scorer;

    public Situation(@NotNull Game game, @NotNull World world, @NotNull Army army, @NotNull TrooperType selfType, @NotNull List<Trooper> allies,
                     @NotNull List<Trooper> enemies, @NotNull List<Bonus> bonuses) {
        this.game = game;
        this.world = world;
        this.army = army;
        this.allies = new ArrayList<>(allies.size());
        this.enemies = new ArrayList<>(enemies.size());
        this.bonuses = bonuses;

        Warrior self = null;
        for (int i = 0, n = allies.size(); i < n; i++) {
            Warrior warrior = new Warrior(i, allies.get(i));
            this.allies.add(warrior);
            if (warrior.type == selfType) {
                self = warrior;
            }
        }
        assert self != null : "Where am I? " + allies;
        this.self = self;

        for (int i = 0, n = enemies.size(); i < n; i++) {
            this.enemies.add(new Warrior(i, enemies.get(i)));
        }

        if (!enemies.isEmpty()) {
            this.scorer = new Scorer.CombatSituation(this);
        } else {
            Warrior leader = findLeader();
            if (leader.type == selfType) {
                this.scorer = new Scorer.Leader(this);
            } else {
                this.scorer = new Scorer.Follower(this, leader);
            }
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

    public boolean isReachable(double maxRange, @NotNull Trooper viewer, @NotNull Point object, @NotNull TrooperStance objectStance) {
        return world.isVisible(maxRange, viewer.getX(), viewer.getY(), viewer.getStance(), object.x, object.y, objectStance);
    }
}
