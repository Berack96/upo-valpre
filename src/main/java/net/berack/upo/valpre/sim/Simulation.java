package net.berack.upo.valpre.sim;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import net.berack.upo.valpre.rand.Rng;
import net.berack.upo.valpre.sim.stats.Result;
import net.berack.upo.valpre.sim.stats.Statistics;

/**
 * Process an entire run of the simulation.
 */
public final class Simulation {
    public final Rng rng;
    public final long timeStartedNano;
    public final EndCriteria[] criterias;
    public final long seed;

    private final Net net;
    private final Map<String, NodeState> states;
    private final PriorityQueue<Event> fel;
    private double time = 0.0d;
    private long eventProcessed = 0;

    /**
     * Creates a new run of the simulation with the given nodes and random number
     * generator.
     * 
     * @param states    The nodes in the network.
     * @param rng       The random number generator to use.
     * @param criterias when the simulation has to end.
     */
    public Simulation(Net net, Rng rng, EndCriteria... criterias) {
        this.timeStartedNano = System.nanoTime();
        this.net = net;
        this.states = new HashMap<>();
        this.fel = new PriorityQueue<>();
        this.criterias = criterias;
        this.seed = rng.getSeed();
        this.rng = rng;

        boolean hasLimit = false;
        for (var node : net) {
            // check for ending criteria in simulation
            if (node.spawnArrivals != Integer.MAX_VALUE)
                hasLimit = true;

            // Initial arrivals (if spawned)
            this.states.put(node.name, new NodeState());
            if (node.shouldSpawnArrival(0))
                this.addArrival(node);
        }

        if (!hasLimit && (criterias == null || criterias.length == 0))
            throw new IllegalArgumentException("At least one end criteria is needed!");
    }

    /**
     * Runs the simulation until a given criteria is met.
     * 
     * @return The final statistics the network.
     */
    public Result run() {
        while (!this.hasEnded())
            this.processNextEvent();
        return this.endSimulation();
    }

    /**
     * Processes the next event in the future event list.
     * This method will throw NullPointerException if there are no more events.
     * You should check if the simulation has ended before calling this method.
     * 
     * @see #hasEnded()
     * @throws NullPointerException if there are no more events to process.
     */
    public void processNextEvent() {
        var event = fel.poll();
        if (event == null)
            throw new NullPointerException("No more events to process!");

        var node = event.node;
        var state = this.states.get(node.name);
        this.time = event.time;
        this.eventProcessed += 1;

        switch (event.type) {
            case AVAILABLE -> {
                state.stats.updateTimes(this.time, state.numServerBusy, state.numServerUnavailable, node.maxServers);
                state.numServerUnavailable--;
                this.addDepartureIfPossible(node, state);
            }
            case ARRIVAL -> {
                state.queue.add(this.time);
                state.stats.updateArrival(this.time, state.queue.size());
                state.stats.updateTimes(this.time, state.numServerBusy, state.numServerUnavailable, node.maxServers);
                this.addDepartureIfPossible(node, state);
            }
            case DEPARTURE -> {
                var arrivalTime = state.queue.poll();
                state.stats.updateDeparture(this.time, arrivalTime);
                state.stats.updateTimes(this.time, state.numServerBusy, state.numServerUnavailable, node.maxServers);
                state.numServerBusy--;

                this.addUnavailableIfPossible(node, state);
                this.addDepartureIfPossible(node, state);

                var next = this.net.getChildOf(node, this.rng);
                if (next != null)
                    this.addArrival(next);

                if (node.shouldSpawnArrival(state.stats.numArrivals))
                    this.addArrival(node);
            }
        }
    }

    /**
     * Ends the simulation and returns the statistics of the network.
     * 
     * @return The statistics of the network.
     */
    public Result endSimulation() {
        var elapsed = System.nanoTime() - this.timeStartedNano;
        var nodes = new HashMap<String, Statistics>();
        for (var entry : this.states.entrySet())
            nodes.put(entry.getKey(), entry.getValue().stats);

        return new Result(this.seed, this.time, elapsed, nodes);
    }

    /**
     * Get the current time.
     * 
     * @return a double representing the current time of the simulation.
     */
    public double getTime() {
        return this.time;
    }

    /**
     * Get the number of events processed.
     * 
     * @return the number of events processed.
     */
    public long getEventsProcessed() {
        return this.eventProcessed;
    }

    /**
     * Get the list of future events.
     * This method returns a copy of the list, so the original list is not modified.
     * 
     * @return a list of future events.
     */
    public List<Event> getFutureEventList() {
        return new ArrayList<>(this.fel);
    }

    /**
     * Get the node requested by the name passed as a string.
     * 
     * @param node the name of the node
     * @return the node
     */
    public ServerNode getNode(String node) {
        return this.net.getNode(node);
    }

    /**
     * Get the node state requested by the name passed as a string.
     * 
     * @param node the name of the node
     * @return the current state of the node
     */
    public NodeState getNodeState(String node) {
        return this.states.get(node);
    }

    /**
     * Adds an arrival event to the future event list. The event is created based
     * on the given node, and no delay is added.
     * 
     * @param node The node to create the event for.
     */
    public void addArrival(ServerNode node) {
        var event = Event.newArrival(node, this.time);
        fel.add(event);
    }

    /**
     * Adds a departure event to the future event list. The event is created based
     * on the given node, and the delay is determined by the node's service
     * distribution.
     * 
     * @param node  The node to create the event for.
     * @param state The current state of the node
     */
    public void addDepartureIfPossible(ServerNode node, NodeState state) {
        var canServe = node.maxServers > state.numServerBusy + state.numServerUnavailable;
        var hasRequests = state.queue.size() > state.numServerBusy;

        if (canServe && hasRequests) {
            state.numServerBusy++;
            var delay = node.getServiceTime(this.rng);
            var event = Event.newDeparture(node, this.time + delay);
            fel.add(event);
        }
    }

    /**
     * Add an AVAILABLE event in the case that the node has an unavailability time.
     * 
     * @param node  The node to create the event for
     * @param state The current state of the node
     */
    public void addUnavailableIfPossible(ServerNode node, NodeState state) {
        var delay = node.getUnavailableTime(rng);
        if (delay > 0) {
            state.numServerUnavailable++;
            var event = Event.newAvailable(node, time + delay);
            this.fel.add(event);
        }
    }

    /**
     * Determines if the simulation has finshed based on the given criteria.
     * 
     * @return True if the simulation should end, false otherwise.
     */
    public boolean hasEnded() {
        if (fel.isEmpty()) {
            return true;
        }
        for (var c : this.criterias) {
            if (c.shouldEnd(this)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Represents a summary of the state of a server node in the network.
     * It is used by the simulation to track the number of arrivals and departures,
     * the maximum queue length, the busy time, and the response time.
     */
    public static class NodeState {
        public int numServerBusy = 0;
        public int numServerUnavailable = 0;
        public final Statistics stats = new Statistics();
        public final ArrayDeque<Double> queue = new ArrayDeque<>();
    }
}