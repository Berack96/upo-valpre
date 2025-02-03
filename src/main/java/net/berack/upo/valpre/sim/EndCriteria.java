package net.berack.upo.valpre.sim;

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
    public boolean shouldEnd(Simulation run);

    /**
     * Parses the given string to create an array of end criteria.
     * The string passed must be in the following format:
     * [criteria1];[criteria2];...;[criteriaN]
     * 
     * and each criteria must be in the following format:
     * ClassName:param1,param2,...,paramN
     * 
     * If the string is empty or null, an empty array is returned.
     * If one of the criteria is not valid, an exception is thrown.
     * 
     * @param criterias The string to parse.
     * @return An array of end criteria.
     * @throws IllegalArgumentException If one of the criteria is not valid.
     */
    public static EndCriteria[] parse(String criterias) {
        if (criterias == null || criterias.isEmpty())
            return new EndCriteria[0];

        var criteria = criterias.split(";");
        var endCriteria = new EndCriteria[criteria.length];
        for (int i = 0; i < criteria.length; i++) {
            var current = criteria[i].substring(1, criteria[i].length() - 1); // Remove the brackets
            var parts = current.split(":");
            if (parts.length != 2)
                throw new IllegalArgumentException("Invalid criteria: " + current);

            var className = parts[0];
            var params = parts[1].split(",");
            endCriteria[i] = switch (className) {
                case "MaxArrivals" -> new MaxArrivals(params[0], Integer.parseInt(params[1]));
                case "MaxDepartures" -> new MaxDepartures(params[0], Integer.parseInt(params[1]));
                case "MaxTime" -> new MaxTime(Double.parseDouble(params[0]));
                default -> throw new IllegalArgumentException("Invalid criteria: " + current);
            };
        }
        return endCriteria;
    }

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
        public boolean shouldEnd(Simulation run) {
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
        public boolean shouldEnd(Simulation run) {
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
        public boolean shouldEnd(Simulation run) {
            return run.getTime() >= this.maxTime;
        }
    }
}