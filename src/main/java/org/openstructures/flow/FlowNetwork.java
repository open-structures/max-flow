package org.openstructures.flow;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import org.open_structures.memento.Memento;
import org.open_structures.memento.Restorable;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Objects.requireNonNull;

/**
 * A flow network is a directed graph where each edge has a capacity and can receive a flow.
 * The amount of flow on an edge cannot exceed its capacity.
 */
public class FlowNetwork implements Restorable<FlowNetwork.State> {

    private final Node source, sink;

    private final Table<Node, Node, Integer> capacitiesTable = HashBasedTable.create();

    public FlowNetwork(Node source, Node sink) {
        this.source = requireNonNull(source);
        this.sink = requireNonNull(sink);
    }

    public Set<Node> getPredecessors(Node head) {
        checkNotNull(head);
        return capacitiesTable.column(head).keySet();
    }

    public Set<Node> getSuccessors(Node tail) {
        checkNotNull(tail);
        return capacitiesTable.row(tail).keySet();
    }

    public int getArcCapacity(Node tail, Node head) {
        checkNotNull(tail);
        checkNotNull(head);
        return capacitiesTable.contains(tail, head) ? capacitiesTable.get(tail, head) : 0;
    }

    public void setArcCapacity(int capacity, Node tail, Node head) {
        checkArgument(capacity >= 0);
        checkNotNull(tail);
        checkNotNull(head);
        checkArgument(!tail.equals(head));
        if (capacity == 0) {
            if (capacitiesTable.contains(tail, head)) {
                capacitiesTable.remove(tail, head);
            }
        } else {
            capacitiesTable.put(tail, head, capacity);
        }
    }

    public int getNumberOfNodes() {
        Set<Node> allNodes = newHashSet();
        allNodes.addAll(capacitiesTable.rowKeySet());
        allNodes.addAll(capacitiesTable.columnKeySet());
        allNodes.add(source);
        allNodes.add(sink);
        return allNodes.size();
    }

    public void increaseArcCapacity(int capacityToAdd, Node tail, Node head) {
        checkNotNull(tail);
        checkNotNull(head);
        checkArgument(capacityToAdd > 0);

        int existingCapacity = getArcCapacity(tail, head);
        setArcCapacity(existingCapacity + capacityToAdd, tail, head);
    }

    public Node getSink() {
        return sink;
    }

    public Node getSource() {
        return source;
    }

    @Override
    public State getState() {
        return new State(this, ImmutableTable.copyOf(capacitiesTable));
    }

    @Override
    public void restore(State state) {
        checkNotNull(state);
        checkArgument(this.equals(state.originFlowNetwork));
        capacitiesTable.clear();
        capacitiesTable.putAll(state.capacitiesTable);
    }

    public static class State implements Memento {
        private final FlowNetwork originFlowNetwork;
        private final ImmutableTable<Node, Node, Integer> capacitiesTable;

        private State(FlowNetwork originFlowNetwork, ImmutableTable<Node, Node, Integer> capacitiesTable) {
            this.originFlowNetwork = requireNonNull(originFlowNetwork);
            this.capacitiesTable = requireNonNull(capacitiesTable);
        }
    }
}
