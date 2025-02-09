package net.berack.upo.valpre.sim;

import java.util.ArrayDeque;
import java.util.List;

import net.berack.upo.valpre.rand.Rng;
import net.berack.upo.valpre.sim.Net.Connection;
import net.berack.upo.valpre.sim.stats.NodeStats;

/**
 * Represents a summary of the state of a server node in the network.
 * It is used by the simulation to track the number of arrivals and departures,
 * the maximum queue length, the busy time, and the response time.
 * It also has a connection to the node and the net where it is.
 */
public class ServerNodeState {
    public int numServerBusy = 0;
    public int numServerUnavailable = 0;
    public final ArrayDeque<Double> queue = new ArrayDeque<>();

    public final int index;
    public final ServerNode node;
    public final NodeStats stats = new NodeStats();
    public final List<Connection> children;

    /**
     * Create a new node state based on the index and the net passed as input
     * 
     * @param index the index of the node
     * @param net   the net where the node is
     */
    ServerNodeState(int index, Net net) {
        this.index = index;
        this.node = net.getNode(index);
        this.children = net.getChildren(index);
    }

    /**
     * Check if the queue is full based on the maximum queue length of the node
     * 
     * @return true if the queue is full
     */
    public boolean isQueueFull() {
        return this.queue.size() >= this.node.maxQueue;
    }

    /**
     * Check if the node can serve a new request based on the number of servers
     * 
     * @return true if the node can serve
     */
    public boolean canServe() {
        return this.node.maxServers > this.numServerBusy + this.numServerUnavailable;
    }

    /**
     * Check if the node has requests to serve based on the number of busy servers
     * 
     * @return true if the node has requests
     */
    public boolean hasRequests() {
        return this.queue.size() > this.numServerBusy;
    }

    /**
     * Determines if the node should spawn an arrival based on the number of
     * arrivals.
     * 
     * @return True if the node should spawn an arrival, false otherwise.
     */
    public boolean shouldSpawnArrival() {
        return this.node.spawnArrivals > this.stats.numArrivals;
    }

    /**
     * Update stats and queue when an unavailability event finish. The
     * unavailability
     * time is the time of the event.
     * 
     * @param time the time of the event
     */
    public void updateAvailable(double time) {
        this.stats.updateTimes(time, this.numServerBusy, this.numServerUnavailable, this.node.maxServers);
        this.numServerUnavailable--;
    }

    /**
     * Update stats and queue when an arrival event occurs. The arrival time is the
     * time of the event.
     * 
     * @param time the time of the event
     */
    public void updateArrival(double time) {
        this.queue.add(time);
        this.stats.updateArrival(time, this.queue.size());
        this.stats.updateTimes(time, this.numServerBusy, this.numServerUnavailable, node.maxServers);
    }

    /**
     * Update stats and queue when a departure event occurs. The departure time is
     * the time of the event.
     * 
     * @param time the time of the event
     */
    public void updateDeparture(double time) {
        var arrivalTime = this.queue.poll();
        this.stats.updateDeparture(time, arrivalTime);
        this.stats.updateTimes(time, this.numServerBusy, this.numServerUnavailable, node.maxServers);
        this.numServerBusy--;
    }

    /**
     * Create an arrival event based on the node and the time passed as input
     * 
     * @param time the time of the event
     * @return the arrival event
     */
    public Event spawnArrivalIfPossilbe(double time) {
        if (this.shouldSpawnArrival())
            return Event.newArrival(this.index, time);
        return null;
    }

    /**
     * Create a departure event if the node can serve and has requests. The event is
     * created based on the node and the delay is determined by the node's service
     * time distribution.
     * 
     * @param time the time of the event
     * @param rng  the random number generator
     * @return the departure event if the node can serve and has requests, null
     *         otherwise
     */
    public Event spawnDepartureIfPossible(double time, Rng rng) {
        if (this.canServe() && this.hasRequests()) {
            this.numServerBusy++;
            var delay = node.getServiceTime(rng);
            return Event.newDeparture(this.index, time + delay);
        }
        return null;
    }

    /**
     * Create an unavailable event if the node is unavailable. The event is created
     * based on the given node, and the delay is determined by the node's
     * unavailability distribution.
     * 
     * @param time The time of the event
     * @param rng  The random number generator
     * @return The event if the node is unavailable, null otherwise
     */
    public Event spawnUnavailableIfPossible(double time, Rng rng) {
        var delay = node.getUnavailableTime(rng);
        if (delay > 0) {
            this.numServerUnavailable++;
            return Event.newAvailable(this.index, time + delay);
        }
        return null;
    }

    /**
     * Create an arrival event to a child node based on the node and the time passed
     * as input
     * 
     * @param time the time of the event
     * @param rng  the random number generator
     * @return the arrival event to a child node if the node has children, null
     *         otherwise
     */
    public Event spawnArrivalToChild(double time, Rng rng) {
        if (!this.children.isEmpty()) {
            var random = rng.random();
            for (var child : this.children) {
                random -= child.weight;
                if (random <= 0)
                    return Event.newArrival(child.index, time);
            }
        }
        return null;
    }
}