import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class PointSet implements Set<Point> {
    private int size = 0;
    private final BitSet data = new BitSet(Board.WIDTH * Board.HEIGHT);

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
    public boolean addAll(Collection<? extends Point> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
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
