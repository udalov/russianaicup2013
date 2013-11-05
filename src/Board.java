import model.CellType;

public class Board {
    private final CellType[][] cells;
    private final int n;
    private final int m;

    public Board(@NotNull CellType[][] cells) {
        this.cells = cells;
        this.n = cells.length;
        this.m = cells[0].length;
    }

    public boolean free(int x, int y) {
        return 0 <= x && x < n && 0 <= y && y < m && cells[x][y] == CellType.FREE;
    }
}
