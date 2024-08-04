package org.openstructures.flow;

import com.google.common.collect.ImmutableMap;
import org.open_structures.memento.Memento;
import org.open_structures.memento.Restorable;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Objects.requireNonNull;

/**
 * Definitions: for arc (i,j) we refer to node i as the tail of arc (i,j) and node j as its head.
 * Recommended reading is Chapter 7 from Network Flows Theory, Algorithms, and Applications by Ravindra K. Ahuja
 */
public class PushRelabelMaxFlow implements Restorable<PushRelabelMaxFlow.State> {
    private final Map<Node, Integer> nodeDistanceMap = newHashMap();
    private final Map<Node, Integer> nodeExcessMap = newHashMap();
    private final FlowNetwork flowNetwork;
    private final AdmissibleNodeSelectionStrategy admissibleNodeSelectionStrategy;

    public PushRelabelMaxFlow(FlowNetwork flowNetwork, AdmissibleNodeSelectionStrategy admissibleNodeSelectionStrategy) {
        this.flowNetwork = requireNonNull(flowNetwork);
        this.admissibleNodeSelectionStrategy = requireNonNull(admissibleNodeSelectionStrategy);
    }

    public PushRelabelMaxFlow(FlowNetwork flowNetwork) {
        this(flowNetwork, new RandomAdmissibleNodeSelectionStrategy());
    }

    /**
     * Calculates nodes' distances and floods all nodes adjacent to the source.
     * This creates the first set of active nodes what allows to kick off the basic operation in this algorithm {@link PushRelabelMaxFlow#pushRelabel()}
     */
    public void preprocess() {
        final Node s = getSource();
        calculateDistances();
        for (Node n : newHashSet(getSuccessors(s))) {
            pushFlow(flowNetwork.getArcCapacity(s, n), s, n); // push everything from source
        }
    }

    public void calculateDistances() {
        final Node s = getSource();
        final Node t = getSink();
        nodeDistanceMap.clear();
        nodeDistanceMap.put(s, flowNetwork.getNumberOfNodes());
        nodeDistanceMap.put(t, 0);
        calculateDistance(newLinkedList(getPredecessors(t)), 0);
    }

    private void calculateDistance(Queue<Node> nodesQueue, int currentDistance) {
        Queue<Node> nextLevelNodes = newLinkedList();
        while (!nodesQueue.isEmpty()) {
            Node node = nodesQueue.poll();
            if (!nodeDistanceMap.containsKey(node)) {
                nodeDistanceMap.put(node, currentDistance + 1);
                nextLevelNodes.addAll(getPredecessors(node));
            }
        }
        if (!nextLevelNodes.isEmpty()) {
            calculateDistance(nextLevelNodes, currentDistance + 1);
        }
    }

    private void pushRelabelNode(Node n) {
        checkNotNull(n);
        checkArgument(getNodeExcess(n) > 0, "No excess means there is nothing to push");

        Optional<Node> admissibleNodeOptional = admissibleNodeSelectionStrategy.getAdmissibleNode(this, n);
        if (admissibleNodeOptional.isPresent()) {
            Node admissibleNode = admissibleNodeOptional.get();
            int capacityToPush = Math.min(nodeExcessMap.get(n), getArcCapacity(n, admissibleNode));
            pushFlow(capacityToPush, n, admissibleNode);
        } else {
            OptionalInt minSuccessorDistance = getSuccessors(n).stream().mapToInt(this::getNodeDistance).min();
            if (minSuccessorDistance.isPresent()) {
                nodeDistanceMap.put(n, minSuccessorDistance.getAsInt() + 1);
            } else {
                throw new IllegalStateException("Active node " + n + " does not have successors.");
            }
        }
    }

    public Node getSink() {
        return flowNetwork.getSink();
    }

    public Node getSource() {
        return flowNetwork.getSource();
    }

    public int getNodeDistance(Node node) {
        return nodeDistanceMap.getOrDefault(node, -1);
    }

    /**
     * The basic operation in this algorithm is to select an active node and try to remove its excess by pushing flow to its neighbors.
     */
    private void pushRelabel() {
        while (hasActiveNodes()) {
            Node activeNode = getActiveNode();
            pushRelabelNode(activeNode);
        }
    }

    /**
     * The main way to affect performance of push relabel algorithm is by specifying the rule to select active nodes.
     * Here we select an active node with the highest value of the distance label.
     */
    private Node getActiveNode() {
        return getActiveNodes()
                .stream()
                .max(Comparator.comparingInt(this::getNodeDistance))
                .orElse(null);
    }

    public void preflowPush() {
        preprocess();
        pushRelabel();
    }

    /**
     * Active node is a node with strictly positive excess.
     * The source and sink nodes are never active.
     * Feasible flow has to satisfy flow bound constraint,
     * that is the amount of flow coming into an intermediate node equals the amount of flow coming out.
     * So the presence of active nodes indicates that the solution is infeasible.
     * The idea behind push relabel algorithm is that after flooding all nodes adjacent to the source (making them active)
     * it then strives to achieve feasibility. The basic operation in this algorithm is to select an active node
     * and try to remove its excess by pushing flow to its neighbors.
     */
    private boolean hasActiveNodes() {
        final Node s = getSource();
        final Node t = getSink();
        for (Node nodeWithExcess : nodeExcessMap.keySet()) {
            if (!nodeWithExcess.equals(s) && !nodeWithExcess.equals(t)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Pushes specified amount of flow from one node to another.
     * The amount can't exceed the excess of the tail and capacity of the arc.
     */
    public void pushFlow(int amount, Node tail, Node head) {
        checkArgument(amount > 0, "Amount of flow must be greater than 0");
        checkArgument(amount <= getNodeExcess(tail) || isSource(tail), "Can't push more than excess");
        checkArgument(amount <= getArcCapacity(tail, head), "Can't push more than residual capacity");

        setArcCapacity(getArcCapacity(tail, head) - amount, tail, head);
        // setting the "reverse" arc capacity
        setArcCapacity(getArcCapacity(head, tail) + amount, head, tail);
        addToExcess(amount, head);
        if (!isSource(tail)) {
            reduceExcess(amount, tail);
        }
    }

    private boolean isSource(Node node) {
        return flowNetwork.getSource().equals(node);
    }

    private void reduceExcess(int amount, Node node) {
        checkArgument(amount > 0);
        checkArgument(getNodeExcess(node) > 0);

        if (amount == getNodeExcess(node)) {
            nodeExcessMap.remove(node);
        } else {
            int newExcess = nodeExcessMap.get(node) - amount;
            checkState(newExcess > 0, "Excess can not be negative");
            nodeExcessMap.put(node, newExcess);
        }
    }

    private void setArcCapacity(int capacity, Node tail, Node head) {
        flowNetwork.setArcCapacity(capacity, tail, head);
    }

    private void addToExcess(int amount, Node node) {
        if (nodeExcessMap.containsKey(node)) {
            nodeExcessMap.compute(node, (n, oldExcess) -> oldExcess != null ? oldExcess + amount : amount);
        } else {
            nodeExcessMap.put(node, amount);
        }
    }

    /**
     * Excess is a difference between flow coming into the node and the flow going out.
     * Excess can't be negative, that is amount of flow going out of the node has to be less or equal the flow coming in.
     * Source is the only node that can have negative excess.
     */
    public int getNodeExcess(Node node) {
        checkNotNull(node);
        return nodeExcessMap.getOrDefault(node, 0);
    }

    public int getArcCapacity(Node tail, Node head) {
        checkNotNull(tail);
        checkNotNull(head);
        return flowNetwork.getArcCapacity(tail, head);
    }

    public int getFlowAmount() {
        return getNodeExcess(getSink());
    }

    public Set<Node> getSuccessors(Node tail) {
        checkNotNull(tail);
        return flowNetwork.getSuccessors(tail).stream().filter(nodeDistanceMap::containsKey).collect(Collectors.toSet());
    }

    private Set<Node> getPredecessors(Node head) {
        checkNotNull(head);
        return flowNetwork.getPredecessors(head);
    }

    public boolean isArcAdmissible(Node tail, Node head) {
        return getNodeDistance(tail) == getNodeDistance(head) + 1;
    }

    public Set<Node> getActiveNodes() {
        Node s = getSource();
        Node t = getSink();
        return nodeExcessMap.keySet().stream().filter(n -> !n.equals(s) && !n.equals(t)).collect(Collectors.toSet());
    }

    @Override
    public State getState() {
        return new State(flowNetwork.getState(), ImmutableMap.copyOf(nodeDistanceMap), ImmutableMap.copyOf(nodeExcessMap));
    }

    @Override
    public void restore(State state) {
        checkNotNull(state);
        flowNetwork.restore(state.flowNetworkState);
        nodeDistanceMap.clear();
        nodeDistanceMap.putAll(state.nodeDistanceMap);
        nodeExcessMap.clear();
        nodeExcessMap.putAll(state.nodeExcessMap);
    }

    private static class RandomAdmissibleNodeSelectionStrategy implements AdmissibleNodeSelectionStrategy {

        @Override
        public Optional<Node> getAdmissibleNode(PushRelabelMaxFlow pushRelabelMaxFlow, Node n) {
            return pushRelabelMaxFlow.getSuccessors(n).stream()
                    .filter(successor -> pushRelabelMaxFlow.isArcAdmissible(n, successor))
                    .findFirst();
        }
    }

    public static class State implements Memento {
        private final FlowNetwork.State flowNetworkState;
        private final ImmutableMap<Node, Integer> nodeDistanceMap;
        private final ImmutableMap<Node, Integer> nodeExcessMap;

        private State(FlowNetwork.State flowNetworkState, ImmutableMap<Node, Integer> nodeDistanceMap, ImmutableMap<Node, Integer> nodeExcessMap) {
            this.flowNetworkState = requireNonNull(flowNetworkState);
            this.nodeDistanceMap = requireNonNull(nodeDistanceMap);
            this.nodeExcessMap = requireNonNull(nodeExcessMap);
        }
    }
}
