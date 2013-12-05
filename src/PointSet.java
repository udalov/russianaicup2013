import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class PointSet implements Set<Point> {
    private int size;
    private final long[] data;

    private PointSet(int size, @NotNull long[] data) {
        this.data = data;
        this.size = size;
    }

    public PointSet() {
        // ceil(20 * 30 / 64) = 10
        this(0, new long[10]);
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
        int i = ((Point) o).index();
        return (data[i >> 6] & (1L << (i & 63))) != 0;
    }

    @Override
    public boolean add(@NotNull Point p) {
        int i = p.index();
        long k = 1L << (i & 63);
        if ((data[i >> 6] & k) != 0) return false;
        data[i >> 6] |= k;
        size++;
        return true;
    }

    @Override
    public void clear() {
        size = 0;
        Arrays.fill(data, 0);
    }


    @NotNull
    public PointSet copy() {
        return new PointSet(size, data.clone());
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
