package xyz.acygn.mokapot.examples.readyreminder;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.PriorityQueue;

/**
 * A list of times at which things become ready. These can be added
 * asynchronously, and it's possible to ask for things that have become ready
 * already, in order.
 *
 * @author Alex Smith
 * @param <T> The class that represents an event.
 */
public class ReadyReminderServer<T> {

    private final PriorityQueue<Entry> queue = new PriorityQueue<>();

    public void submitEvent(T event, Instant readyAt) {
        queue.add(new Entry(event, readyAt));
        System.err.println("# submitEvent(" + event + ", " + readyAt + ")");
    }

    public T extractReadyEvent() {
        Entry first = queue.peek();
        if (first == null || first.readyAt.isAfter(Instant.now())) {
        System.err.println("# extractReadyEvent() == null");
            return null;
        }
        System.err.println("# extractReadyEvent() == " + first.event);
        return queue.remove().event;
    }

    private class Entry implements Comparable<Entry> {

        private Entry(T event, Instant readyAt) {
            this.event = event;
            this.readyAt = readyAt;
        }

        private final T event;
        private final Instant readyAt;

        @Override
        public int compareTo(Entry other) {
            return readyAt.compareTo(other.readyAt);
        }
    }
}
