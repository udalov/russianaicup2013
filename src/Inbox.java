import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

public class Inbox implements Iterable<Message> {
    private static class MessageTTL {
        private final Message message;
        private int ttl;

        private MessageTTL(@NotNull Message message, int ttl) {
            this.message = message;
            this.ttl = ttl;
        }
    }

    private final LinkedList<MessageTTL> messages = new LinkedList<>();

    @Override
    @NotNull
    public Iterator<Message> iterator() {
        return new Iterator<Message>() {
            private final ListIterator<MessageTTL> it = messages.listIterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Message next() {
                MessageTTL message = it.next();
                if (--message.ttl == 0) {
                    it.remove();
                }
                return message.message;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Hey, dude");
            }
        };
    }

    public void add(@NotNull Message message, int timeToLive) {
        for (Iterator<MessageTTL> iterator = messages.iterator(); iterator.hasNext(); ) {
            MessageTTL messageTTL = iterator.next();
            if (messageTTL.message.getKind() == message.getKind()) {
                iterator.remove();
            }
        }
        messages.addLast(new MessageTTL(message, timeToLive));
    }

    @Override
    public String toString() {
        return messages.isEmpty() ? "empty" : messages.size() + " messages";
    }
}
