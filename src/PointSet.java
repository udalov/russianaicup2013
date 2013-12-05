import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class PointSet implements Set<Point> {
    private int size;
    // TODO: long[]
    private final BitSet data;

    private PointSet(@NotNull BitSet data) {
        this.data = data;
        this.size = data.cardinality();
    }

    public PointSet() {
        this(new BitSet(Board.WIDTH * Board.HEIGHT));
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        return o instanceof Point && data.get(((Point) o).index());
    }

    @Override
    public boolean add(@NotNull Point p) {
        int i = p.index();
        if (data.get(i)) return false;
        data.set(i);
        size++;
        return true;
    }

    @Override
    public void clear() {
        size = 0;
        data.clear();
    }


    @NotNull
    public PointSet copy() {
        return new PointSet((BitSet) data.clone());
    }


    @Override
    @NotNull
    public Iterator<Point> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public <T> T[] toArray(@NotNull T[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends Point> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }
}
