public class Message {
    public enum Kind {
        OUT_OF_THE_WAY,
        NEED_HELP
    }

    private final Kind kind;
    private final Point data;

    public Message(@NotNull Kind kind, @NotNull Point data) {
        this.kind = kind;
        this.data = data;
    }

    @NotNull
    public Kind getKind() {
        return kind;
    }

    @NotNull
    public Point getData() {
        return data;
    }

    @Override
    public String toString() {
        return kind + " " + data;
    }
}
