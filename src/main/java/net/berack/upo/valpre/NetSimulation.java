package net.berack.upo.valpre;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import net.berack.upo.valpre.rand.Rng;

public class NetSimulation {
    public final long seed;
    private final Rng rng;
    private final Map<String, ServerNode> servers = new HashMap<>();

    public NetSimulation(long seed) {
        this.seed = seed;
        this.rng = new Rng(seed);
    }

    public void addNode(ServerNode node) {
        this.servers.put(node.name, node);
    }

    public Map<String, Statistics> run(long total, String untilDepartureNode) {
        // Initialization
        var timeNow = 0.0d;
        var stats = new HashMap<String, Statistics>();
        var fel = new PriorityQueue<Event>();
        for (var node : this.servers.values()) {
            var s = new Statistics(this.rng);
            s.addArrivalIf(node.isSource, node, timeNow, fel);
            stats.put(node.name, s);
        }

        // Main Simulation Loop
        var nodeStop = stats.get(untilDepartureNode);
        while (nodeStop.numDepartures < total) {
            var event = fel.poll();
            if (event == null) {
                break;
            }

            timeNow = event.time;
            var statsNode = stats.get(event.node.name);
            switch (event.type) {
                case ARRIVAL -> statsNode.processArrival(event, timeNow, fel);
                case DEPARTURE -> statsNode.processDeparture(event, timeNow, fel);
            }
        }
        return stats;
    }

    public static class Statistics {
        public int numArrivals = 0;
        public int numDepartures = 0;
        public int maxQueueLength = 0;
        public double busyTime = 0.0;
        public double responseTime = 0.0;
        public double lastEventTime = 0.0;

        private int numServerBusy = 0;
        private ArrayDeque<Double> queue = new ArrayDeque<>();
        private final Rng rng;

        public Statistics(Rng rng) {
            this.rng = rng;
        }

        public void reset() {
            this.numArrivals = 0;
            this.numDepartures = 0;
            this.numServerBusy = 0;
            this.busyTime = 0.0;
            this.responseTime = 0.0;
            this.queue.clear();
        }

        private void processArrival(Event event, double timeNow, PriorityQueue<Event> fel) {
            this.numArrivals++;
            this.queue.add(event.time);
            this.maxQueueLength = Math.max(this.maxQueueLength, this.queue.size());

            if (event.node.maxServers > this.numServerBusy) {
                this.numServerBusy++;
                var time = event.node.distribution.sample(this.rng);
                var departure = Event.newDeparture(event.node, timeNow + time);
                fel.add(departure);
            } else {
                this.busyTime += timeNow - this.lastEventTime;
            }
            this.lastEventTime = timeNow;

            this.addArrivalIf(event.node.isSource, event.node, timeNow, fel);
        }

        private void processDeparture(Event event, double timeNow, PriorityQueue<Event> fel) {
            var startService = this.queue.poll();
            var response = timeNow - startService;

            if (this.queue.size() < this.numServerBusy) {
                this.numServerBusy--;
            } else {
                var time = event.node.distribution.sample(this.rng);
                var departure = Event.newDeparture(event.node, timeNow + time);
                fel.add(departure);
            }

            this.numDepartures++;
            this.responseTime += response;
            this.busyTime += timeNow - this.lastEventTime;
            this.lastEventTime = timeNow;

            var next = event.node.getChild(rng);
            this.addArrivalIf(!event.node.isSink, next, timeNow, fel);
        }

        private void addArrivalIf(boolean condition, ServerNode node, double timeNow, PriorityQueue<Event> fel) {
            if (condition && node != null) {
                var delay = node.distribution.sample(this.rng);
                fel.add(Event.newArrival(node, timeNow + delay));
            }
        }
    }
}
