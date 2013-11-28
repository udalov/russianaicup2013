import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class PointMap<T> implements Map<Point, T> {
    private int size = 0;
    private final Object[] data = new Object[Board.WIDTH * Board.HEIGHT];

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return data[((Point) key).index()] != null;
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public T get(Object key) {
        return (T) data[((Point) key).index()];
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public T put(@NotNull Point point, @NotNull T value) {
        int i = point.index();
        T old = (T) data[i];
        if (old == null) size++;
        data[i] = value;
        return old;
    }

    @Override
    public void clear() {
        size = 0;
        Arrays.fill(data, null);
    }


    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(@NotNull Map<? extends Point, ? extends T> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Set<Point> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Collection<T> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Set<Entry<Point, T>> entrySet() {
        throw new UnsupportedOperationException();
    }
}
