package net.berack.upo.valpre;

public class Event implements Comparable<Event> {
    public final double time;
    public final Type type;
    public final ServerNode node;

    private Event(Type type, ServerNode node, double time) {
        this.type = type;
        this.time = time;
        this.node = node;
    }

    public int compareTo(Event other) {
        if (this.time < other.time)
            return -1;
        if (this.time == other.time)
            return 0;
        return 1;
    }

    public static Event newArrival(ServerNode node, double time) {
        return new Event(Type.ARRIVAL, node, time);
    }

    public static Event newDeparture(ServerNode node, double time) {
        return new Event(Type.DEPARTURE, node, time);
    }

    public static enum Type {
        ARRIVAL,
        DEPARTURE,
    }
}
