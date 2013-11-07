import model.Bonus;
import model.CellType;
import model.Trooper;
import model.World;

public class Board {
    public enum Cell {
        FREE,
        BONUS,
        TROOPER,
        OBSTACLE
    }

    private final Cell[][] cells;
    private final int n;
    private final int m;

    public Board(@NotNull World world) {
        CellType[][] cells = world.getCells();
        int n = cells.length;
        int m = cells[0].length;

        this.cells = new Cell[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                this.cells[i][j] = cells[i][j] == CellType.FREE ? Cell.FREE : Cell.OBSTACLE;
            }
        }
        for (Bonus bonus : world.getBonuses()) {
            this.cells[bonus.getX()][bonus.getY()] = Cell.BONUS;
        }
        for (Trooper trooper : world.getTroopers()) {
            this.cells[trooper.getX()][trooper.getY()] = Cell.TROOPER;
        }

        this.n = n;
        this.m = m;
    }

    @Nullable
    public Cell get(@NotNull Point point) {
        int x = point.x;
        int y = point.y;
        return 0 <= x && 0 <= y && x < n && y < m ? cells[x][y] : null;
    }

    public boolean isPassable(@NotNull Point point) {
        Cell cell = get(point);
        return cell == Cell.FREE || cell == Cell.BONUS;
    }
}
