import model.Direction;
import model.Trooper;
import model.TrooperType;
import model.World;

import java.util.*;

public class Army {
    private final List<Point> dislocations = new ArrayList<>();
    private int curDisIndex;

    private final List<TrooperType> order = new ArrayList<>(TrooperType.values().length);
    private final Map<TrooperType, Inbox> inboxes = new HashMap<>();

    private final Set<TrooperType> requestedHelp = new HashSet<>();

    public Army(@NotNull Trooper firstTrooper, @NotNull World world) {
        Board board = new Board(world);
        Point start = Point.create(firstTrooper);
        Point center = Point.center();
        dislocations.add(findFreePointNearby(board, start));
        dislocations.add(findFreePointNearby(board, start.halfwayTo(center)));
        dislocations.add(findFreePointNearby(board, center));
        dislocations.add(findFreePointNearby(board, center.halfwayTo(start.opposite())));
        dislocations.add(findFreePointNearby(board, start.opposite()));
        dislocations.add(findFreePointNearby(board, start.horizontalOpposite()));
        dislocations.add(findFreePointNearby(board, center));
        dislocations.add(findFreePointNearby(board, start.verticalOpposite()));

        curDisIndex = 0;

        for (Trooper trooper : world.getTroopers()) {
            inboxes.put(trooper.getType(), new Inbox());
        }
    }

    @NotNull
    private Point findFreePointNearby(@NotNull Board board, @NotNull Point p) {
        Set<Point> visited = new HashSet<>();
        Queue<Point> queue = new LinkedList<>();
        queue.offer(p);
        while (!queue.isEmpty()) {
            Point cur = queue.poll();
            Board.Cell cell = board.get(cur);
            if (cell != null && cell != Board.Cell.OBSTACLE) return cur;
            for (Direction direction : Util.DIRECTIONS) {
                Point next = cur.go(direction);
                if (!visited.contains(next)) {
                    visited.add(next);
                    queue.offer(next);
                }
            }
        }
        throw new IllegalStateException("Impossible as it may seem, there are no free points nearby: " + p);
    }

    @NotNull
    public Point getOrUpdateDislocation(@NotNull Collection<Trooper> allies) {
        Point dislocation = dislocations.get(curDisIndex);
        int curDist = 0;
        for (Trooper ally : allies) {
            curDist += Point.create(ally).manhattanDistance(dislocation);
        }
        if (curDist < 4 * allies.size()) {
            curDisIndex = (curDisIndex + 1) % dislocations.size();
            dislocation = dislocations.get(curDisIndex);
        }
        return dislocation;
    }

    @NotNull
    public List<TrooperType> getOrder() {
        return order;
    }

    public void sendMessage(@NotNull Trooper ally, @NotNull Message message, int timeToLive) {
        inboxes.get(ally.getType()).add(message, timeToLive);
    }

    @NotNull
    public Inbox getMessages(@NotNull Trooper ally) {
        return inboxes.get(ally.getType());
    }

    public void requestHelp(@NotNull Point whereTo, @NotNull TrooperType selfType, @NotNull Iterable<Trooper> alliesWithoutMe, int timeToLive) {
        // A trooper may request help only once
        if (!requestedHelp.add(selfType)) return;

        for (Trooper ally : alliesWithoutMe) {
            sendMessage(ally, new Message(Message.Kind.NEED_HELP, whereTo), timeToLive);
        }
    }
}
