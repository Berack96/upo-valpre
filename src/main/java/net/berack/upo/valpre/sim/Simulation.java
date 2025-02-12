package net.berack.upo.valpre.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import net.berack.upo.valpre.rand.Rng;
import net.berack.upo.valpre.sim.stats.Result;

/**
 * Process an entire run of the simulation.
 */
public final class Simulation {
    public final Rng rng;
    public final long timeStartedNano;
    public final EndCriteria[] criterias;
    public final long seed;

    private final ServerNodeState[] states;
    private final PriorityQueue<Event> fel;
    private double time = 0.0d;
    private long eventProcessed = 0;

    /**
     * Creates a new simulation for the given network.
     * The random number generator is used to generate random numbers for the
     * simulation.
     * The simulation will end when the given criteria are met.
     * NOTE: the network passed is only used to create the initial states of the
     * nodes, so the simulation is not affected by changes to the network after
     * the creation of this object.
     * 
     * @param net       The network to simulate.
     * @param rng       The random number generator to use.
     * @param criterias when the simulation has to end.
     */
    public Simulation(Net net, Rng rng, EndCriteria... criterias) {
        this.timeStartedNano = System.nanoTime();
        this.states = net.buildNodeStates();
        this.fel = new PriorityQueue<>();
        this.criterias = criterias;
        this.seed = rng.getSeed();
        this.rng = rng;

        boolean hasLimit = false;
        for (var state : this.states) {
            var node = state.node;

            // check for ending criteria in simulation
            if (node.spawnArrivals != Integer.MAX_VALUE)
                hasLimit = true;

            // Initial arrivals (if spawned)
            this.addToFel(state.spawnArrivalIfPossilbe(0.0d));
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

        var state = this.states[event.nodeIndex];
        this.time = event.time;
        this.eventProcessed += 1;

        switch (event.type) {
            case AVAILABLE -> {
                state.updateAvailable(time);
                this.addToFel(state.spawnDepartureIfPossible(time, this.rng));
            }
            case ARRIVAL -> {
                state.updateArrival(time);
                this.addToFel(state.spawnDepartureIfPossible(time, this.rng));
            }
            case DEPARTURE -> {
                state.updateDeparture(time);

                this.addToFel(state.spawnUnavailableIfPossible(time, this.rng));
                this.addToFel(state.spawnDepartureIfPossible(time, this.rng));
                this.addToFel(state.spawnArrivalIfPossilbe(time));

                // Spawn arrival to child node if queue is not full otherwise drop
                var ev = state.spawnArrivalToChild(time, this.rng);
                if (ev != null && !this.states[ev.nodeIndex].isQueueFull())
                    this.addToFel(ev);
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
        var builder = new Result.Builder();
        for (var i = 0; i < this.states.length; i++) {
            var state = this.states[i];
            builder.addNode(state.node.name, state.stats);
        }

        return builder.seed(this.seed).times(this.time, elapsed * 1e-6).build();
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
     * @throws NullPointerException if the node does not exist.
     */
    public ServerNode getNode(String node) {
        return this.getNodeState(node).node;
    }

    /**
     * Get the node state requested by the name passed as a string.
     * 
     * @param node the name of the node
     * @return the current state of the node
     * @throws NullPointerException if the node does not exist.
     */
    public ServerNodeState getNodeState(String node) {
        for (var state : this.states) {
            if (state.node.name.equals(node))
                return state;
        }

        throw new NullPointerException("Node not found: " + node);
    }

    /**
     * Add an arrival event to the future event list if the event is not null,
     * otherwise do nothing.
     * 
     * @param e the event to add
     */
    public void addToFel(Event e) {
        if (e != null)
            this.fel.add(e);
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
}