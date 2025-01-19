package net.berack.upo.valpre;

/**
 * Represents an event in the simulation.
 */
public class Event implements Comparable<Event> {
    public final double time;
    public final Type type;
    public final ServerNode node;

    /**
     * Create a new event.
     * 
     * @param type The type of event.
     * @param node The node that the event is associated with.
     * @param time The time at which the event occurs.
     */
    private Event(Type type, ServerNode node, double time) {
        this.type = type;
        this.time = time;
        this.node = node;
    }

    @Override
    public int compareTo(Event other) {
        if (this.time < other.time)
            return -1;
        if (this.time == other.time)
            return 0;
        return 1;
    }

    /**
     * Create a new event.
     * 
     * @param node The node that the event is associated with.
     * @param time The time at which the event occurs.
     * @param type The type of event.
     * 
     * @return The new event.
     */
    public static Event newType(ServerNode node, double time, Type type) {
        return new Event(type, node, time);
    }

    /**
     * Create a new arrival event.
     * 
     * @param node The node that the event is associated with.
     * @param time The time at which the event occurs.
     * @return The new event.
     */
    public static Event newArrival(ServerNode node, double time) {
        return new Event(Type.ARRIVAL, node, time);
    }

    /**
     * Create a new departure event.
     * 
     * @param node The node that the event is associated with.
     * @param time The time at which the event occurs.
     * @return The new event.
     */
    public static Event newDeparture(ServerNode node, double time) {
        return new Event(Type.DEPARTURE, node, time);
    }

    /**
     * The type of event.
     */
    public static enum Type {
        ARRIVAL,
        DEPARTURE,
    }
}
