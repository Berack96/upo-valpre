package net.berack.upo.valpre.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import net.berack.upo.valpre.rand.Rng;

/**
 * TODO
 */
public final class Net {
    private final HashMap<String, Integer> indices = new HashMap<>();
    private final List<ServerNode> servers = new ArrayList<>();
    private final List<List<Connection>> connections = new ArrayList<>();
    private final List<Double> sum = new ArrayList<>();

    /**
     * Adds a new server node to the network.
     * The unique identifier for the nodes is the name and, if you try to add a node
     * that has the same name of another, then the method will return an exception
     * 
     * @param node The server node to add.
     * @throws IllegalArgumentException if the node already exist
     */
    public void addNode(ServerNode node) {
        if (this.indices.containsKey(node.name))
            throw new IllegalArgumentException("Node already exist");

        this.servers.add(node);
        this.indices.put(node.name, this.servers.size() - 1);
        this.connections.add(new ArrayList<>());
        this.sum.add(0.0);
    }

    /**
     * Adds a connection between the nodes with the given weight to select it.
     * The weight must be > 0 and the nodes must be already added to the net.
     * If the connection is already present then the new weight is used.
     * 
     * @param parent The parent node.
     * @param child  The child node to add.
     * @param weight The probability of the child node.
     * @throws NullPointerException     if one of the two nodes are not in the net
     * @throws IllegalArgumentException if the weight is negative or zero
     */
    public void addConnection(String parent, String child, double weight) {
        var nodeP = this.indices.get(parent);
        var nodeC = this.indices.get(child);

        if (weight <= 0)
            throw new IllegalArgumentException("Weight must be > 0");
        if (nodeP == nodeC && nodeP == null)
            throw new NullPointerException("One of the nodes does not exist");

        var list = this.connections.get(nodeP);
        for (var conn : list) {
            if (conn.index == nodeC) {
                conn.weight = weight;
                return;
            }
        }
        list.add(new Connection(nodeC, weight));
    }

    /**
     * Get the total number of the nodes in the net
     * 
     * @return the size of the net
     */
    public int size() {
        return this.servers.size();
    }

    /**
     * Get one of the child nodes from the parent specified. If the index is out of
     * bounds then an
     * exception is thrown. If the node has no child then null is returned;
     * 
     * @param parent the parent node
     * @param rng    the random number generator used for getting one of the child
     * @return the resultig node
     */
    public ServerNode getChildOf(ServerNode parent, Rng rng) {
        var index = this.indices.get(parent.name);
        var random = rng.random();
        for (var conn : this.connections.get(index)) {
            random -= conn.weight / 1.0;
            if (random <= 0) {
                return this.servers.get(conn.index);
            }
        }
        return null;
    }

    /**
     * Normalizes the weights in each connections so that their sum equals 1.
     * This method should be called by the user if they have inserted weights that
     * are not summing to 1 or are unsure.
     */
    public void normalizeWeights() {
        for (var list : this.connections) {
            var sum = 0.0d;
            for (var conn : list)
                sum += conn.weight;
            for (var conn : list)
                conn.weight /= sum;
        }
    }

    /**
     * Apply a consumer to all the nodes. The implementation uses a stream and for
     * this reason you should consider to make thread safe the consumer.
     * 
     * @param consumer a function that takes in input a ServerNode
     */
    public void forEachNode(Consumer<ServerNode> consumer) {
        this.servers.stream().forEach(consumer);
    }

    public static class Connection {
        public final int index;
        public double weight;

        private Connection(int index, double weight) {
            this.index = index;
            this.weight = weight;
        }
    }
}
