public class Debug {
    public static final boolean ENABLED = Thread.currentThread().getName().equals("local");

    public static void log(@Nullable Object o) {
        if (ENABLED) {
            System.out.println(o);
        }
    }
}
