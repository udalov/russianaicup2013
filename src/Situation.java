import model.*;

import java.util.List;

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

    public Situation(@NotNull Game game, @NotNull World world, @NotNull Army army, @NotNull Trooper self, @NotNull List<Trooper> allies, int myIndex,
                     @NotNull List<Trooper> enemies, @NotNull List<Bonus> bonuses) {
        this.game = game;
        this.world = world;
        this.army = army;
        this.self = self;
        this.allies = allies;
        this.myIndex = myIndex;
        this.enemies = enemies;
        this.bonuses = bonuses;
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
}
