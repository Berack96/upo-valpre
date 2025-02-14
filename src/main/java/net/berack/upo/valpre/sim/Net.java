package net.berack.upo.valpre.sim;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * A class that represents a network of queues, each with its own servers.
 * The network in question is created by adding a node and then establishing
 * connections between nodes. In order to start a simulation, at least one node
 * must be a Source or must generate at least one event to be processed.
 */
public final class Net implements Iterable<ServerNode> {
    private final List<ServerNode> servers = new ArrayList<>();
    private final HashMap<ServerNode, Integer> indices = new HashMap<>();
    private final List<List<Connection>> connections = new ArrayList<>();

    /**
     * Adds a new server node to the network.
     * The unique identifier for the nodes is the name and, if you try to add a node
     * that has the same name of another, then the method will return an exception
     * 
     * @param node The server node to add.
     * @throws IllegalArgumentException if the node already exist
     * @return the index of the created node
     */
    public int addNode(ServerNode node) {
        if (this.indices.containsKey(node))
            throw new IllegalArgumentException("Node already exist");

        var index = this.servers.size();
        this.servers.add(node);
        this.indices.put(node, index);
        this.connections.add(new ArrayList<>());
        return index;
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
    public void addConnection(ServerNode parent, ServerNode child, double weight) {
        var nodeP = this.indices.get(parent);
        var nodeC = this.indices.get(child);
        this.addConnection(nodeP, nodeC, weight);
    }

    /**
     * Adds a connection between the nodes with the given weight to select it.
     * The weight must be > 0 and the nodes must be already added to the net.
     * If the connection is already present then the new weight is used.
     * 
     * @param parent The parent node index.
     * @param child  The child node index to add.
     * @param weight The probability of the child node.
     * @throws IndexOutOfBoundsException if one of the two nodes are not in the net
     * @throws IllegalArgumentException  if the weight is negative or zero
     */
    public void addConnection(int parent, int child, double weight) {
        if (weight <= 0)
            throw new IllegalArgumentException("Weight must be > 0");

        var max = this.servers.size() - 1;
        if (parent < 0 || child < 0 || parent > max || child > max)
            throw new IndexOutOfBoundsException("One of the nodes does not exist");

        var list = this.connections.get(parent);
        list.removeIf(conn -> conn.index == child);
        list.add(new Connection(child, weight));
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
     * Return the index of the node based on the name passed as input.
     * Note that this will iterate over all the nodes.
     * 
     * @param name the name of the node
     * @return the node
     */
    public int getNodeIndex(String name) {
        for (var entry : this.indices.entrySet()) {
            if (entry.getKey().name.equals(name))
                return entry.getValue();
        }
        return -1;
    }

    /**
     * Return a node based on the hash of the string name passed as input
     * 
     * @param name the name of the node
     * @return the node
     */
    public ServerNode getNode(String name) {
        var index = this.getNodeIndex(name);
        return index < 0 ? null : this.servers.get(index);
    }

    /**
     * Return a node based on the index, faster than recovering it by the name
     * 
     * @param index the index of the node
     * @return the node
     * @throws IndexOutOfBoundsException if the index is not in the range
     */
    public ServerNode getNode(int index) {
        return this.servers.get(index);
    }

    /**
     * Get a list of all the children of the parent.
     * In the list there is the node and the weight associated with.
     * 
     * @param parent the parent node
     * @throws IndexOutOfBoundsException If the index is not in the range
     * @return the resultig node
     */
    public List<Connection> getChildren(int parent) {
        var children = new ArrayList<Connection>();
        for (var conn : this.connections.get(parent)) {
            var listEntry = new Connection(conn.index, conn.weight);
            children.add(listEntry);
        }

        return children;
    }

    /**
     * Normalizes the weights in each connections so that their sum equals 1.
     * This method should be called by the user if they have inserted weights that
     * are not summing to 1 or are unsure.
     */
    public void normalizeWeights() {
        for (var node = 0; node < this.connections.size(); node++) {
            var list = this.connections.get(node);

            var sum = 0.0d;
            for (var conn : list)
                sum += conn.weight;

            var newOne = new ArrayList<Connection>();
            for (var conn : list) {
                var newWeight = conn.weight / sum;
                newOne.add(new Connection(conn.index, newWeight));
            }

            this.connections.set(node, newOne);
        }
    }

    /**
     * Build the node states for the simulation.
     * This method is used to create the state of each node in the network.
     * Note that each call to this method will create a new state for each node.
     * 
     * @return the array of node states
     */
    public ServerNodeState[] buildNodeStates() {
        var states = new ServerNodeState[this.servers.size()];
        for (var i = 0; i < states.length; i++)
            states[i] = new ServerNodeState(i, this);
        return states;
    }

    /**
     * Save the current net to a file.
     * The resulting file is saved with Kryo.
     * 
     * @param file the name of the file
     * @throws FileNotFoundException if the path doesn't exist
     */
    public void save(String file) throws FileNotFoundException {
        var kryo = new Kryo();
        kryo.setRegistrationRequired(false);

        try (var out = new Output(new FileOutputStream(file))) {
            kryo.writeClassAndObject(out, this);
        }
    }

    @Override
    public Iterator<ServerNode> iterator() {
        return this.servers.iterator();
    }

    /**
     * Load the net from the file passed as input.
     * The net will be the same as the one saved.
     * 
     * @param file the file to load
     * @return a new Net object
     * @throws KryoException if the file saved is not a net
     * @throws IOException   if the file is not found
     */
    public static Net load(String file) throws KryoException, IOException {
        try (var stream = new FileInputStream(file)) {
            return Net.load(stream);
        }
    }

    /**
     * Load the net from the stream passed as input.
     * The net will be the same as the one saved.
     * 
     * @param stream the input stream to read
     * @return a new Net object
     * @throws KryoException if the file saved is not a net
     */
    public static Net load(InputStream stream) throws KryoException {
        var kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());

        try (var in = new Input(stream)) {
            return (Net) kryo.readClassAndObject(in);
        }
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        try {
            for (var node : this.servers) {
                builder.append(node)
                        .append(" -> ");

                for (var child : this.getChildren(this.indices.get(node))) {
                    var childNode = this.servers.get(child.index);
                    builder.append(childNode.name)
                            .append("(")
                            .append(child.weight)
                            .append("), ");
                }

                builder.delete(builder.length() - 2, builder.length())
                        .append("\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return builder.toString();
    }

    /**
     * A Static inner class used to represent the connection of a node
     */
    public static class Connection {
        public final int index;
        public final double weight;

        private Connection(int index, double weight) {
            this.index = index;
            this.weight = weight;
        }
    }
}
