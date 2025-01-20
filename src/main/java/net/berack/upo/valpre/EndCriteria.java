package net.berack.upo.valpre;

/**
 * Criteria to determine when to end the simulation.
 */
public interface EndCriteria {
    /**
     * Determines if the simulation should end based on the statistics of the nodes.
     * 
     * @param run The current run of the network.
     * @return True if the simulation should end, false otherwise.
     */
    public boolean shouldEnd(NetSimulation.SimulationRun run);

    /**
     * Ends the simulation when the given node has reached the specified number of
     * arrivals.
     */
    public static class MaxArrivals implements EndCriteria {
        private final String nodeName;
        private final int maxArrivals;

        /**
         * Creates a new criteria to end the simulation when the given node has reached
         * the specified number of arrivals.
         * 
         * @param nodeName    The name of the node to check.
         * @param maxArrivals The maximum number of arrivals to wait for.
         */
        public MaxArrivals(String nodeName, int maxArrivals) {
            this.nodeName = nodeName;
            this.maxArrivals = maxArrivals;
        }

        @Override
        public boolean shouldEnd(NetSimulation.SimulationRun run) {
            return run.getNode(nodeName).stats.numArrivals >= this.maxArrivals;
        }
    }

    /**
     * Ends the simulation when the given node has reached the specified number of
     * departures.
     */
    public static class MaxDepartures implements EndCriteria {
        private final String nodeName;
        private final int maxDepartures;

        /**
         * Creates a new criteria to end the simulation when the given node has reached
         * the specified number of departures.
         * 
         * @param nodeName      The name of the node to check.
         * @param maxDepartures The maximum number of departures to wait for.
         */
        public MaxDepartures(String nodeName, int maxDepartures) {
            this.nodeName = nodeName;
            this.maxDepartures = maxDepartures;
        }

        @Override
        public boolean shouldEnd(NetSimulation.SimulationRun run) {
            return run.getNode(nodeName).stats.numDepartures >= this.maxDepartures;
        }
    }

    /**
     * Ends the simulation when the given node has reached the specified number of
     * departures.
     */
    public static class MaxTime implements EndCriteria {
        private final double maxTime;

        /**
         * Creates a new criteria to end the simulation when the given node has reached
         * the specified number of departures.
         * 
         * @param maxTime The maximum time to wait for.
         */
        public MaxTime(double maxTime) {
            this.maxTime = maxTime;
        }

        @Override
        public boolean shouldEnd(NetSimulation.SimulationRun run) {
            return run.getTime() >= this.maxTime;
        }
    }
}